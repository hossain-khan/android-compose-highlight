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

private data class StreamingSnippet(
    val title: String,
    val language: String,
    val code: String,
)

private val STREAMING_SNIPPETS =
    listOf(
        StreamingSnippet(
            title = "Kotlin Flow",
            language = "kotlin",
            code =
                """
// Simulated LLM Response: Kotlin StateFlow & Channel pipeline
class NewsRepository(
    private val api: NewsApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val _newsStream = MutableSharedFlow<List<Article>>(replay = 1)
    val newsStream: SharedFlow<List<Article>> = _newsStream.asSharedFlow()

    fun fetchBreakingNews(): Flow<Result<List<Article>>> = flow {
        emit(Result.Loading)
        try {
            val response = api.getHeadlines(country = "us")
            _newsStream.emit(response.articles)
            emit(Result.Success(response.articles))
        } catch (e: IOException) {
            emit(Result.Error(e))
        }
    }.flowOn(dispatcher)
}
                """.trimIndent(),
        ),
        StreamingSnippet(
            title = "Python Agent",
            language = "python",
            code =
                """
# Simulated LLM Response: Python Async Agent
import asyncio
from typing import AsyncGenerator, List
from pydantic import BaseModel

class AgentMessage(BaseModel):
    role: str
    content: str

class StreamingAgent:
    def __init__(self, model_name: str = "gpt-4o"):
        self.model_name = model_name
        self.history: List[AgentMessage] = []

    async def stream_tokens(self, prompt: str) -> AsyncGenerator[str, None]:
        self.history.append(AgentMessage(role="user", content=prompt))
        for token in prompt.split():
            await asyncio.sleep(0.04)
            yield f"{token} "
                """.trimIndent(),
        ),
        StreamingSnippet(
            title = "TypeScript Hook",
            language = "typescript",
            code =
                """
// Simulated LLM Response: React useStreamingCode hook
import { useState, useEffect, useRef } from "react";

interface StreamState {
  text: string;
  isDone: boolean;
  tokensCount: number;
}

export function useStreamingText(source: string, delayMs: number = 30): StreamState {
  const [text, setText] = useState("");
  const [isDone, setIsDone] = useState(false);
  const indexRef = useRef(0);

  useEffect(() => {
    setText("");
    setIsDone(false);
    indexRef.current = 0;

    const timer = setInterval(() => {
      if (indexRef.current < source.length) {
        setText((prev) => prev + source.charAt(indexRef.current));
        indexRef.current += 1;
      } else {
        setIsDone(true);
        clearInterval(timer);
      }
    }, delayMs);

    return () => clearInterval(timer);
  }, [source, delayMs]);

  return { text, isDone, tokensCount: text.split(/\s+/).length };
}
                """.trimIndent(),
        ),
    )

@Composable
internal fun StreamingSection() {
    var selectedSnippetIndex by remember { mutableIntStateOf(0) }
    val currentSnippet = STREAMING_SNIPPETS[selectedSnippetIndex]

    var streamedCode by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
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
        startStreaming(currentSnippet.code)
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Zero-Flicker Streaming Highlighting",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        "StreamingSyntaxHighlightedCode renders incoming tokens immediately with 0 ms UI latency " +
                            "while preserving existing syntax colors via span-transfer (applySnapshotSpans). " +
                            "Engine highlight jobs are debounced (200 ms) so rapid token streams do not overload the JS engine.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        // Snippet picker chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            STREAMING_SNIPPETS.forEachIndexed { index, snippet ->
                FilterChip(
                    selected = selectedSnippetIndex == index,
                    onClick = {
                        if (selectedSnippetIndex != index) {
                            selectedSnippetIndex = index
                            startStreaming(snippet.code)
                        }
                    },
                    label = { Text(snippet.title) },
                )
            }
        }

        // Controls & Progress bar
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { startStreaming(currentSnippet.code) },
                enabled = !isStreaming,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.cell_tower_24dp),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isStreaming) "Streaming..." else "Re-stream")
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
                    streamedCode = currentSnippet.code
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
                if (currentSnippet.code.isNotEmpty()) {
                    (streamedCode.length.toFloat() / currentSnippet.code.length).coerceIn(0f, 1f)
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
                text = "Characters: ${streamedCode.length} / ${currentSnippet.code.length}",
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
            language = currentSnippet.language,
            showLineNumbers = true,
            debounceMs = StreamingSyntaxHighlightedCodeDefaults.DEBOUNCE_MS,
            onHighlightComplete = { result: HighlightResult ->
                highlightCount++
                lastHighlightDurationMs = result.durationMs
            },
        )
    }
}
