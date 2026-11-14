/*
 * Sonora — almacenamiento local para la integración con Jamendo
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.jamendo

import android.content.Context
import android.content.SharedPreferences
import com.dd3boh.outertune.BuildConfig

/**
 * Guarda el client_id de Jamendo y las URLs de streaming/descarga de cada canción,
 * para poder reproducirlas o descargarlas aunque la app se haya reiniciado.
 */
object JamendoStore {
    /** Prefijo de los ids de canciones de Jamendo dentro de la base de datos de la app. */
    const val SONG_PREFIX = "JM"
    const val ARTIST_PREFIX = "JMA"
    const val ALBUM_PREFIX = "JMB"

    private const val PREFS = "sonora_jamendo"
    private const val KEY_CLIENT_ID = "client_id"
    private const val KEY_STREAM = "stream_"
    private const val KEY_DOWNLOAD = "download_"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isJamendoSong(id: String?): Boolean =
        id != null && id.startsWith(SONG_PREFIX) && !id.startsWith(ARTIST_PREFIX) && !id.startsWith(ALBUM_PREFIX)

    fun trackIdOf(songId: String): String = songId.removePrefix(SONG_PREFIX)

    /** client_id configurado en la app; si no hay, el que se compiló desde GitHub (secreto JAMENDO_CLIENT_ID). */
    fun clientId(context: Context): String =
        prefs(context).getString(KEY_CLIENT_ID, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.JAMENDO_CLIENT_ID

    fun setClientId(context: Context, value: String) {
        prefs(context).edit().putString(KEY_CLIENT_ID, value.trim()).apply()
    }

    fun saveUrls(context: Context, track: JamendoTrack) {
        prefs(context).edit().apply {
            if (track.audio.isNotBlank()) putString(KEY_STREAM + track.id, track.audio)
            if (track.downloadAllowed && track.audioDownload.isNotBlank()) {
                putString(KEY_DOWNLOAD + track.id, track.audioDownload)
            }
        }.apply()
    }

    /** URL para reproducir en streaming (mp3 de buena calidad). */
    fun streamUrl(context: Context, songId: String): String {
        val trackId = trackIdOf(songId)
        return prefs(context).getString(KEY_STREAM + trackId, null)
            ?: "https://prod-1.storage.jamendo.com/?trackid=$trackId&format=mp32"
    }

    /**
     * URL para descargar. Solo existe si Jamendo permite la descarga de esa canción;
     * en ese caso se usa el archivo oficial de descarga, si no, el de streaming.
     */
    fun downloadUrl(context: Context, songId: String): String =
        prefs(context).getString(KEY_DOWNLOAD + trackIdOf(songId), null) ?: streamUrl(context, songId)

    fun isDownloadAllowed(context: Context, songId: String): Boolean =
        prefs(context).contains(KEY_DOWNLOAD + trackIdOf(songId))
}
