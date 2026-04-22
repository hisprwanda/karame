package org.saudigitus.emis.ui.home

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.dhis2.commons.Constants.DATA_SET_NAME
import org.dhis2.commons.Constants.PROGRAM_UID
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.model.Module
import org.saudigitus.emis.data.model.OU
import org.saudigitus.emis.data.model.app_config.EMISConfigItem
import org.saudigitus.emis.data.model.app_config.Filters
import org.saudigitus.emis.data.model.app_config.Registration
import org.saudigitus.emis.helper.ISEMISSync
import org.saudigitus.emis.ui.base.BaseViewModel
import org.saudigitus.emis.ui.components.DropdownItem
import org.saudigitus.emis.ui.components.DropdownState
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.ui.teis.FilterType
import org.saudigitus.emis.utils.Constants
import javax.inject.Inject


class HomeViewModel
@Inject constructor(
    private val repository: DataManager,
    private val semisSync: ISEMISSync,
) : BaseViewModel(repository) {

    private val _filter = MutableStateFlow<Filters?>(null)
    private val filter: StateFlow<Filters?> = _filter

    private val _registration = MutableStateFlow<Registration?>(null)
    private val registration: StateFlow<Registration?> = _registration

    private val _isFilterOpened = MutableStateFlow(true)
    private val isFilterOpened: StateFlow<Boolean> = _isFilterOpened

    private val viewModelState = MutableStateFlow(
        HomeUiState(
            toolbarHeaders = this.toolbarHeaders.value,
        ),
    )

    val uiState = viewModelState
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            viewModelState.value,
        )

    override fun setConfig(program: String) {
    }

    private suspend fun loadFiltersSequentially() {
        val filterType = mapOf(
            "grade" to FilterType.GRADE,
            "class" to FilterType.SECTION
        )

        val results = mutableListOf<DropdownState>()

        val academicYearId = schoolCalendar.value?.academicYear
        var academicYearState: DropdownState? = null

        academicYearId?.let {
            val options = options(academicYearId)
            val displayName = getDataElementName(academicYearId)
            academicYearState = DropdownState(
                FilterType.ACADEMIC_YEAR,
                displayName = displayName,
                data = options,
            )

            setAcademicYear(options.find { it.code == currentSchoolCalendar.value?.academicYear?.code })
        }

        filter.value?.dataElements?.forEach { item ->
            val elementId = item?.dataElement.orEmpty()
            if (elementId.isBlank()) return@forEach

            val options = options(elementId)
            val displayName = getDataElementName(elementId)
            val type = filterType.getOrDefault(item?.code, FilterType.NONE)

            val state = DropdownState(
                type,
                displayName = displayName,
                order = item?.order ?: 0,
                data = options,
            )

            results.add(state)
        }

        results.sortBy { r -> r.order }
        viewModelState.update {
            it.copy(
                academicYearState = academicYearState,
                dataElementFilters = results
            )
        }
    }


    override fun setProgram(program: String) {
        _program.value = program

        viewModelScope.launch {
            val config = repository.getConfig(Constants.KEY)?.find { it.program == program }

            if (config != null) {
                _registration.value = config.registration
                _filter.value = config.filters

                val modules = moduleVisibility(config)

                viewModelState.update {
                    it.copy(
                        key = config.key,
                        trackedEntityType = repository.getTrackedEntityType(program).orEmpty(),
                        modules = modules
                    )
                }
                loadFiltersSequentially()
            }
        }
    }

    private fun moduleVisibility(config: EMISConfigItem?): List<Module> {
        return listOf(
            Module(
                key = "absenteeism",
                display = config?.absenteeism?.enabled ?: false
            ),
            Module(
                key = "attendance",
                display = config?.attendance?.enabled ?: false
            ),
            Module(
                key = "performance",
                display = config?.performance?.enabled ?: false
            )
        )
    }

    override fun setDate(date: String) {}
    override fun save() {}

    private suspend fun getDataElementName(uid: String) =
        repository.getDataElement(uid)?.displayFormName().orEmpty()

    fun refreshData() {
        getTeis()
    }

    private fun getTeis() {
        viewModelScope.launch {
            if (!viewModelState.value.isNull) {
                val dataElements = listOfNotNull(
                    schoolCalendar.value?.academicYear,
                    registration.value?.grade,
                    registration.value?.section,
                )

                repository.getTeisBy(
                    ou = "${viewModelState.value.school?.uid}",
                    program = "${uiState.value.programSettings?.getString(PROGRAM_UID)}",
                    stage = "${registration.value?.programStage}",
                    dataElementIds = dataElements,
                    dataValues = viewModelState.value.options,
                ).collect { teiList ->
                    setTeis(teiList)
                }

                setInfoCard(
                    viewModelState.updateAndGet {
                        it.copy(
                            isLoading = false,
                            isFetchingTei = false,
                            infoCard = InfoCard(
                                grade = viewModelState.value.grade?.itemName.orEmpty(),
                                section = viewModelState.value.section?.itemName.orEmpty(),
                                academicYear = viewModelState.value.academicYear?.itemName.orEmpty(),
                                orgUnitName = viewModelState.value.school?.displayName.orEmpty(),
                                teiCount = teis.value.size,
                                isStaff = viewModelState.value.isStaff,
                            ),
                        )
                    }.infoCard,
                )
            }
        }
    }

    fun setBundle(bundle: Bundle?) {
        setConfig(bundle?.getString(PROGRAM_UID).orEmpty())

        _toolbarHeaders.update {
            it.copy(title = "${bundle?.getString(DATA_SET_NAME)}")
        }
        viewModelState.update {
            it.copy(
                toolbarHeaders = toolbarHeaders.value,
                programSettings = bundle,
            )
        }
    }

    private fun <T> updateToolbar(obj: T?): ToolbarHeaders {
        return when (obj) {
            is DropdownItem -> {
                val subtitle = if (viewModelState.value.school?.displayName != null) {
                    "${obj.itemName} | ${viewModelState.value.school?.displayName}"
                } else {
                    obj.itemName
                }

                _toolbarHeaders.update {
                    it.copy(subtitle = subtitle)
                }

                toolbarHeaders.value
            }

            is OU -> {
                val subtitle = if (viewModelState.value.academicYear?.itemName != null) {
                    "${viewModelState.value.academicYear?.itemName} | ${obj.displayName}"
                } else {
                    obj.displayName
                }

                _toolbarHeaders.update {
                    it.copy(subtitle = subtitle)
                }

                toolbarHeaders.value
            }

            else -> {
                toolbarHeaders.value
            }
        }
    }

    private fun setAcademicYear(academicYear: DropdownItem?) {
        viewModelState.update {
            it.copy(
                toolbarHeaders = updateToolbar(academicYear),
                academicYear = academicYear,
            )
        }
        invokeInFilters()
    }

    private suspend fun reloadFilters(): MutableList<DropdownState> {
        val filters = uiState.value.dataElementFilters.toMutableList()

        if (filters.isNotEmpty()) {
            val isRemoved = filters.removeIf { f -> f.filterType == FilterType.GRADE }

            if (isRemoved) {
                filters.add(
                    DropdownState(
                        FilterType.GRADE,
                        displayName = getDataElementName("${registration.value?.grade}"),
                        data = options("${registration.value?.grade}"),
                    ),
                )
            }
        }
        filters.sortBy { it.order }

        return filters
    }

    private fun setSchool(ou: OU?) {
        viewModelScope.launch {
            setOU("${ou?.uid}")

            viewModelState.update {
                it.copy(
                    toolbarHeaders = updateToolbar(ou),
                    school = ou,
                )
            }
            val updatedFilters = async { reloadFilters() }.await()
            viewModelState.update { it.copy(dataElementFilters = updatedFilters) }
        }
        invokeInFilters()
    }

    private fun setGrade(grade: DropdownItem?) {
        viewModelState.update {
            it.copy(grade = grade)
        }
        invokeInFilters()
    }

    private fun setSection(section: DropdownItem?) {
        viewModelState.update {
            it.copy(section = section)
        }
        invokeInFilters()
    }

    private suspend fun options(uid: String) = repository.getOptions(
        ou = viewModelState.value.school?.uid,
        program = program.value,
        dataElement = uid,
    )

    private fun closeFilterSection() {
        if (infoCard.value.hasData() && isFilterOpened.value) {
            viewModelState.update {
                it.copy(displayFilters = false)
            }
        }
    }

    private fun onFilterClick() {
        _isFilterOpened.value = !isFilterOpened.value
        viewModelState.update {
            it.copy(displayFilters = !it.displayFilters)
        }
    }

    private fun <T> onFilterItemClick(filterType: FilterType, filterItem: T) {
        when (filterType) {
            FilterType.ACADEMIC_YEAR -> {
                setAcademicYear(filterItem as DropdownItem)
            }

            FilterType.GRADE -> {
                setGrade(filterItem as DropdownItem)
            }

            FilterType.SECTION -> {
                setSection(filterItem as DropdownItem)
            }

            FilterType.SCHOOL -> {
                setSchool(filterItem as OU)
            }

            FilterType.NONE -> {}
        }
    }

    fun onUiEvent(homeUiEvent: HomeUiEvent) {
        when (homeUiEvent) {
            is HomeUiEvent.HideShowFilter -> {
                onFilterClick()
            }

            is HomeUiEvent.OnFilterChange<*> -> {
                onFilterItemClick(homeUiEvent.filterType, homeUiEvent.obj)
            }

            is HomeUiEvent.OnDownloadStudent -> {
                viewModelScope.launch {
                    val dataElementIds = listOf(
                        schoolCalendar.value?.academicYear,
                        registration.value?.grade,
                        registration.value?.section,
                    ).mapNotNull { it }

                    val dataValues = viewModelState.value.options

                    if (dataValues.isNotEmpty() && program.value.isNotEmpty()) {
                        semisSync.downloadTEIsByUids(
                            ou = viewModelState.value.school?.uid.orEmpty(),
                            program = program.value,
                            dataElementIds = dataElementIds,
                            dataValues = dataValues,
                        )
                    }
                }
            }

            else -> {}
        }
    }

    private fun invokeInFilters() {
        val isFetching =
            if (
                uiState.value.academicYear != null
                && uiState.value.school != null
                && uiState.value.grade != null
                && uiState.value.section != null
                && uiState.value.dataElementFilters.isNotEmpty()
            ) {
                true
            } else if (
                uiState.value.academicYear != null
                && uiState.value.school != null
                && uiState.value.dataElementFilters.isEmpty()
            ) {
                true
            } else {
                false
            }

        viewModelState.update {
            it.copy(isFetchingTei = isFetching)
        }

        if (isFetching) {
            closeFilterSection()
        }
        getTeis()
    }
}
