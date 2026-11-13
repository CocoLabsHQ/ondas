package com.cocolabshq.ondas.data

import com.cocolabshq.ondas.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class StationOrder(val param: String, val label: String, val reverse: Boolean) {
    POPULAR("clickcount", "Más escuchadas", true),
    TRENDING("clicktrend", "En tendencia", true),
    VOTES("votes", "Más votadas", true),
    BITRATE("bitrate", "Mejor calidad", true),
    NAME("name", "Nombre (A-Z)", false),
}

/** Filtros de la pantalla Descubrir. Todos son opcionales y combinables. */
data class StationQuery(
    val text: String = "",
    val countryCode: String? = null,
    val tag: String? = null,
    val language: String? = null,
    val order: StationOrder = StationOrder.POPULAR,
    val minBitrate: Int = 0,
) {
    val isDefault get() = this == StationQuery()
}

data class FilterOption(val label: String, val value: String, val count: Int)

@Serializable
internal data class StationDto(
    val stationuuid: String = "",
    val name: String = "",
    val url: String = "",
    @SerialName("url_resolved") val urlResolved: String = "",
    val homepage: String = "",
    val favicon: String = "",
    val tags: String = "",
    val country: String = "",
    val countrycode: String = "",
    val language: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
)

@Serializable
internal data class CountryDto(
    val name: String = "",
    @SerialName("iso_3166_1") val iso: String = "",
    val stationcount: Int = 0,
)

@Serializable
internal data class NameCountDto(val name: String = "", val stationcount: Int = 0)

/**
 * Cliente de https://api.radio-browser.info (directorio comunitario y gratuito).
 * Si un servidor falla, prueba el siguiente.
 */
class RadioApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val hosts = listOf(
        "https://de1.api.radio-browser.info",
        "https://nl1.api.radio-browser.info",
        "https://at1.api.radio-browser.info",
    )

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        var last: Exception? = null
        for (host in hosts) {
            try {
                val req = Request.Builder()
                    .url(host + path)
                    .header("User-Agent", "Ondas/${BuildConfig.VERSION_NAME} (Android)")
                    .build()
                client.newCall(req).execute().use { res ->
                    if (res.isSuccessful) return@withContext res.body?.string().orEmpty()
                    last = IOException("HTTP ${res.code}")
                }
            } catch (e: IOException) {
                last = e
            }
        }
        throw last ?: IOException("Sin conexión")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun search(q: StationQuery, offset: Int, limit: Int): List<Station> {
        val path = buildString {
            append("/json/stations/search?hidebroken=true")
            append("&limit=$limit&offset=$offset")
            append("&order=${q.order.param}&reverse=${q.order.reverse}")
            if (q.text.isNotBlank()) append("&name=${enc(q.text.trim())}")
            q.countryCode?.let { append("&countrycode=${enc(it)}") }
            q.tag?.let { append("&tag=${enc(it)}&tagExact=true") }
            q.language?.let { append("&language=${enc(it)}&languageExact=true") }
            if (q.minBitrate > 0) append("&bitrateMin=${q.minBitrate}")
        }
        return json.decodeFromString<List<StationDto>>(get(path))
            .map {
                Station(
                    id = it.stationuuid,
                    name = it.name.trim(),
                    streamUrl = it.urlResolved.ifBlank { it.url },
                    favicon = it.favicon,
                    homepage = it.homepage,
                    country = it.country,
                    countryCode = it.countrycode,
                    tags = it.tags,
                    language = it.language,
                    codec = it.codec,
                    bitrate = it.bitrate,
                )
            }
            .filter { it.id.isNotBlank() && it.name.isNotBlank() && it.streamUrl.isNotBlank() }
    }

    suspend fun countries(): List<FilterOption> =
        json.decodeFromString<List<CountryDto>>(get("/json/countries?hidebroken=true&order=name"))
            .filter { it.iso.length == 2 && it.name.isNotBlank() }
            .map { FilterOption("${flagEmoji(it.iso)} ${it.name}", it.iso.uppercase(), it.stationcount) }

    suspend fun tags(): List<FilterOption> =
        json.decodeFromString<List<NameCountDto>>(get("/json/tags?hidebroken=true&order=stationcount&reverse=true&limit=200"))
            .filter { it.name.isNotBlank() }
            .map { FilterOption(it.name.capitalizeFirst(), it.name, it.stationcount) }

    suspend fun languages(): List<FilterOption> =
        json.decodeFromString<List<NameCountDto>>(get("/json/languages?hidebroken=true&order=stationcount&reverse=true&limit=120"))
            .filter { it.name.isNotBlank() }
            .map { FilterOption(it.name.capitalizeFirst(), it.name, it.stationcount) }

    /** Buena práctica de la API: avisar que se escuchó una emisora (alimenta el ranking). */
    suspend fun registerClick(stationId: String) {
        runCatching { get("/json/url/$stationId") }
    }
}
