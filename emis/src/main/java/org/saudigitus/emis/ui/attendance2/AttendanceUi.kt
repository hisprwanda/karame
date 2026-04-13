package org.saudigitus.emis.ui.attendance2

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.dhis2.ui.theme.colorPrimary
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.mobile.ui.designsystem.component.ListCard
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardTitleModel
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.saudigitus.emis.R
import org.saudigitus.emis.data.model.mapper.map
import org.saudigitus.emis.ui.attendance.AttendanceSummaryDialog
import org.saudigitus.emis.ui.attendance.BulkAssignComponent
import org.saudigitus.emis.ui.attendance.ButtonStep
import org.saudigitus.emis.ui.attendance2.components.AttendanceButton
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiEvent
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiState
import org.saudigitus.emis.ui.components.Info
import org.saudigitus.emis.ui.components.ShowCard
import org.saudigitus.emis.ui.components.Toolbar
import org.saudigitus.emis.ui.components.ToolbarActionState
import org.saudigitus.emis.ui.form.attendance.fields.AttendanceField
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.ui.theme.light_error
import org.saudigitus.emis.ui.theme.light_success
import org.saudigitus.emis.utils.hasStudent

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttendanceUi(
    uiState: AttendanceUiState,
    teiCardMapper: TEICardMapper,
    snackbarHostState: SnackbarHostState,
    dateValidator: (Long) -> Boolean,
    onEvent: (AttendanceUiEvent) -> Unit,
) {
    if ((uiState as? AttendanceUiState.HasAttendance)?.displayBulk == true) {
        BulkAssignComponent(
            onDismissRequest = { onEvent(AttendanceUiEvent.DismissBulk) },
            attendanceButtonModels = uiState.attendanceButtonState.buttons,
            onAttendanceStatus = { onEvent(AttendanceUiEvent.AddAttendance(attendance = it)) },
            onClear = { onEvent(AttendanceUiEvent.ClearAttendance) },
            onCancel = { onEvent(AttendanceUiEvent.DismissBulk) },
        )
    }

    if ((uiState as? AttendanceUiState.HasAttendance)?.displaySummary == true) {
        AttendanceSummaryDialog(
            title = stringResource(R.string.attendance_summary),
            data = uiState.attendanceSummary,
            themeColor = colorPrimary,
            onCancel = { onEvent(AttendanceUiEvent.DismissSummary) },
            onDone = { onEvent(AttendanceUiEvent.SaveAttendance) },
        )
    }

    Scaffold(
        topBar = {
            Toolbar(
                headers = uiState.toolbarHeaders,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorPrimary,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                navigationAction = { onEvent(AttendanceUiEvent.BackHandler) },
                disableNavigation = false,
                actionState = ToolbarActionState(
                    syncVisibility = true,
                    filterVisibility = false,
                    showCalendar = true,
                ),
                calendarAction = { onEvent(AttendanceUiEvent.AddDate(it)) },
                dateValidator = dateValidator,
                syncAction = { onEvent(AttendanceUiEvent.SyncHandler) }
            )
        },
        floatingActionButton = {
            if (uiState !is AttendanceUiState.HasAttendance) return@Scaffold

            ExtendedFloatingActionButton(
                text = {
                    Text(
                        text = if (uiState.attendanceStep == ButtonStep.EDITING) {
                            stringResource(R.string.update)
                        } else {
                            stringResource(R.string.submit)
                        },
                        color = colorPrimary,
                        style = LocalTextStyle.current.copy(
                            fontFamily = FontFamily(Font(R.font.rubik_medium)),
                        ),
                    )
                },
                icon = {
                    Icon(
                        imageVector = if (uiState.attendanceStep == ButtonStep.EDITING) {
                            Icons.Default.Edit
                        } else {
                            Icons.Default.Save
                        },
                        contentDescription = null,
                        tint = colorPrimary,
                    )
                },
                onClick = {
                    when (uiState.attendanceStep) {
                        ButtonStep.HOLD_SAVING -> {
                            onEvent(AttendanceUiEvent.AddStep(ButtonStep.SAVING))
                        }

                        else -> {
                            if (uiState.canTakeAttendance) {
                                onEvent(AttendanceUiEvent.AddStep(ButtonStep.HOLD_SAVING))
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = {
            if (uiState !is AttendanceUiState.HasAttendance) return@Scaffold

            SnackbarHost(hostState = snackbarHostState) {
                Snackbar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    containerColor = when {
                        uiState.isAttendanceCompleted -> light_success
                        else -> light_error
                    },
                    contentColor = Color.White,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(
                                when {
                                    uiState.isAttendanceCompleted -> R.drawable.success_icon
                                    else -> R.drawable.ic_error_outline
                                }
                            ),
                            contentDescription = it.visuals.message,
                        )

                        Text(
                            text = it.visuals.message,
                            style = LocalTextStyle.current.copy(
                                fontFamily = FontFamily(Font(R.font.rubik_regular)),
                            ),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFF2C98F0))
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                            .copy(
                                topStart = CornerSize(16.dp),
                                topEnd = CornerSize(16.dp),
                                bottomStart = CornerSize(0.dp),
                                bottomEnd = CornerSize(0.dp),
                            ),
                    ),
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (uiState) {
                    is AttendanceUiState.IDLE -> Unit
                    is AttendanceUiState.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProgressIndicator(type = ProgressIndicatorType.CIRCULAR_SMALL)
                        }
                    }

                    is AttendanceUiState.HasAttendance -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                        ) {
                            ShowCard(
                                uiState.infoCard,
                                false,
                                enabledIconButton = uiState.attendanceStep == ButtonStep.HOLD_SAVING,
                                onIconClick = { onEvent(AttendanceUiEvent.LaunchBulk) },
                            )

                            if (!uiState.canTakeAttendance) {
                                Info(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 108.dp),
                        ) {
                            items(
                                uiState.students,
                                key = { student -> student.tei.uid().orEmpty() },
                            ) { student ->
                                val card =
                                    student.map(teiCardMapper = teiCardMapper, showSync = false)
                                val isInactive = student.enrollments.getOrNull(0)
                                    ?.status() == EnrollmentStatus.CANCELLED

                                Column(
                                    modifier = Modifier
                                        .padding(bottom = 5.dp)
                                        .background(color = Color.White),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(color = Color.White),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        ListCard(
                                            modifier = Modifier.testTag("TEI_ITEM"),
                                            listAvatar = card.avatar,
                                            title = ListCardTitleModel(text = card.title),
                                            additionalInfoList = card.additionalInfo,
                                            actionButton = card.actionButton,
                                            expandLabelText = card.expandLabelText,
                                            shrinkLabelText = card.shrinkLabelText,
                                            onCardClick = card.onCardCLick,
                                            shadow = false,
                                        )
                                        AttendanceButton(
                                            key = student.tei.uid(),
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            state = uiState.attendanceButtonState,
                                            onClick = {
                                                onEvent(
                                                    AttendanceUiEvent.AddAttendance(
                                                        student,
                                                        it
                                                    )
                                                )
                                            },
                                        )
                                    }
                                    AnimatedVisibility(
                                        uiState.fieldsData.hasStudent(student.tei.uid())
                                            || uiState.displayReasonField.getOrDefault(student.tei.uid(), false)
                                    ) {
                                        AttendanceField(
                                            key = student.tei.uid(),
                                            field = uiState.fields.firstOrNull(),
                                            enabled = !isInactive && uiState.attendanceStep != ButtonStep.EDITING,
                                            fieldsData = uiState.fieldsData,
                                            onValueChange = { key, fieldUid, value ->
                                                onEvent(
                                                    AttendanceUiEvent.AddReasonOfAbsence(
                                                        key,
                                                        fieldUid,
                                                        value
                                                    )
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
