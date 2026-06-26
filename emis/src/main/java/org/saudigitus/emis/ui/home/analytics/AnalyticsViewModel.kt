package org.saudigitus.emis.ui.home.analytics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.saudigitus.emis.data.local.AnalyticsRepository
import javax.inject.Inject

class AnalyticsViewModel
@Inject constructor(
    private val analyticsRepository: AnalyticsRepository
): ViewModel() {

    private val viewModelState = MutableStateFlow(AnalyticsUiState())

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value,
        )

    fun loadAnalyticsIndicators(
        tei: String,
        program: String,
    ) {
        require(tei.isNotEmpty()) { "tei cannot be empty" }
        require(program.isNotEmpty()) { "program cannot be empty" }

        viewModelScope.launch {
            runCatching {
                analyticsRepository.getAnalyticsGroup(tei, program)
            }.fold(
                onSuccess = { analyticsGroup ->
                    viewModelState.update {
                        it.copy(
                            isLoading = false,
                            analyticsGroup = analyticsGroup
                        )
                    }
                },
                onFailure = {
                    Log.e("ANALYTICS_OUTPUT", "${it.message}")
                    viewModelState.update { it.copy(isLoading = false) }
                }
            )
        }
    }
}

class AnalyticsViewModelFactory(
    private val repository: AnalyticsRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AnalyticsViewModel::class.java)) {
            return AnalyticsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

