/*
 * Sonora — ViewModel de música en línea (Jamendo)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.DownloadService
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.jamendo.JamendoApi
import com.dd3boh.outertune.jamendo.JamendoException
import com.dd3boh.outertune.jamendo.JamendoOrder
import com.dd3boh.outertune.jamendo.JamendoStore
import com.dd3boh.outertune.jamendo.JamendoTrack
import com.dd3boh.outertune.playback.DownloadUtil
import com.dd3boh.outertune.playback.ExoDownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class JamendoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val database: MusicDatabase,
    val downloadUtil: DownloadUtil,
) : ViewModel() {

    val query = MutableStateFlow("")
    val selectedTag = MutableStateFlow<String?>(null)
    val tracks = MutableStateFlow<List<JamendoTrack>>(emptyList())
    val isLoading = MutableStateFlow(false)
    val canLoadMore = MutableStateFlow(false)

    /** null = sin error; "missing_client_id" = falta configurar; otro texto = error de red/API. */
    val error = MutableStateFlow<String?>(null)
    val clientId = MutableStateFlow(JamendoStore.clientId(context))

    private var loadJob: Job? = null
    private val pageSize = 40

    init {
        refresh()
    }

    fun setClientId(value: String) {
        JamendoStore.setClientId(context, value)
        clientId.value = JamendoStore.clientId(context)
        refresh()
    }

    fun search(text: String) {
        query.value = text
        refresh()
    }

    fun selectTag(tag: String?) {
        selectedTag.value = if (selectedTag.value == tag) null else tag
        refresh()
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        if (!isLoading.value && canLoadMore.value) load(reset = false)
    }

    private fun load(reset: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val offset = if (reset) 0 else tracks.value.size
                val result = JamendoApi.tracks(
                    clientId = clientId.value,
                    query = query.value.ifBlank { null },
                    tags = selectedTag.value,
                    order = if (query.value.isBlank()) JamendoOrder.POPULAR_WEEK else JamendoOrder.RELEVANCE,
                    limit = pageSize,
                    offset = offset,
                )
                result.forEach { JamendoStore.saveUrls(context, it) }
                tracks.value = if (reset) result else (tracks.value + result).distinctBy { it.id }
                canLoadMore.value = result.size >= pageSize
            } catch (e: JamendoException) {
                error.value = e.message
                if (reset) tracks.value = emptyList()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                error.value = e.message ?: e.javaClass.simpleName
                if (reset) tracks.value = emptyList()
            } finally {
                isLoading.value = false
            }
        }
    }

    /**
     * Registra las canciones en la base de datos local (necesario para historial, estadísticas,
     * playlists y descargas). Si [addToLibrary] es true, también quedan en "Tu biblioteca".
     */
    suspend fun ensureInDatabase(list: List<JamendoTrack>, addToLibrary: Boolean = false) =
        withContext(Dispatchers.IO) {
            val now = LocalDateTime.now()
            list.forEach { track ->
                JamendoStore.saveUrls(context, track)
                val meta = track.toMediaMetadata()
                database.insert(meta) { entity -> if (addToLibrary) entity.copy(inLibrary = now) else entity }
                if (addToLibrary) {
                    database.song(track.songId).first()?.song?.let { existing ->
                        if (existing.inLibrary == null) database.update(existing.copy(inLibrary = now))
                    }
                }
            }
        }

    suspend fun songFromDatabase(track: JamendoTrack): Song? = withContext(Dispatchers.IO) {
        ensureInDatabase(listOf(track))
        database.song(track.songId).first()
    }

    fun download(track: JamendoTrack) {
        viewModelScope.launch {
            ensureInDatabase(listOf(track), addToLibrary = true)
            downloadUtil.download(track.toMediaMetadata())
        }
    }

    fun downloadAll(list: List<JamendoTrack>) {
        val allowed = list.filter { it.downloadAllowed }
        viewModelScope.launch {
            ensureInDatabase(allowed, addToLibrary = true)
            downloadUtil.download(allowed.map { it.toMediaMetadata() })
        }
    }

    fun removeDownload(track: JamendoTrack) {
        DownloadService.sendRemoveDownload(context, ExoDownloadService::class.java, track.songId, false)
    }
}
