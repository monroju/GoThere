package com.example.gothere.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gothere.ai.AIMessage
import com.example.gothere.ai.AIContentBlock
import com.example.gothere.ai.AIRole
import com.example.gothere.viewmodel.AIViewModel

/**
 * Wave 2 — AI conversational entry point. Android counterpart of iOS
 * `AIWhereToStartView`. Surfaced from the top-bar overflow menu. Talks to
 * [AIViewModel], which proxies the conversation through api.getgothere.app/ai
 * with on-device tool execution against VisaCatalog, cost data, and the wizard.
 *
 * @param onRequestUpgrade invoked when a paywall-gated user taps "Upgrade".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    onRequestUpgrade: () -> Unit,
    vm: AIViewModel = viewModel()
) {
    val messages by vm.messages.collectAsState()
    val isWaiting by vm.isWaiting.collectAsState()
    val lastError by vm.lastError.collectAsState()
    val sentCount by vm.sentCount.collectAsState()

    var input by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Analytics: one "conversation opened" event per screen entry (mirrors iOS).
    LaunchedEffect(Unit) { vm.logConversationOpened() }

    val visibleMessages = remember(messages) { messages.filter { it.isVisibleToUser } }

    // Auto-scroll to the newest content.
    LaunchedEffect(visibleMessages.size, isWaiting) {
        val target = visibleMessages.size + if (isWaiting) 1 else 0
        if (target > 0) listState.animateScrollToItem((target - 1).coerceAtLeast(0))
    }

    val hasAllAccess = vm.hasAllAccess()
    val gated = sentCount >= AIViewModel.MAX_FREE_MESSAGES && !hasAllAccess
    val canSend = input.trim().isNotEmpty() && !isWaiting && !gated

    Column(Modifier.fillMaxSize()) {
        DisclaimerBanner()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (visibleMessages.isEmpty()) {
                item { EmptyStateCard(onExample = { input = it }) }
            }
            items(visibleMessages, key = { it.id }) { msg -> MessageBubble(msg) }
            if (isWaiting) item { TypingIndicator() }
        }

        lastError?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        InputBar(
            input = input,
            onInputChange = { input = it },
            canSend = canSend,
            gated = gated,
            showQuota = !hasAllAccess,
            remaining = (AIViewModel.MAX_FREE_MESSAGES - sentCount).coerceAtLeast(0),
            onSend = {
                val toSend = input
                input = ""
                vm.send(toSend)
            },
            onUpgrade = onRequestUpgrade
        )
    }
}

@Composable
private fun DisclaimerBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            "Informational only. Verify with the official source — and a licensed lawyer for legal questions.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyStateCard(onExample: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Tell me about your situation.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Where do you want to go, what's your household look like, do you have any ancestry that might open doors? I'll suggest visa paths and ballpark costs from GoThere's data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "EXAMPLES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        listOf(
            "I'm a remote worker, couple, US$5k/mo budget, drawn to Portugal or Spain",
            "My grandmother was born in Ireland. Am I eligible for an Irish passport?",
            "Retiring in Mexico — which city is most affordable on $3k/mo?"
        ).forEach { ExamplePrompt(it, onExample) }
    }
}

@Composable
private fun ExamplePrompt(text: String, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(text) }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Outlined.NorthWest,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(14.dp)
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MessageBubble(msg: AIMessage) {
    val isUser = msg.role == AIRole.User
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 6.dp, top = 10.dp).size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .padding(12.dp)
        ) {
            if (isUser) {
                Text(
                    text = msg.displayText,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                // Claude is told the chat renders Markdown — render bold/italic/
                // bullets so replies don't show literal **/_/### characters.
                Text(
                    text = renderMarkdown(msg.displayText),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp).size(20.dp)
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(3) { i ->
                val alpha by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = i * 150),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$i"
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .alpha(alpha)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * Minimal, dependency-free Markdown → AnnotatedString for chat replies. Handles
 * the subset Claude actually emits per the system prompt: `**bold**`,
 * `*italic*`/`_italic_`, `#`-headings (rendered bold), and `-`/`*`/`•` bullets.
 * Not a full CommonMark parser — just enough so responses read cleanly.
 */
@Composable
private fun renderMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.split("\n")
    lines.forEachIndexed { idx, raw ->
        val heading = Regex("^#{1,6}\\s+").find(raw)
        val bullet = Regex("^\\s*[-*•]\\s+").find(raw)
        when {
            heading != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                appendInlineMarkdown(raw.removeRange(heading.range))
            }
            bullet != null -> {
                append("•  ")
                appendInlineMarkdown(raw.removeRange(bullet.range))
            }
            else -> appendInlineMarkdown(raw)
        }
        if (idx < lines.size - 1) append("\n")
    }
}

/** Inline pass for **bold** and *italic* / _italic_ spans. */
private fun AnnotatedString.Builder.appendInlineMarkdown(text: String) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text[i] == '*' || text[i] == '_' -> {
                val marker = text[i]
                val end = text.indexOf(marker, i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    input: String,
    onInputChange: (String) -> Unit,
    canSend: Boolean,
    gated: Boolean,
    showQuota: Boolean,
    remaining: Int,
    onSend: () -> Unit,
    onUpgrade: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp)
    ) {
        if (showQuota) {
            when {
                gated -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpgrade() }
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "You've used your 5 free messages. Upgrade to keep going.",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                remaining <= 2 -> Text(
                    text = "$remaining free message${if (remaining == 1) "" else "s"} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything…") },
                maxLines = 5,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() })
            )
            FilledIconButton(
                onClick = onSend,
                enabled = canSend
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

/** A user message is shown only if it has visible text (hides tool_result
 *  loopbacks); an assistant message only if it produces display text. */
private val AIMessage.isVisibleToUser: Boolean
    get() = when (role) {
        AIRole.User -> content.any { it is AIContentBlock.Text }
        AIRole.Assistant -> displayText.isNotEmpty()
    }
