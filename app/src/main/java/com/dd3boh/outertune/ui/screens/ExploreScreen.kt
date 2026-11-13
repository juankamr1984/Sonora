/*
 * Sonora — pestaña Buscar / Explorar
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.dd3boh.outertune.LocalPlayerAwareWindowInsets
import com.dd3boh.outertune.LocalPlayerConnection
import com.dd3boh.outertune.R
import com.dd3boh.outertune.models.toMediaMetadata
import com.dd3boh.outertune.playback.queues.ListQueue
import com.dd3boh.outertune.ui.sonora.CategoryCard
import com.dd3boh.outertune.ui.sonora.SonoraCardGradients
import com.dd3boh.outertune.ui.sonora.SonoraSectionHeader
import com.dd3boh.outertune.ui.sonora.sonoraGradientFor
import com.dd3boh.outertune.viewmodels.ExploreViewModel
import kotlinx.coroutines.launch

private data class ExploreCategory(val title: String, val icon: ImageVector, val gradient: Int, val route: String)

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()
    val genres by viewModel.genres.collectAsState()
    val emptyMsg = stringResource(R.string.sonora_mix_empty)

    val categories = listOf(
        ExploreCategory(stringResource(R.string.songs), Icons.Rounded.MusicNote, 0, Screens.Songs.route),
        ExploreCategory(stringResource(R.string.artists), Icons.Rounded.Person, 1, Screens.Artists.route),
        ExploreCategory(stringResource(R.string.albums), Icons.Rounded.Album, 2, Screens.Albums.route),
        ExploreCategory(stringResource(R.string.playlists), Icons.AutoMirrored.Rounded.QueueMusic, 3, Screens.Playlists.route),
        ExploreCategory(stringResource(R.string.liked_songs), Icons.Rounded.Favorite, 5, "auto_playlist/liked"),
        ExploreCategory(stringResource(R.string.folders), Icons.Rounded.Folder, 4, Screens.Folders.route),
        ExploreCategory(stringResource(R.string.history), Icons.Rounded.History, 6, "history"),
        ExploreCategory(stringResource(R.string.stats), Icons.AutoMirrored.Rounded.TrendingUp, 7, "stats"),
        ExploreCategory(stringResource(R.string.sonora_recap), Icons.Rounded.AutoAwesome, 1, "recap"),
        ExploreCategory(stringResource(R.string.library), Icons.Rounded.LibraryMusic, 0, Screens.Library.route),
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().let {
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = it.calculateTopPadding(),
                bottom = it.calculateBottomPadding() + 16.dp
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (genres.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SonoraSectionHeader(stringResource(R.string.sonora_genres), modifier = Modifier)
            }
            items(genres, key = { "genre_${it.id}" }) { genre ->
                CategoryCard(
                    title = genre.title,
                    colors = sonoraGradientFor(genre.title),
                    icon = Icons.Rounded.Piano,
                    onClick = {
                        scope.launch {
                            val songs = viewModel.songsForGenre(genre.id)
                            if (songs.isEmpty()) {
                                Toast.makeText(context, emptyMsg, Toast.LENGTH_SHORT).show()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(title = genre.title, items = songs.map { it.toMediaMetadata() })
                                )
                            }
                        }
                    }
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            SonoraSectionHeader(stringResource(R.string.sonora_browse_all))
        }
        items(categories, key = { "cat_${it.route}_${it.title}" }) { cat ->
            CategoryCard(
                title = cat.title,
                colors = SonoraCardGradients[cat.gradient % SonoraCardGradients.size],
                icon = cat.icon,
                onClick = { navController.navigate(cat.route) }
            )
        }
    }
}
