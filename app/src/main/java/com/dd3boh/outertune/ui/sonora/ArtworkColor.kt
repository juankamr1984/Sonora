/*
 * Sonora — color dominante de la portada (para el mini-player)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.sonora

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.dd3boh.outertune.models.MediaMetadata
import com.dd3boh.outertune.ui.theme.SonoraColors
import com.dd3boh.outertune.utils.coilCoroutine
import kotlinx.coroutines.withContext

/**
 * Devuelve un color oscuro derivado de la portada, animado al cambiar de canción.
 * Se oscurece para que el texto blanco siempre sea legible.
 */
@Composable
fun rememberArtworkColor(mediaMetadata: MediaMetadata?, fallback: Color = SonoraColors.CardHigh): Color {
    val context = LocalContext.current
    var target by remember { mutableStateOf(fallback) }

    LaunchedEffect(mediaMetadata?.id) {
        if (mediaMetadata == null) {
            target = fallback
            return@LaunchedEffect
        }
        val extracted = withContext(coilCoroutine) {
            runCatching {
                val result = context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(mediaMetadata.getThumbnailModel(64, 64))
                        .allowHardware(false)
                        .build()
                )
                result.image?.toBitmap()?.let { bitmap ->
                    val palette = Palette.from(bitmap).maximumColorCount(12).generate()
                    val swatch = palette.darkVibrantSwatch ?: palette.vibrantSwatch
                        ?: palette.darkMutedSwatch ?: palette.dominantSwatch
                    swatch?.rgb?.let { Color(it) }
                }
            }.getOrNull()
        }
        target = extracted?.let { lerp(it, Color.Black, 0.3f) } ?: fallback
    }

    val animated by animateColorAsState(target, animationSpec = tween(600), label = "artworkColor")
    return animated
}
