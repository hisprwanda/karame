package org.dhis2.utils.granularsync

import androidx.fragment.app.FragmentActivity
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import org.dhis2.commons.Constants.CONFLICT_TYPE
import org.dhis2.commons.Constants.UID
import org.dhis2.commons.sync.ConflictType
import org.dhis2.commons.sync.OnDismissListener
import org.dhis2.commons.sync.OnNoConnectionListener
import org.dhis2.commons.sync.OnSyncNavigationListener
import org.dhis2.commons.sync.SyncComponentProvider
import org.dhis2.commons.sync.SyncContext
import org.dhis2.data.service.SyncGranularWorker

class SyncStatusDialogProvider : SyncComponentProvider {
    override fun showSyncStatusDialog(
        activity: FragmentActivity,
        syncContext: SyncContext,
        dismissListener: OnDismissListener?,
        onSyncNavigationListener: OnSyncNavigationListener?,
        onNoConnectionListener: OnNoConnectionListener?,
    ) {
        val syncBuilder =
            SyncStatusDialog
                .Builder()
                .withContext(activity, onSyncNavigationListener)
                .withSyncContext(syncContext)

        with(syncBuilder) {
            dismissListener?.let { onDismissListener(it) }
            onNoConnectionListener?.let { onNoConnectionListener(it) }
        }
        syncBuilder
            .show(syncContext.conflictType().name)
    }

    override fun syncSilently(
        activity: FragmentActivity,
        programUid: String,
        onComplete: (() -> Unit)?,
        onOffline: (() -> Unit)?,
    ) {
        val inputData = Data.Builder()
            .putString(UID, programUid)
            .putString(CONFLICT_TYPE, ConflictType.PROGRAM.name)
            .build()
        val request = OneTimeWorkRequest.Builder(SyncGranularWorker::class.java)
            .setInputData(inputData)
            .build()

        val workManager = WorkManager.getInstance(activity)
        workManager.enqueueUniqueWork(
            "ATTENDANCE_SYNC_$programUid",
            ExistingWorkPolicy.REPLACE,
            request,
        )

        var notified = false
        workManager.getWorkInfoByIdLiveData(request.id).observe(activity) { workInfo ->
            if (!notified && workInfo != null && workInfo.state.isFinished) {
                notified = true
                onComplete?.invoke()
            }
        }
    }
}
