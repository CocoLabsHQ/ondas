@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cocolabshq.ondas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cocolabshq.ondas.BuildConfig
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.data.AppSettings
import com.cocolabshq.ondas.data.ColorSlot
import com.cocolabshq.ondas.data.CustomColors
import com.cocolabshq.ondas.ui.theme.ThemeDef
import com.cocolabshq.ondas.ui.theme.Themes
import com.cocolabshq.ondas.update.UpdateState
import java.io.File

private class SlotRow(val slot: ColorSlot, val label: String, val get: (CustomColors) -> Int)

private val slotRows = listOf(
    SlotRow(ColorSlot.PRIMARY, "Color principal (botones y acentos)") { it.primary },
    SlotRow(ColorSlot.SECONDARY, "Color secundario") { it.secondary },
    SlotRow(ColorSlot.BG_TOP, "Fondo (arriba)") { it.bgTop },
    SlotRow(ColorSlot.BG_BOTTOM, "Fondo (abajo)") { it.bgBottom },
    SlotRow(ColorSlot.SURFACE, "Tarjetas y barras") { it.surface },
)

@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings) {
    val countries by vm.countries.collectAsStateWithLifecycle()
    val update by vm.update.collectAsStateWithLifecycle()
    var countrySheet by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<SlotRow?>(null) }
    var veil by remember(settings.wallpaperVeil) { mutableFloatStateOf(settings.wallpaperVeil) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setWallpaper(uri)
    }

    val tiles = Themes.available + Themes.custom(settings.custom)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) }

        item { SectionTitle("Tema") }
        items(tiles.chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { def ->
                    ThemeTile(def, def.id == settings.themeId, { vm.setTheme(def.id) }, Modifier.weight(1f))
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }

        item { SectionTitle("Personalizar colores") }
        item {
            SettingsCard {
                val preview = remember(settings.custom) { Themes.custom(settings.custom) }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Vista previa en vivo (el texto se ajusta solo para contrastar)
                    Box(
                        Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(preview.bgTop, preview.bgBottom)))
                            .padding(12.dp),
                    ) {
                        Column(Modifier.align(Alignment.TopStart)) {
                            Text("Así se ve tu tema", color = preview.text, fontWeight = FontWeight.Bold)
                            Text("Texto sobre el fondo", color = preview.text.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                        }
                        Row(
                            Modifier.align(Alignment.BottomStart).clip(RoundedCornerShape(12.dp)).background(preview.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(preview.primary))
                            Box(Modifier.size(14.dp).clip(CircleShape).background(preview.secondary))
                            Text("Tarjeta", color = preview.onSurface, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text("Toca un color para elegirlo de la paleta. Al cambiarlo se activa el tema Personalizado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                slotRows.forEach { row ->
                    ListItem(
                        headlineContent = { Text(row.label) },
                        trailingContent = {
                            Box(
                                Modifier.size(32.dp).clip(CircleShape).background(Color(row.get(settings.custom)))
                                    .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape),
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { picking = row },
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Color del texto", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("auto" to "Automático", "light" to "Claro", "dark" to "Oscuro").forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.custom.textMode == mode,
                                onClick = { vm.setTextMode(mode) },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            }
        }

        item { SectionTitle("Fondo de pantalla") }
        item {
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.wallpaperPath != null) {
                        AsyncImage(
                            model = File(settings.wallpaperPath),
                            contentDescription = "Fondo actual",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Icon(Icons.Rounded.Image, null, Modifier.size(18.dp))
                            Text(if (settings.wallpaperPath == null) "  Elegir foto" else "  Cambiar foto")
                        }
                        if (settings.wallpaperPath != null) {
                            OutlinedButton(onClick = { vm.clearWallpaper() }) { Text("Quitar") }
                        }
                    }
                    if (settings.wallpaperPath != null) {
                        Text("Velo sobre la foto (mejora la legibilidad)", style = MaterialTheme.typography.bodySmall)
                        Slider(value = veil, onValueChange = { veil = it }, onValueChangeFinished = { vm.setVeil(veil) }, valueRange = 0f..0.9f)
                    }
                }
            }
        }

        item { SectionTitle("Contenido") }
        item {
            SettingsCard {
                val name = countries.firstOrNull { it.value == settings.homeCountry }?.label ?: settings.homeCountry
                ListItem(
                    headlineContent = { Text("Mi país") },
                    supportingContent = { Text("$name · atajo \"Mi país\" en Descubrir") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { vm.ensureFilters(); countrySheet = true },
                )
                ListItem(
                    headlineContent = { Text("Borrar historial de recientes") },
                    leadingContent = { Icon(Icons.Rounded.DeleteOutline, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { confirmClear = true },
                )
            }
        }

        item { SectionTitle("Actualizaciones") }
        item {
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Buscar actualizaciones") },
                    supportingContent = {
                        Text(if (update is UpdateState.Checking) "Buscando…" else "Versión instalada: ${BuildConfig.VERSION_NAME}")
                    },
                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { vm.checkForUpdates(manual = true) },
                )
            }
        }

        item { SectionTitle("Acerca de") }
        item {
            SettingsCard {
                ListItem(
                    headlineContent = { Text("Ondas ${BuildConfig.VERSION_NAME}") },
                    supportingContent = { Text("Emisoras de todo el mundo gracias al directorio comunitario radio-browser.info") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
        }
    }

    picking?.let { row ->
        ColorPickerDialog(
            title = row.label,
            initial = Color(row.get(settings.custom)),
            onDismiss = { picking = null },
            onPick = { c -> vm.setCustomColor(row.slot, c.toArgb()); picking = null },
        )
    }
    if (countrySheet) {
        OptionSheet("Mi país", countries, settings.homeCountry,
            onPick = { o -> if (o != null) vm.setHomeCountry(o.value); countrySheet = false },
            onDismiss = { countrySheet = false })
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("¿Borrar recientes?") },
            text = { Text("Tus favoritas no se tocan.") },
            confirmButton = { TextButton(onClick = { vm.clearRecents(); confirmClear = false }) { Text("Borrar") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    // Sobre el fondo: color de acento si contrasta, si no el texto normal
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = glassColors(),
        modifier = Modifier.fillMaxWidth(),
        content = content,
    )
}

@Composable
private fun ThemeTile(def: ThemeDef, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.height(88.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(def.bgTop, def.bgBottom)))
            .border(if (selected) 3.dp else 1.dp, if (selected) def.primary else def.text.copy(alpha = 0.25f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(Modifier.align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(def.primary))
                Box(Modifier.size(12.dp).clip(CircleShape).background(def.secondary))
                Box(Modifier.size(12.dp).clip(CircleShape).background(def.surface))
            }
            Text(def.name, color = def.text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = def.primary, modifier = Modifier.align(Alignment.TopEnd))
    }
}
