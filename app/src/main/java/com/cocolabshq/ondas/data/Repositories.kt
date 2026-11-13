package com.cocolabshq.ondas.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

data class AppSettings(
    val themeId: String = "indie_wave",
    val wallpaperPath: String? = null,
    val wallpaperVeil: Float = 0.45f,
    val homeCountry: String = "AR",
)

private val Context.dataStore by preferencesDataStore(name = "ondas_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val theme = stringPreferencesKey("theme")
        val wallpaper = stringPreferencesKey("wallpaper")
        val veil = floatPreferencesKey("veil")
        val country = stringPreferencesKey("home_country")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeId = p[K.theme] ?: "indie_wave",
            wallpaperPath = p[K.wallpaper]?.takeIf { File(it).exists() },
            wallpaperVeil = p[K.veil] ?: 0.45f,
            homeCountry = p[K.country] ?: "AR",
        )
    }

    suspend fun setTheme(id: String) = context.dataStore.edit { it[K.theme] = id }
    suspend fun setVeil(v: Float) = context.dataStore.edit { it[K.veil] = v }
    suspend fun setHomeCountry(code: String) = context.dataStore.edit { it[K.country] = code }
    suspend fun setWallpaper(path: String?) = context.dataStore.edit {
        if (path == null) it.remove(K.wallpaper) else it[K.wallpaper] = path
    }

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
