package org.saudigitus.emis

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.dhis2.commons.Constants
import org.dhis2.commons.navigator.AppNavigator
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.sync.OnDismissListener
import org.dhis2.commons.sync.SyncContext
import org.dhis2.commons.sync.SyncDialog
import org.saudigitus.emis.ui.attendance.AttendanceScreen
import org.saudigitus.emis.ui.attendance.AttendanceViewModel
import org.saudigitus.emis.ui.home.HomeRoute
import org.saudigitus.emis.ui.home.HomeViewModel
import org.saudigitus.emis.ui.performance.PerformanceScreen
import org.saudigitus.emis.ui.performance.PerformanceViewModel
import org.saudigitus.emis.ui.subjects.SubjectScreen
import org.saudigitus.emis.ui.subjects.SubjectViewModel
import org.saudigitus.emis.ui.teis.TeiScreen
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.ui.theme.EMISAndroidTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var teiCardMapper: TEICardMapper

    @Inject
    lateinit var networkUtils: NetworkUtils

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            val isExpandedScreen = (widthSizeClass == WindowWidthSizeClass.Medium) || (widthSizeClass == WindowWidthSizeClass.Expanded)

            EMISAndroidTheme(
                darkTheme = false,
                dynamicColor = false,
            ) {
                viewModel.setBundle(intent?.extras)
                viewModel.setProgram(intent?.extras?.getString(Constants.PROGRAM_UID) ?: "")

                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoutes.HOME_ROUTE,
                    ) {
                        composable(AppRoutes.HOME_ROUTE) {
                            HomeRoute(
                                activity = this@MainActivity,
                                isExpandedScreen = isExpandedScreen,
                                viewModel = viewModel,
                                navController = navController,
                                navBack = { finish() },
                                sync = ::syncProgram,
                            )
                        }
                        composable(AppRoutes.TEI_LIST_ROUTE) {
                            TeiScreen(
                                viewModel = viewModel,
                                teiCardMapper = teiCardMapper,
                                onBack = navController::navigateUp,
                                onSync = ::syncProgram,
                                onCardClick = ::launchTeiDashboard,
                            )
                        }
                        composable(
                            route = "${AppRoutes.ATTENDANCE_ROUTE}/{ou}",
                            arguments = listOf(
                                navArgument("ou") {
                                    type = NavType.StringType
                                },
                            ),
                        ) {
                            val attendanceViewModel: AttendanceViewModel = hiltViewModel()
                            val teis by viewModel.teis.collectAsStateWithLifecycle()
                            val infoCard by attendanceViewModel.infoCard.collectAsStateWithLifecycle()

                            attendanceViewModel.setDefaults(stringResource(R.string.attendance))

                            attendanceViewModel.setProgram(intent?.extras?.getString(Constants.PROGRAM_UID) ?: "")
                            attendanceViewModel.setTeis(teis)
                            attendanceViewModel.setInfoCard(viewModel.infoCard.collectAsStateWithLifecycle().value)
                            attendanceViewModel.setOU(it.arguments?.getString("ou") ?: "")

                            AttendanceScreen(
                                navController,
                                this@MainActivity.supportFragmentManager,
                                attendanceViewModel,
                                teiCardMapper,
                                infoCard,
                                navController::navigateUp,
                                ::syncProgram,
                            )
                        }
                        composable(
                            route = "${AppRoutes.ABSENTEEISM_ROUTE}/{ou}/{academicYear}/{grade}/{section}",
                            arguments = listOf(
                                navArgument("ou") {
                                    type = NavType.StringType
                                },
                                navArgument("academicYear") {
                                    type = NavType.StringType
                                },
                                navArgument("grade") {
                                    type = NavType.StringType
                                },
                                navArgument("section") {
                                    type = NavType.StringType
                                },
                            ),
                        ) {
                            val attendanceViewModel: AttendanceViewModel = hiltViewModel()
                            val infoCard by attendanceViewModel.infoCard.collectAsStateWithLifecycle()

                            attendanceViewModel.setDefaults(stringResource(R.string.absenteeism), true)
                            attendanceViewModel.setOptions(
                                it.arguments?.getString("academicYear") ?: "",
                                it.arguments?.getString("grade") ?: "",
                                it.arguments?.getString("section") ?: "",
                            )
                            attendanceViewModel.setProgram(intent?.extras?.getString(Constants.PROGRAM_UID) ?: "")
                            attendanceViewModel.setInfoCard(viewModel.infoCard.collectAsStateWithLifecycle().value)
                            attendanceViewModel.setOU(it.arguments?.getString("ou") ?: "")

                            AttendanceScreen(
                                navController,
                                this@MainActivity.supportFragmentManager,
                                attendanceViewModel,
                                teiCardMapper,
                                infoCard = infoCard,
                                navController::navigateUp,
                                ::syncProgram,
                            )
                        }
                        composable(
                            route = "${AppRoutes.PERFORMANCE_ROUTE}/{ou}/{stage}/{dataElement}/{subjectName}",
                            arguments = listOf(
                                navArgument("ou") {
                                    type = NavType.StringType
                                },
                                navArgument("stage") {
                                    type = NavType.StringType
                                },
                                navArgument("dataElement") {
                                    type = NavType.StringType
                                },
                                navArgument("subjectName") {
                                    type = NavType.StringType
                                },
                            ),
                        ) {
                            val performanceViewModel = hiltViewModel<PerformanceViewModel>()
                            val uiState by performanceViewModel.uiState.collectAsStateWithLifecycle()
                            val infoCard by performanceViewModel.infoCard.collectAsStateWithLifecycle()
                            val stats by performanceViewModel.cache.collectAsStateWithLifecycle()
                            val teis by viewModel.teis.collectAsStateWithLifecycle()
                            val performanceStep by performanceViewModel.buttonStep.collectAsStateWithLifecycle()

                            val stage = it.arguments?.getString("stage") ?: ""
                            val dl = it.arguments?.getString("dataElement") ?: ""
                            val ou = it.arguments?.getString("ou") ?: ""

                            performanceViewModel.setOU(ou)
                            performanceViewModel.setProgram(intent?.extras?.getString(Constants.PROGRAM_UID) ?: "")
                            performanceViewModel.loadSubjects(stage)
                            performanceViewModel.setTeis(teis, performanceViewModel::updateTEISList)
                            performanceViewModel.setInfoCard(viewModel.infoCard.collectAsStateWithLifecycle().value)
                            performanceViewModel.setDefault(stage, dl)

                            PerformanceScreen(
                                isExpandedScreen = isExpandedScreen,
                                state = uiState,
                                teiCardMapper = teiCardMapper,
                                onNavBack = navController::navigateUp,
                                infoCard = infoCard,
                                defaultSelection = it.arguments?.getString("subjectName") ?: "",
                                setPerformanceState = performanceViewModel::fieldState,
                                performanceStats = Pair("${stats.size}", "${teis.size.minus(stats.size)}"),
                                performanceStep = performanceStep,
                                setDate = performanceViewModel::setDate,
                                onNext = performanceViewModel::onClickNext,
                                step = performanceViewModel::setButtonStep,
                                onFilterClick = performanceViewModel::updateDataFields,
                                onSave = performanceViewModel::save,
                                sync = ::syncProgram,
                            )
                        }
                        composable(
                            route = "${AppRoutes.SUBJECT_ROUTE}/{ou}",
                            arguments = listOf(
                                navArgument("ou") {
                                    type = NavType.StringType
                                },
                            ),
                        ) {
                            val subjectViewModel = hiltViewModel<SubjectViewModel>()
                            val state by subjectViewModel.uiState.collectAsStateWithLifecycle()
                            val stage by subjectViewModel.programStage.collectAsStateWithLifecycle()
                            val infoCard by viewModel.infoCard.collectAsStateWithLifecycle()
                            subjectViewModel.setProgram(intent?.extras?.getString(Constants.PROGRAM_UID) ?: "")
                            val ou = it.arguments?.getString("ou") ?: ""

                            SubjectScreen(
                                state = state,
                                onBack = navController::navigateUp,
                                onFilterClick = subjectViewModel::performOnFilterClick,
                                infoCard = infoCard,
                                onClick = { subjectId, subjectName ->
                                    navController.navigate("${AppRoutes.PERFORMANCE_ROUTE}/$ou/$stage/$subjectId/$subjectName")
                                },
                                sync = ::syncProgram,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun syncProgram(
        refresh : (() -> Unit)? = null,
        offlineAction: (() -> Unit)? = null,
    ) {
        if (networkUtils.isOnline()) {
            SyncDialog(
                activity = this@MainActivity,
                recordUid = viewModel.program.value,
                syncContext = SyncContext.Global(),
                onNoConnectionListener = {
                    Snackbar.make(
                        this.window.decorView.rootView,
                        getString(R.string.sync_offline_check_connection),
                        Snackbar.LENGTH_SHORT,
                    ).show()
                },
                dismissListener = object : OnDismissListener {
                    override fun onDismiss(hasChanged: Boolean) {
                        viewModel.refreshData()
                        refresh?.invoke()
                    }
                }
            ).show()
        } else {
            Snackbar.make(
                this.window.decorView.rootView,
                getString(R.string.sync_offline_check_connection),
                Snackbar.LENGTH_SHORT,
            ).show()
            offlineAction?.invoke()
        }
    }

    private fun launchTeiDashboard(
        tei: String,
        enrollment: String,
    ) {
        AppNavigator(
            activity = this@MainActivity,
            tei = tei,
            program = viewModel.program.value,
            enrollment = enrollment,
            academicYear = viewModel.uiState.value.academicYear?.itemName.orEmpty()
        ).navigateToDashboard()
    }
}
