package com.cocolabshq.ondas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.data.Station

@Composable
fun FavoritesScreen(vm: MainViewModel) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    StationListScreen("Favoritas", list, Icons.Rounded.FavoriteBorder, "Toca el corazón en una emisora para guardarla aquí", vm)
}

@Composable
fun RecentsScreen(vm: MainViewModel) {
    val list by vm.recents.collectAsStateWithLifecycle()
    StationListScreen("Recientes", list, Icons.Rounded.History, "Aquí aparecerán las emisoras que escuches", vm,
        action = if (list.isNotEmpty()) ({ TextButton(onClick = { vm.clearRecents() }) { Text("Borrar") } }) else null)
}

@Composable
private fun StationListScreen(
    title: String,
    stations: List<Station>,
    emptyIcon: ImageVector,
    emptyText: String,
    vm: MainViewModel,
    action: (@Composable () -> Unit)? = null,
) {
    val favIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                action?.invoke()
            }
        }
        if (stations.isEmpty()) {
            item { EmptyState(emptyIcon, emptyText) }
        } else {
            items(stations, key = { it.id }) { s ->
                StationRow(
                    station = s,
                    isFav = s.id in favIds,
                    isCurrent = player.station?.id == s.id,
                    onClick = { vm.play(s, stations) },
                    onFav = { vm.toggleFavorite(s) },
                )
            }
        }
    }
}
