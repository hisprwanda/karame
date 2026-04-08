package org.saudigitus.emis.data.local.repository

import android.widget.Toast
import dhis2.org.R
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dhis2.commons.bindings.enrollment
import org.dhis2.commons.date.toUi
import org.dhis2.commons.network.NetworkUtils
import org.dhis2.commons.resources.ResourceManager
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.imports.ImportStatus
import org.hisp.dhis.android.core.imports.TrackerImportConflictTableInfo
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstanceTableInfo
import org.saudigitus.emis.data.model.Auth
import org.saudigitus.emis.data.model.DataValue
import org.saudigitus.emis.data.model.EventBulkRequest
import org.saudigitus.emis.data.model.EventRequest
import org.saudigitus.emis.data.model.TransferredTei
import org.saudigitus.emis.data.model.TransferredType
import org.saudigitus.emis.data.model.app_config.EMISConfig
import org.saudigitus.emis.data.model.app_config.TransferEvent
import org.saudigitus.emis.network.BaseNetwork
import org.saudigitus.emis.network.HttpClientHelper
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.DateHelper
import org.saudigitus.emis.utils.Utils.mapToType
import timber.log.Timber
import javax.inject.Inject

class SyncHelperRepository @Inject constructor(
    private val d2: D2,
    httpClientHelper: HttpClientHelper,
    override val networkUtils: NetworkUtils,
    private val resourceManager: ResourceManager,
) : BaseNetwork(networkUtils, httpClientHelper.httpClient()) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun cleanBasedOnEvents() {
        scope.launch(Dispatchers.IO) {
            val transferredTrackers = getTransferredTeis()
                .filter { it.type == TransferredType.TRACKER_ID }
                .map { it.value }

            val ownershipConflicts = d2.importModule().trackerImportConflicts()
                .byTrackedEntityInstanceUid().`in`(transferredTrackers)
                .byStatus().eq(ImportStatus.ERROR)
                .blockingGet()

            val trackerWithConflicts = ownershipConflicts.mapNotNull { it.trackedEntityInstance() }

            val trackerSet = mutableSetOf<String>()
            trackerSet.addAll(trackerWithConflicts)
            trackerSet.addAll(transferredTrackers)

            val bulkEvents = buildEventRequest(trackerSet.toList())

            Timber.tag("SYNC_HELPER").e("Cleaning...")

            if (bulkEvents != null && checkServerAvailability()) {
                val auth = getAuth()
                val baseUrl = d2.systemInfoModule().systemInfo().blockingGet()?.contextPath()
                    ?.removeSuffix("/")?.trim()
                    ?: auth?.validateAndFormatBaseUrl().orEmpty()

                val postEventRoute = "$baseUrl/${auth?.formatEndpoint().orEmpty()}"

                post<EventBulkRequest>(postEventRoute, bulkEvents)
                    .onSuccess {
                        if (it.status == HttpStatusCode.OK || it.status == HttpStatusCode.Created) {
                            pruneNotOwnedTEs(trackerSet.toList())
                        }
                    }.onFailure {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                resourceManager.context,
                                resourceManager.getString(R.string.internet_connection_is_unstable),
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
            } else if (!checkServerAvailability()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        resourceManager.context,
                        resourceManager.getString(R.string.server_is_unavailable),
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            } else {
                pruneNotOwnedTEs(trackerSet.toList())
            }
        }
    }

    private suspend fun getTransferredTeis() =
        withContext(Dispatchers.IO) {
            try {
                val dataStore = d2.dataStoreModule()
                    .dataStore()
                    .byNamespace().eq("semis")
                    .byKey().eq("transfers")
                    .one().blockingGet()

                val transferred = EMISConfig.translateFromJson<TransferEvent>(dataStore?.value())
                    ?: return@withContext emptyList()

                val requiredDataElements = listOfNotNull(
                    transferred.academicYear,
                    transferred.enrollment,
                    transferred.trackerId,
                )

                d2.eventModule().events()
                    .byProgramUid().eq(transferred.program)
                    .withTrackedEntityDataValues()
                    .byDeleted().isFalse
                    .blockingGet()
                    .filter { event ->
                        val values = event.trackedEntityDataValues().orEmpty()

                        val presentElements = values
                            .map { it.dataElement() }

                        requiredDataElements.all { it in presentElements }
                    }
                    .flatMap { event ->
                        event.trackedEntityDataValues().orEmpty()
                            .mapNotNull { dataValue ->
                                val de = dataValue.dataElement()
                                val value = dataValue.value()

                                val type = mapToType(de.orEmpty(), transferred)

                                type?.let {
                                    TransferredTei(
                                        dataElement = de.orEmpty(),
                                        value = value.orEmpty(),
                                        type = it
                                    )
                                }
                            }
                    }
            } catch (_: Exception) {
                emptyList()
            }
        }

    private fun buildEventRequest(trackerIds: List<String>): EventBulkRequest? {
        val eventRequests = d2.eventModule().events()
            .byTrackedEntityInstanceUids(trackerIds)
            .byAggregatedSyncState().`in`(State.TO_POST, State.TO_UPDATE, State.ERROR)
            .withTrackedEntityDataValues()
            .blockingGet()
            .map { event ->
                val dataValues = event.trackedEntityDataValues()?.map { dataValue ->
                    DataValue(
                        dataElement = dataValue.dataElement().orEmpty(),
                        value = dataValue.value()
                    )
                } ?: emptyList()

                val trackedEntityInstance = d2.enrollment(event.enrollment().orEmpty())
                    ?.trackedEntityInstance().orEmpty()

                EventRequest(
                    orgUnit = event.organisationUnit().orEmpty(),
                    program = event.program().orEmpty(),
                    programStage = event.programStage().orEmpty(),
                    status = event.status()?.name.orEmpty(),
                    trackedEntity = trackedEntityInstance,
                    enrollment = event.enrollment().orEmpty(),
                    occurredAt = DateHelper.formatDate(event.eventDate().toUi()).orEmpty(),
                    dataValues = dataValues
                )
            }

        return if (eventRequests.isNotEmpty()) {
            EventBulkRequest(events = eventRequests)
        } else null
    }

    private suspend fun pruneNotOwnedTEs(trackers: List<String>) = withContext(Dispatchers.IO) {
        trackers.forEach { tei ->
            d2.databaseAdapter().delete(
                TrackedEntityInstanceTableInfo.TABLE_INFO.name(),
                "${TrackedEntityInstanceTableInfo.Columns.UID} = '$tei'",
                emptyArray()
            )

            d2.databaseAdapter().delete(
                TrackerImportConflictTableInfo.TABLE_INFO.name(),
                "${TrackerImportConflictTableInfo.Columns.TRACKED_ENTITY_INSTANCE} = '$tei'",
                emptyArray()
            )
        }
    }

    private suspend fun checkServerAvailability() = withContext(Dispatchers.IO) {
        return@withContext try {
            d2.systemInfoModule().ping().blockingGet()
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun getAuth(): Auth? {
        return try {
            val datastore = d2.dataStoreModule().dataStore()
                .byNamespace().eq(Constants.NAMESPACE)
                .byKey().eq(Constants.AUTH_KEY)
                .one().blockingGet()

            EMISConfig.translateFromJson<Auth?>(datastore?.value())
        } catch (_: Exception) {
            null
        }
    }
}