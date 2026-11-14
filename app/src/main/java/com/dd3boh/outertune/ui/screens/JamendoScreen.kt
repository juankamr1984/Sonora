/*
 * Sonora — Música en línea (Jamendo): búsqueda, géneros, streaming, descargas offline y playlists
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.FileDownloadOff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalDownloadUtil
import com.dd3boh.outertune.LocalMenuState
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.jamendo.JamendoTrack
import com.dd3boh.outertune.playback.DownloadUtil
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.dialog.AddToPlaylistDialog
import com.dd3boh.outertune.ui.menu.SongMenu
import com.dd3boh.outertune.ui.sonora.SonoraArtwork
import com.dd3boh.outertune.ui.sonora.SonoraSectionHeader
import com.dd3boh.outertune.ui.theme.SonoraColors
import com.dd3boh.outertune.viewmodels.JamendoViewModel
import kotlinx.coroutines.launch

/** Géneros de Jamendo (etiqueta de la API -> nombre visible). */
private val JamendoGenres = listOf(
    "pop" to "Pop",
    "rock" to "Rock",
    "electronic" to "Electrónica",
    "hiphop" to "Hip hop",
    "latin" to "Latina",
    "jazz" to "Jazz",
    "chillout" to "Chill",
    "lounge" to "Lounge",
    "acoustic" to "Acústica",
    "classical" to "Clásica",
    "soundtrack" to "Bandas sonoras",
    "reggae" to "Reggae",
    "metal" to "Metal",
    "relaxation" to "Relajación",
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JamendoScreen(
    navController: NavController,
    viewModel: JamendoViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val clientId by viewModel.clientId.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val currentQuery by viewModel.query.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    var searchText by rememberSaveable { mutableStateOf(currentQuery) }
    var showSettings by remember { mutableStateOf(false) }
    var playlistSongId by remember { mutableStateOf<String?>(null) }

    val onlineTitle = stringResource(R.string.sonora_online)
    val notAllowedMsg = stringResource(R.string.sonora_online_download_not_allowed)
    val downloadStartedMsg = stringResource(R.string.sonora_online_download_started)
    val downloadRemovedMsg = stringResource(R.string.sonora_online_download_removed)

    val listState = rememberLazyListState()
    val reachedEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= listState.layoutInfo.totalItemsCount - 3
        }
    }
    LaunchedEffect(reachedEnd) {
        if (reachedEnd && tracks.isNotEmpty()) viewModel.loadMore()
    }

    fun play(list: List<JamendoTrack>, index: Int, shuffled: Boolean = false) {
        if (list.isEmpty()) return
        scope.launch {
            viewModel.ensureInDatabase(list)
            playerConnection.playQueue(
                ListQueue(
                    title = onlineTitle,
                    items = list.map { it.toMediaMetadata() },
                    startIndex = index,
                    startShuffled = shuffled,
                )
            )
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = statusBarTop, bottom = bottomPadding + 16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Encabezado
        item(key = "header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                }
                Text(
                    text = onlineTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.sonora_online_settings))
                }
            }
        }

        // Configuración inicial (client_id)
        if (clientId.isBlank()) {
            item(key = "setup") {
                ClientIdSetupCard(
                    initial = "",
                    onSave = viewModel::setClientId,
                    onOpenPortal = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, "https://devportal.jamendo.com".toUri())
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                )
            }
            return@LazyColumn
        }

        // Barra de búsqueda
        item(key = "search") {
            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text(stringResource(R.string.sonora_online_search_hint), color = Color(0xFF555555)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF121212)) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Rounded.Close, contentDescription = null, tint = Color(0xFF121212))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.search(searchText)
                }),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF121212),
                    unfocusedTextColor = Color(0xFF121212),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color(0xFF121212),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Géneros
        item(key = "genres") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                item {
                    GenreChip(
                        label = stringResource(R.string.sonora_online_all),
                        selected = selectedTag == null,
                        onClick = { if (selectedTag != null) viewModel.selectTag(selectedTag) }
                    )
                }
                items(JamendoGenres, key = { it.first }) { (tag, label) ->
                    GenreChip(label = label, selected = selectedTag == tag, onClick = { viewModel.selectTag(tag) })
                }
            }
        }

        // Título + acciones
        item(key = "title") {
            SonoraSectionHeader(
                title = stringResource(
                    if (currentQuery.isBlank()) R.string.sonora_online_popular else R.string.sonora_online_results
                )
            )
        }
        if (tracks.isNotEmpty()) {
            item(key = "actions") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Button(onClick = { play(tracks, 0) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sonora_online_play_all))
                    }
                    OutlinedButton(onClick = {
                        viewModel.downloadAll(tracks)
                        Toast.makeText(context, downloadStartedMsg, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sonora_online_download_all))
                    }
                }
            }
        }

        // Error
        error?.let { message ->
            item(key = "error") {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sonora_online_error, message),
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = { viewModel.refresh() }) {
                        Text(stringResource(R.string.sonora_online_retry))
                    }
                }
            }
        }

        if (!isLoading && error == null && tracks.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.sonora_online_no_results),
                    color = SonoraColors.TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Resultados
        itemsIndexed(tracks, key = { _, t -> "jm_${t.id}" }) { index, track ->
            JamendoTrackRow(
                track = track,
                px = with(density) { 52.dp.roundToPx() },
                isActive = mediaMetadata?.id == track.songId,
                isPlaying = isPlaying,
                onClick = { play(tracks, index) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    scope.launch {
                        val song = viewModel.songFromDatabase(track) ?: return@launch
                        menuState.show {
                            SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss)
                        }
                    }
                },
                onAddToPlaylist = {
                    scope.launch {
                        viewModel.ensureInDatabase(listOf(track), addToLibrary = true)
                        playlistSongId = track.songId
                    }
                },
                onDownload = {
                    if (!track.downloadAllowed) {
                        Toast.makeText(context, notAllowedMsg, Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.download(track)
                        Toast.makeText(context, downloadStartedMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                onRemoveDownload = {
                    viewModel.removeDownload(track)
                    Toast.makeText(context, downloadRemovedMsg, Toast.LENGTH_SHORT).show()
                },
            )
        }

        if (isLoading) {
            item(key = "loading") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        item(key = "attribution") {
            Text(
                text = stringResource(R.string.sonora_online_attribution),
                color = SonoraColors.TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Diálogo para agregar a una playlist local
    playlistSongId?.let { songId ->
        AddToPlaylistDialog(
            navController = navController,
            songIds = listOf(songId),
            onPreAdd = { listOf(songId) },
            onDismiss = { playlistSongId = null }
        )
    }

    // Diálogo para cambiar el client_id
    if (showSettings) {
        var value by remember { mutableStateOf(clientId) }
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(stringResource(R.string.sonora_online_settings)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.sonora_online_client_id)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setClientId(value)
                    showSettings = false
                }) { Text(stringResource(R.string.sonora_online_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text(stringResource(android.R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SonoraColors.CardHigh,
            labelColor = Color.White,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = Color(0xFF121212),
        ),
        border = null,
        shape = RoundedCornerShape(50),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JamendoTrackRow(
    track: JamendoTrack,
    px: Int,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownload: () -> Unit,
) {
    val downloadState by LocalDownloadUtil.current.getDownload(track.songId).collectAsState(initial = null)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            SonoraArtwork(model = track.image.ifBlank { null }, size = 52.dp, shape = RoundedCornerShape(4.dp))
            if (isActive && isPlaying) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = track.name,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artistName,
                fontSize = 13.sp,
                color = SonoraColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                Icons.AutoMirrored.Rounded.PlaylistAdd,
                contentDescription = stringResource(R.string.sonora_online_add_playlist),
                tint = SonoraColors.TextSecondary
            )
        }
        when {
            downloadState == null -> IconButton(onClick = onDownload) {
                Icon(
                    imageVector = if (track.downloadAllowed) Icons.Rounded.Download else Icons.Rounded.FileDownloadOff,
                    contentDescription = null,
                    tint = if (track.downloadAllowed) SonoraColors.TextSecondary else SonoraColors.TextMuted,
                )
            }

            downloadState == DownloadUtil.STATE_DOWNLOADING -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp)
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            }

            else -> IconButton(onClick = onRemoveDownload) {
                Icon(Icons.Rounded.DownloadDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun ClientIdSetupCard(
    initial: String,
    onSave: (String) -> Unit,
    onOpenPortal: () -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SonoraColors.CardHigh)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.sonora_online_setup_title),
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.sonora_online_setup_desc), color = SonoraColors.TextSecondary)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onOpenPortal) {
            Text(stringResource(R.string.sonora_online_setup_open))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            label = { Text(stringResource(R.string.sonora_online_client_id)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { onSave(value) }, enabled = value.isNotBlank()) {
            Text(stringResource(R.string.sonora_online_save))
        }
    }
}
