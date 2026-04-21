package org.saudigitus.emis.ui.performance

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.dhis2.commons.date.DateUtils
import org.dhis2.form.model.ActionType
import org.dhis2.form.model.RowAction
import org.hisp.dhis.android.core.common.ValueType
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.FormRepository
import org.saudigitus.emis.data.model.EventTuple
import org.saudigitus.emis.service.RuleEngineRepository
import org.saudigitus.emis.ui.attendance.ButtonStep
import org.saudigitus.emis.ui.base.BaseViewModel
import org.saudigitus.emis.ui.form.Field
import org.saudigitus.emis.utils.DateHelper
import javax.inject.Inject

class PerformanceViewModel
@Inject constructor(
    private val repository: DataManager,
    private val formRepository: FormRepository,
    private val ruleRepository: RuleEngineRepository,
) : BaseViewModel(repository) {

    private val viewModelState = MutableStateFlow(
        PerformanceUiState(
            toolbarHeaders = this.toolbarHeaders.value,
            students = this.teis.value,
            isValidating = false
        ),
    )

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value,
        )

    private val _cache = MutableStateFlow<List<EventTuple>>(emptyList())
    val cache: StateFlow<List<EventTuple>> = _cache

    private val _programStage = MutableStateFlow("")
    private val programStage: StateFlow<String> = _programStage

    private val _dataElement = MutableStateFlow("")
    private val dataElement: StateFlow<String> = _dataElement

    private val _saveOnce = MutableStateFlow(0)
    private val saveOnce: StateFlow<Int> = _saveOnce

    private val fieldValidationJobs = mutableMapOf<String, Job>()

    init {
        _toolbarHeaders.update {
            it.copy(
                title = "Performance",
                subtitle = null,
            )
        }
        viewModelState.update {
            it.copy(toolbarHeaders = this.toolbarHeaders.value)
        }
    }

    override fun setConfig(program: String) {}

    override fun setProgram(program: String) {
        _program.value = program
    }

    fun loadSubjects(stage: String) {
        viewModelScope.launch {
            viewModelState.update {
                it.copy(subjects = repository.getSubjects(stage))
            }
        }
    }

    override fun setDate(date: String) {
        _toolbarHeaders.update {
            it.copy(subtitle = DateHelper.formatDateWithWeekDay(date))
        }
        viewModelState.update {
            it.copy(toolbarHeaders = toolbarHeaders.value)
        }
    }

    override fun save() {
        viewModelScope.launch {
            when (buttonStep.value) {
                ButtonStep.EDITING -> {
                    setButtonStep(ButtonStep.HOLD_SAVING)
                }
                ButtonStep.HOLD_SAVING -> {
                    setButtonStep(ButtonStep.SAVING)
                }
                else -> {
                    if (!viewModelState.value.isValidating) {
                        cache.value.forEach { item ->
                            formRepository.save(item)
                        }

                        _cache.value = emptyList()
                        setButtonStep(ButtonStep.EDITING)
                    }
                }
            }
        }
    }

    fun onClickNext(
        tei: String,
        ou: String,
        fieldData: Triple<String, String?, ValueType?>,
    ) {
        val data = mutableListOf<EventTuple>()
        data.addAll(cache.value)

        val eventTuple = EventTuple(
            ou,
            program.value,
            programStage.value,
            tei,
            RowAction(
                id = dataElement.value.ifEmpty { fieldData.first },
                type = ActionType.ON_NEXT,
                value = fieldData.second,
                valueType = fieldData.third,
            ),
            eventDate.value,
        )

        data.removeIf { it.tei == tei }

        data.add(eventTuple)

        _cache.value = data
        viewModelState.update { it.copy(isValidating = false) }
    }

    private fun getFields(stage: String, dl: String) {
        viewModelScope.launch {
            _programStage.value = stage
            viewModelState.update {
                it.copy(formFields = formRepository.keyboardInputTypeByStage(program.value, stage, dl))
            }
        }
    }

    fun setDefault(
        stage: String,
        dl: String,
    ) {
        if (saveOnce.value == 0) {
            _saveOnce.value = 1
            getFields(stage, dl)
            updateDataFields(dl)
        }
    }

    fun updateDataFields(dl: String) {
        getFields(programStage.value, dl)
        viewModelScope.launch {
            _dataElement.value = dl
            formRepository
                .getEvents(
                    ou = ou.value,
                    program = program.value,
                    programStage = programStage.value,
                    dataElement = dl,
                    teis = teiUIds.value.map { it.first },
                ).conflate()
                .distinctUntilChanged()
                .collectLatest { events ->
                    Log.e("EVENTS", "$events")
                    viewModelState.update {
                        it.copy(formData = events)
                    }
                }
        }
    }

    fun updateTEISList() {
        viewModelState.update {
            it.copy(students = this.teis.value)
        }
    }

    fun fieldState(
        key: String,
        event: String,
        dataElement: String,
        value: String,
        valueType: ValueType?,
    ) {
        val currentFields = viewModelState.value.fieldsState.toMutableList()
        val index = currentFields.indexOfFirst { it.key == key && it.dataElement == dataElement }

        val field = Field(
            key = key,
            event = event,
            dataElement = dataElement,
            value = value,
            valueType = valueType,
            hasError = false,
            errorMessage = null,
        )

        if (index >= 0) {
            currentFields[index] = field
        } else {
            currentFields.add(field)
        }

        viewModelState.update { it.copy(fieldsState = currentFields) }

        fieldValidationJobs[key]?.cancel()
        viewModelState.update { it.copy(isValidating = true) }

        fieldValidationJobs[key] = viewModelScope.launch {
            try {
                val error = validateDataEntry(event, value)

                val updatedFields = viewModelState.value.fieldsState.toMutableList()
                val idx =
                    updatedFields.indexOfFirst { it.key == key && it.dataElement == dataElement }

                if (idx >= 0) {
                    val validatedField = updatedFields[idx].copy(
                        hasError = error != null,
                        errorMessage = error,
                    )
                    updatedFields[idx] = validatedField
                    viewModelState.update { it.copy(fieldsState = updatedFields) }
                }

                val stillValidating = fieldValidationJobs.any { it.value.isActive }
                viewModelState.update { it.copy(isValidating = stillValidating) }
            } catch (_: Exception) {
                viewModelState.update { it.copy(isValidating = false) }
            }
        }
    }

    private suspend fun validateDataEntry(
        event: String,
        value: String,
    ): String? {
        val effect = ruleRepository.evaluateDataEntry(
            ou = ou.value,
            program = program.value,
            stage = programStage.value,
            dataElement = dataElement.value,
            event = event,
            eventDate = DateHelper.formatDate(DateUtils.getInstance().today.time).orEmpty(),
            value = value,
        )
        return effect?.ruleAction?.values["content"]
    }
}
