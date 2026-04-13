package org.saudigitus.emis.ui.attendance2

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiEvent
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.utils.DateHelper

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    teiCardMapper: TEICardMapper,
    program: String,
    students: List<SearchTeiModel>,
    infoCard: InfoCard,
    onBack: () -> Unit,
    sync: (refresh: (() -> Unit)?, offlineAction: (() -> Unit)?) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val schoolCalendar by viewModel.schoolCalendar.collectAsStateWithLifecycle()
    val currentSchoolCalendar by viewModel.currentSchoolCalendar.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.initialize(program, students, infoCard)
    }

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
                        viewModel.refresh()
                    },
                    {  }
                )
            }
        }
    }

    AttendanceUi(
        uiState = state,
        teiCardMapper = teiCardMapper,
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
                is AttendanceUiEvent.BackHandler -> onBack.invoke()
                is AttendanceUiEvent.SyncHandler -> {
                    sync.invoke(
                        {
                            viewModel.refresh()
                        },
                        {  }
                    )
                }
                else -> viewModel.handleUiEvent(it)
            }
        }
    )
}