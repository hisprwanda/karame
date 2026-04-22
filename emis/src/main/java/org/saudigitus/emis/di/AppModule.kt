package org.saudigitus.emis.di

import org.dhis2.commons.network.NetworkUtils
import org.dhis2.form.ui.provider.HintProvider
import org.dhis2.form.ui.provider.HintProviderImpl
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.saudigitus.emis.data.local.AnalyticsRepository
import org.saudigitus.emis.data.local.AttendanceRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.FormRepository
import org.saudigitus.emis.data.local.repository.AnalyticsRepositoryImpl
import org.saudigitus.emis.data.local.repository.AttendanceRepositoryImpl
import org.saudigitus.emis.data.local.repository.DataManagerImpl
import org.saudigitus.emis.data.local.repository.FormRepositoryImpl
import org.saudigitus.emis.data.local.util.AttendanceTransformation
import org.saudigitus.emis.helper.ISEMISSync
import org.saudigitus.emis.helper.SEMISSync
import org.saudigitus.emis.service.RuleEngineRepository
import org.saudigitus.emis.ui.attendance2.AttendanceViewModel
import org.saudigitus.emis.ui.home.HomeViewModel
import org.saudigitus.emis.ui.performance.PerformanceViewModel
import org.saudigitus.emis.ui.subjects.SubjectViewModel
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.utils.Transformations

val emisModule = module {

    single<NetworkUtils> {
        NetworkUtils(get())
    }

    single<RuleEngineRepository> {
        RuleEngineRepository(get())
    }

    single<TEICardMapper> {
        TEICardMapper(
            context = get(),
            resourceManager = get()
        )
    }

    single<Transformations> {
        Transformations(get())
    }

    single<AttendanceTransformation> {
        AttendanceTransformation(
            d2 = get(),
            transformation = get()
        )
    }

    single<DataManager> {
        DataManagerImpl(
            d2 = get(),
            transformations = get(),
            networkUtils = get(),
            ruleEngineRepository = get()
        )
    }

    single<ISEMISSync> {
        SEMISSync(
            d2 = get(),
            networkUtils = get()
        )
    }

    single<HintProvider> {
        HintProviderImpl(get())
    }

    single<FormRepository> {
        FormRepositoryImpl(
            d2 = get(),
            hintProvider = get(),
            dataManager = get()
        )
    }

    single<AttendanceRepository> {
        AttendanceRepositoryImpl(
            d2 = get(),
            repository = get(),
            transformations = get()
        )
    }

    single<AnalyticsRepository> {
        AnalyticsRepositoryImpl(
            d2 = get(),
            repository = get(),
            resourceManager = get()
        )
    }

    viewModelOf(::AttendanceViewModel)
    viewModelOf(::PerformanceViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::SubjectViewModel)
}
