package com.cocolabshq.ondas.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Emisora. Es a la vez el modelo de red/UI y la fila de Room (favoritas y recientes). */
@Entity(tableName = "stations")
data class Station(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val favicon: String = "",
    val homepage: String = "",
    val country: String = "",
    val countryCode: String = "",
    val tags: String = "",
    val language: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long = 0L,
)

fun Station.tagList(): List<String> = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

fun flagEmoji(code: String): String =
    if (code.length != 2 || !code.all { it.isLetter() }) "🌐"
    else code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

fun String.capitalizeFirst(): String = replaceFirstChar { it.uppercase() }
