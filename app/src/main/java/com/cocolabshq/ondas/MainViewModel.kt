package com.cocolabshq.ondas

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cocolabshq.ondas.data.AppSettings
import com.cocolabshq.ondas.data.ColorSlot
import com.cocolabshq.ondas.data.FilterOption
import com.cocolabshq.ondas.data.Station
import com.cocolabshq.ondas.data.StationQuery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.cocolabshq.ondas.update.UpdateChecker
import com.cocolabshq.ondas.update.UpdateState
import java.io.File

data class DiscoverState(
    val query: StationQuery = StationQuery(),
    val items: List<Station> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ondas = app as OndasApp
    private val repo = ondas.repo
    private val settingsRepo = ondas.settings
    val player = ondas.player

    // null = todavía cargando (evita un "parpadeo" con el tema por defecto)
    val settings: StateFlow<AppSettings?> =
        settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val favorites = repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recents = repo.recents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteIds: StateFlow<Set<String>> =
        favorites.map { l -> l.map { it.id }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ---- Descubrir ----
    private val _discover = MutableStateFlow(DiscoverState())
    val discover = _discover.asStateFlow()
    private var searchJob: Job? = null

    private val _countries = MutableStateFlow<List<FilterOption>>(emptyList())
    private val _tags = MutableStateFlow<List<FilterOption>>(emptyList())
    private val _languages = MutableStateFlow<List<FilterOption>>(emptyList())
    val countries = _countries.asStateFlow()
    val tags = _tags.asStateFlow()
    val languages = _languages.asStateFlow()

    init {
        updateQuery({ it })
        ensureFilters()
    }

    fun updateQuery(transform: (StationQuery) -> StationQuery, debounce: Boolean = false) {
        val q = transform(_discover.value.query)
        _discover.update { it.copy(query = q) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(400)
            load(reset = true)
        }
    }

    fun clearFilters() = updateQuery({ StationQuery() })

    fun loadMore() {
        val s = _discover.value
        if (s.loading || s.endReached || s.items.isEmpty()) return
        searchJob = viewModelScope.launch { load(reset = false) }
    }

    fun retry() {
        if (_discover.value.items.isEmpty()) updateQuery({ it }) else loadMore()
    }

    private suspend fun load(reset: Boolean) {
        val before = _discover.value
        val offset = if (reset) 0 else before.items.size
        _discover.update {
            it.copy(
                loading = true,
                error = null,
                items = if (reset) emptyList() else it.items,
                endReached = if (reset) false else it.endReached,
            )
        }
        try {
            val page = repo.api.search(before.query, offset, PAGE)
            _discover.update {
                it.copy(items = (it.items + page).distinctBy { s -> s.id }, endReached = page.size < PAGE, loading = false)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _discover.update { it.copy(error = "No se pudo cargar. Revisa tu conexión.", loading = false) }
        }
    }

    fun ensureFilters() {
        if (_countries.value.isEmpty()) viewModelScope.launch { runCatching { repo.api.countries() }.onSuccess { _countries.value = it } }
        if (_tags.value.isEmpty()) viewModelScope.launch { runCatching { repo.api.tags() }.onSuccess { _tags.value = it } }
        if (_languages.value.isEmpty()) viewModelScope.launch { runCatching { repo.api.languages() }.onSuccess { _languages.value = it } }
    }

    // ---- Acciones ----
    fun play(station: Station, list: List<Station>) = player.play(station, list)

    fun toggleFavorite(station: Station) {
        val isFav = station.id in favoriteIds.value
        viewModelScope.launch { repo.setFavorite(station, !isFav) }
    }

    fun clearRecents() = viewModelScope.launch { repo.clearRecents() }

    fun setTheme(id: String) = viewModelScope.launch { settingsRepo.setTheme(id) }
    fun setVeil(v: Float) = viewModelScope.launch { settingsRepo.setVeil(v) }
    fun setHomeCountry(code: String) = viewModelScope.launch { settingsRepo.setHomeCountry(code) }
    fun setCustomColor(slot: ColorSlot, argb: Int) = viewModelScope.launch { settingsRepo.setCustomColor(slot, argb) }
    fun setTextMode(mode: String) = viewModelScope.launch { settingsRepo.setTextMode(mode) }
    fun clearWallpaper() = viewModelScope.launch { settingsRepo.setWallpaper(null) }
    fun setWallpaper(uri: Uri) = viewModelScope.launch {
        settingsRepo.importWallpaper(uri)?.let { settingsRepo.setWallpaper(it) }
    }

    // ---- Actualizaciones (GitHub Releases) ----
    private val checker = UpdateChecker()
    private val _update = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val update = _update.asStateFlow()
    private var downloadJob: Job? = null

    /** manual = true cuando el usuario toca "Buscar actualizaciones" (ignora "Más tarde" y avisa si ya está al día). */
    fun checkForUpdates(manual: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (!manual) {
                val (last, snooze) = settingsRepo.updateTimes()
                if (now < snooze || now - last < SIX_HOURS) return@launch
            } else {
                _update.value = UpdateState.Checking
            }
            try {
                val info = checker.check(BuildConfig.VERSION_NAME)
                settingsRepo.setLastCheck(now)
                _update.value = when {
                    info != null -> UpdateState.Available(info)
                    manual -> UpdateState.UpToDate
                    else -> UpdateState.Idle
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _update.value = if (manual) UpdateState.Error("No se pudo buscar actualizaciones. Revisa tu conexión.") else UpdateState.Idle
            }
        }
    }

    /** "Más tarde": no vuelve a avisar durante 24 horas. */
    fun snoozeUpdate() {
        _update.value = UpdateState.Idle
        viewModelScope.launch { settingsRepo.setSnoozeUntil(System.currentTimeMillis() + DAY) }
    }

    fun downloadUpdate() {
        val info = (_update.value as? UpdateState.Available)?.info ?: return
        _update.value = UpdateState.Downloading(info, 0f)
        downloadJob = viewModelScope.launch {
            try {
                val file = checker.download(info, File(ondas.cacheDir, "updates")) { p ->
                    _update.value = UpdateState.Downloading(info, p)
                }
                _update.value = UpdateState.Ready(info, file)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _update.value = UpdateState.Error("La descarga falló. Inténtalo de nuevo más tarde.")
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        _update.value = UpdateState.Idle
    }

    fun dismissUpdateMessage() {
        _update.value = UpdateState.Idle
    }

    private companion object {
        const val PAGE = 40
        const val SIX_HOURS = 6 * 60 * 60 * 1000L
        const val DAY = 24 * 60 * 60 * 1000L
    }
}
