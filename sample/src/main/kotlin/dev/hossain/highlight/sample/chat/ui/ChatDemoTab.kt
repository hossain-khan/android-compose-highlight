package dev.hossain.highlight.sample.chat.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.hossain.highlight.sample.chat.state.ChatViewModel

/**
 * Entry point composable for the LLM chat streaming demo tab.
 *
 * Creates (or retrieves via [viewModel]) a [ChatViewModel] scoped to the host activity
 * and delegates rendering to [ChatScreen].
 *
 * This composable integrates with the `DemoTab.Chat` tab in the sample app navigation.
 *
 * @param modifier Optional [Modifier] forwarded to [ChatScreen].
 */
@Composable
internal fun ChatDemoTab(modifier: Modifier = Modifier) {
    val chatViewModel: ChatViewModel = viewModel()
    ChatScreen(
        viewModel = chatViewModel,
        modifier = modifier,
    )
}
