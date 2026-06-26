package org.saudigitus.emis.ui.attendance2

import android.util.Log
import androidx.compose.ui.util.fastFilterNotNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.resources.ResourceManager
import org.hisp.dhis.android.core.maintenance.D2Error
import org.joda.time.format.ISODateTimeFormat.date
import org.saudigitus.emis.R
import org.saudigitus.emis.data.local.AttendanceRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.FormRepository
import org.saudigitus.emis.data.model.SearchTeiModel
import org.saudigitus.emis.data.model.app_config.Attendance
import org.saudigitus.emis.data.model.schoolcalendar_config.SchoolCalendar
import org.saudigitus.emis.data.model.schoolcalendar_config.SchoolCalendarConfig
import org.saudigitus.emis.ui.attendance.ButtonStep
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiEvent
import org.saudigitus.emis.ui.attendance2.state.AttendanceUiState
import org.saudigitus.emis.ui.attendance2.state.SyncUiPhase
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.Constants.KEY
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.DateHelper.stringToLocalDate
import org.saudigitus.emis.utils.getOption
import java.time.ZoneId
import javax.inject.Inject

data class SnackbarMessage(
    val message: String,
    val isError: Boolean = true,
)

class AttendanceViewModel @Inject constructor(
    private val repository: DataManager,
    private val attendanceRepository: AttendanceRepository,
    private val formRepository: FormRepository,
    private val resourceManager: ResourceManager,
    private val networkUtils: NetworkUtils,
) : ViewModel() {

    private var attendanceConfig: Attendance? = null
    private var studentsIds: List<String> = emptyList()
    private var selectedDate: String = DateHelper.formatDate(System.currentTimeMillis())
        .orEmpty()

    private var cachedButtonModel: AttendanceButtonModel? = null

    private val _hasCachedData = MutableStateFlow(false)
    val hasCachedData: StateFlow<Boolean> = _hasCachedData

    private val _snackbarEvent = MutableSharedFlow<SnackbarMessage?>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val snackbarEvent: SharedFlow<SnackbarMessage?> = _snackbarEvent

    private val _execSync = MutableSharedFlow<Boolean?>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val execSync: SharedFlow<Boolean?> = _execSync

    private val _schoolCalendar = MutableStateFlow<SchoolCalendarConfig?>(null)
    val schoolCalendar: StateFlow<SchoolCalendarConfig?> = _schoolCalendar

    private val _currentSchoolCalendar = MutableStateFlow<SchoolCalendar?>(null)
    val currentSchoolCalendar: StateFlow<SchoolCalendar?> = _currentSchoolCalendar;

    private val _uiState = MutableStateFlow<AttendanceUiState>(AttendanceUiState.LOADING)

    val uiState = _uiState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _uiState.value,
        )

    init {
        loadSchoolCalendar()
    }

    fun initialize(program: String, students: List<SearchTeiModel>, infoCard: InfoCard) {
        viewModelScope.launch {
            val config = repository.getConfig(KEY)?.find { it.program == program }
            attendanceConfig = config?.attendance

            val currentState = uiState.value as? AttendanceUiState.HasAttendance ?: return@launch

            _uiState.value =
                currentState.copy(program = program, students = students, infoCard = infoCard)

            studentsIds = students.map { it.tei.uid() }

            loadStaticDependencies(program)
            loadAttendanceEventsByDate(selectedDate)
        }
    }

    fun handleUiEvent(uiEvent: AttendanceUiEvent) {
        when (uiEvent) {
            is AttendanceUiEvent.AddDate -> {
                loadAttendanceEventsByDate(uiEvent.date)
            }

            is AttendanceUiEvent.AddStep -> {
                setButtonStep(uiEvent.step)
            }

            is AttendanceUiEvent.AddAttendance -> {
                if (uiEvent.tei != null) {
                    updateAttendanceEvent(uiEvent.tei, uiEvent.attendance)
                } else {
                    bulkAttendance(uiEvent.attendance)
                }
                launchBulk(false)
            }

            is AttendanceUiEvent.AddReasonOfAbsence -> {
                updateAttendanceReason(uiEvent.tei, uiEvent.dataElement, uiEvent.reason)
            }

            is AttendanceUiEvent.LaunchBulk -> {
                launchBulk()
            }

            is AttendanceUiEvent.DismissBulk -> {
                launchBulk(false)
            }

            is AttendanceUiEvent.DismissSummary -> {
                _uiState.update {
                    (it as? AttendanceUiState.HasAttendance)?.copy(displaySummary = false) ?: it
                }
            }

            is AttendanceUiEvent.ClearAttendance -> {
                clearAllEvents()
            }

            is AttendanceUiEvent.SaveAttendance -> {
                saveAttendanceEvents()
            }

            else -> Unit
        }
    }

    private fun loadSchoolCalendar() {
        viewModelScope.launch {
            val schoolCalendar = repository.dateValidation(Constants.CALENDAR_KEY)
            val default = schoolCalendar?.defaults

            val currentSchoolCalendar = schoolCalendar?.schoolCalendar?.find {
                it?.academicYear?.code == default?.academicYear
            }

            _schoolCalendar.value = schoolCalendar
            _currentSchoolCalendar.value = currentSchoolCalendar

            val canTakeAttendance = validateCalendar(
                DateHelper.formatDate(System.currentTimeMillis()).orEmpty(),
                schoolCalendar,
                currentSchoolCalendar
            )

            _uiState.value = AttendanceUiState.HasAttendance(
                toolbarHeaders = ToolbarHeaders(
                    resourceManager.getString(R.string.attendance),
                    DateHelper.formatDateWithWeekDay(
                        DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
                    ),
                ),
                infoCard = InfoCard(),
                canTakeAttendance = canTakeAttendance,
            )
        }
    }

    private suspend fun loadStaticDependencies(program: String) {
        val config = repository.getConfig(KEY)?.find { it.program == program }
        val currentState = uiState.value as? AttendanceUiState.HasAttendance ?: return

        val fields = formRepository.getFormFields(
            program,
            config?.attendance?.programStage.orEmpty(),
            config?.attendance?.absenceReason.orEmpty()
        )

        _uiState.value = currentState.copy(fields = fields)
    }

    private fun loadAttendanceEventsByDate(
        date: String = DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
    ) {
        selectedDate = date
        viewModelScope.launch {
            val current = _uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            val program = current.program
            val baseButtonState = current.attendanceButtonState

            // Show the loading spinner and clear stale reason fields up front (atomic).
            _uiState.update { s ->
                val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                st.copy(
                    attendanceButtonState = st.attendanceButtonState.copy(isLoading = true),
                    displayReasonField = emptyMap(),
                )
            }

            val canTakeAttendance =
                validateCalendar(date, schoolCalendar.value, currentSchoolCalendar.value)
            val attendanceStatus = attendanceRepository.getAttendanceStatus(program, date)

            val updatedButtonState = attendanceRepository.loadAttendanceEvents(
                teiUids = studentsIds,
                program = program,
                programStage = attendanceConfig?.programStage.orEmpty(),
                dataElement = attendanceConfig?.status.orEmpty(),
                reasonDataElement = attendanceConfig?.absenceReason.orEmpty(),
                eventDate = date,
                attendanceButtonState = baseButtonState,
            )

            val updatedFormData = transform2FormData(updatedButtonState, current.fields)
            val updatedSummary = attendanceRepository.attendanceSummary(updatedButtonState)

            // A freshly loaded day carries no pending in-memory marks; clearing this means the
            // "leave without submitting?" guard only fires when something was actually marked.
            _hasCachedData.value = false

            // Single atomic write of the loaded data fields only. Transient flags (syncPhase,
            // displaySummary, attendanceStep, execSync) are preserved from the latest state so a
            // concurrent submit/sync can never be clobbered by this reload.
            _uiState.update { s ->
                val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                st.copy(
                    toolbarHeaders = st.toolbarHeaders.copy(
                        subtitle = DateHelper.formatDateWithWeekDay(date)
                    ),
                    canTakeAttendance = canTakeAttendance,
                    selectedDate = date,
                    attendanceButtonState = updatedButtonState,
                    fieldsData = updatedFormData,
                    attendanceStatus = attendanceStatus,
                    attendanceSummary = updatedSummary,
                    displayReasonField = emptyMap(),
                )
            }
        }
    }

    // Pure: derives the absence-reason form rows from a button state. No state mutation, so it is
    // safe to call from inside an atomic update or alongside a concurrent reload.
    private fun transform2FormData(
        buttonState: AttendanceButtonState,
        fields: List<FormFieldState>,
    ): List<FormFieldData> {
        return buttonState.attendanceEvents
            .mapNotNull { it.event }
            .filter { it.reasonOfAbsence != null }
            .mapNotNull {
                val formField = fields.firstOrNull() ?: return@mapNotNull null
                val option = formField.getOption(it.reasonOfAbsence.orEmpty())

                FormFieldData(
                    tei = it.tei,
                    event = it.event.orEmpty(),
                    dataElement = formField.dataElementUid,
                    value = it.value,
                    optionModel = option
                )
            }
    }

    private fun updateAttendanceEvent(
        tei: SearchTeiModel?,
        buttonModel: AttendanceButtonModel
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            _hasCachedData.value = true

            val updatedAttendanceButtonState = attendanceRepository.updateAttendanceEvent(
                eventDate = currentState.selectedDate,
                tei = tei,
                currentState.attendanceButtonState,
                buttonModel = buttonModel
            )

            val displayReasonField = displayAbsenceReason(
                currentState.displayReasonField,
                tei?.tei?.uid().orEmpty(),
                buttonModel.key == Constants.ABSENT
            )

            val updatedFormFieldData = removeFromFormFieldData(
                tei?.tei?.uid().orEmpty(),
                currentState.fieldsData
            )

            val updatedSummary =
                attendanceRepository.attendanceSummary(updatedAttendanceButtonState)

            _uiState.update { s ->
                (s as? AttendanceUiState.HasAttendance)?.copy(
                    attendanceButtonState = updatedAttendanceButtonState,
                    fieldsData = updatedFormFieldData,
                    displayReasonField = displayReasonField,
                    attendanceSummary = updatedSummary
                ) ?: s
            }
        }
    }

    private fun bulkAttendance(buttonModel: AttendanceButtonModel) {
        viewModelScope.launch {
            val currentState = _uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            _hasCachedData.value = true

            // Apply every student's mark on a single accumulated state and write once.
            // Updating each student through its own coroutine raced on uiState and dropped
            // events for large classes, which then failed the "record for all" check.
            var buttonState = currentState.attendanceButtonState
            var displayReasonField = currentState.displayReasonField
            var fieldsData = currentState.fieldsData

            currentState.students.forEach { student ->
                buttonState = attendanceRepository.updateAttendanceEvent(
                    eventDate = currentState.selectedDate,
                    tei = student,
                    buttonState,
                    buttonModel = buttonModel,
                )
                displayReasonField = displayAbsenceReason(
                    displayReasonField,
                    student.tei.uid().orEmpty(),
                    buttonModel.key == Constants.ABSENT,
                )
                fieldsData = removeFromFormFieldData(
                    student.tei.uid().orEmpty(),
                    fieldsData,
                )
            }

            val finalButtonState = buttonState
            val finalFieldsData = fieldsData
            val finalDisplayReasonField = displayReasonField
            val finalSummary = attendanceRepository.attendanceSummary(finalButtonState)

            _uiState.update { s ->
                (s as? AttendanceUiState.HasAttendance)?.copy(
                    attendanceButtonState = finalButtonState,
                    fieldsData = finalFieldsData,
                    displayReasonField = finalDisplayReasonField,
                    attendanceSummary = finalSummary,
                ) ?: s
            }
        }
    }

    private fun launchBulk(launch: Boolean = true) {
        _uiState.update {
            (it as? AttendanceUiState.HasAttendance)?.copy(displayBulk = launch) ?: it
        }
    }

    private fun updateAttendanceReason(
        tei: String,
        dataElement: String,
        value: String
    ) {
        val currentState = _uiState.value as? AttendanceUiState.HasAttendance ?: return
        val currentButtonState = currentState.attendanceButtonState
        val attendanceEvents = currentButtonState.attendanceEvents.toMutableList()

        val event = attendanceEvents.find { it.event?.tei == tei } ?: return
        _hasCachedData.value = true

        val updatedEvent = event.event?.copy(
            reasonDataElement = dataElement,
            reasonOfAbsence = value
        )
        val eventWithDecorator = event.copy(
            event = updatedEvent,
        )

        val hasBeenRemoved = attendanceEvents.removeIf { it.event?.tei == tei }

        if (hasBeenRemoved) {
            attendanceEvents.add(eventWithDecorator)
        }

        val updatedButtonState = currentButtonState.copy(attendanceEvents = attendanceEvents)
        val updatedFieldsData = transform2FormData(updatedButtonState, currentState.fields)
        val updatedSummary = attendanceRepository.attendanceSummary(updatedButtonState)

        _uiState.update { s ->
            (s as? AttendanceUiState.HasAttendance)?.copy(
                attendanceButtonState = updatedButtonState,
                fieldsData = updatedFieldsData,
                attendanceSummary = updatedSummary
            ) ?: s
        }
    }

    private fun displayAbsenceReason(
        displayReasonField: Map<String, Boolean> = emptyMap(),
        tei: String,
        status: Boolean
    ): Map<String, Boolean> {
        val updatedMap = displayReasonField
            .filterNot { it.key == tei }
            .toMutableMap()

        updatedMap[tei] = status

        return updatedMap.toMap()
    }

    private fun hasInvalidData(attendanceButtonState: AttendanceButtonState): Boolean {
        return attendanceRepository.hasInvalidData(attendanceButtonState)
    }

    private fun clearAllEvents() {
        val currentState = _uiState.value as? AttendanceUiState.HasAttendance ?: return
        val updatedButtonState = attendanceRepository.clearAll(currentState.attendanceButtonState)
        _hasCachedData.value = true

        _uiState.update { s ->
            (s as? AttendanceUiState.HasAttendance)?.copy(
                attendanceButtonState = updatedButtonState,
                fieldsData = emptyList(),
                displayReasonField = emptyMap(),
                attendanceSummary = emptyList()
            ) ?: s
        }
    }

    private fun removeFromFormFieldData(
        tei: String,
        currentFormFieldData: List<FormFieldData>
    ): List<FormFieldData> {
        return currentFormFieldData.toMutableList().filterNot { it.tei == tei }
    }

    private fun setButtonStep(step: ButtonStep) {
        viewModelScope.launch {
            val currentState = _uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            val currentButtonState = currentState.attendanceButtonState

            val hasInvalidData = hasInvalidData(currentButtonState)
            if (step == ButtonStep.SAVING && hasInvalidData) {
                _snackbarEvent.emit(
                    SnackbarMessage(
                        resourceManager.getString(R.string.select_reason_for_all),
                        isError = true,
                    )
                )
                return@launch
            }

            val isCompleted = attendanceRepository.isAttendanceCompleted(currentState.students, currentButtonState)
            if (step == ButtonStep.SAVING && !isCompleted) {
                _snackbarEvent.emit(
                    SnackbarMessage(
                        resourceManager.getString(R.string.you_must_record_for_all),
                        isError = true,
                    )
                )
                return@launch
            }

            // Decide the lock/label phase by connectivity at submit time, so offline shows
            // "Saved locally" (not "Uploading") and never enters a never-ending sync state.
            val phase = if (step == ButtonStep.SAVING) {
                if (networkUtils.isOnline()) SyncUiPhase.UPLOADING else SyncUiPhase.SAVED_LOCAL
            } else {
                currentState.syncPhase
            }

            _uiState.update { s ->
                val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                st.copy(
                    attendanceStep = step,
                    attendanceButtonState = st.attendanceButtonState.copy(
                        isEditing = step != ButtonStep.EDITING
                    ),
                    displaySummary = step == ButtonStep.SAVING,
                    isAttendanceCompleted = isCompleted,
                    // Lock editing the instant Submit is accepted, before the (slow for large
                    // classes) save runs. Cleared on sync completion / offline / save failure.
                    syncPhase = phase,
                )
            }

            if (step == ButtonStep.SAVING) {
                saveAttendanceEvents()
            }
        }
    }

    private fun saveAttendanceEvents() {
        viewModelScope.launch {
            val current = _uiState.value as? AttendanceUiState.HasAttendance ?: return@launch

            runCatching {
                attendanceRepository.saveAttendance(
                    program = current.program,
                    programStage = attendanceConfig?.programStage.orEmpty(),
                    attendanceEvents = current.attendanceButtonState.attendanceEvents
                )
            }.onSuccess {
                // Data is now safely on the device. Clear the unsaved guard immediately so that
                // pressing back / changing date during the background upload just leaves, instead
                // of nagging to discard already-saved work.
                _hasCachedData.value = false
                _uiState.update { s ->
                    val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                    st.copy(
                        attendanceStep = ButtonStep.EDITING,
                        attendanceButtonState = st.attendanceButtonState.copy(isEditing = false),
                        displaySummary = true,
                        execSync = true,
                        // keep syncPhase as set by setButtonStep (UPLOADING / SAVED_LOCAL);
                        // it is resolved by the sync callbacks.
                    )
                }
                loadAttendanceEventsByDate(selectedDate.ifEmpty { current.selectedDate })
                _execSync.emit(true)
                _snackbarEvent.emit(
                    SnackbarMessage(
                        resourceManager.getString(R.string.attendance_saved),
                        isError = false,
                    )
                )
            }.onFailure { error ->
                val friendlyMessage = when (error) {
                    is D2Error -> {
                        "${error.errorCode()} – ${
                            error.message ?: resourceManager
                                .getString(R.string.error_unexpected)
                        }"
                    }

                    else -> error.message ?: resourceManager
                        .getString(R.string.error_unexpected)
                }

                _uiState.update { s ->
                    (s as? AttendanceUiState.HasAttendance)?.copy(
                        displaySummary = false,
                        syncPhase = SyncUiPhase.IDLE,
                    ) ?: s
                }
                _snackbarEvent.emit(SnackbarMessage(friendlyMessage, isError = true))
            }
        }
    }


    fun validateCalendar(
        strDate: String,
        schoolCalendar: SchoolCalendarConfig? = null,
        currentSchoolCalendar: SchoolCalendar? = null,
    ): Boolean {
        val dateMillis = DateHelper.convertDateToMilliseconds(strDate)
        val date = stringToLocalDate(DateHelper.formatDate(dateMillis)!!)
        val today = System.currentTimeMillis()

        return if (schoolCalendar != null && currentSchoolCalendar != null) {
            val startDate = currentSchoolCalendar.academicYear?.startDate
            val endDate = currentSchoolCalendar.academicYear?.endDate

            val startMillis = stringToLocalDate(startDate!!)
                .atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()!!
            val endMillis = stringToLocalDate(endDate!!)
                .atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()?.toEpochMilli()!!

            val isValid = (
                !DateHelper.isWeekend(date) && currentSchoolCalendar.weekDays?.saturday == false &&
                    currentSchoolCalendar.weekDays.sunday == false
                ) &&
                currentSchoolCalendar.holidays?.let { holiday ->
                    DateHelper.isHoliday(holiday.fastFilterNotNull(), dateMillis)
                } == true && (dateMillis in startMillis..endMillis) && dateMillis <= today

            isValid
        } else {
            dateMillis <= today
        }
    }

    /** Manual toolbar (download) sync started — lock editing and show "Synchronizing…". */
    fun startManualSync() {
        _uiState.update {
            (it as? AttendanceUiState.HasAttendance)?.copy(syncPhase = SyncUiPhase.SYNCING) ?: it
        }
    }

    /** A sync (upload or manual) finished — return to IDLE and reload the day from the DB. */
    fun onSyncFinished() {
        refresh()
    }

    /**
     * Offline submit: data is saved on the device only. Keep the "Saved locally" label briefly so
     * the teacher sees the confirmation, then settle the FAB back to its normal action.
     */
    fun onSavedOffline() {
        viewModelScope.launch {
            delay(3000L)
            _uiState.update { s ->
                val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                if (st.syncPhase == SyncUiPhase.SAVED_LOCAL) {
                    st.copy(syncPhase = SyncUiPhase.IDLE)
                } else {
                    st
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _hasCachedData.value = false

            _uiState.update { s ->
                val st = s as? AttendanceUiState.HasAttendance ?: return@update s
                st.copy(
                    attendanceStep = ButtonStep.EDITING,
                    attendanceButtonState = st.attendanceButtonState.copy(isEditing = false),
                    execSync = false,
                    displayReasonField = emptyMap(),
                    isAttendanceCompleted = false,
                    syncPhase = SyncUiPhase.IDLE,
                )
            }
            delay(10L)
            val date = selectedDate.ifEmpty {
                (_uiState.value as? AttendanceUiState.HasAttendance)?.selectedDate.orEmpty()
            }
            loadAttendanceEventsByDate(date)
        }
    }
}