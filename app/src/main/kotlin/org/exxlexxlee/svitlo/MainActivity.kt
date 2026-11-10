package org.exxlexxlee.svitlo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import org.exxlexxlee.svitlo.telemetry.ScreenTracker
import org.exxlexxlee.svitlo.telemetry.TrackNavigation
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import org.exxlexxlee.svitlo.navigation.AppNavHost
import org.exxlexxlee.svitlo.ui.common.LocalNavController
import org.exxlexxlee.svitlo.ui.theme.SvitloTheme
import org.exxlexxlee.svitlo.update.PlayAppUpdater
import org.koin.androidx.compose.koinViewModel
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val screenTracker: ScreenTracker by inject()
    private val appUpdater by lazy {
        PlayAppUpdater(AppUpdateManagerFactory.create(this))
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) appUpdater.checkForPendingInstall()
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ -> appUpdater.checkForUpdate(updateLauncher) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent {
            val appViewModel: AppViewModel = koinViewModel()
            val isDarkTheme by appViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            TrackNavigation(navController, screenTracker)
            val updateSnackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(appUpdater.isUpdateDownloaded) {
                if (appUpdater.isUpdateDownloaded) {
                    val result = updateSnackbarHostState.showSnackbar(
                        message = getString(R.string.update_downloaded),
                        actionLabel = getString(R.string.update_install),
                        withDismissAction = true,
                        duration = SnackbarDuration.Indefinite,
                    )
                    if (result == SnackbarResult.ActionPerformed) appUpdater.completeUpdate()
                }
            }

            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    ) { isDarkTheme },
                )
            }

            SvitloTheme(darkTheme = isDarkTheme) {
                CompositionLocalProvider(LocalNavController provides navController) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.fillMaxSize(),
                        )
                        SnackbarHost(
                            hostState = updateSnackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .safeDrawingPadding(),
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdater.checkForPendingInstall()
    }

    override fun onDestroy() {
        appUpdater.dispose()
        super.onDestroy()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        appUpdater.checkForUpdate(updateLauncher)
    }
}
