package com.example.gothere.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Thin network layer over the GoThere AI proxy. Non-streaming: POSTs the
 * Messages-API-shaped body and decodes the single assistant turn from the
 * proxy's JSON response. (iOS additionally supports SSE streaming; Android
 * starts non-streaming — the proxy serves both off the same endpoint.)
 *
 * The proxy adds the Anthropic key + system prompt server-side; this client
 * only forwards the conversation and the shared app token.
 */
class AIClient(
    private val endpoint: String = PROXY_URL,
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 60_000
) {

    class AIException(val statusCode: Int, message: String) : Exception(message)

    /** One round-trip: send the full transcript + tool catalog, get the next
     *  assistant message back. Runs on the IO dispatcher. */
    suspend fun send(messages: List<AIMessage>, tools: JSONArray): AIMessage =
        withContext(Dispatchers.IO) {
            val payload = AIProxyWire.encodeRequest(messages, tools).toString()
            val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-System-Prompt-Version", AIProxyWire.SYSTEM_PROMPT_VERSION)
                setRequestProperty("X-GoThere-App-Token", APP_TOKEN)
            }
            try {
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

                if (code !in 200..299) {
                    throw AIException(code, "Proxy returned HTTP $code: ${body.take(500)}")
                }
                AIProxyWire.decodeResponseMessage(body)
            } finally {
                conn.disconnect()
            }
        }

    companion object {
        /** Matches the iOS endpoint exactly so both clients share the proxy. */
        const val PROXY_URL = "https://api.getgothere.app/ai/messages"

        /**
         * Shared app secret, also set as AI_PROXY_APP_TOKEN in functions/.env.
         * A client-embedded secret is extractable — this is hardening (it stops
         * anonymous abuse of the open endpoint), not real authentication. The
         * proxy only *enforces* it once AI_PROXY_REQUIRE_TOKEN is flipped on, so
         * shipping this build before the flip is safe.
         */
        const val APP_TOKEN = "gthr_app_RRoTKHz-Wch7Ci4Wvdiy15ogMG0bHQw3"
    }
}
