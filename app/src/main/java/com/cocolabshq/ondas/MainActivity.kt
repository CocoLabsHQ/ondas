package com.cocolabshq.ondas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cocolabshq.ondas.ui.MainScaffold
import com.cocolabshq.ondas.ui.theme.AppBackground
import com.cocolabshq.ondas.ui.theme.OndasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { OndasRoot() }
    }
}

@Composable
private fun OndasRoot(vm: MainViewModel = viewModel()) {
    val loaded by vm.settings.collectAsStateWithLifecycle()
    val settings = loaded
    if (settings == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0E0A24)))
        return
    }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    OndasTheme(settings.themeId, settings.custom) {
        AppBackground(settings.wallpaperPath, settings.wallpaperVeil) {
            MainScaffold(vm, settings)
        }
    }
}
