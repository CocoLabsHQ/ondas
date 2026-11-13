@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cocolabshq.ondas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.data.flagEmoji
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun PlayerScreen(vm: MainViewModel, onBack: () -> Unit) {
    val p by vm.player.state.collectAsStateWithLifecycle()
    val favIds by vm.favoriteIds.collectAsStateWithLifecycle()
    val st = p.station ?: return
    val ctx = LocalContext.current

    var menu by remember { mutableStateOf(false) }
    var timerDialog by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            RoundButton(Icons.Rounded.KeyboardArrowDown, "Cerrar", onBack)
            Text("AHORA SONANDO", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp)
            Box {
                RoundButton(Icons.Rounded.MoreVert, "Más opciones", { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(p.sleepMinutesLeft?.let { "Temporizador (quedan $it min)" } ?: "Temporizador de apagado") },
                        onClick = { menu = false; timerDialog = true },
                    )
                    if (st.homepage.isNotBlank()) {
                        DropdownMenuItem(
                            text = { Text("Abrir sitio web") },
                            onClick = {
                                menu = false
                                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(st.homepage))) }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        StationArt(st, Modifier.fillMaxWidth(0.7f).aspectRatio(1f).clip(RoundedCornerShape(32.dp)))
        Spacer(Modifier.height(24.dp))

        Text(st.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val meta = listOfNotNull(
                st.country.ifBlank { null }?.let { "${flagEmoji(st.countryCode)} $it" },
                listOf(st.codec, if (st.bitrate > 0) "${st.bitrate}k" else "").filter { it.isNotBlank() }.joinToString(" ").ifBlank { null },
            ).joinToString(" · ")
            Text(meta, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            LivePill()
        }
        Spacer(Modifier.height(8.dp))
        Text(
            p.error ?: p.trackTitle ?: if (p.isBuffering) "Conectando…" else " ",
            color = if (p.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.weight(1f))
        WaveForm(p.isPlaying, Modifier.fillMaxWidth().height(100.dp))
        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VolumeDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = p.volume, onValueChange = { vm.player.setVolume(it) }, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            Icon(Icons.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))

        val isFav = st.id in favIds
        Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            RoundButton(
                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (isFav) "Quitar de favoritas" else "Añadir a favoritas",
                { vm.toggleFavorite(st) },
                tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            RoundButton(Icons.Rounded.SkipPrevious, "Anterior", { vm.player.previous() })
            Box(
                Modifier.size(76.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)))
                    .clickable { vm.player.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (p.isBuffering) {
                    CircularProgressIndicator(Modifier.size(32.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                } else {
                    Icon(
                        if (p.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (p.isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            RoundButton(Icons.Rounded.SkipNext, "Siguiente", { vm.player.next() })
            RoundButton(Icons.Rounded.Share, "Compartir", {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Estoy escuchando ${st.name} en Ondas 📻\n${st.homepage.ifBlank { st.streamUrl }}")
                }
                ctx.startActivity(Intent.createChooser(send, null))
            })
        }
    }

    if (timerDialog) {
        AlertDialog(
            onDismissRequest = { timerDialog = false },
            title = { Text("Temporizador de apagado") },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { m ->
                        TextButton(onClick = { vm.player.setSleepTimer(m); timerDialog = false }) { Text("$m minutos") }
                    }
                    if (p.sleepMinutesLeft != null) {
                        TextButton(onClick = { vm.player.setSleepTimer(null); timerDialog = false }) { Text("Desactivar") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { timerDialog = false }) { Text("Cerrar") } },
        )
    }
}

@Composable
private fun RoundButton(icon: ImageVector, desc: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = desc, tint = tint) }
}

@Composable
private fun LivePill() {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFF3B30).copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
        Spacer(Modifier.width(5.dp))
        Text("EN VIVO", color = Color(0xFFFF3B30), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** Onda animada decorativa: se mueve mientras suena y se aplana en pausa. */
@Composable
private fun WaveForm(playing: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val amp by animateFloatAsState(if (playing) 1f else 0.06f, tween(600), label = "amp")
    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.secondary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2f
        val layers = listOf(Triple(1.0f, 1.0f, 1f), Triple(1.7f, 0.6f, 0.55f), Triple(2.6f, 0.35f, 0.3f))
        layers.forEachIndexed { i, (freq, a, alpha) ->
            val path = Path()
            val steps = 120
            val dir = if (i % 2 == 0) 1f else -1f
            for (s in 0..steps) {
                val nx = s / steps.toFloat()
                val env = sin(PI.toFloat() * nx)
                val arg = nx * 2f * PI.toFloat() * freq * 2f + phase * (i + 1) * dir
                val y = mid + sin(arg) * (h / 2.2f) * a * amp * env
                if (s == 0) path.moveTo(0f, y) else path.lineTo(w * nx, y)
            }
            drawPath(
                path,
                Brush.horizontalGradient(listOf(c1, c2), startX = 0f, endX = w),
                alpha = alpha,
                style = Stroke(width = (5 - i).dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
