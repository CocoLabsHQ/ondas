@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cocolabshq.ondas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.data.AppSettings
import com.cocolabshq.ondas.data.StationOrder
import com.cocolabshq.ondas.data.capitalizeFirst
import com.cocolabshq.ondas.data.flagEmoji

private enum class Sheet { COUNTRY, TAG, LANGUAGE }

// Etiqueta visible -> tag de radio-browser
private val quickGenres = listOf(
    "Rock" to "rock", "Pop" to "pop", "Jazz" to "jazz", "Clásica" to "classical",
    "Noticias" to "news", "Electrónica" to "electronic", "Hip hop" to "hip hop",
    "Folk" to "folk", "Cumbia" to "cumbia", "Tango" to "tango", "Reggae" to "reggae",
    "Latina" to "latin", "Charlas" to "talk", "Deportes" to "sports",
)

@Composable
fun DiscoverScreen(vm: MainViewModel, settings: AppSettings) {
    val state by vm.discover.collectAsStateWithLifecycle()
    val favIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val player by vm.player.state.collectAsStateWithLifecycle()
    val countries by vm.countries.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val languages by vm.languages.collectAsStateWithLifecycle()

    val q = state.query
    val home = settings.homeCountry
    var text by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    val gridState = rememberLazyGridState()

    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - 6
        }
    }
    LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) vm.loadMore() }
    LaunchedEffect(q) { gridState.scrollToItem(0) }

    val customCountry = q.countryCode != null && q.countryCode != home

    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        state = gridState,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "title", span = { GridItemSpan(maxLineSpan) }) {
            Text("Ondas", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    vm.updateQuery({ cur -> cur.copy(text = it) }, debounce = true)
                },
                placeholder = { Text("Buscar emisoras…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                trailingIcon = {
                    if (text.isNotEmpty()) {
                        IconButton(onClick = { text = ""; vm.updateQuery({ cur -> cur.copy(text = "") }) }) {
                            Icon(Icons.Rounded.Close, "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "filters", span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!q.isDefault || text.isNotEmpty()) {
                    item {
                        ThemedChip("Limpiar ✕", false, onClick = { text = ""; vm.clearFilters() })
                    }
                }
                item { ThemedChip("🌍 Mundo", q.countryCode == null, { vm.updateQuery({ it.copy(countryCode = null) }) }) }
                item { ThemedChip("${flagEmoji(home)} Mi país", q.countryCode == home, { vm.updateQuery({ it.copy(countryCode = home) }) }) }
                item {
                    val name = countries.firstOrNull { it.value == q.countryCode }?.label ?: q.countryCode
                    ThemedChip(if (customCountry) name.orEmpty() else "País", customCountry, { vm.ensureFilters(); sheet = Sheet.COUNTRY }, dropdown = true)
                }
                item {
                    ThemedChip(q.tag?.capitalizeFirst() ?: "Género", q.tag != null, { vm.ensureFilters(); sheet = Sheet.TAG }, dropdown = true)
                }
                item {
                    ThemedChip(q.language?.capitalizeFirst() ?: "Idioma", q.language != null, { vm.ensureFilters(); sheet = Sheet.LANGUAGE }, dropdown = true)
                }
                item {
                    DropdownChip(
                        label = q.order.label,
                        selected = q.order != StationOrder.POPULAR,
                        options = StationOrder.entries.map { o -> o.label to { vm.updateQuery({ it.copy(order = o) }) } },
                    )
                }
                item {
                    DropdownChip(
                        label = if (q.minBitrate > 0) "≥ ${q.minBitrate} kbps" else "Calidad",
                        selected = q.minBitrate > 0,
                        options = listOf(0 to "Cualquiera", 64 to "≥ 64 kbps", 128 to "≥ 128 kbps", 192 to "≥ 192 kbps")
                            .map { (v, l) -> l to { vm.updateQuery({ it.copy(minBitrate = v) }) } },
                    )
                }
            }
        }
        item(key = "genres", span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quickGenres.size) { i ->
                    val (label, tag) = quickGenres[i]
                    ThemedChip(label, q.tag == tag, { vm.updateQuery({ it.copy(tag = if (it.tag == tag) null else tag) }) })
                }
            }
        }

        items(state.items, key = { it.id }) { s ->
            StationCard(
                station = s,
                isFav = s.id in favIds,
                isCurrent = player.station?.id == s.id,
                onClick = { vm.play(s, state.items) },
                onFav = { vm.toggleFavorite(s) },
            )
        }

        if (state.loading) {
            item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp))
                }
            }
        }
        if (state.error != null) {
            item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error)
                        Button(onClick = { vm.retry() }, modifier = Modifier.padding(top = 8.dp)) { Text("Reintentar") }
                    }
                }
            }
        }
        if (!state.loading && state.error == null && state.items.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(Icons.Rounded.Radio, "No hay emisoras con esos filtros")
            }
        }
    }

    when (sheet) {
        Sheet.COUNTRY -> OptionSheet("País", countries, q.countryCode,
            onPick = { o -> vm.updateQuery({ it.copy(countryCode = o?.value) }); sheet = null },
            onDismiss = { sheet = null }, allLabel = "🌍 Todo el mundo")
        Sheet.TAG -> OptionSheet("Género", tags, q.tag,
            onPick = { o -> vm.updateQuery({ it.copy(tag = o?.value) }); sheet = null },
            onDismiss = { sheet = null }, allLabel = "Todos los géneros")
        Sheet.LANGUAGE -> OptionSheet("Idioma", languages, q.language,
            onPick = { o -> vm.updateQuery({ it.copy(language = o?.value) }); sheet = null },
            onDismiss = { sheet = null }, allLabel = "Todos los idiomas")
        null -> {}
    }
}

@Composable
private fun ThemedChip(label: String, selected: Boolean, onClick: () -> Unit, dropdown: Boolean = false) {
    val trailing: (@Composable () -> Unit)? =
        if (dropdown) ({ Icon(Icons.Rounded.ArrowDropDown, null, Modifier.size(18.dp)) }) else null
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        trailingIcon = trailing,
        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)),
    )
}

@Composable
private fun DropdownChip(label: String, selected: Boolean, options: List<Pair<String, () -> Unit>>) {
    var open by remember { mutableStateOf(false) }
    Box {
        ThemedChip(label, selected, { open = true }, dropdown = true)
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; action() })
            }
        }
    }
}
