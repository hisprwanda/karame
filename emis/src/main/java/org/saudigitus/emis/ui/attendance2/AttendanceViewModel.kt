package org.saudigitus.emis.ui.attendance2

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
import kotlinx.coroutines.launch
import org.dhis2.commons.resources.ResourceManager
import org.hisp.dhis.android.core.maintenance.D2Error
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
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.Constants.KEY
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.DateHelper.stringToLocalDate
import org.saudigitus.emis.utils.getOption
import java.time.ZoneId
import javax.inject.Inject

class AttendanceViewModel @Inject constructor(
    private val repository: DataManager,
    private val attendanceRepository: AttendanceRepository,
    private val formRepository: FormRepository,
    private val resourceManager: ResourceManager,
) : ViewModel() {

    private var attendanceConfig: Attendance? = null
    private var studentsIds: List<String> = emptyList()
    private var selectedDate: String = DateHelper.formatDate(System.currentTimeMillis())
        .orEmpty()

    private var cachedButtonModel: AttendanceButtonModel? = null

    private val _hasCachedData = MutableStateFlow(false)
    val hasCachedData: StateFlow<Boolean> = _hasCachedData

    private val _snackbarEvent = MutableSharedFlow<String?>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val snackbarEvent: SharedFlow<String?> = _snackbarEvent

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
            loadAttendanceEventsByDate()
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
                setButtonStep(ButtonStep.HOLD_SAVING)
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

            _uiState.value = AttendanceUiState.HasAttendance(
                toolbarHeaders = ToolbarHeaders(
                    resourceManager.getString(R.string.attendance),
                    DateHelper.formatDateWithWeekDay(
                        DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
                    ),
                ),
                infoCard = InfoCard(),
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
            val currentState = uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            val toolbarHeaders = currentState.toolbarHeaders
            val currentButtonState = currentState.attendanceButtonState

            val attendanceStatus = attendanceRepository.getAttendanceStatus(
                currentState.program,
                selectedDate
            )

            _uiState.value = currentState.copy(
                attendanceButtonState = currentButtonState.copy(isLoading = true)
            )

            val updatedButtonState = attendanceRepository.loadAttendanceEvents(
                teiUids = studentsIds,
                program = currentState.program,
                programStage = attendanceConfig?.programStage.orEmpty(),
                dataElement = attendanceConfig?.status.orEmpty(),
                reasonDataElement = attendanceConfig?.absenceReason.orEmpty(),
                eventDate = date,
                attendanceButtonState = currentState.attendanceButtonState,
            )

            val updatedFormData = transform2FormData(updatedButtonState)
            val updatedSummary = attendanceRepository.attendanceSummary(updatedButtonState)

            _uiState.value = currentState.copy(
                toolbarHeaders = toolbarHeaders.copy(
                    subtitle = DateHelper.formatDateWithWeekDay(date)
                ),
                canTakeAttendance = validateCalendar(date, schoolCalendar.value, currentSchoolCalendar.value),
                selectedDate = date,
                attendanceButtonState = updatedButtonState,
                fieldsData = updatedFormData,
                attendanceStatus = attendanceStatus,
                attendanceSummary = updatedSummary,
            )
        }
    }

    private fun transform2FormData(buttonState: AttendanceButtonState): List<FormFieldData> {
        val currentState = uiState.value as AttendanceUiState.HasAttendance
        _uiState.value = currentState.copy(fieldsData = emptyList())

        return buttonState.attendanceEvents
            .mapNotNull { it.event }
            .filter { it.reasonOfAbsence != null }
            .mapNotNull {
                val formField = currentState.fields.firstOrNull() ?: return@mapNotNull null
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
            val currentState = uiState.value as AttendanceUiState.HasAttendance
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

            _uiState.value = currentState.copy(
                attendanceButtonState = updatedAttendanceButtonState,
                fieldsData = updatedFormFieldData,
                displayReasonField = displayReasonField,
                attendanceSummary = updatedSummary
            )
        }
    }

    private fun bulkAttendance(buttonModel: AttendanceButtonModel) {
        viewModelScope.launch {
            val currentState = uiState.value as AttendanceUiState.HasAttendance

            currentState.students.forEach {
                updateAttendanceEvent(
                    it,
                    buttonModel
                )
                delay(10L)
            }
        }
    }

    private fun launchBulk(launch: Boolean = true) {
        val currentState = uiState.value as AttendanceUiState.HasAttendance
        _uiState.value = currentState.copy(displayBulk = launch)
    }

    private fun updateAttendanceReason(
        tei: String,
        dataElement: String,
        value: String
    ) {
        val currentState = uiState.value as AttendanceUiState.HasAttendance
        val currentButtonState = currentState.attendanceButtonState
        val attendanceEvents = currentButtonState.attendanceEvents.toMutableList()
        _hasCachedData.value = true

        val event = attendanceEvents.find { it.event?.tei == tei }

        if (event == null) return

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
        val updatedFieldsData = transform2FormData(updatedButtonState)
        val updatedSummary = attendanceRepository.attendanceSummary(updatedButtonState)

        _uiState.value = currentState.copy(
            attendanceButtonState = updatedButtonState,
            fieldsData = updatedFieldsData,
            attendanceSummary = updatedSummary
        )
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
        val currentState = uiState.value as AttendanceUiState.HasAttendance
        val updatedButtonState = attendanceRepository.clearAll(currentState.attendanceButtonState)
        _hasCachedData.value = true

        _uiState.value = currentState.copy(
            attendanceButtonState = updatedButtonState,
            fieldsData = emptyList(),
            displayReasonField = emptyMap(),
            attendanceSummary = emptyList()
        )
    }

    private fun removeFromFormFieldData(
        tei: String,
        currentFormFieldData: List<FormFieldData>
    ): List<FormFieldData> {
        return currentFormFieldData.toMutableList().filterNot { it.tei == tei }
    }

    private fun setButtonStep(step: ButtonStep) {
        viewModelScope.launch {
            val currentState = uiState.value as? AttendanceUiState.HasAttendance ?: return@launch
            val currentButtonState = currentState.attendanceButtonState

            val hasInvalidData = hasInvalidData(currentButtonState)
            if (step == ButtonStep.SAVING && hasInvalidData) {
                _snackbarEvent.emit(resourceManager.getString(R.string.select_reason_for_all))
                return@launch
            }

            val isCompleted = attendanceRepository.isAttendanceCompleted(currentState.students, currentButtonState)
            if (step == ButtonStep.SAVING && !isCompleted) {
                _snackbarEvent.emit(resourceManager.getString(R.string.you_must_record_for_all))
                return@launch
            }

            _uiState.value = currentState.copy(
                attendanceStep = step,
                attendanceButtonState = currentButtonState.copy(
                    isEditing = step != ButtonStep.EDITING
                ),
                displaySummary = step == ButtonStep.SAVING,
                isAttendanceCompleted = isCompleted
            )
        }
    }

    private fun saveAttendanceEvents() {
        viewModelScope.launch {
            val current = uiState.value as AttendanceUiState.HasAttendance

            runCatching {
                attendanceRepository.saveAttendance(
                    program = current.program,
                    programStage = attendanceConfig?.programStage.orEmpty(),
                    attendanceEvents = current.attendanceButtonState.attendanceEvents
                )
            }.onSuccess {
                _uiState.value = current.copy(attendanceStep = ButtonStep.EDITING, displaySummary = false, execSync = true)
                loadAttendanceEventsByDate(current.selectedDate)
                _snackbarEvent.emit(resourceManager.getString(R.string.attendance_saved))
                _execSync.emit(true)
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

                _snackbarEvent.emit(friendlyMessage)
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

    fun refresh() {
        viewModelScope.launch {
            val currentState = uiState.value as AttendanceUiState.HasAttendance
            val currentButtonState = currentState.attendanceButtonState
            _hasCachedData.value = false

            _uiState.value = currentState.copy(
                attendanceStep = ButtonStep.EDITING,
                attendanceButtonState = currentButtonState.copy(isEditing = false),
                execSync = false,
                displayReasonField = emptyMap(),
            )
            delay(10L)
            loadAttendanceEventsByDate(currentState.selectedDate)
        }
    }
}