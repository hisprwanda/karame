package org.saudigitus.emis.ui.subjects

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.ui.base.BaseViewModel
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.utils.Constants
import javax.inject.Inject

class SubjectViewModel
@Inject constructor(
    private val repository: DataManager,
) : BaseViewModel(repository) {

    private val _uiState = MutableStateFlow(
        SubjectUIState(ToolbarHeaders(title = "Subjects")),
    )
    val uiState: StateFlow<SubjectUIState> = _uiState

    private val _programStage = MutableStateFlow("")
    val programStage: StateFlow<String> = _programStage

    override fun setConfig(program: String) {
        viewModelScope.launch {
            val config = repository.getConfig(Constants.KEY)?.find { it.program == program }

            if (config?.performance != null) {
                val stages = config.performance.programStages
                    ?.filterNotNull()
                    ?: emptyList()

                _uiState.update {
                    it.copy(filters = repository.getTerms(stages))
                }

                val selected = programStage.value.ifEmpty {
                    uiState.value.filters.getOrNull(0)?.id
                }

                if (selected != null) {
                    performOnFilterClick(selected)
                }
            }
        }
    }

    override fun setProgram(program: String) {
        setConfig(program)
    }

    override fun setDate(date: String) {}

    override fun save() {}

    fun performOnFilterClick(stage: String) {
        _programStage.value = stage
        viewModelScope.launch {
            _uiState.update {
                it.copy(subjects = repository.getSubjects(stage))
            }
        }
    }
}
