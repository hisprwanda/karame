package org.saudigitus.emis.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.hisp.dhis.android.core.D2
import org.saudigitus.emis.data.model.Auth
import org.saudigitus.emis.data.model.app_config.EMISConfig
import org.saudigitus.emis.utils.Constants
import javax.inject.Inject

class HttpClientHelper @Inject constructor(private val d2: D2) {

    private fun getAuth(): Auth? {
        return try {
            val datastore = d2.dataStoreModule().dataStore()
                .byNamespace().eq(Constants.NAMESPACE)
                .byKey().eq(Constants.AUTH_KEY)
                .one().blockingGet()

            EMISConfig.translateFromJson<Auth?>(datastore?.value())
        }catch(_: Exception) {
            null
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    fun httpClient(): HttpClient {
        val auth = getAuth()

        return HttpClient {
            install(DefaultRequest) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Authorization, "Basic ${auth?.key.orEmpty()}")
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        encodeDefaults = true
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        explicitNulls = false
                    },
                    contentType = ContentType.Application.Json
                )
            }
            install(HttpRequestRetry) {
                retryOnException(5, true)
                exponentialDelay()
            }
        }
    }
}