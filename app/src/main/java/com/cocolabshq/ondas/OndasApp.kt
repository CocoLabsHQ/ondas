package com.cocolabshq.ondas

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.room.Room
import com.cocolabshq.ondas.data.AppDatabase
import com.cocolabshq.ondas.data.RadioApi
import com.cocolabshq.ondas.data.SettingsRepository
import com.cocolabshq.ondas.data.StationRepository
import com.cocolabshq.ondas.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Contenedor simple de dependencias (sin Hilt para mantener el proyecto liviano). */
class OndasApp : Application(), ImageLoaderFactory {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val db by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "ondas.db").build() }

    val repo by lazy { StationRepository(db.stations(), RadioApi()) }
    val settings by lazy { SettingsRepository(this) }
    val player by lazy { PlayerController(this, appScope, repo) }

    /** Muchos servidores de logos rechazan clientes sin User-Agent; además se soportan logos SVG. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .okHttpClient {
            OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) Ondas/${BuildConfig.VERSION_NAME}")
                            .build(),
                    )
                }
                .build()
        }
        .components { add(SvgDecoder.Factory()) }
        .crossfade(true)
        .build()
}
