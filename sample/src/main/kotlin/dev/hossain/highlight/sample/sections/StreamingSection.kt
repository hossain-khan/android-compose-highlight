@file:OptIn(ExperimentalHighlightApi::class)

package dev.hossain.highlight.sample.sections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.hossain.highlight.engine.HighlightResult
import dev.hossain.highlight.sample.R
import dev.hossain.highlight.ui.ExperimentalHighlightApi
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCode
import dev.hossain.highlight.ui.StreamingSyntaxHighlightedCodeDefaults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TYPESCRIPT_STREAMING_SNIPPET =
    """
/**
 * Simulated LLM streaming response: TypeScript Chat Agent & Telemetry pipeline.
 * Demonstrates imports, types, generics, template literals, regex, and async iterators.
 */
import { EventEmitter } from "events";
import type { ModelConfig, StreamChunk } from "@ai/core";

// Configuration constants
const DEFAULT_TIMEOUT_MS = 5_000;
const MAX_RETRY_ATTEMPTS = 3;
const API_BASE_URL = "https://api.gateway.internal/v1/chat";
const TOKEN_PATTERN = /[\w]+|[^\s\w]/g;

export type ConnectionStatus = "idle" | "connecting" | "streaming" | "completed";

export interface StreamEvent<T = string> {
  readonly id: string;
  readonly payload: T;
  readonly timestamp: number;
  readonly isFinal: boolean;
}

/**
 * Handles real-time token streaming with exponential backoff retry.
 */
export class StreamingAgent extends EventEmitter {
  private status: ConnectionStatus = "idle";
  private abortController: AbortController | null = null;

  constructor(private readonly config: ModelConfig) {
    super();
  }

  async *streamResponse(prompt: string): AsyncGenerator<StreamEvent, void, unknown> {
    this.status = "streaming";
    this.abortController = new AbortController();
    let tokenIndex = 0;

    try {
      const endpoint = `${'$'}{API_BASE_URL}?model=${'$'}{this.config.modelName}&temp=${'$'}{this.config.temperature}`;
      console.log(`[StreamAgent] Initiating connection to: ${'$'}{endpoint}`);

      const tokens = prompt.match(TOKEN_PATTERN) ?? [];
      for (const token of tokens) {
        tokenIndex += 1;
        yield {
          id: `chunk_${'$'}{tokenIndex}`,
          payload: token,
          timestamp: Date.now(),
          isFinal: tokenIndex === tokens.length,
        };
      }
    } catch (error: unknown) {
      console.error("[StreamAgent] Stream failed:", error);
      throw error;
    } finally {
      this.status = "completed";
      this.emit("statusChange", this.status);
    }
  }
}
    """.trimIndent()

@Composable
internal fun StreamingSection() {
    val currentSnippetCode = TYPESCRIPT_STREAMING_SNIPPET

    var streamedCode by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var triggerOnNewline by remember { mutableStateOf(true) }
    var highlightCount by remember { mutableIntStateOf(0) }
    var lastHighlightDurationMs by remember { mutableStateOf<Long?>(null) }

    val scope = rememberCoroutineScope()
    var streamingJob by remember { mutableStateOf<Job?>(null) }

    fun startStreaming(fullText: String) {
        streamingJob?.cancel()
        streamedCode = ""
        isStreaming = true
        isPaused = false
        highlightCount = 0
        lastHighlightDurationMs = null

        streamingJob =
            scope.launch {
                // Tokenize by small chunks (1-4 characters / partial words) to simulate LLM token streaming
                var i = 0
                while (i < fullText.length) {
                    while (isPaused) {
                        delay(50)
                    }
                    val chunkSize = (1..4).random().coerceAtMost(fullText.length - i)
                    streamedCode += fullText.substring(i, i + chunkSize)
                    i += chunkSize
                    delay((15..35).random().toLong()) // 30-50 tokens/sec
                }
                isStreaming = false
                isPaused = false
            }
    }

    DisposableEffect(Unit) {
        onDispose {
            streamingJob?.cancel()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader("LLM / Real-time Code Streaming")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
        ) {
            Text(
                text =
                    "Renders streaming tokens with 0 ms UI latency via span-transfer. " +
                        "Progressive backfill highlights completed lines on newlines (\\n, 150 ms throttle) " +
                        "and debounces stream pauses (200 ms).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
            )
        }

        // Options row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = triggerOnNewline,
                onClick = { triggerOnNewline = !triggerOnNewline },
                label = { Text("Progressive Backfill (\\n)") },
                leadingIcon = {
                    if (triggerOnNewline) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.check_24dp),
                            contentDescription = null,
                        )
                    }
                },
            )
        }

        // Controls & Progress bar
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { startStreaming(currentSnippetCode) },
                enabled = !isStreaming,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.cell_tower_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isStreaming) "Streaming..." else "Stream")
            }

            OutlinedButton(
                onClick = { isPaused = !isPaused },
                enabled = isStreaming,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.timer_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isPaused) "Resume" else "Pause")
            }

            OutlinedButton(
                onClick = {
                    streamingJob?.cancel()
                    isStreaming = false
                    isPaused = false
                    streamedCode = currentSnippetCode
                },
                enabled = isStreaming,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.check_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Complete")
            }
        }

        if (isStreaming) {
            val progress =
                if (currentSnippetCode.isNotEmpty()) {
                    (streamedCode.length.toFloat() / currentSnippetCode.length).coerceIn(0f, 1f)
                } else {
                    0f
                }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Live telemetry summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Characters: ${streamedCode.length} / ${currentSnippetCode.length}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    "Highlight cycles: $highlightCount" +
                        (lastHighlightDurationMs?.let { " (${it}ms)" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // The streaming code block
        StreamingSyntaxHighlightedCode(
            code = streamedCode.ifEmpty { " " },
            language = "typescript",
            showLineNumbers = true,
            debounceMs = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
            triggerOnNewline = triggerOnNewline,
            onHighlightComplete = { result: HighlightResult ->
                highlightCount++
                lastHighlightDurationMs = result.durationMs
            },
        )
    }
}
