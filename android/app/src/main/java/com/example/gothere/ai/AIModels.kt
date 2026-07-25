package com.example.gothere.ai

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Wire format for the GoThere AI conversation — Kotlin port of the iOS
 * `AIMessage.swift` contract. Mirrors a subset of the Anthropic Messages API
 * content blocks. The app handles tool execution locally and loops back through
 * the proxy at api.getgothere.app/ai/messages, which adds the API key + system
 * prompt server-side. Keep in sync with iOS — both clients hit the same proxy.
 *
 * JSON is hand-rolled with org.json (the same dependency-free approach
 * `WizardRepository` uses) so we don't pull in Moshi/Gson/kotlinx.serialization.
 */

enum class AIRole(val wire: String) {
    User("user"),
    Assistant("assistant");

    companion object {
        fun from(wire: String?): AIRole = if (wire == "assistant") Assistant else User
    }
}

/**
 * A single content block inside a Messages-API turn. Exactly three types:
 * plain text, tool_use (model → app), and tool_result (app → model).
 *
 * `ToolUse.input` is kept as a raw [JSONObject] so an assistant turn carrying a
 * tool_use block round-trips byte-faithfully when we re-send the transcript on
 * the next loop iteration.
 */
sealed class AIContentBlock {
    data class Text(val text: String) : AIContentBlock()
    data class ToolUse(val id: String, val name: String, val input: JSONObject) : AIContentBlock()
    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : AIContentBlock()

    fun toJson(): JSONObject = when (this) {
        is Text -> JSONObject().put("type", "text").put("text", text)
        is ToolUse -> JSONObject()
            .put("type", "tool_use")
            .put("id", id)
            .put("name", name)
            .put("input", input)
        is ToolResult -> JSONObject()
            .put("type", "tool_result")
            .put("tool_use_id", toolUseId)
            .put("content", content)
            .apply { if (isError) put("is_error", true) }
    }

    companion object {
        fun fromJson(o: JSONObject): AIContentBlock? = when (o.optString("type")) {
            "text" -> Text(o.optString("text"))
            "tool_use" -> ToolUse(
                id = o.optString("id"),
                name = o.optString("name"),
                input = o.optJSONObject("input") ?: JSONObject()
            )
            "tool_result" -> ToolResult(
                toolUseId = o.optString("tool_use_id"),
                content = o.optString("content"),
                isError = o.optBoolean("is_error", false)
            )
            else -> null
        }
    }
}

/** Per-turn message — used both on the wire and in the on-device transcript. */
data class AIMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: AIRole,
    val content: List<AIContentBlock>
) {
    /** Flat plain-text projection for the chat bubble. Tool-use renders as a
     *  small "calling X" line; tool-result blocks are hidden. Mirrors iOS. */
    val displayText: String
        get() = content.mapNotNull { block ->
            when (block) {
                is AIContentBlock.Text -> block.text
                is AIContentBlock.ToolUse -> "_Calling ${block.name}…_"
                is AIContentBlock.ToolResult -> null
            }
        }.joinToString("\n")

    /** True when the message contains at least one tool_use block — the app must
     *  execute the tool(s) and feed results back on the next turn. */
    val hasPendingToolUses: Boolean
        get() = content.any { it is AIContentBlock.ToolUse }

    /** Encodes to the proxy message shape. The proxy reads `role` + `content`
     *  and ignores `id`, but we send it for parity with iOS. */
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("role", role.wire)
        .put("content", JSONArray().apply { content.forEach { put(it.toJson()) } })

    companion object {
        fun fromJson(o: JSONObject): AIMessage {
            val arr = o.optJSONArray("content") ?: JSONArray()
            val blocks = (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { AIContentBlock.fromJson(it) }
            }
            return AIMessage(
                id = o.optString("id").ifEmpty { UUID.randomUUID().toString() },
                role = AIRole.from(o.optString("role")),
                content = blocks
            )
        }
    }
}

/**
 * Body sent to `POST api.getgothere.app/ai/messages`. The proxy adds the API
 * key, the system prompt (keyed by version), and the model name on its side.
 */
object AIProxyWire {
    const val SYSTEM_PROMPT_VERSION = "v1"

    fun encodeRequest(messages: List<AIMessage>, tools: JSONArray): JSONObject = JSONObject()
        .put("system_prompt_version", SYSTEM_PROMPT_VERSION)
        .put("messages", JSONArray().apply { messages.forEach { put(it.toJson()) } })
        .put("tools", tools)

    /** Decodes the non-streaming proxy response into the assistant [AIMessage]. */
    fun decodeResponseMessage(body: String): AIMessage {
        val root = JSONObject(body)
        val msg = root.optJSONObject("message")
            ?: throw IllegalStateException("Response missing 'message' object")
        return AIMessage.fromJson(msg)
    }
}
