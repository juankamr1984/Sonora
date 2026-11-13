/*
 * Sonora — Resumen de escucha (estadísticas locales, nada sale del dispositivo)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.sonora.MediaCard
import com.dd3boh.outertune.ui.sonora.SonoraArtwork
import com.dd3boh.outertune.ui.sonora.SonoraCardGradients
import com.dd3boh.outertune.ui.sonora.SonoraSectionHeader
import com.dd3boh.outertune.ui.sonora.sonoraSubtitle
import com.dd3boh.outertune.ui.sonora.sonoraThumbnailModel
import com.dd3boh.outertune.ui.theme.SonoraColors
import com.dd3boh.outertune.viewmodels.RecapPeriod
import com.dd3boh.outertune.viewmodels.RecapViewModel
import java.text.NumberFormat

@Composable
fun RecapScreen(
    navController: NavController,
    viewModel: RecapViewModel = hiltViewModel(),
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val density = LocalDensity.current

    val period by viewModel.period.collectAsState()
    val totalMs by viewModel.totalPlayTimeMs.collectAsState()
    val distinctSongs by viewModel.distinctSongs.collectAsState()
    val plays by viewModel.plays.collectAsState()
    val topSongs by viewModel.topSongs.collectAsState()
    val topArtists by viewModel.topArtists.collectAsState()
    val topAlbums by viewModel.topAlbums.collectAsState()

    val numberFormat = NumberFormat.getIntegerInstance()
    val minutes = totalMs / 60_000L
    val title = stringResource(R.string.sonora_recap)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        contentPadding = PaddingValues(
            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Portada del resumen
        item(key = "hero") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(SonoraCardGradients[1][0], SonoraCardGradients[0][1], MaterialTheme.colorScheme.background)
                        )
                    )
                    .padding(top = statusBarTop)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        RecapPeriod.THIS_YEAR to R.string.sonora_recap_this_year,
                        RecapPeriod.LAST_30_DAYS to R.string.sonora_recap_last_30,
                        RecapPeriod.ALL_TIME to R.string.sonora_recap_all_time,
                    ).forEach { (p, label) ->
                        FilterChip(
                            selected = period == p,
                            onClick = { viewModel.period.value = p },
                            label = { Text(stringResource(label)) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color.Black.copy(alpha = 0.25f),
                                labelColor = Color.White,
                                selectedContainerColor = Color.White,
                                selectedLabelColor = Color.Black,
                            ),
                            border = null,
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = numberFormat.format(minutes),
                    color = Color.White,
                    fontSize = 64.sp,
                    lineHeight = 66.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    text = stringResource(R.string.sonora_recap_minutes),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatPill(numberFormat.format(distinctSongs), stringResource(R.string.sonora_recap_songs), Modifier.weight(1f))
                    StatPill(numberFormat.format(plays), stringResource(R.string.sonora_recap_plays), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (topSongs.isEmpty() && topArtists.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.sonora_recap_empty),
                    color = SonoraColors.TextSecondary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        // Canciones top
        if (topSongs.isNotEmpty()) {
            item(key = "songs_title") { SonoraSectionHeader(stringResource(R.string.sonora_recap_top_songs)) }
            itemsIndexed(topSongs, key = { _, s -> "song_${s.id}" }) { index, song ->
                val px = with(density) { 56.dp.roundToPx() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playerConnection.playQueue(
                                ListQueue(title = title, items = topSongs.map { it.toMediaMetadata() }, startIndex = index)
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(36.dp)
                    )
                    SonoraArtwork(model = song.sonoraThumbnailModel(px), size = 56.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(song.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            song.sonoraSubtitle(),
                            color = SonoraColors.TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            item(key = "play_top") {
                Button(
                    onClick = {
                        playerConnection.playQueue(ListQueue(title = title, items = topSongs.map { it.toMediaMetadata() }))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.sonora_recap_play_top))
                }
            }
        }

        // Artistas top
        if (topArtists.isNotEmpty()) {
            item(key = "artists_title") { SonoraSectionHeader(stringResource(R.string.sonora_recap_top_artists)) }
            item(key = "artists") {
                val px = with(density) { 132.dp.roundToPx() }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    itemsIndexed(topArtists, key = { _, a -> a.id }) { index, artist ->
                        MediaCard(
                            title = "${index + 1}. ${artist.title}",
                            subtitle = null,
                            model = artist.sonoraThumbnailModel(px),
                            circle = true,
                            size = 132.dp,
                            onClick = { navController.navigate("artist/${artist.id}") },
                        )
                    }
                }
            }
        }

        // Álbum del año
        topAlbums.firstOrNull()?.let { album ->
            item(key = "album_title") { SonoraSectionHeader(stringResource(R.string.sonora_recap_top_album)) }
            item(key = "album") {
                val px = with(density) { 96.dp.roundToPx() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SonoraColors.CardHigh)
                        .clickable { navController.navigate("album/${album.id}") }
                        .padding(12.dp)
                ) {
                    SonoraArtwork(model = album.sonoraThumbnailModel(px), size = 96.dp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(album.title, fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(album.sonoraSubtitle(), color = SonoraColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(12.dp)
    ) {
        Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
