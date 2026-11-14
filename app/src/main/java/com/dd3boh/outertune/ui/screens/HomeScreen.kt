/*
 * Sonora — pantalla de Inicio (basada en OuterTune, GPL-3.0)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalMenuState
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.db.entities.Album
import com.dd3boh.outertune.db.entities.Artist
import com.dd3boh.outertune.db.entities.LocalItem
import com.dd3boh.outertune.db.entities.Playlist
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.extensions.togglePlayPause
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.component.HideOnScrollFAB
import com.dd3boh.outertune.ui.component.ScrollToTopManager
import com.dd3boh.outertune.ui.menu.AlbumMenu
import com.dd3boh.outertune.ui.menu.ArtistMenu
import com.dd3boh.outertune.ui.menu.PlaylistMenu
import com.dd3boh.outertune.ui.menu.SongMenu
import com.dd3boh.outertune.ui.sonora.MediaCard
import com.dd3boh.outertune.ui.sonora.MixCard
import com.dd3boh.outertune.ui.sonora.QuickAccessTile
import com.dd3boh.outertune.ui.sonora.SonoraCardGradients
import com.dd3boh.outertune.ui.sonora.SonoraHorizontalSpacing
import com.dd3boh.outertune.ui.sonora.SonoraSectionHeader
import com.dd3boh.outertune.ui.sonora.sonoraSubtitle
import com.dd3boh.outertune.ui.sonora.sonoraThumbnailModel
import com.dd3boh.outertune.ui.theme.SonoraColors
import com.dd3boh.outertune.viewmodels.HomeViewModel
import kotlinx.coroutines.launch
import java.time.LocalTime

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val density = LocalDensity.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()
    val lazyListState = rememberLazyListState()

    val greeting = stringResource(
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.sonora_good_morning
            in 12..18 -> R.string.sonora_good_afternoon
            else -> R.string.sonora_good_evening
        }
    )
    val mixEmptyMsg = stringResource(R.string.sonora_mix_empty)
    val mixDailyTitle = stringResource(R.string.sonora_mix_daily)
    val mixLikedTitle = stringResource(R.string.sonora_mix_liked)
    val mixRediscoverTitle = stringResource(R.string.sonora_mix_rediscover)

    // Reproduce una lista de canciones como cola, empezando en el índice indicado
    fun playSongs(title: String, songs: List<Song>, startIndex: Int = 0, shuffled: Boolean = false) {
        if (songs.isEmpty()) {
            Toast.makeText(context, mixEmptyMsg, Toast.LENGTH_SHORT).show()
            return
        }
        playerConnection.playQueue(
            ListQueue(
                title = title,
                items = songs.map { it.toMediaMetadata() },
                startShuffled = shuffled,
                startIndex = startIndex,
            )
        )
    }

    fun openItem(item: LocalItem, sourceTitle: String, siblings: List<Song>) {
        when (item) {
            is Song -> {
                if (item.id == mediaMetadata?.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    val index = siblings.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
                    playSongs(sourceTitle, siblings.ifEmpty { listOf(item) }, index)
                }
            }
            is Album -> navController.navigate("album/${item.id}")
            is Artist -> navController.navigate("artist/${item.id}")
            is Playlist -> navController.navigate("local_playlist/${item.id}")
        }
    }

    fun showMenu(item: LocalItem) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is Song -> SongMenu(originalSong = item, navController = navController, onDismiss = menuState::dismiss)
                is Album -> AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss)
                is Artist -> ArtistMenu(originalArtist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                is Playlist -> PlaylistMenu(
                    navController = navController,
                    playlist = item,
                    coroutineScope = scope,
                    onDismiss = menuState::dismiss
                )
            }
        }
    }

    val quickAccess: List<LocalItem> = remember(recentlyPlayed, keepListening) {
        (recentlyPlayed + keepListening.orEmpty()).distinctBy { it.id }.take(8)
    }
    val libraryIsEmpty = quickPicks.isNullOrEmpty() && recentlyPlayed.isEmpty() && playlists.isNullOrEmpty()

    val playerInsets = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to SonoraColors.Accent.copy(alpha = 0.22f),
                    0.35f to MaterialTheme.colorScheme.background,
                )
            )
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh
            ),
        contentAlignment = Alignment.TopStart
    ) {
        val tileWidth = (maxWidth - 16.dp * 2 - 8.dp) / 2

        ScrollToTopManager(navController, lazyListState)
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                top = statusBarTop + 8.dp,
                bottom = playerInsets.calculateBottomPadding() + 16.dp
            )
        ) {
            // Encabezado: saludo + accesos
            item(key = "header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.history))
                    }
                    IconButton(onClick = { navController.navigate("stats") }) {
                        Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = stringResource(R.string.stats))
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            }

            if (libraryIsEmpty) {
                item(key = "empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sonora_empty_library),
                            color = SonoraColors.TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { navController.navigate("settings/local") }) {
                            Text(stringResource(R.string.sonora_scan_now))
                        }
                    }
                }
            }

            // Grilla de acceso rápido (2 columnas)
            if (quickAccess.isNotEmpty()) {
                item(key = "quick_access") {
                    val source = stringResource(R.string.sonora_recently_played)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        quickAccess.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { item ->
                                    val px = with(density) { 56.dp.roundToPx() }
                                    QuickAccessTile(
                                        title = item.title,
                                        model = item.sonoraThumbnailModel(px),
                                        isActive = item is Song && item.id == mediaMetadata?.id && isPlaying,
                                        circle = item is Artist,
                                        onClick = { openItem(item, source, quickAccess.filterIsInstance<Song>()) },
                                        onLongClick = { showMenu(item) },
                                        modifier = Modifier.width(tileWidth)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Banner del Resumen Sonora
            if (recentlyPlayed.isNotEmpty()) {
                item(key = "recap_banner") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(SonoraCardGradients[1]))
                            .clickable { navController.navigate("recap") }
                            .padding(16.dp)
                    ) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sonora_recap),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp
                            )
                            Text(
                                text = stringResource(R.string.sonora_recap_banner),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Mezclas generadas
            if (!libraryIsEmpty) {
                item(key = "mixes_title") { SonoraSectionHeader(stringResource(R.string.sonora_made_for_you)) }
                item(key = "mixes") {
                    LazyRow(
                        horizontalArrangement = SonoraHorizontalSpacing,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        item {
                            MixCard(
                                title = mixDailyTitle,
                                subtitle = stringResource(R.string.sonora_mix_daily_desc),
                                colors = SonoraCardGradients[0],
                                onClick = { scope.launch { playSongs(mixDailyTitle, viewModel.buildMix()) } }
                            )
                        }
                        item {
                            MixCard(
                                title = mixLikedTitle,
                                subtitle = stringResource(R.string.sonora_mix_liked_desc),
                                colors = SonoraCardGradients[5],
                                onClick = { scope.launch { playSongs(mixLikedTitle, viewModel.buildMix(likedOnly = true)) } }
                            )
                        }
                        item {
                            MixCard(
                                title = stringResource(R.string.sonora_online),
                                subtitle = stringResource(R.string.sonora_online_desc),
                                colors = SonoraCardGradients[4],
                                onClick = { navController.navigate("jamendo") }
                            )
                        }
                        item {
                            MixCard(
                                title = mixRediscoverTitle,
                                subtitle = stringResource(R.string.sonora_mix_rediscover_desc),
                                colors = SonoraCardGradients[2],
                                onClick = { playSongs(mixRediscoverTitle, forgottenFavorites.orEmpty(), shuffled = true) }
                            )
                        }
                    }
                }
            }

            // Escuchado recientemente
            if (recentlyPlayed.isNotEmpty()) {
                item(key = "recent_title") { SonoraSectionHeader(stringResource(R.string.sonora_recently_played)) }
                item(key = "recent") {
                    val source = stringResource(R.string.sonora_recently_played)
                    val px = with(density) { 148.dp.roundToPx() }
                    LazyRow(
                        horizontalArrangement = SonoraHorizontalSpacing,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(recentlyPlayed, key = { it.id }) { song ->
                            MediaCard(
                                title = song.title,
                                subtitle = song.sonoraSubtitle(),
                                model = song.sonoraThumbnailModel(px),
                                isActive = song.id == mediaMetadata?.id,
                                onClick = { openItem(song, source, recentlyPlayed) },
                                onLongClick = { showMenu(song) },
                            )
                        }
                    }
                }
            }

            // Vuelve a escuchar (canciones, álbumes y artistas más escuchados)
            keepListening?.takeIf { it.isNotEmpty() }?.let { keepItems ->
                item(key = "keep_title") { SonoraSectionHeader(stringResource(R.string.sonora_jump_back_in)) }
                item(key = "keep") {
                    val source = stringResource(R.string.sonora_jump_back_in)
                    val px = with(density) { 148.dp.roundToPx() }
                    LazyRow(
                        horizontalArrangement = SonoraHorizontalSpacing,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(keepItems, key = { it.id }) { item ->
                            MediaCard(
                                title = item.title,
                                subtitle = item.sonoraSubtitle(),
                                model = item.sonoraThumbnailModel(px),
                                circle = item is Artist,
                                isActive = item is Song && item.id == mediaMetadata?.id,
                                onClick = { openItem(item, source, keepItems.filterIsInstance<Song>()) },
                                onLongClick = { showMenu(item) },
                            )
                        }
                    }
                }
            }

            // Tus playlists
            playlists?.takeIf { it.isNotEmpty() }?.let { lists ->
                item(key = "playlists_title") { SonoraSectionHeader(stringResource(R.string.sonora_your_playlists)) }
                item(key = "playlists") {
                    val px = with(density) { 148.dp.roundToPx() }
                    LazyRow(
                        horizontalArrangement = SonoraHorizontalSpacing,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(lists, key = { it.id }) { playlist ->
                            MediaCard(
                                title = playlist.title,
                                subtitle = null,
                                model = playlist.sonoraThumbnailModel(px),
                                onClick = { openItem(playlist, playlist.title, emptyList()) },
                                onLongClick = { showMenu(playlist) },
                            )
                        }
                    }
                }
            }

            // Descubre en tu biblioteca
            quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                item(key = "discover_title") { SonoraSectionHeader(stringResource(R.string.sonora_discover)) }
                item(key = "discover") {
                    val source = stringResource(R.string.sonora_discover)
                    val px = with(density) { 148.dp.roundToPx() }
                    LazyRow(
                        horizontalArrangement = SonoraHorizontalSpacing,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        itemsIndexed(picks, key = { _, it -> it.id }) { _, song ->
                            MediaCard(
                                title = song.title,
                                subtitle = song.sonoraSubtitle(),
                                model = song.sonoraThumbnailModel(px),
                                isActive = song.id == mediaMetadata?.id,
                                onClick = { openItem(song, source, picks) },
                                onLongClick = { showMenu(song) },
                            )
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = !libraryIsEmpty,
            lazyListState = lazyListState,
            icon = Icons.Rounded.Shuffle,
            onClick = { scope.launch { playSongs(mixDailyTitle, viewModel.buildMix()) } }
        )

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop),
        )
    }
}
