package org.saudigitus.emis.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.dhis2.commons.Constants
import org.saudigitus.emis.AppRoutes
import org.saudigitus.emis.R
import org.saudigitus.emis.ui.components.DropDown
import org.saudigitus.emis.ui.components.DropDownOu
import org.saudigitus.emis.ui.components.DropDownWithSelectionByCode
import org.saudigitus.emis.ui.components.NavBar
import org.saudigitus.emis.ui.components.NavigationItem
import org.saudigitus.emis.ui.components.ShowCard
import org.saudigitus.emis.ui.components.Toolbar
import org.saudigitus.emis.ui.components.ToolbarActionState
import org.saudigitus.emis.ui.home.analytics.AnalyticsScreen
import org.saudigitus.emis.ui.teis.FilterType
import org.saudigitus.emis.utils.Constants.ABSENTEEISM
import org.saudigitus.emis.utils.Constants.ATTENDANCE
import org.saudigitus.emis.utils.Constants.PERFORMANCE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeRoute(
    activity: FragmentActivity,
    isExpandedScreen: Boolean,
    viewModel: HomeViewModel,
    navController: NavHostController,
    navBack: () -> Unit,
    sync: () -> Unit,
) {
    val internalNavController = rememberNavController()
    var route by rememberSaveable { mutableStateOf(NavigationItem.HOME) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Toolbar(
                headers = uiState.toolbarHeaders,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C98F0),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                navigationAction = { navBack() },
                disableNavigation = false,
                actionState = ToolbarActionState(
                    syncVisibility = true,
                    showFavorite = true,
                ),
                filterAction = {
                    viewModel.onUiEvent(HomeUiEvent.HideShowFilter)
                },
                syncAction = { sync() },
            )
        },
        bottomBar = {
            NavBar(destination = route.ordinal) {
                route = when (it) {
                    NavigationItem.HOME.ordinal -> NavigationItem.HOME
                    NavigationItem.ANALYTICS.ordinal -> NavigationItem.ANALYTICS
                    else -> NavigationItem.NONE
                }
            }
        },
    ) { paddingValues ->
        NavHost(
            internalNavController,
            startDestination = route.name,
        ) {
            composable(NavigationItem.HOME.name) {
                HomeUI(paddingValues, isExpandedScreen, uiState = uiState) {
                    when (it) {
                        is HomeUiEvent.NavTo -> navController.navigate(it.route)
                        is HomeUiEvent.Sync -> sync()
                        else -> {
                            viewModel.onUiEvent(it)
                        }
                    }
                }
            }
            composable(NavigationItem.ANALYTICS.name) {
                AnalyticsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    activity = activity,
                    program = uiState.programSettings?.getString(Constants.PROGRAM_UID).orEmpty()
                )
            }
        }
    }
}

@Composable
fun HomeUI(
    paddingValues: PaddingValues,
    isExpandedScreen: Boolean,
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF2C98F0))
            .padding(paddingValues),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(visible = uiState.displayFilters) {
            Column(
                modifier = Modifier.padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                uiState.academicYearState?.let {
                    AnimatedVisibility(visible = it.data.isNotEmpty()) {
                        DropDownWithSelectionByCode(
                            dropdownState = it,
                            defaultSelection = uiState.filterSelection.first,
                            onItemClick = { item ->
                                onEvent(
                                    HomeUiEvent.OnFilterChange(
                                        FilterType.ACADEMIC_YEAR,
                                        item
                                    )
                                )
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = true) {
                    DropDownOu(
                        placeholder = stringResource(R.string.school),
                        leadingIcon = ImageVector.vectorResource(R.drawable.ic_location_on),
                        selectedSchool = uiState.school,
                        program = uiState.programSettings?.getString(Constants.PROGRAM_UID)
                            .orEmpty(),
                        onItemClick = {
                            onEvent(HomeUiEvent.OnFilterChange(FilterType.SCHOOL, it))
                        }
                    )
                }
                uiState.dataElementFilters.forEach { filter ->
                    val defaultSelection = when (filter.filterType) {
                        FilterType.GRADE -> uiState.filterSelection.second
                        FilterType.SECTION -> uiState.filterSelection.third
                        else -> null
                    }

                    AnimatedVisibility(visible = filter.data.isNotEmpty()) {
                        DropDown(
                            dropdownState = filter,
                            defaultSelection = defaultSelection,
                            onItemClick = { item ->
                                onEvent(HomeUiEvent.OnFilterChange(filter.filterType, item))
                            }
                        )
                    }
                }
            }
        }
        if (uiState.isFetchingTei) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }
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
            horizontalAlignment = Alignment.Start,
        ) {
            ShowCard(
                infoCard = uiState.infoCard,
                onClick = { onEvent(HomeUiEvent.NavTo(AppRoutes.TEI_LIST_ROUTE)) },
            )

            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = if (!isExpandedScreen) {
                    GridCells.Adaptive(128.dp)
                } else {
                    GridCells.Adaptive(200.dp)
                },
                contentPadding = PaddingValues(vertical = 5.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                horizontalArrangement = Arrangement.spacedBy(
                    16.dp,
                    Alignment.CenterHorizontally,
                ),
            ) {
                if (uiState.hasModules(ATTENDANCE)) {
                    item {
                        HomeItem(
                            modifier = Modifier.fillMaxWidth(),
                            icon = painterResource(R.drawable.s_calendar),
                            label = stringResource(R.string.attendance),
                            enabled = uiState.infoCard.hasData(),
                            onClick = {
                                onEvent(HomeUiEvent.NavTo("${AppRoutes.ATTENDANCE_ROUTE}/${uiState.school?.uid}"))
                            },
                        )
                    }
                }
                if (uiState.hasModules(ABSENTEEISM)) {
                    item {
                        HomeItem(
                            modifier = Modifier.fillMaxWidth(),
                            icon = painterResource(R.drawable.s_calendar),
                            label = stringResource(R.string.absenteeism),
                            enabled = uiState.infoCard.hasData(),
                            onClick = {
                                onEvent(
                                    HomeUiEvent.NavTo(
                                        AppRoutes.absenteeismRoute(
                                            uiState.academicYear?.code,
                                            uiState.school?.uid,
                                            uiState.grade?.code,
                                            uiState.section?.code,
                                        ),
                                    ),
                                )
                            },
                        )
                    }
                }
                if (uiState.hasModules(PERFORMANCE)) {
                    item {
                        HomeItem(
                            modifier = Modifier.fillMaxWidth(),
                            icon = painterResource(R.drawable.performance),
                            label = stringResource(R.string.performance),
                            enabled = uiState.infoCard.hasData(),
                            onClick = { onEvent(HomeUiEvent.NavTo("${AppRoutes.SUBJECT_ROUTE}/${uiState.school?.uid}")) },
                        )
                    }
                }
            }
        }
    }
}
