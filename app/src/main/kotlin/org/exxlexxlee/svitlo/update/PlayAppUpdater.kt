package org.exxlexxlee.svitlo.update

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class PlayAppUpdater(private val manager: AppUpdateManager) {
    var isUpdateDownloaded by mutableStateOf(false)
        private set

    private var disposed = false
    private val listener = InstallStateUpdatedListener { state ->
        if (!disposed) {
            isUpdateDownloaded = state.installStatus() == InstallStatus.DOWNLOADED
        }
    }

    init {
        manager.registerListener(listener)
    }

    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (disposed) return@addOnSuccessListener
                isUpdateDownloaded = info.installStatus() == InstallStatus.DOWNLOADED
                if (!isUpdateDownloaded &&
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.installStatus() != InstallStatus.DOWNLOADING &&
                    info.installStatus() != InstallStatus.PENDING &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    try {
                        manager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    } catch (exception: android.content.IntentSender.SendIntentException) {
                        Log.w(TAG, "Unable to start Play update", exception)
                    }
                }
            }
            .addOnFailureListener { Log.d(TAG, "Play update check unavailable", it) }
    }

    fun checkForPendingInstall() {
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (!disposed) {
                    isUpdateDownloaded = info.installStatus() == InstallStatus.DOWNLOADED
                }
            }
            .addOnFailureListener { Log.d(TAG, "Pending Play update check unavailable", it) }
    }

    fun completeUpdate() {
        isUpdateDownloaded = false
        manager.completeUpdate().addOnFailureListener {
            if (!disposed) isUpdateDownloaded = true
            Log.w(TAG, "Unable to install Play update", it)
        }
    }

    fun dispose() {
        disposed = true
        manager.unregisterListener(listener)
    }

    private companion object {
        const val TAG = "PlayAppUpdater"
    }
}
