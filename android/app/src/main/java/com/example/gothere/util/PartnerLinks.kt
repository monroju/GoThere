package com.example.gothere.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Affiliate links for the Resources screen, fetched from
 * https://getgothere.app/api/partners.json (built from the site's `_partners.json`),
 * same feed as iOS `PartnerLinks.swift`.
 *
 * The feed lists only partners that are live with a tracking link, so turning a
 * partner on is a site deploy, never an app release. A resource is matched to a
 * partner by its URL host; anything not in the feed keeps its plain URL.
 */
object PartnerLinks {
    private const val FEED = "https://getgothere.app/api/partners.json"
    private const val PREFS = "partner_links"
    private const val KEY_MAP = "map_v1"
    private const val REFRESH_MS = 6 * 3600 * 1000L

    /** Resource URL host -> app partner id. */
    private val hostToPartner = mapOf(
        "wise.com" to "wise",
        "feather-insurance.com" to "feather",
        "safetywing.com" to "safetywing",
        "genki.world" to "genki",
        "cignaglobal.com" to "cigna_global",
    )

    /** Site ids that differ from the app partner id (country-specific entries). */
    private val aliases = mapOf("feather_es" to "feather", "feather_de" to "feather")

    @Volatile private var map: Map<String, String> = emptyMap()
    @Volatile private var loaded = false
    @Volatile private var lastFetch = 0L

    private fun load(context: Context) {
        if (loaded) return
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_MAP, null)
        map = raw?.let { runCatching { decodeMap(it) }.getOrNull() } ?: emptyMap()
        loaded = true
    }

    /** Partner id for a resource URL, or null when it is not a partner we track. */
    fun partnerFor(url: String): String? {
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase()?.removePrefix("www.") ?: return null
        return hostToPartner[host]
    }

    /** Tracking link when the partner is live (per country if the feed says so), else [url]. */
    fun resolve(context: Context, url: String, countryId: String): String {
        load(context)
        return resolve(map, url, countryId)
    }

    fun isPartner(context: Context, url: String, countryId: String): Boolean =
        resolve(context, url, countryId) != url

    /** Cheap to call on every screen open: refetches at most every 6 hours. */
    suspend fun refresh(context: Context) {
        load(context)
        val now = System.currentTimeMillis()
        if (now - lastFetch < REFRESH_MS) return
        lastFetch = now
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = URL(FEED).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                try {
                    if (conn.responseCode != 200) return@runCatching
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val fresh = parseFeed(body)
                    map = fresh
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString(KEY_MAP, JSONObject(fresh as Map<*, *>).toString()).apply()
                } finally {
                    conn.disconnect()
                }
            } // Offline or bad JSON: keep the cached map.
        }
    }

    // Pure helpers, unit tested.

    fun parseFeed(json: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        val arr = JSONObject(json).getJSONArray("partners")
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val url = p.optString("url")
            if (!url.startsWith("https://")) continue
            val id = aliases[p.getString("id")] ?: p.getString("id")
            val countries = p.optJSONArray("countries")?.let { a -> (0 until a.length()).map { a.getString(it) } }
                ?: listOf("*")
            if (countries.isEmpty() || "*" in countries) out[id] = url
            else countries.forEach { out["$id@$it"] = url }
        }
        return out
    }

    fun resolve(map: Map<String, String>, url: String, countryId: String): String {
        val id = partnerFor(url) ?: return url
        return map["$id@$countryId"] ?: map[id] ?: url
    }

    private fun decodeMap(raw: String): Map<String, String> {
        val o = JSONObject(raw)
        return o.keys().asSequence().associateWith { o.getString(it) }
    }
}
