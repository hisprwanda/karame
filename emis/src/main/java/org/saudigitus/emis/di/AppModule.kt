package org.saudigitus.emis.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.form.ui.provider.HintProvider
import org.dhis2.form.ui.provider.HintProviderImpl
import org.hisp.dhis.android.core.D2
import org.saudigitus.emis.data.local.AnalyticsRepository
import org.saudigitus.emis.data.local.AttendanceRepository
import org.saudigitus.emis.data.local.DataManager
import org.saudigitus.emis.data.local.FormRepository
import org.saudigitus.emis.data.local.UserPreferencesRepository
import org.saudigitus.emis.data.local.repository.AnalyticsRepositoryImpl
import org.saudigitus.emis.data.local.repository.AttendanceRepositoryImpl
import org.saudigitus.emis.data.local.repository.DataManagerImpl
import org.saudigitus.emis.data.local.repository.FormRepositoryImpl
import org.saudigitus.emis.data.local.repository.UserPreferencesRepositoryImpl
import org.saudigitus.emis.data.local.util.AttendanceTransformation
import org.saudigitus.emis.helper.ISEMISSync
import org.saudigitus.emis.helper.SEMISSync
import org.saudigitus.emis.service.RuleEngineRepository
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper
import org.saudigitus.emis.utils.Transformations
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesNetworkUtils(
        @ApplicationContext context: Context,
    ): NetworkUtils = NetworkUtils(context)

    @Provides
    @Singleton
    fun providesRuleEngineRepository(d2: D2) = RuleEngineRepository(d2)

    @Provides
    @Singleton
    fun providesTEICardMapper(
        @ApplicationContext context: Context,
        resourcesManager: ResourceManager,
    ) = TEICardMapper(context, resourcesManager)

    @Provides
    @Singleton
    fun providesTransformations(d2: D2): Transformations = Transformations(d2)

    @Provides
    @Singleton
    fun provideAttendanceTransformations(d2: D2, transformations: Transformations) =
        AttendanceTransformation(d2, transformations)

    @Provides
    @Singleton
    fun providesDataManager(
        d2: D2,
        transformations: Transformations,
        networkUtils: NetworkUtils,
        ruleEngineRepository: RuleEngineRepository,
    ): DataManager = DataManagerImpl(d2, transformations, networkUtils, ruleEngineRepository)

    @Provides
    @Singleton
    fun providesSEMISSync(
        d2: D2,
        networkUtils: NetworkUtils,
    ): ISEMISSync = SEMISSync(d2, networkUtils)

    @Provides
    @Singleton
    fun providesUserPreferences(
        @ApplicationContext context: Context,
    ): UserPreferencesRepository = UserPreferencesRepositoryImpl(context)

    @Provides
    @Singleton
    fun providesHintProvider(@ApplicationContext context: Context): HintProvider =
        HintProviderImpl(context)

    @Provides
    @Singleton
    fun providesFormRepository(
        d2: D2,
        hintProvider: HintProvider,
        dataManager: DataManager,
    ): FormRepository {
        return FormRepositoryImpl(d2, hintProvider, dataManager)
    }


    @Provides
    @Singleton
    fun providesAttendanceRepository(
        d2: D2,
        dataManager: DataManager,
        attendanceTransformation: AttendanceTransformation
    ): AttendanceRepository =
        AttendanceRepositoryImpl(d2, dataManager, attendanceTransformation)

    @Provides
    @Singleton
    fun providesAnalyticsRepository(
        d2: D2,
        dataManager: DataManager,
        resourcesManager: ResourceManager
    ): AnalyticsRepository = AnalyticsRepositoryImpl(d2, dataManager, resourcesManager)
}
