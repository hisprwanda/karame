package org.saudigitus.emis.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import org.dhis2.commons.network.NetworkUtils
import org.saudigitus.emis.network.exceptions.NetworkException

abstract class BaseNetwork(
    open val networkUtils: NetworkUtils,
    open val httpClient: HttpClient,
) {

    suspend inline fun <T> safeCall(
        crossinline block: suspend () -> T
    ): Result<T> {
        try {
            if (!networkUtils.isOnline()) {
                return Result.failure(NetworkException.NoInternet)
            }

            return runCatching { block() }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * This is HTTP post method
     * @param route is the API endpoint
     * @param body is the request body
     * @return Result<T> is the response body
     */
    suspend inline fun <reified T> post(route: String, body: T): Result<HttpResponse> =
        safeCall {
            httpClient.post(route) {
                setBody(body)
            }
        }


    /**
     * This is HTTP get method
     * @param route is the API endpoint
     * @return Result<T> is the response body
     */
    suspend inline fun <reified T> get(route: String): Result<T> =
        safeCall {
            httpClient.get(route).body()
        }

    /**
     * This is HTTP put method
     * @param route is the API endpoint
     * @param body is the request body
     * @return Result<T> is the response body
     */
    suspend inline fun <reified T, reified E> put(route: String, body: E): Result<Pair<Int, T>> =
        safeCall {
            val response = httpClient.put(route) {
                setBody(body)
            }

            Pair(response.status.value, response.body<T>())
        }
}
