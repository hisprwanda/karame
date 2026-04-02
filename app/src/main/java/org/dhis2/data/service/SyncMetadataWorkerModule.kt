package org.dhis2.data.service

import dagger.Module
import dagger.Provides
import org.dhis2.commons.di.dagger.PerService
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.prefs.PreferenceProvider
import org.dhis2.commons.resources.ResourceManager
import org.dhis2.data.service.workManager.WorkManagerController
import org.dhis2.utils.analytics.AnalyticsHelper
import org.hisp.dhis.android.core.D2
import org.saudigitus.emis.data.local.repository.SyncHelperRepository
import org.saudigitus.emis.network.HttpClientHelper

@Module
class SyncMetadataWorkerModule {
    @Provides
    @PerService
    fun syncRepository(d2: D2): SyncRepository {
        return SyncRepositoryImpl(d2)
    }

    @Provides
    @PerService
    fun provideHttpClientHelper(
        d2: D2
    ): HttpClientHelper = HttpClientHelper(d2)

    @Provides
    @PerService
    fun syncHelperRepository(
        d2: D2,
        httpClientHelper: HttpClientHelper,
        networkUtils: NetworkUtils,
        resourceManager: ResourceManager,
    ): SyncHelperRepository {
        return SyncHelperRepository(d2, httpClientHelper, networkUtils, resourceManager)
    }

    @Provides
    @PerService
    internal fun syncPresenter(
        d2: D2,
        preferences: PreferenceProvider,
        workManagerController: WorkManagerController,
        analyticsHelper: AnalyticsHelper,
        syncStatusController: SyncStatusController,
        syncRepository: SyncRepository,
        syncHelperRepository: SyncHelperRepository
    ): SyncPresenter {
        return SyncPresenterImpl(
            d2,
            preferences,
            workManagerController,
            analyticsHelper,
            syncStatusController,
            syncRepository,
            syncHelperRepository,
        )
    }
}
