/*
 * Sonora — cliente mínimo de la API pública de Jamendo (v3.0)
 * Música con licencias Creative Commons. Uso no comercial: https://devportal.jamendo.com
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.jamendo

import com.dd3boh.outertune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class JamendoTrack(
    val id: String,
    val name: String,
    val durationSec: Int,
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumName: String,
    val image: String,
    val audio: String,
    val audioDownload: String,
    val downloadAllowed: Boolean,
    val licenseUrl: String,
) {
    val songId: String get() = JamendoStore.SONG_PREFIX + id

    fun toMediaMetadata(): MediaMetadata = MediaMetadata(
        id = songId,
        title = name,
        artists = listOf(MediaMetadata.Artist(id = JamendoStore.ARTIST_PREFIX + artistId, name = artistName)),
        duration = durationSec,
        thumbnailUrl = image.ifBlank { null },
        album = if (albumId.isNotBlank()) MediaMetadata.Album(id = JamendoStore.ALBUM_PREFIX + albumId, title = albumName) else null,
        genre = null,
        isLocal = false,
        localPath = null,
    )
}

class JamendoException(message: String) : IOException(message)

/** Orden de resultados soportado por /tracks. */
enum class JamendoOrder(val value: String) {
    POPULAR_WEEK("popularity_week"),
    POPULAR_MONTH("popularity_month"),
    POPULAR_ALL("popularity_total"),
    RELEVANCE("relevance"),
    NEWEST("releasedate_desc"),
}

object JamendoApi {
    private const val BASE = "https://api.jamendo.com/v3.0"

    /**
     * Busca canciones. [query] busca en nombre de canción, artista, álbum y etiquetas;
     * [tags] filtra por género (por ejemplo "rock", "electronic").
     */
    suspend fun tracks(
        clientId: String,
        query: String? = null,
        tags: String? = null,
        order: JamendoOrder = JamendoOrder.POPULAR_MONTH,
        limit: Int = 40,
        offset: Int = 0,
    ): List<JamendoTrack> = withContext(Dispatchers.IO) {
        if (clientId.isBlank()) throw JamendoException("missing_client_id")

        val params = linkedMapOf(
            "client_id" to clientId,
            "format" to "json",
            "limit" to limit.coerceIn(1, 200).toString(),
            "offset" to offset.toString(),
            "audioformat" to "mp32",
            "imagesize" to "300",
            "include" to "licenses",
            "order" to if (!query.isNullOrBlank() && order == JamendoOrder.POPULAR_MONTH) JamendoOrder.RELEVANCE.value else order.value,
        )
        if (!query.isNullOrBlank()) params["search"] = query.trim()
        if (!tags.isNullOrBlank()) params["fuzzytags"] = tags.trim()

        val url = BASE + "/tracks/?" + params.entries.joinToString("&") { (k, v) ->
            "$k=" + URLEncoder.encode(v, "UTF-8")
        }
        parseTracks(httpGet(url))
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw JamendoException("HTTP $code")
            return body
        } finally {
            conn.disconnect()
        }
    }

    private fun parseTracks(body: String): List<JamendoTrack> {
        val root = JSONObject(body)
        val headers = root.optJSONObject("headers")
        if (headers != null && headers.optString("status") != "success") {
            throw JamendoException(headers.optString("error_message").ifBlank { "error" })
        }
        val results = root.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val t = results.optJSONObject(i) ?: return@mapNotNull null
            val audio = t.optString("audio")
            if (audio.isBlank()) return@mapNotNull null // sin audio disponible
            JamendoTrack(
                id = t.optString("id"),
                name = t.optString("name"),
                durationSec = t.optString("duration").toIntOrNull() ?: t.optInt("duration", -1),
                artistId = t.optString("artist_id"),
                artistName = t.optString("artist_name"),
                albumId = t.optString("album_id"),
                albumName = t.optString("album_name"),
                image = t.optString("image").ifBlank { t.optString("album_image") },
                audio = audio,
                audioDownload = t.optString("audiodownload"),
                downloadAllowed = t.optBoolean("audiodownload_allowed", false),
                licenseUrl = t.optString("license_ccurl"),
            )
        }
    }
}
