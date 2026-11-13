/*
 * Sonora — ViewModels de Buscar/Explorar y Resumen
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dd3boh.outertune.db.MusicDatabase
import com.dd3boh.outertune.db.entities.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    val genres = database.topGenres(12)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    suspend fun songsForGenre(genreId: String): List<Song> =
        database.songsByGenre(genreId).first()
}

enum class RecapPeriod { THIS_YEAR, LAST_30_DAYS, ALL_TIME;

    fun fromMillis(): Long = when (this) {
        THIS_YEAR -> LocalDate.now().withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        LAST_30_DAYS -> LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC).toEpochMilli()
        ALL_TIME -> 0L
    }

    /** Año/mes de inicio para consultas de artistas (basadas en playCount). */
    fun fromYearMonth(): Pair<Int, Int> = when (this) {
        THIS_YEAR -> LocalDate.now().year to 1
        LAST_30_DAYS -> LocalDate.now().minusDays(30).let { it.year to it.monthValue }
        ALL_TIME -> 0 to 1
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecapViewModel @Inject constructor(
    val database: MusicDatabase,
) : ViewModel() {
    val period = MutableStateFlow(RecapPeriod.THIS_YEAR)

    val totalPlayTimeMs = period.flatMapLatest { database.totalPlayTime(it.fromMillis()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)

    val distinctSongs = period.flatMapLatest { database.distinctSongsPlayed(it.fromMillis()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val plays = period.flatMapLatest { database.playEventCount(it.fromMillis()) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val topSongs = period.flatMapLatest { database.mostPlayedSongs(it.fromMillis(), limit = 5) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topArtists = period.flatMapLatest {
        val (year, month) = it.fromYearMonth()
        database.mostPlayedArtists(year, month, limit = 5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val topAlbums = period.flatMapLatest { database.mostPlayedAlbums(it.fromMillis(), limit = 1) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
