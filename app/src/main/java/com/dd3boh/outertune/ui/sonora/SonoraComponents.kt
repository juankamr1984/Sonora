/*
 * Sonora — componentes visuales compartidos (Home, Buscar, Resumen)
 *
 * SPDX-License-Identifier: GPL-3.0
 */
package com.dd3boh.outertune.ui.sonora

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.dd3boh.outertune.db.entities.Album
import com.dd3boh.outertune.db.entities.Artist
import com.dd3boh.outertune.db.entities.LocalItem
import com.dd3boh.outertune.db.entities.Playlist
import com.dd3boh.outertune.db.entities.Song
import com.dd3boh.outertune.ui.theme.SonoraColors
import com.dd3boh.outertune.ui.theme.SonoraSectionTitle
import com.dd3boh.outertune.utils.getThumbnailModel
import kotlin.math.roundToInt

/** Paletas de degradado originales para tarjetas de mezclas y géneros. */
val SonoraCardGradients: List<List<Color>> = listOf(
    listOf(Color(0xFFFF8686), Color(0xFFB8455A)),
    listOf(Color(0xFF6C5CE7), Color(0xFF2D1F8F)),
    listOf(Color(0xFF00B894), Color(0xFF006B57)),
    listOf(Color(0xFFFDCB6E), Color(0xFFC77D14)),
    listOf(Color(0xFF0984E3), Color(0xFF07407A)),
    listOf(Color(0xFFE84393), Color(0xFF8E1C57)),
    listOf(Color(0xFF55EFC4), Color(0xFF1E8C73)),
    listOf(Color(0xFFE17055), Color(0xFF8A3320)),
)

fun sonoraGradientFor(key: String): List<Color> =
    SonoraCardGradients[(key.hashCode() and 0x7FFFFFFF) % SonoraCardGradients.size]

/** Modelo de imagen para cualquier elemento de la biblioteca local. */
fun LocalItem.sonoraThumbnailModel(px: Int): Any? = when (this) {
    is Song -> song.getThumbnailModel(px, px)
    is Playlist -> thumbnails.firstOrNull()?.let { getThumbnailModel(it, px, px) }
    else -> thumbnailUrl?.let { getThumbnailModel(it, px, px) }
}

fun LocalItem.sonoraSubtitle(): String = when (this) {
    is Song -> artists.joinToString { it.name }
    is Album -> artists.joinToString { it.name }
    is Artist -> ""
    is Playlist -> "$songCount"
    else -> ""
}

@Composable
private fun dpToPx(size: Dp): Int = with(LocalDensity.current) { (size.value * density).roundToInt() }

@Composable
fun SonoraArtwork(
    model: Any?,
    size: Dp,
    shape: Shape = RoundedCornerShape(6.dp),
    placeholder: ImageVector = Icons.Rounded.MusicNote,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(SonoraColors.CardHigh)
    ) {
        Icon(
            imageVector = placeholder,
            contentDescription = null,
            tint = SonoraColors.TextMuted,
            modifier = Modifier.size(size / 2.5f)
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Título de sección grande y en negrita. */
@Composable
fun SonoraSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
        Text(
            text = title,
            style = SonoraSectionTitle,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Tarjeta compacta de acceso rápido (grilla de 2 columnas en Inicio). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickAccessTile(
    title: String,
    model: Any?,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SonoraColors.CardHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        SonoraArtwork(
            model = model,
            size = 56.dp,
            shape = if (circle) CircleShape else RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
        )
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        if (isActive) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp)
            )
        }
    }
}

/** Tarjeta vertical con portada grande (carruseles horizontales). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaCard(
    title: String,
    subtitle: String?,
    model: Any?,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    circle: Boolean = false,
    isActive: Boolean = false,
) {
    Column(
        horizontalAlignment = if (circle) Alignment.CenterHorizontally else Alignment.Start,
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(6.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        SonoraArtwork(
            model = model,
            size = size,
            shape = if (circle) CircleShape else RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = SonoraColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tarjeta de mezcla con degradado y título grande (portada generada, sin imágenes externas). */
@Composable
fun MixCard(
    title: String,
    subtitle: String,
    colors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
) {
    Column(
        modifier = modifier
            .width(size)
            .clip(RoundedCornerShape(6.dp))
            .combinedClickableCompat(onClick)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.linearGradient(colors))
        ) {
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.55f)
                    .padding(8.dp)
            )
            Text(
                text = title,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = SonoraColors.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Tarjeta de categoría para la pantalla Buscar (título + icono girado en la esquina). */
@Composable
fun CategoryCard(
    title: String,
    colors: List<Color>,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.linearGradient(colors))
            .combinedClickableCompat(onClick)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .fillMaxWidth(0.7f)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
                .size(52.dp)
                .rotate(20f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableCompat(onClick: () -> Unit): Modifier =
    this.combinedClickable(onClick = onClick)

val SonoraHorizontalSpacing = Arrangement.spacedBy(12.dp)
