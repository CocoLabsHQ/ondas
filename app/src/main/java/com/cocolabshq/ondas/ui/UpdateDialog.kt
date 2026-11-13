package com.cocolabshq.ondas.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.update.ApkInstaller
import com.cocolabshq.ondas.update.UpdateState
import java.util.Locale

/** Avisos de actualización: "Actualizar" o "Más tarde". */
@Composable
fun UpdateDialogs(vm: MainViewModel) {
    val state by vm.update.collectAsStateWithLifecycle()
    val ctx = LocalContext.current

    when (val s = state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = { vm.snoozeUpdate() },
            title = { Text("Nueva versión ${s.info.version}") },
            text = {
                Column {
                    if (s.info.sizeBytes > 0) {
                        Text(String.format(Locale.getDefault(), "Tamaño: %.1f MB", s.info.sizeBytes / 1_048_576.0))
                    }
                    if (s.info.notes.isNotBlank()) {
                        Text(s.info.notes, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { vm.downloadUpdate() }) { Text("Actualizar") } },
            dismissButton = { TextButton(onClick = { vm.snoozeUpdate() }) { Text("Más tarde") } },
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Descargando ${s.info.version}") },
            text = {
                Column {
                    LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(s.progress * 100).toInt()} %", modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { vm.cancelDownload() }) { Text("Cancelar") } },
        )

        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = { vm.dismissUpdateMessage() },
            title = { Text("Lista para instalar") },
            text = {
                Text(
                    "Android te va a preguntar si quieres actualizar Ondas. " +
                        "Si es la primera vez, también te pedirá permitir la instalación desde Ondas: " +
                        "actívalo, vuelve y toca Instalar otra vez.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (ApkInstaller.canInstall(ctx)) ctx.startActivity(ApkInstaller.installIntent(ctx, s.file))
                    else ctx.startActivity(ApkInstaller.unknownSourcesIntent(ctx))
                }) { Text("Instalar") }
            },
            dismissButton = { TextButton(onClick = { vm.dismissUpdateMessage() }) { Text("Más tarde") } },
        )

        UpdateState.UpToDate -> AlertDialog(
            onDismissRequest = { vm.dismissUpdateMessage() },
            title = { Text("Estás al día") },
            text = { Text("Ya tienes la última versión de Ondas.") },
            confirmButton = { TextButton(onClick = { vm.dismissUpdateMessage() }) { Text("OK") } },
        )

        is UpdateState.Error -> AlertDialog(
            onDismissRequest = { vm.dismissUpdateMessage() },
            title = { Text("No se pudo actualizar") },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = { vm.dismissUpdateMessage() }) { Text("OK") } },
        )

        UpdateState.Idle, UpdateState.Checking -> {}
    }
}
