package com.cocolabshq.ondas.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {
    @Query("SELECT * FROM stations WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE")
    fun favorites(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE lastPlayedAt > 0 ORDER BY lastPlayedAt DESC LIMIT 60")
    fun recents(): Flow<List<Station>>

    @Query("SELECT * FROM stations WHERE id = :id")
    suspend fun byId(id: String): Station?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(station: Station)

    @Query("UPDATE stations SET isFavorite = :fav WHERE id = :id")
    suspend fun setFavorite(id: String, fav: Boolean)

    @Query("UPDATE stations SET lastPlayedAt = :time WHERE id = :id")
    suspend fun setPlayed(id: String, time: Long)

    @Query("UPDATE stations SET lastPlayedAt = 0")
    suspend fun resetRecents()

    @Query("DELETE FROM stations WHERE isFavorite = 0 AND lastPlayedAt = 0")
    suspend fun purge()
}

// IMPORTANTE al actualizar: si cambias las columnas de Station, sube `version`
// y agrega una Migration (si no, los favoritos de los usuarios se perderían).
@Database(entities = [Station::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stations(): StationDao
}
