package org.saudigitus.emis.ui.attendance2

import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    sync: (refresh: (() -> Unit)?, offlineAction: (() -> Unit)?) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasCachedData by viewModel.hasCachedData.collectAsStateWithLifecycle()
    val schoolCalendar by viewModel.schoolCalendar.collectAsStateWithLifecycle()
    val currentSchoolCalendar by viewModel.currentSchoolCalendar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            if (message != null) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.execSync.collectLatest { status ->
            if (status != null && status) {
                sync.invoke(
                    {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        viewModel.refresh()
                    },
                    {
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
                    sync.invoke(
                        {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            viewModel.refresh()
                        },
                        {
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