package com.cocolabshq.ondas.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cocolabshq.ondas.data.Station
import com.cocolabshq.ondas.data.StationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val trackTitle: String? = null,
    val error: String? = null,
    val sleepMinutesLeft: Int? = null,
    val volume: Float = 1f,
)

/** Puente entre la UI y PlaybackService (vía MediaController). */
class PlayerController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val repo: StationRepository,
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var queue: List<Station> = emptyList()
    private var pending: (() -> Unit)? = null
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    error = if (playbackState == Player.STATE_READY) null else it.error,
                )
            }
        }

        // Muchas radios envían el tema actual (ICY metadata) como título.
        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val t = mediaMetadata.title?.toString()
            _state.update { s -> s.copy(trackTitle = t?.takeIf { it.isNotBlank() && it != s.station?.name }) }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update { it.copy(error = "No se pudo reproducir esta emisora", isBuffering = false, isPlaying = false) }
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching { future.get() }.onSuccess { c ->
                controller = c
                c.addListener(listener)
                _state.update { it.copy(isPlaying = c.isPlaying, volume = c.volume) }
                val id = c.currentMediaItem?.mediaId
                if (!id.isNullOrEmpty() && _state.value.station == null) {
                    scope.launch { repo.byId(id)?.let { s -> _state.update { it.copy(station = s) } } }
                }
                pending?.invoke()
                pending = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(station: Station, list: List<Station> = listOf(station)) {
        queue = list
        _state.update { it.copy(station = station, error = null, trackTitle = null, isBuffering = true) }
        val start: () -> Unit = {
            controller?.let { c ->
                val item = MediaItem.Builder()
                    .setMediaId(station.id)
                    .setUri(station.streamUrl)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(station.name)
                            .setArtist(station.country)
                            .setArtworkUri(station.favicon.takeIf { it.isNotBlank() }?.let(Uri::parse))
                            .build(),
                    )
                    .build()
                c.setMediaItem(item)
                c.prepare()
                c.play()
            }
        }
        if (controller == null) pending = start else start()
        scope.launch {
            repo.markPlayed(station)
            repo.api.registerClick(station.id)
        }
    }

    fun toggle() {
        val c = controller ?: return
        when {
            c.playbackState == Player.STATE_IDLE -> { c.prepare(); c.play() }
            c.isPlaying -> c.pause()
            else -> {
                if (c.isCurrentMediaItemLive) c.seekToDefaultPosition() // volver al directo
                c.play()
            }
        }
    }

    fun next() = step(+1)
    fun previous() = step(-1)

    private fun step(delta: Int) {
        val cur = _state.value.station ?: return
        if (queue.size < 2) return
        val i = queue.indexOfFirst { it.id == cur.id }
        play(queue[(i + delta).mod(queue.size)], queue)
    }

    fun setVolume(v: Float) {
        controller?.volume = v
        _state.update { it.copy(volume = v) }
    }

    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        _state.update { it.copy(sleepMinutesLeft = minutes) }
        if (minutes == null) return
        sleepJob = scope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000)
                left--
                _state.update { it.copy(sleepMinutesLeft = left) }
            }
            controller?.pause()
            _state.update { it.copy(sleepMinutesLeft = null) }
        }
    }
}
