package com.example.gothere.ai

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** One deadline extracted from a document (mirrors the function's schema). */
data class DocDeadline(
    val dateIso: String?,
    val dateAsWritten: String,
    val sourceQuote: String,
    val action: String
)

/** Structured result from the analyzeDocument function (flat JSON, no wrapper). */
data class DocumentAnalysis(
    val unreadable: Boolean,
    val isDocument: Boolean,
    val docType: String?,
    val category: String?,
    val sender: String?,
    val originalLanguage: String?,
    val confidence: String?,
    val summary: String?,
    val nextStep: String?,
    val deadlines: List<DocDeadline>
)

class DocAnalysisException(val code: Int, message: String) : Exception(message)

/**
 * Calls the `analyzeDocument` Cloud Function (Claude vision). Clones AIClient's
 * HttpURLConnection + org.json plumbing, but hits a different endpoint and parses
 * the response FLAT (analyzeDocument returns the report_document object directly,
 * unlike aiProxy which wraps in {"message": ...}).
 */
object DocumentAnalysisClient {
    private const val ENDPOINT = "https://us-central1-gothere-e5ea7.cloudfunctions.net/analyzeDocument"

    suspend fun analyze(jpegBytes: ByteArray, country: String?): DocumentAnalysis =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("image", JSONObject().apply {
                    put("media_type", "image/jpeg")
                    put("data", Base64.encodeToString(jpegBytes, Base64.NO_WRAP))
                })
                if (!country.isNullOrEmpty()) put("country", country)
            }.toString()

            val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 90_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("X-GoThere-App-Token", AIClient.APP_TOKEN)
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) {
                    val msg = runCatching { JSONObject(text).optString("error") }.getOrNull()
                    throw DocAnalysisException(
                        code,
                        if (msg.isNullOrEmpty()) "The document reader is unavailable right now. Please try again." else msg
                    )
                }
                parse(JSONObject(text))
            } finally {
                conn.disconnect()
            }
        }

    private fun clean(s: String?): String? = s?.trim()?.trim('"', '\'')

    private fun String.orNull(): String? = ifEmpty { null }

    private fun parse(o: JSONObject): DocumentAnalysis {
        val deadlines = mutableListOf<DocDeadline>()
        o.optJSONArray("deadlines")?.let { arr ->
            for (i in 0 until arr.length()) {
                val d = arr.optJSONObject(i) ?: continue
                deadlines.add(
                    DocDeadline(
                        dateIso = d.optString("date_iso").orNull(),
                        dateAsWritten = d.optString("date_as_written"),
                        sourceQuote = d.optString("source_quote"),
                        action = d.optString("action")
                    )
                )
            }
        }
        return DocumentAnalysis(
            unreadable = o.optBoolean("unreadable", false),
            isDocument = o.optBoolean("is_document", true),
            docType = o.optString("doc_type").orNull(),
            category = o.optString("category").orNull(),
            sender = o.optString("sender").orNull(),
            originalLanguage = o.optString("original_language").orNull(),
            confidence = o.optString("confidence").orNull(),
            summary = clean(o.optString("summary").orNull()),
            nextStep = clean(o.optString("next_step").orNull()),
            deadlines = deadlines
        )
    }
}
