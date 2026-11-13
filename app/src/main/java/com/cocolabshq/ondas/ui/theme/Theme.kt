package com.cocolabshq.ondas.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import java.io.File

/** Definición de un tema. Para agregar uno nuevo basta con sumar una línea a [Themes.all]. */
data class ThemeDef(
    val id: String,
    val name: String,
    val dark: Boolean,
    val primary: Color,
    val secondary: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val text: Color,
    val dynamic: Boolean = false,
)

object Themes {
    val all = listOf(
        ThemeDef("indie_wave", "Indie Wave", true, Color(0xFFFF7AC6), Color(0xFFFFB347), Color(0xFF2A0F55), Color(0xFF0E0A24), Color(0xFF2A1858), Color(0xFFF5EBFF)),
        ThemeDef("medianoche", "Medianoche", true, Color(0xFF8AB4FF), Color(0xFF7EE0C3), Color(0xFF0F1530), Color(0xFF080B1A), Color(0xFF1B2347), Color(0xFFE8ECFF)),
        ThemeDef("amoled", "AMOLED", true, Color(0xFFB388FF), Color(0xFF64FFDA), Color(0xFF000000), Color(0xFF000000), Color(0xFF141414), Color(0xFFEDEDED)),
        ThemeDef("cafe_jazz", "Café Jazz", true, Color(0xFFE0A15C), Color(0xFFC97B5A), Color(0xFF2B1810), Color(0xFF150B07), Color(0xFF3D2519), Color(0xFFF7E8D8)),
        ThemeDef("bosque", "Bosque", true, Color(0xFF7FD48A), Color(0xFFC8E26B), Color(0xFF0F2A1E), Color(0xFF07150F), Color(0xFF1A3D2C), Color(0xFFE4F5E6)),
        ThemeDef("oceano", "Océano", true, Color(0xFF4DD0E1), Color(0xFFFFCA6B), Color(0xFF08303F), Color(0xFF041620), Color(0xFF0F4357), Color(0xFFE0F7FA)),
        ThemeDef("papel", "Papel", false, Color(0xFF6B4EFF), Color(0xFFE8590C), Color(0xFFFBF6EE), Color(0xFFEFE6D6), Color(0xFFFFFFFF), Color(0xFF2A2320)),
        ThemeDef("atardecer", "Atardecer", false, Color(0xFFD6336C), Color(0xFFF08C00), Color(0xFFFFE9D6), Color(0xFFFFD1C2), Color(0xFFFFF7F1), Color(0xFF3B1E1E)),
        ThemeDef("dynamic", "Dinámico", true, Color(0xFF6750A4), Color(0xFF625B71), Color(0xFF1C1B1F), Color(0xFF141218), Color(0xFF2B2930), Color(0xFFE6E1E5), dynamic = true),
    )

    /** "Dinámico" (Material You) solo existe desde Android 12. */
    val available get() = all.filter { !it.dynamic || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }

    fun byId(id: String) = available.firstOrNull { it.id == id } ?: all.first()
}

private fun ThemeDef.toScheme(): ColorScheme {
    val onPrimary = if (primary.luminance() > 0.5f) Color(0xFF1A1020) else Color.White
    val container = lerp(surface, primary, 0.28f)
    val variant = lerp(surface, text, 0.08f)
    return if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = container, onPrimaryContainer = text,
            secondary = secondary, onSecondary = onPrimary, secondaryContainer = container, onSecondaryContainer = text,
            background = bgBottom, onBackground = text, surface = surface, onSurface = text,
            surfaceVariant = variant, onSurfaceVariant = text.copy(alpha = 0.7f), outline = text.copy(alpha = 0.4f),
            surfaceContainerLow = surface, surfaceContainer = surface,
            surfaceContainerHigh = variant, surfaceContainerHighest = variant,
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary, primaryContainer = container, onPrimaryContainer = text,
            secondary = secondary, onSecondary = onPrimary, secondaryContainer = container, onSecondaryContainer = text,
            background = bgBottom, onBackground = text, surface = surface, onSurface = text,
            surfaceVariant = variant, onSurfaceVariant = text.copy(alpha = 0.7f), outline = text.copy(alpha = 0.4f),
            surfaceContainerLow = surface, surfaceContainer = surface,
            surfaceContainerHigh = variant, surfaceContainerHighest = variant,
        )
    }
}

val LocalGradient = staticCompositionLocalOf { listOf(Color.Black, Color.Black) }

@Composable
fun OndasTheme(themeId: String, content: @Composable () -> Unit) {
    val def = Themes.byId(themeId)
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val useDynamic = def.dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val scheme = when {
        useDynamic && systemDark -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> def.toScheme()
    }
    val dark = if (useDynamic) systemDark else def.dark
    val gradient = if (useDynamic) listOf(scheme.background, scheme.surfaceVariant) else listOf(def.bgTop, def.bgBottom)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalGradient provides gradient) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}

/** Fondo: foto de galería con "velo" del color del tema, o degradado del tema. */
@Composable
fun AppBackground(wallpaperPath: String?, veil: Float, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (wallpaperPath != null) {
            AsyncImage(
                model = File(wallpaperPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = veil)))
        } else {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(LocalGradient.current)))
        }
        content()
    }
}
