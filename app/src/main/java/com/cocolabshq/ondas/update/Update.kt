package com.cocolabshq.ondas.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Repositorio de GitHub donde se publican los Releases con el APK. */
const val UPDATE_REPO = "CocoLabsHQ/ondas"

data class UpdateInfo(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val pageUrl: String,
    val sizeBytes: Long,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}

@Serializable
internal data class AssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
    val size: Long = 0,
)

@Serializable
internal data class ReleaseDto(
    @SerialName("tag_name") val tag: String = "",
    val body: String? = null,
    @SerialName("html_url") val htmlUrl: String = "",
    val assets: List<AssetDto> = emptyList(),
)

class UpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** Devuelve la versión nueva si existe, o null si ya tienes la última. Lanza excepción si no hay red. */
    suspend fun check(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$UPDATE_REPO/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Ondas/$currentVersion (Android)")
            .build()
        val body = client.newCall(req).execute().use { res ->
            if (res.code == 404) return@withContext null // todavía no hay releases
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
            res.body?.string().orEmpty()
        }
        val rel = json.decodeFromString<ReleaseDto>(body)
        val apk = rel.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
        if (apk == null || !isNewer(rel.tag, currentVersion)) return@withContext null
        UpdateInfo(
            version = rel.tag,
            notes = rel.body.orEmpty().replace(Regex("[#*`]+"), "").trim().take(600),
            apkUrl = apk.url,
            pageUrl = rel.htmlUrl,
            sizeBytes = apk.size,
        )
    }

    /** Descarga el APK a [dir] informando el progreso (0..1). */
    suspend fun download(info: UpdateInfo, dir: File, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "Ondas-${info.version}.apk")
            val req = Request.Builder().url(info.apkUrl).header("User-Agent", "Ondas (Android)").build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
                val body = res.body ?: throw IOException("Respuesta vacía")
                val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                body.byteStream().use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(32 * 1024)
                        var read = 0L
                        var lastReported = 0f
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            read += n
                            if (total > 0) {
                                val p = (read.toFloat() / total).coerceIn(0f, 1f)
                                if (p - lastReported >= 0.01f) { lastReported = p; onProgress(p) }
                            }
                        }
                    }
                }
            }
            file
        }

    companion object {
        /** "v0.2.0" > "0.1.0" -> true */
        fun isNewer(remote: String, local: String): Boolean {
            fun parse(v: String) = v.trim().removePrefix("v").removePrefix("V")
                .split('-', '+')[0].split('.').map { it.toIntOrNull() ?: 0 }
            val a = parse(remote)
            val b = parse(local)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}

object ApkInstaller {
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()

    /** Pantalla de Android donde se permite "instalar apps desconocidas" para Ondas. */
    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
