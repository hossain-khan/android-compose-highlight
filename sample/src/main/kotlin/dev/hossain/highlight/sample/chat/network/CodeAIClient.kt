package dev.hossain.highlight.sample.chat.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory for creating a configured Ktor HTTP client for API communication.
 */
object CodeAIClient {
    /**
     * Creates and returns a configured HttpClient instance with JSON serialization support.
     *
     * The client is configured with:
     * - Android engine for optimal performance
     * - kotlinx.serialization for JSON handling
     * - Permissive JSON parsing (ignores unknown keys)
     *
     * @return Configured HttpClient instance
     */
    fun create(): HttpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = false
                        ignoreUnknownKeys = true
                        isLenient = true
                    },
                )
            }
        }
}
