package com.cocolabshq.ondas.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class StationRepository(private val dao: StationDao, val api: RadioApi) {
    val favorites = dao.favorites()
    val recents = dao.recents()

    suspend fun byId(id: String) = dao.byId(id)

    suspend fun setFavorite(station: Station, fav: Boolean) {
        dao.insertIgnore(station)
        dao.setFavorite(station.id, fav)
        if (!fav) dao.purge()
    }

    suspend fun markPlayed(station: Station) {
        dao.insertIgnore(station)
        dao.setPlayed(station.id, System.currentTimeMillis())
    }

    suspend fun clearRecents() {
        dao.resetRecents()
        dao.purge()
    }
}

/** Colores del tema "Personalizado" (ARGB). El color del texto se calcula solo para tener buen contraste. */
data class CustomColors(
    val primary: Int = 0xFFFF7AC6.toInt(),
    val secondary: Int = 0xFFFFB347.toInt(),
    val bgTop: Int = 0xFF2A0F55.toInt(),
    val bgBottom: Int = 0xFF0E0A24.toInt(),
    val surface: Int = 0xFF2A1858.toInt(),
    val textMode: String = "auto", // auto | light | dark
)

enum class ColorSlot { PRIMARY, SECONDARY, BG_TOP, BG_BOTTOM, SURFACE }

data class AppSettings(
    val themeId: String = "indie_wave",
    val wallpaperPath: String? = null,
    val wallpaperVeil: Float = 0.45f,
    val homeCountry: String = "AR",
    val custom: CustomColors = CustomColors(),
)

private val Context.dataStore by preferencesDataStore(name = "ondas_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val wallpaper = stringPreferencesKey("wallpaper")
        val veil = floatPreferencesKey("veil")
        val country = stringPreferencesKey("home_country")
        val cPrimary = intPreferencesKey("c_primary")
        val cSecondary = intPreferencesKey("c_secondary")
        val cBgTop = intPreferencesKey("c_bg_top")
        val cBgBottom = intPreferencesKey("c_bg_bottom")
        val cSurface = intPreferencesKey("c_surface")
        val cText = stringPreferencesKey("c_text")
        val lastCheck = longPreferencesKey("update_last_check")
        val snoozeUntil = longPreferencesKey("update_snooze_until")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        val d = CustomColors()
        AppSettings(
            themeId = p[K.theme] ?: "indie_wave",
            wallpaperPath = p[K.wallpaper]?.takeIf { File(it).exists() },
            wallpaperVeil = p[K.veil] ?: 0.45f,
            homeCountry = p[K.country] ?: "AR",
            custom = CustomColors(
                primary = p[K.cPrimary] ?: d.primary,
                secondary = p[K.cSecondary] ?: d.secondary,
                bgTop = p[K.cBgTop] ?: d.bgTop,
                bgBottom = p[K.cBgBottom] ?: d.bgBottom,
                surface = p[K.cSurface] ?: d.surface,
                textMode = p[K.cText] ?: d.textMode,
            ),
        )
    }

    suspend fun setTheme(id: String) = context.dataStore.edit { it[K.theme] = id }
    suspend fun setVeil(v: Float) = context.dataStore.edit { it[K.veil] = v }
    suspend fun setHomeCountry(code: String) = context.dataStore.edit { it[K.country] = code }
    suspend fun setWallpaper(path: String?) = context.dataStore.edit {
        if (path == null) it.remove(K.wallpaper) else it[K.wallpaper] = path
    }

    /** Cambiar cualquier color activa automáticamente el tema "Personalizado". */
    suspend fun setCustomColor(slot: ColorSlot, argb: Int) = context.dataStore.edit {
        val key = when (slot) {
            ColorSlot.PRIMARY -> K.cPrimary
            ColorSlot.SECONDARY -> K.cSecondary
            ColorSlot.BG_TOP -> K.cBgTop
            ColorSlot.BG_BOTTOM -> K.cBgBottom
            ColorSlot.SURFACE -> K.cSurface
        }
        it[key] = argb
        it[K.theme] = "custom"
    }

    suspend fun setTextMode(mode: String) = context.dataStore.edit {
        it[K.cText] = mode
        it[K.theme] = "custom"
    }

    // ---- Control de avisos de actualización ----
    suspend fun updateTimes(): Pair<Long, Long> {
        val p = context.dataStore.data.first()
        return (p[K.lastCheck] ?: 0L) to (p[K.snoozeUntil] ?: 0L)
    }

    suspend fun setLastCheck(t: Long) = context.dataStore.edit { it[K.lastCheck] = t }
    suspend fun setSnoozeUntil(t: Long) = context.dataStore.edit { it[K.snoozeUntil] = t }

    /** Copia la foto elegida a almacenamiento privado para no depender de permisos de la galería. */
    suspend fun importWallpaper(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.filesDir.listFiles()?.filter { it.name.startsWith("wallpaper_") }?.forEach { it.delete() }
            val out = File(context.filesDir, "wallpaper_${System.currentTimeMillis()}.img")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out.absolutePath
        }.getOrNull()
    }
}
