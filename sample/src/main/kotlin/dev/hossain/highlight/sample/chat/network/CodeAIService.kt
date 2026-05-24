package dev.hossain.highlight.sample.chat.network

import dev.hossain.highlight.sample.chat.model.CodeChatRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service that sends requests to the Hossain Dev Bytes AI API endpoint and returns
 * streaming tokens via a [Flow].
 *
 * Uses [HttpURLConnection] with Server-Sent Events (SSE) for streaming responses.
 * The API endpoint is `POST https://hossain.dev/api/ai-code-chat`.
 *
 * Usage example:
 * ```kotlin
 * val service = CodeAIService()
 * service.streamChat(request).collect { token ->
 *     // append token to the current response
 * }
 * ```
 */
internal class CodeAIService {
    companion object {
        private const val API_URL = "https://hossain.dev/api/ai-code-chat"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 60_000
    }

    /**
     * Sends a chat request to the AI API and returns a cold [Flow] of streaming tokens.
     *
     * The flow emits each token string as it arrives from the SSE stream and
     * completes when the stream ends or a `[DONE]` marker is received.
     *
     * Throws [ChatException.RateLimitExceeded] if the API returns HTTP 429.
     * Throws [ChatException.ApiError] for other non-200 status codes.
     * Throws [ChatException.NetworkError] for IO/network failures.
     *
     * @param request The chat request to send.
     * @return A [Flow] emitting streaming tokens from the AI response.
     */
    fun streamChat(request: CodeChatRequest): Flow<String> =
        flow {
            val url = URL(API_URL)
            val connection =
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    doInput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "text/event-stream")
                    setRequestProperty("Cache-Control", "no-cache")
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }

            try {
                connection.outputStream.use { out ->
                    out.write(request.toJson().toByteArray(Charsets.UTF_8))
                    out.flush()
                }

                val statusCode = connection.responseCode
                when {
                    statusCode == 429 -> {
                        throw ChatException.RateLimitExceeded
                    }

                    statusCode != 200 -> {
                        val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                        throw ChatException.ApiError(statusCode, errorBody)
                    }
                }

                connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val currentLine = reader.readLine() ?: break
                        if (SseStreamParser.isDataLine(currentLine)) {
                            val data = SseStreamParser.extractDataContent(currentLine)
                            if (SseStreamParser.isDoneMarker(data)) break
                            val token = SseStreamParser.parseToken(data)
                            if (token != null) emit(token)
                        }
                    }
                }
            } catch (e: ChatException) {
                throw e
            } catch (e: Exception) {
                throw ChatException.NetworkError(e)
            } finally {
                connection.disconnect()
            }
        }.flowOn(Dispatchers.IO)
}
