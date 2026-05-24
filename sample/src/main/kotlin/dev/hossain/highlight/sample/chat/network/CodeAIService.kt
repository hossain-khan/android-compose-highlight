package dev.hossain.highlight.sample.chat.network

import dev.hossain.highlight.sample.chat.model.ChatMessage
import dev.hossain.highlight.sample.chat.model.CodeChatRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Service layer for communicating with the AI code chat API.
 *
 * Handles streaming responses from the server via Server-Sent Events (SSE),
 * with proper error handling and exponential backoff retry strategy.
 *
 * @param client Configured Ktor HttpClient instance
 */
class CodeAIService(
    private val client: HttpClient,
) {
    private val baseUrl = "https://hossain.dev/api/ai-code-chat"
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    /**
     * Sends a chat message to the API and streams the response tokens.
     *
     * The response is streamed as Server-Sent Events (SSE), where each event contains
     * a single token. This function accumulates tokens and invokes the [onToken] callback
     * for each one, allowing real-time display of the streaming response.
     *
     * @param prompt The user's programming question (max 2000 characters)
     * @param language Optional programming language (e.g., "kotlin", "python")
     * @param sessionId Optional session UUID for conversation tracking
     * @param conversationHistory Optional list of previous messages for context
     * @param onToken Callback invoked for each token received from the stream
     * @param onError Callback invoked if an error occurs
     * @param onComplete Callback invoked when the stream is complete
     */
    suspend fun chat(
        prompt: String,
        language: String? = null,
        sessionId: String? = null,
        conversationHistory: List<ChatMessage>? = null,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
    ) {
        return try {
            // Build the request
            val request =
                CodeChatRequest(
                    prompt = prompt,
                    language = language,
                    sessionId = sessionId,
                    conversationHistory = conversationHistory,
                )

            // Make the HTTP request
            val response: HttpResponse =
                client.post(baseUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            // Handle response status
            if (!response.status.isSuccess()) {
                val statusCode = response.status.value
                val errorMessage =
                    when (statusCode) {
                        400 -> "Invalid request. Please check your prompt length (max 2000 chars)"
                        429 -> "Daily API limit reached (150 requests/day). Please try again tomorrow"
                        500 -> "Server error. Please try again later"
                        else -> "HTTP Error $statusCode"
                    }
                onError(errorMessage)
                return
            }

            // Parse streaming response
            response.bodyAsText().lines().forEach { line ->
                if (line.isNotBlank()) {
                    // Check for stream completion
                    if (SseStreamParser.isStreamComplete(line)) {
                        onComplete()
                        return@forEach
                    }

                    // Try to extract and emit token
                    val token = SseStreamParser.parseToken(line)
                    if (token != null) {
                        onToken(token)
                    }
                }
            }

            // If we got here without hitting [DONE], stream ended normally
            onComplete()
        } catch (e: Exception) {
            val errorMessage =
                when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> {
                        "Request timed out. Please try again"
                    }

                    e.message?.contains("connection", ignoreCase = true) == true -> {
                        "Connection failed. Please check your internet"
                    }

                    else -> {
                        e.message ?: "Unknown error occurred"
                    }
                }
            onError(errorMessage)
        }
    }

    /**
     * Sends a chat message with exponential backoff retry strategy.
     *
     * Automatically retries on transient failures with exponential backoff between attempts.
     * Use this for production code to handle temporary network issues gracefully.
     *
     * @param prompt The user's programming question
     * @param language Optional programming language
     * @param sessionId Optional session UUID
     * @param conversationHistory Optional conversation history
     * @param onToken Callback for each token
     * @param onError Callback on error (called after all retries exhausted)
     * @param onComplete Callback on success
     * @param maxRetries Maximum number of retry attempts (default 3)
     * @param initialBackoffMs Initial backoff delay in milliseconds (default 1000)
     */
    suspend fun chatWithRetry(
        prompt: String,
        language: String? = null,
        sessionId: String? = null,
        conversationHistory: List<ChatMessage>? = null,
        onToken: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit,
        maxRetries: Int = 3,
        initialBackoffMs: Long = 1000,
    ) {
        var lastError: String? = null

        repeat(maxRetries) { attempt ->
            try {
                var isComplete = false
                chat(
                    prompt = prompt,
                    language = language,
                    sessionId = sessionId,
                    conversationHistory = conversationHistory,
                    onToken = onToken,
                    onError = { error ->
                        lastError = error
                        // Don't retry on client errors (4xx)
                        if (error.contains("400") || error.contains("429")) {
                            throw Exception(error)
                        }
                    },
                    onComplete = {
                        isComplete = true
                    },
                )

                if (isComplete) {
                    return // Success
                }
            } catch (e: Exception) {
                lastError = e.message
                if (attempt < maxRetries - 1) {
                    val backoffMs = initialBackoffMs * (attempt + 1)
                    delay(backoffMs)
                }
            }
        }

        // All retries exhausted
        onError(lastError ?: "Max retries exceeded")
    }
}
