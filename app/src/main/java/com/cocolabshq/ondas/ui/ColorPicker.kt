package com.cocolabshq.ondas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val presets = listOf(
    0xFFFF7AC6, 0xFFFF5252, 0xFFFF8A3D, 0xFFFFB347, 0xFFFFD54F, 0xFFC8E26B,
    0xFF7FD48A, 0xFF4DD0A8, 0xFF4DD0E1, 0xFF64B5F6, 0xFF8AB4FF, 0xFFB388FF,
    0xFFE040FB, 0xFFF06292, 0xFF8D6E63, 0xFF90A4AE, 0xFFFFFFFF, 0xFFBDBDBD,
    0xFF2A1858, 0xFF0F1530, 0xFF0F2A1E, 0xFF3D2519, 0xFF1A1A1A, 0xFF000000,
).map { Color(it) }

/** Selector de color: paleta rápida + matiz / intensidad / brillo. */
@Composable
fun ColorPickerDialog(title: String, initial: Color, onDismiss: () -> Unit, onPick: (Color) -> Unit) {
    val start = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(start[0]) }
    var s by remember { mutableFloatStateOf(start[1]) }
    var v by remember { mutableFloatStateOf(start[2]) }
    val current = Color.hsv(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))

    fun setFrom(c: Color) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(c.toArgb(), hsv)
        h = hsv[0]; s = hsv[1]; v = hsv[2]
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).background(current)
                            .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), CircleShape),
                    )
                    Text(String.format("#%06X", 0xFFFFFF and current.toArgb()), style = MaterialTheme.typography.titleMedium)
                }
                presets.chunked(6).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { c ->
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(c)
                                    .border(
                                        if (c.toArgb() == current.toArgb()) 3.dp else 1.dp,
                                        if (c.toArgb() == current.toArgb()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        CircleShape,
                                    )
                                    .clickable { setFrom(c) },
                            )
                        }
                    }
                }
                Text("Matiz", style = MaterialTheme.typography.labelMedium)
                GradientSlider(h / 360f, Brush.horizontalGradient((0..6).map { Color.hsv(minOf(it * 60f, 359f), 1f, 1f) })) { h = it * 360f }
                Text("Intensidad", style = MaterialTheme.typography.labelMedium)
                GradientSlider(s, Brush.horizontalGradient(listOf(Color.hsv(h.coerceIn(0f, 360f), 0f, v), Color.hsv(h.coerceIn(0f, 360f), 1f, v)))) { s = it }
                Text("Brillo", style = MaterialTheme.typography.labelMedium)
                GradientSlider(v, Brush.horizontalGradient(listOf(Color.Black, Color.hsv(h.coerceIn(0f, 360f), s, 1f)))) { v = it }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(current) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun GradientSlider(value: Float, brush: Brush, onChange: (Float) -> Unit) {
    var width by remember { mutableFloatStateOf(1f) }
    val update by rememberUpdatedState(onChange)
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(brush)
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { update((it.x / width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> update((change.position.x / width).coerceIn(0f, 1f)) }
            },
    ) {
        Box(
            Modifier
                .offset { IntOffset((value.coerceIn(0f, 1f) * width).roundToInt() - 14.dp.roundToPx(), 0) }
                .size(28.dp)
                .border(3.dp, Color.White, CircleShape),
        )
    }
}
