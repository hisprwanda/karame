package org.saudigitus.emis.ui.attendance2

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.dhis2.commons.R
import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiEvent
import org.saudigitus.emis.ui.components.launchBottomSheet
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.utils.DateHelper

@Composable
fun AttendanceScreen(
    activity: FragmentActivity,
    viewModel: AttendanceViewModel,
    teiCardMapper: TEICardMapper,
    students: List<SearchTeiModel>,
    onBack: () -> Unit,
    sync: (refresh: (() -> Unit)?, offlineAction: (() -> Unit)?) -> Unit,
    syncSilent: (refresh: (() -> Unit)?, offlineAction: (() -> Unit)?) -> Unit = sync,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasCachedData by viewModel.hasCachedData.collectAsStateWithLifecycle()
    val schoolCalendar by viewModel.schoolCalendar.collectAsStateWithLifecycle()
    val currentSchoolCalendar by viewModel.currentSchoolCalendar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarIsError by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { event ->
            if (event != null) {
                snackbarIsError = event.isError
                snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.execSync.collectLatest { status ->
            if (status != null && status) {
                viewModel.setSyncing(true)
                syncSilent.invoke(
                    {
                        viewModel.setSyncing(false)
                        snackbarHostState.currentSnackbarData?.dismiss()
                        viewModel.refresh()
                    },
                    {
                        viewModel.setSyncing(false)
                        snackbarHostState.currentSnackbarData?.dismiss()
                        viewModel.refresh()
                    }
                )
            }
        }
    }

    fun navigationBack() {
        if (hasCachedData) {
            launchBottomSheet(
                activity.getString(R.string.not_saved),
                activity.getString(R.string.attendance_not_saved),
                supportFragmentManager = activity.supportFragmentManager,
                onDiscard = {
                    onBack.invoke()
                },
                onKeepEdition = { },
            )
        } else {
           onBack.invoke()
        }
    }

    BackHandler {
        navigationBack()
    }


    AttendanceUi(
        uiState = state,
        teiCardMapper = teiCardMapper,
        students = students,
        snackbarHostState = snackbarHostState,
        snackbarIsError = snackbarIsError,
        dateValidator = {
            viewModel.validateCalendar(
                strDate = DateHelper.formatDate(it).orEmpty(),
                schoolCalendar = schoolCalendar,
                currentSchoolCalendar = currentSchoolCalendar
            )
        },
        onEvent = {
            when (it) {
                is AttendanceUiEvent.BackHandler -> navigationBack()
                is AttendanceUiEvent.SyncHandler -> {
                    viewModel.setSyncing(true)
                    syncSilent.invoke(
                        {
                            viewModel.setSyncing(false)
                            snackbarHostState.currentSnackbarData?.dismiss()
                            viewModel.refresh()
                        },
                        {
                            viewModel.setSyncing(false)
                            snackbarHostState.currentSnackbarData?.dismiss()
                            viewModel.refresh()
                        }
                    )
                }
                else -> viewModel.handleUiEvent(it)
            }
        }
    )
}