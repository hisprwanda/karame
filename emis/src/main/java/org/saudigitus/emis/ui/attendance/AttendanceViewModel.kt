package org.saudigitus.emis.ui.attendance

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hisp.dhis.android.core.common.ValueType
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.FormRepository
import org.saudigitus.emis.data.model.Summary
import org.saudigitus.emis.data.model.app_config.Attendance
import org.saudigitus.emis.data.model.dto.Absence
import org.saudigitus.emis.data.model.dto.AttendanceEntity
import org.saudigitus.emis.ui.base.BaseViewModel
import org.saudigitus.emis.ui.form.Field
import org.saudigitus.emis.ui.form.FormData
import org.saudigitus.emis.ui.form.FormField
import org.saudigitus.emis.utils.Constants.KEY
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.Utils.WHITE
import org.saudigitus.emis.utils.getOption
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AttendanceViewModel
@Inject constructor(
    private val repository: DataManager,
    private val formRepository: FormRepository,
) : BaseViewModel(repository) {

    private val _datastoreAttendance = MutableStateFlow<Attendance?>(null)
    private val datastoreAttendance: StateFlow<Attendance?> = _datastoreAttendance

    private val _attendanceOptions = MutableStateFlow<List<AttendanceOption>>(emptyList())
    val attendanceOptions: StateFlow<List<AttendanceOption>> = _attendanceOptions

    private val _attendanceStatus = MutableStateFlow<List<AttendanceEntity>>(emptyList())
    val attendanceStatus: StateFlow<List<AttendanceEntity>> = _attendanceStatus

    private val _attendanceStep = MutableStateFlow(ButtonStep.EDITING)
    val attendanceStep: StateFlow<ButtonStep> = _attendanceStep

    private val _attendanceBtnState =
        MutableStateFlow<List<AttendanceActionButtonState>>(emptyList())
    val attendanceBtnState: StateFlow<List<AttendanceActionButtonState>> = _attendanceBtnState

    private val attendanceCache = mutableSetOf<AttendanceEntity>()
    private var attendanceBtnStateCache = mutableListOf<AttendanceActionButtonState>()

    private val _absenceState = MutableStateFlow(Absence())
    private val absenceState: StateFlow<Absence> = _absenceState

    private val _absenceStateCache = MutableStateFlow<List<Absence>>(emptyList())
    val absenceStateCache: StateFlow<List<Absence>> = _absenceStateCache

    private val _formFields = MutableStateFlow<List<FormField>>(emptyList())
    val formFields: StateFlow<List<FormField>> = _formFields

    private val _fieldState = MutableStateFlow<List<Field>>(emptyList())
    val fieldState: StateFlow<List<Field>> = _fieldState

    private val _formData = MutableStateFlow<List<FormData>>(emptyList())
    val formData: StateFlow<List<FormData>> = _formData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOnlyAbsence = MutableStateFlow(false)
    private val _options = MutableStateFlow<List<String>>(emptyList())

    // -------------------------------
    // CONFIG
    // -------------------------------

    fun setDefaults(title: String, onlyAbsence: Boolean = false) {
        _toolbarHeaders.update { it.copy(title = title) }
        _isOnlyAbsence.value = onlyAbsence
    }

    fun setOptions(academicYear: String, grade: String, section: String) {
        _options.value = listOf(academicYear, grade, section)
    }

    override fun setConfig(program: String) {
        viewModelScope.launch {
            val config = repository.getConfig(KEY)?.find { it.program == program }

            config?.let {
                _datastoreAttendance.value = it.attendance
            }

            loadStaticDependencies(program)
        }
    }

    override fun setProgram(program: String) {
        _program.value = program

        setConfig(program)

        val date = eventDate.value.ifEmpty {
            DateHelper.formatDate(System.currentTimeMillis()).orEmpty()
        }

        attendanceEvents(date)
    }

    override fun setDate(date: String) {
        _eventDate.value = date

        attendanceEvents(date)

        _toolbarHeaders.update {
            it.copy(subtitle = DateHelper.formatDateWithWeekDay(date))
        }
    }

    // -------------------------------
    // CORE LOADING
    // -------------------------------

    private suspend fun loadStaticDependencies(program: String) {
        _attendanceOptions.value = repository.getAttendanceOptions(program)

        _formFields.value = formRepository.keyboardInputTypeByStage(
            program,
            datastoreAttendance.value?.programStage.orEmpty(),
            datastoreAttendance.value?.absenceReason.orEmpty()
        )
    }

    private fun attendanceEvents(date: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                clearCache()

                loadStaticDependencies(program.value)

                val result = repository.getAttendanceEvent(
                    program = program.value,
                    programStage = datastoreAttendance.value?.programStage.orEmpty(),
                    dataElement = datastoreAttendance.value?.status.orEmpty(),
                    reasonDataElement = datastoreAttendance.value?.absenceReason.orEmpty(),
                    teis = teiUIds.value.map { it.first },
                    date = date.toString(),
                )

                _attendanceStatus.value = result

                attendanceCache.clear()
                attendanceCache.addAll(result)

                setInitialAttendanceStatus()

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Timber.tag("ATTENDANCE_DATA").e(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------------
    // UI MAPPING
    // -------------------------------

    private fun setInitialAttendanceStatus() {
        attendanceBtnStateCache.clear()

        val mapped = attendanceStatus.value.map { attendance ->
            attendanceActionButtonMapper(
                index = attendanceOptions.value.indexOfFirst { it.code == attendance.value },
                tei = attendance.tei,
                attendanceValue = attendance.value,
                containerColor = attendanceOptions.value.find {
                    it.code == attendance.value
                }?.color ?: Color.Black,
            )
        }

        attendanceBtnStateCache = mapped.toMutableList()
        _attendanceBtnState.value = mapped

        translateAttendanceStatus2FormData()
    }

    private fun translateAttendanceStatus2FormData() {
        _formData.value = attendanceStatus.value
            .filter { it.reasonOfAbsence != null }
            .mapNotNull { status ->
                val formField = formFields.value.firstOrNull() ?: return@mapNotNull null

                val option = formField.getOption(status.reasonOfAbsence.orEmpty())

                FormData(
                    tei = status.tei,
                    event = status.event.orEmpty(),
                    date = null,
                    dataElement = formField.uid,
                    value = status.value,
                    valueType = null,
                    hasOptions = true,
                    itemOptions = option,
                )
            }
    }

    private fun attendanceActionButtonMapper(
        index: Int,
        tei: String,
        attendanceValue: String,
        containerColor: Color,
    ) = AttendanceActionButtonState(
        btnIndex = index,
        btnId = tei,
        iconTint = 0,
        buttonState = AttendanceButtonSettings(
            buttonType = attendanceValue,
            containerColor = containerColor,
            contentColor = WHITE,
        ),
    )

    private fun getAttendanceUiState(
        index: Int,
        tei: String,
        value: String,
        color: Color?,
    ): MutableList<AttendanceActionButtonState> {

        val existing = attendanceBtnStateCache.find { it.btnId == tei }

        val newItem = attendanceActionButtonMapper(
            index = index,
            tei = tei,
            attendanceValue = value,
            containerColor = color ?: Color.LightGray,
        )

        if (existing == null) {
            attendanceBtnStateCache.add(newItem)
        } else {
            attendanceBtnStateCache.remove(existing)
            attendanceBtnStateCache.add(newItem)
        }

        return attendanceBtnStateCache
    }

    // -------------------------------
    // USER ACTIONS
    // -------------------------------

    fun setAttendance(
        index: Int,
        ou: String,
        tei: String,
        enrollment: String,
        value: String,
        reasonOfAbsence: String? = null,
        color: Color? = null,
        hasPersisted: Boolean = true,
    ) {
        viewModelScope.launch {
            setAttendanceInternal(
                index,
                ou,
                tei,
                enrollment,
                value,
                reasonOfAbsence,
                color,
                hasPersisted
            )
        }
    }

    fun setAbsence(
        index: Int? = null,
        ou: String? = null,
        tei: String? = null,
        enrollment: String? = null,
        value: String? = null,
        color: Color? = null,
        reasonOfAbsence: String? = null,
    ) {
        _absenceState.update {
            it.copy(
                index = index ?: it.index,
                ou = ou ?: it.ou,
                tei = tei ?: it.tei,
                enrollment = enrollment ?: it.enrollment,
                value = value ?: it.value,
                color = color ?: it.color,
                reasonOfAbsence = reasonOfAbsence ?: it.reasonOfAbsence,
            )
        }
    }

    fun fieldState(
        key: String,
        event: String,
        dataElement: String,
        value: String,
        valueType: ValueType?,
    ) {
        val current = fieldState.value.toMutableList()

        val index = current.indexOfFirst {
            it.key == key && it.dataElement == dataElement
        }

        val field = Field(
            key, event, dataElement, value, valueType, false, null
        )

        if (index >= 0) current[index] = field else current.add(field)

        _fieldState.value = current
    }

    override fun save() {
        setAttendance(
            index = absenceState.value.index,
            ou = absenceState.value.ou,
            enrollment = absenceState.value.enrollment,
            tei = absenceState.value.tei,
            value = absenceState.value.value,
            color = absenceState.value.color,
            reasonOfAbsence = absenceState.value.reasonOfAbsence,
        )

        _absenceStateCache.value =
            absenceStateCache.value + absenceState.value
    }


    private suspend fun setAttendanceInternal(
        index: Int,
        ou: String,
        tei: String,
        enrollment: String,
        value: String,
        reasonOfAbsence: String? = null,
        color: Color? = null,
        hasPersisted: Boolean = true,
    ) {
        val data = formData.value.toMutableList()

        data.find { it.tei == tei }?.let {
            data.remove(it)
            _formData.value = data
            repository.deleteEvent(tei, enrollment, eventDate.value)
        }

        val updatedStatus = attendanceStatus.value.toMutableList()

        val indexStatus = updatedStatus.indexOfFirst { it.tei == tei }

        val updatedEntity = AttendanceEntity(
            tei = tei,
            enrollment = enrollment,
            dataElement = datastoreAttendance.value?.status.orEmpty(),
            value = value,
            reasonDataElement = datastoreAttendance.value?.absenceReason,
            reasonOfAbsence = reasonOfAbsence,
            date = eventDate.value,
        )

        if (indexStatus >= 0) {
            updatedStatus[indexStatus] = updatedEntity
        } else {
            updatedStatus.add(updatedEntity)
        }

        _attendanceStatus.value = updatedStatus

       /* _attendanceBtnState.value =
            getAttendanceUiState(index, tei, value, color)

        val attendance = AttendanceEntity(
            tei = tei,
            enrollment = enrollment,
            dataElement = datastoreAttendance.value?.status.orEmpty(),
            value = value,
            reasonDataElement = datastoreAttendance.value?.absenceReason,
            reasonOfAbsence = reasonOfAbsence,
            date = eventDate.value,
        )*/

        attendanceCache.removeIf { it.tei == tei }
        attendanceCache.add(updatedEntity)

        if (hasPersisted) {
            repository.save(
                ou = ou,
                program = program.value,
                programStage = datastoreAttendance.value?.programStage.orEmpty(),
                attendance = updatedEntity,
            )
        }
    }

    private var bulkJob: Job? = null

    fun bulkAttendance(
        value: String,
        reasonOfAbsence: String? = null,
    ) {
        bulkJob?.cancel()

        bulkJob = viewModelScope.launch {

            val updatedList = attendanceStatus.value.toMutableList()

            teiUIds.value.forEach { teiPair ->

                val updatedEntity = AttendanceEntity(
                    tei = teiPair.first,
                    enrollment = teiPair.second,
                    dataElement = datastoreAttendance.value?.status.orEmpty(),
                    value = value,
                    reasonDataElement = datastoreAttendance.value?.absenceReason,
                    reasonOfAbsence = reasonOfAbsence,
                    date = eventDate.value,
                )

                val indexStatus = updatedList.indexOfFirst {
                    it.tei == teiPair.first
                }

                if (indexStatus >= 0) {
                    updatedList[indexStatus] = updatedEntity
                } else {
                    updatedList.add(updatedEntity)
                }

                attendanceCache.removeIf { it.tei == teiPair.first }
                attendanceCache.add(updatedEntity)
            }

            _attendanceStatus.value = updatedList

            // 🔥 rebuild UI ONCE from source of truth
            setInitialAttendanceStatus()
        }
    }

    fun bulkSave(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            attendanceCache.forEach { attendance ->
                repository.save(
                    ou = ou.value,
                    program = program.value,
                    programStage = datastoreAttendance.value?.programStage.orEmpty(),
                    attendance = attendance,
                )
            }
            onSuccess()
        }
    }

    fun setAttendanceStep(attendanceStep: ButtonStep) {
        _attendanceStep.value = attendanceStep
    }

    fun getSummary(): List<Summary> {
        val summaries =
            attendanceOptions.value.map { Pair(it.code, Triple(it.iconName, it.icon, it.color)) }
                .map { status ->

                    val count = attendanceCache.count { it.value.equals(status.first, true) }

                    Summary(
                        count,
                        status.second.first,
                        status.second.second,
                        status.second.third,
                    )
                }

        return summaries
    }

    // -------------------------------
    // HELPERS
    // -------------------------------

    fun clearCache() {
        attendanceCache.clear()
        attendanceBtnStateCache.clear()
        _attendanceBtnState.value = emptyList()
        _absenceStateCache.value = emptyList()
        _formData.value = emptyList()
    }

    fun reset() {
        clearCache()
        _isOnlyAbsence.value = false
        _absenceState.value = Absence()
        _attendanceStep.value = ButtonStep.EDITING
        _datastoreAttendance.value = null
        _attendanceOptions.value = emptyList()
        _formFields.value = emptyList()
        _fieldState.value = emptyList()
        _attendanceStatus.value = emptyList()
        _isLoading.value = false
    }

    fun refreshOnSave() {
        reset()
        setProgram(program.value)
    }
}