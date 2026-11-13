package com.cocolabshq.ondas

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cocolabshq.ondas.data.AppSettings
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
    fun clearWallpaper() = viewModelScope.launch { settingsRepo.setWallpaper(null) }
    fun setWallpaper(uri: Uri) = viewModelScope.launch {
        settingsRepo.importWallpaper(uri)?.let { settingsRepo.setWallpaper(it) }
    }

    private companion object {
        const val PAGE = 40
    }
}
