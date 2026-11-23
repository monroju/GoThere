package com.example.gothere.help

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * One partner organization from https://getgothere.app/api/help.json
 * (spec: Projects/growth-plan/08-get-help-spec.md). Unknown fields are ignored.
 */
data class HelpOrg(
    val id: String,
    val name: String,
    val blurb: String,
    val url: String,
    val audiences: List<String>,
    val apps: List<String>,
    val countries: List<String>
)

/** Pure parsing and filtering, no Android types, so it runs in JVM unit tests. */
object HelpFeedParser {

    /** Returns null when the JSON is malformed or has no `orgs` array; bad entries are skipped. */
    fun parse(json: String?): List<HelpOrg>? {
        if (json.isNullOrBlank()) return null
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val orgs = root.optJSONArray("orgs") ?: return null
        return (0 until orgs.length()).mapNotNull { i -> orgs.optJSONObject(i)?.let(::parseOrg) }
    }

    private fun parseOrg(o: JSONObject): HelpOrg? {
        val id = o.optString("id").trim()
        val name = o.optString("name").trim()
        val url = o.optString("url").trim()
        if (id.isEmpty() || name.isEmpty() || !url.startsWith("https://")) return null
        return HelpOrg(
            id = id,
            name = name,
            blurb = o.optString("blurb").trim(),
            url = url,
            audiences = strings(o.optJSONArray("audiences")),
            apps = strings(o.optJSONArray("apps")),
            countries = strings(o.optJSONArray("countries"))
        )
    }

    private fun strings(a: JSONArray?): List<String> =
        if (a == null) emptyList()
        else (0 until a.length()).mapNotNull { a.optString(it).trim().lowercase().takeIf(String::isNotEmpty) }

    /** Entries whose `apps` list contains [appId]. */
    fun forApp(orgs: List<HelpOrg>, appId: String): List<HelpOrg> =
        orgs.filter { appId in it.apps }

    /**
     * Picks the network body when it parses, else the cached body, else nothing.
     * [cacheable] is true only when the network body was valid and should replace the cache.
     */
    fun resolve(network: String?, cached: String?, appId: String): Resolved {
        parse(network)?.let { return Resolved(forApp(it, appId), cacheable = true) }
        parse(cached)?.let { return Resolved(forApp(it, appId), cacheable = false) }
        return Resolved(emptyList(), cacheable = false)
    }

    data class Resolved(val orgs: List<HelpOrg>, val cacheable: Boolean)
}

/** Plain GET with an 8 second timeout and a last good copy on disk. Sends no personal data. */
class HelpFeedRepository(context: Context) {
    private val cacheFile = File(context.applicationContext.filesDir, CACHE_NAME)

    /** Cached list only, no network. Lets screens draw immediately. */
    suspend fun cached(): List<HelpOrg> = withContext(Dispatchers.IO) {
        HelpFeedParser.resolve(null, readCache(), APP_ID).orgs
    }

    /** Network first, cache when offline or the response is malformed. */
    suspend fun load(): List<HelpOrg> = withContext(Dispatchers.IO) {
        val body = fetch()
        val resolved = HelpFeedParser.resolve(body, readCache(), APP_ID)
        if (resolved.cacheable && body != null) {
            runCatching { cacheFile.writeText(body) }
        }
        resolved.orgs
    }

    private fun readCache(): String? = runCatching { cacheFile.takeIf { it.exists() }?.readText() }.getOrNull()

    private fun fetch(): String? = runCatching {
        val conn = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            useCaches = false
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }.getOrNull()

    companion object {
        const val FEED_URL = "https://getgothere.app/api/help.json"
        const val APP_ID = "gothere"
        private const val CACHE_NAME = "help_feed.json"
        private const val TIMEOUT_MS = 8_000
    }
}
