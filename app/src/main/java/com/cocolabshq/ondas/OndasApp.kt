package com.cocolabshq.ondas

import android.app.Application
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
class OndasApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val db by lazy { Room.databaseBuilder(this, AppDatabase::class.java, "ondas.db").build() }

    val repo by lazy { StationRepository(db.stations(), RadioApi()) }
    val settings by lazy { SettingsRepository(this) }
    val player by lazy { PlayerController(this, appScope, repo) }
}
