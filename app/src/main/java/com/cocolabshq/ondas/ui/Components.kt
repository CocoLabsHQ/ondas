@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cocolabshq.ondas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.cocolabshq.ondas.data.FilterOption
import com.cocolabshq.ondas.data.Station
import com.cocolabshq.ondas.data.capitalizeFirst
import com.cocolabshq.ondas.data.flagEmoji
import com.cocolabshq.ondas.data.tagList
import com.cocolabshq.ondas.player.PlayerUiState
import kotlin.math.abs

private fun fallbackBrush(name: String): Brush {
    val hue = (abs(name.hashCode()) % 360).toFloat()
    return Brush.linearGradient(listOf(Color.hsv(hue, 0.55f, 0.55f), Color.hsv((hue + 45f) % 360f, 0.6f, 0.4f)))
}

/** Logo de la emisora; si no hay o falla la carga, muestra la inicial sobre un degradado. */
@Composable
fun StationArt(station: Station, modifier: Modifier = Modifier) {
    val brush = remember(station.name) { fallbackBrush(station.name) }
    Box(modifier.background(brush), contentAlignment = Alignment.Center) {
        SubcomposeAsyncImage(
            model = station.favicon.ifBlank { null },
            contentDescription = station.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp),
            loading = { Initial(station) },
            error = { Initial(station) },
        )
    }
}

@Composable
private fun Initial(station: Station) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            station.name.trim().take(1).uppercase(),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun Station.subtitle(): String =
    listOfNotNull(tagList().firstOrNull()?.capitalizeFirst(), country.ifBlank { null }).joinToString(" · ")

@Composable
fun StationCard(
    station: Station,
    isFav: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onFav: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Box {
            StationArt(station, Modifier.fillMaxWidth().aspectRatio(1f))
            Box(
                Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onFav),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFav) "Quitar de favoritas" else "Añadir a favoritas",
                    tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(Modifier.padding(10.dp)) {
            Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            Text(
                "${flagEmoji(station.countryCode)} ${station.subtitle()}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StationRow(
    station: Station,
    isFav: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onFav: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        border = if (isCurrent) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            StationArt(station, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(station.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    "${flagEmoji(station.countryCode)} ${station.subtitle()}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onFav) {
                Icon(
                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFav) "Quitar de favoritas" else "Añadir a favoritas",
                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun MiniPlayer(state: PlayerUiState, onToggle: () -> Unit, onOpen: () -> Unit) {
    val st = state.station ?: return
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 4.dp,
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StationArt(st, Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(st.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    state.error ?: state.trackTitle ?: if (state.isBuffering) "Conectando…" else "En vivo · ${st.country}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggle) {
                if (state.isBuffering) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                    )
                }
            }
        }
    }
}

/** Hoja inferior con buscador para elegir país, género o idioma. */
@Composable
fun OptionSheet(
    title: String,
    options: List<FilterOption>,
    selected: String?,
    onPick: (FilterOption?) -> Unit,
    onDismiss: () -> Unit,
    allLabel: String? = null,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        var text by remember { mutableStateOf("") }
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 20.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Buscar…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        val filtered = remember(text, options) {
            if (text.isBlank()) options else options.filter { it.label.contains(text, ignoreCase = true) }
        }
        LazyColumn(Modifier.fillMaxWidth().fillMaxHeight(0.7f)) {
            if (allLabel != null) {
                item {
                    ListItem(
                        headlineContent = { Text(allLabel) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onPick(null) },
                    )
                }
            }
            if (options.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            items(filtered) { o ->
                ListItem(
                    headlineContent = { Text(o.label) },
                    supportingContent = { Text("${o.count} emisoras") },
                    trailingContent = {
                        if (o.value == selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPick(o) },
                )
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
    }
}
