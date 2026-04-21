package org.dhis2.usescases.teiDashboard.dashboardfragments.indicators

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import dhis2.org.analytics.charts.extensions.isNotCurrent
import dhis2.org.analytics.charts.idling.AnalyticsCountingIdlingResource
import dhis2.org.analytics.charts.ui.AnalyticsAdapter
import dhis2.org.analytics.charts.ui.AnalyticsModel
import dhis2.org.analytics.charts.ui.ChartModel
import dhis2.org.analytics.charts.ui.OrgUnitFilterType
import org.dhis2.R
import org.dhis2.commons.dialogs.AlertBottomDialog
import org.dhis2.commons.orgunitselector.OUTreeFragment
import org.dhis2.databinding.FragmentIndicatorsBinding
import org.dhis2.usescases.general.FragmentGlobalAbstract
import org.hisp.dhis.android.core.common.RelativePeriod
import org.saudigitus.emis.ui.home.analytics.AnalyticsViewModel
import org.saudigitus.emis.ui.home.analytics.AnalyticsViewModelFactory
import org.saudigitus.emis.ui.home.analytics.CustomAnalyticsScreen
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.ProgramValidator
import javax.inject.Inject

const val VISUALIZATION_TYPE = "VISUALIZATION_TYPE"

class IndicatorsFragment :
    FragmentGlobalAbstract(),
    IndicatorsView {
    @Inject
    lateinit var presenter: IndicatorsPresenter

    @Inject
    lateinit var analyticsViewModelFactory: AnalyticsViewModelFactory

    @Inject
    lateinit var programValidator: ProgramValidator

    private lateinit var analyticsViewModel: AnalyticsViewModel

    private lateinit var binding: FragmentIndicatorsBinding
    private val adapter: AnalyticsAdapter by lazy {
        AnalyticsAdapter().apply {
            onRelativePeriodCallback = {
                chartModel: ChartModel,
                relativePeriod: RelativePeriod?,
                current: RelativePeriod?,
                lineListingColumnId: Int?,
                ->
                relativePeriod?.let {
                    if (it.isNotCurrent()) {
                        showAlertDialogCurrentPeriod(
                            chartModel,
                            relativePeriod,
                            current,
                            lineListingColumnId,
                        )
                    } else {
                        presenter.filterByPeriod(chartModel, mutableListOf(it), lineListingColumnId)
                    }
                }
            }
            onOrgUnitCallback =
                {
                    chartModel: ChartModel,
                    orgUnitFilterType: OrgUnitFilterType,
                    lineListingColumnId: Int?,
                    ->
                    when (orgUnitFilterType) {
                        OrgUnitFilterType.SELECTION ->
                            showOUTreeSelector(
                                chartModel,
                                lineListingColumnId,
                            )

                        else ->
                            presenter.filterByOrgUnit(
                                chartModel,
                                emptyList(),
                                orgUnitFilterType,
                                lineListingColumnId,
                            )
                    }
                }
            onResetFilterCallback = { chartModel, filterType ->
                presenter.resetFilter(chartModel, filterType)
            }
        }
    }
    private val indicatorInjector by lazy { IndicatorInjector(this) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        indicatorInjector.inject(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_indicators,
                container,
                false,
            )
        binding.indicatorsRecycler.adapter = adapter
        return if (programValidator.isSEMIS(arguments?.getString(Constants.ANALYTICS_PROGRAM).orEmpty())) {
            ComposeView(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                setContent {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        CustomAnalyticsScreen(
                            analyticsViewModel,
                            arguments?.getString(Constants.OWNER_ORG_UNIT).orEmpty(),
                            arguments?.getString(Constants.ACADEMIC_YEAR).orEmpty(),
                            arguments?.getString(Constants.TRACKER_NAME).orEmpty(),
                        )
                    }
                }
            }
        } else {
            binding.root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsViewModel = ViewModelProvider(this, analyticsViewModelFactory)[AnalyticsViewModel::class.java]

        analyticsViewModel.loadAnalyticsIndicators(
            tei = arguments?.getString(Constants.ANALYTICS_TEI).orEmpty(),
            program = arguments?.getString(Constants.ANALYTICS_PROGRAM).orEmpty()
        )
    }

    override fun onResume() {
        super.onResume()
        binding.spinner.visibility = View.VISIBLE
        presenter.init()
    }

    override fun onPause() {
        presenter.onDettach()
        super.onPause()
    }

    override fun swapAnalytics(analytics: List<AnalyticsModel>) {
        try {
            adapter.submitList(analytics)
            binding.spinner.visibility = View.GONE

            if (analytics.isNotEmpty()) {
                binding.emptyIndicators.visibility = View.GONE
            } else {
                binding.emptyIndicators.visibility = View.VISIBLE
            }
        } finally {
            AnalyticsCountingIdlingResource.decrement()
        }
    }

    private fun showAlertDialogCurrentPeriod(
        chartModel: ChartModel,
        relativePeriod: RelativePeriod?,
        current: RelativePeriod?,
        lineListingColumnId: Int?,
    ) {
        val periodList = mutableListOf<RelativePeriod>()
        AlertBottomDialog.instance
            .setTitle(getString(dhis2.org.R.string.include_this_period_title))
            .setMessage(getString(dhis2.org.R.string.include_this_period_body))
            .setNegativeButton(getString(dhis2.org.R.string.no)) {
                relativePeriod?.let { periodList.add(relativePeriod) }
                presenter.filterByPeriod(chartModel, periodList, lineListingColumnId)
            }.setPositiveButton(getString(dhis2.org.R.string.yes)) {
                relativePeriod?.let { periodList.add(relativePeriod) }
                current?.let { periodList.add(current) }
                presenter.filterByPeriod(chartModel, periodList, lineListingColumnId)
            }.show(parentFragmentManager, AlertBottomDialog::class.java.simpleName)
    }

    private fun showOUTreeSelector(
        chartModel: ChartModel,
        lineListingColumnId: Int?,
    ) {
        OUTreeFragment
            .Builder()
            .withPreselectedOrgUnits(
                chartModel.graph.orgUnitsSelected(lineListingColumnId).toMutableList(),
            ).onSelection { selectedOrgUnits ->
                presenter.filterByOrgUnit(
                    chartModel,
                    selectedOrgUnits,
                    OrgUnitFilterType.SELECTION,
                    lineListingColumnId,
                )
            }.build()
            .show(childFragmentManager, "OUTreeFragment")
    }

    override fun onDestroyView() {
        AnalyticsCountingIdlingResource.decrement()
        presenter.onDettach()
        super.onDestroyView()
    }
}
