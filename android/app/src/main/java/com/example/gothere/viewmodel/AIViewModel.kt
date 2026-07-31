package com.example.gothere.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gothere.ai.AIClient
import com.example.gothere.ai.AIContentBlock
import com.example.gothere.ai.AIMessage
import com.example.gothere.ai.AIRole
import com.example.gothere.ai.AIToolRegistry
import com.example.gothere.billing.PurchaseManager
import com.example.gothere.model.WizardConfig
import com.example.gothere.repository.WizardRepository
import com.posthog.PostHog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the AI "Where should I start?" conversation — Android counterpart of
 * iOS `AIService`. Owns the transcript, the proxy → tools → proxy loop, the
 * free-message paywall gate, and PostHog analytics.
 */
class AIViewModel(app: Application) : AndroidViewModel(app) {

    private val client = AIClient()
    private val purchaseManager = PurchaseManager.getInstance(app)
    private val prefs = app.getSharedPreferences("ai_assistant", Context.MODE_PRIVATE)

    /** Loaded once off the main thread; the wizard-tracks tool tolerates null. */
    @Volatile
    private var wizardConfig: WizardConfig? = null

    private val _messages = MutableStateFlow<List<AIMessage>>(emptyList())
    val messages: StateFlow<List<AIMessage>> = _messages.asStateFlow()

    private val _isWaiting = MutableStateFlow(false)
    val isWaiting: StateFlow<Boolean> = _isWaiting.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _sentCount = MutableStateFlow(prefs.getInt(COUNTER_KEY, 0))
    val sentCount: StateFlow<Int> = _sentCount.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            wizardConfig = runCatching { WizardRepository.loadConfig(getApplication()) }.getOrNull()
        }
    }

    // MARK: - Paywall gate

    fun hasAllAccess(): Boolean = purchaseManager.hasAllAccess()

    /** True when the user has used all free messages AND lacks all-access. */
    val isPaywallGated: Boolean
        get() = _sentCount.value >= MAX_FREE_MESSAGES && !purchaseManager.hasAllAccess()

    fun remainingFreeMessages(): Int = (MAX_FREE_MESSAGES - _sentCount.value).coerceAtLeast(0)

    /** One analytics event per screen entry. Mirrors iOS `aiConversationOpened`. */
    fun logConversationOpened() {
        capture("ai_conversation_opened", mapOf("sent_count" to _sentCount.value))
    }

    /** Reset after a successful all-access purchase so the user sees a fresh quota. */
    fun resetFreeMessageCounter() {
        _sentCount.value = 0
        prefs.edit().putInt(COUNTER_KEY, 0).apply()
    }

    // MARK: - Sending

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isWaiting.value) return
        if (isPaywallGated) {
            capture("ai_free_limit_reached")
            _lastError.value = "You've used all free messages. Upgrade to keep going."
            return
        }
        _lastError.value = null
        _messages.value = _messages.value + AIMessage(role = AIRole.User, content = listOf(AIContentBlock.Text(trimmed)))
        bumpCounter()
        capture("ai_message_sent", mapOf("message_index" to _sentCount.value, "char_count" to trimmed.length))

        viewModelScope.launch { runTurnLoop() }
    }

    private suspend fun runTurnLoop() {
        _isWaiting.value = true
        try {
            val tools = AIToolRegistry.toolsJson()
            var safetyTurns = 6
            while (safetyTurns-- > 0) {
                val assistant = try {
                    client.send(_messages.value, tools)
                } catch (e: AIClient.AIException) {
                    _lastError.value = "Couldn't reach the assistant (HTTP ${e.statusCode})."
                    capture("ai_error_returned", mapOf("error" to "http_${e.statusCode}"))
                    return
                } catch (e: Exception) {
                    _lastError.value = "Connection problem: ${e.message ?: "unknown"}"
                    capture("ai_error_returned", mapOf("error" to (e.message ?: "unknown")))
                    return
                }

                _messages.value = _messages.value + assistant
                if (!assistant.hasPendingToolUses) return

                val toolResults = assistant.content.mapNotNull { block ->
                    (block as? AIContentBlock.ToolUse)?.let { executeTool(it) }
                }
                if (toolResults.isEmpty()) return
                _messages.value = _messages.value + AIMessage(role = AIRole.User, content = toolResults)
            }
            // Exceeded the safety limit — surface a soft message.
            _messages.value = _messages.value + AIMessage(
                role = AIRole.Assistant,
                content = listOf(AIContentBlock.Text("I'm tripping over my own tools. Try rephrasing your question and I'll start over."))
            )
        } finally {
            _isWaiting.value = false
        }
    }

    private fun executeTool(block: AIContentBlock.ToolUse): AIContentBlock.ToolResult {
        capture("ai_tool_called", mapOf("tool" to block.name))
        val result = AIToolRegistry.execute(block.name, block.input, wizardConfig)
        return AIContentBlock.ToolResult(
            toolUseId = block.id,
            content = result.payload,
            isError = result.isError
        )
    }

    private fun bumpCounter() {
        val next = _sentCount.value + 1
        _sentCount.value = next
        prefs.edit().putInt(COUNTER_KEY, next).apply()
    }

    private fun capture(event: String, props: Map<String, Any>? = null) {
        runCatching { PostHog.capture(event, properties = props) }
    }

    companion object {
        const val MAX_FREE_MESSAGES = 5
        private const val COUNTER_KEY = "ai_sent_message_count"
    }
}
