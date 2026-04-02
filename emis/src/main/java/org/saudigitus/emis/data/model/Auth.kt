package org.saudigitus.emis.data.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Auth(
    val baseUrl: String?,
    val endpoint: String?,
    val type: String?,
    val token: String?,
    val key: String? = null
) {
    fun validateAndFormatBaseUrl(): String? {
        if (baseUrl.isNullOrBlank()) return null

        return try {
            val uri = java.net.URI(baseUrl.trim())

            if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) {
                null
            } else {
                baseUrl.trim().removeSuffix(" / ").trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatEndpoint(): String? {
        if (endpoint.isNullOrBlank()) return null
        return endpoint.removePrefix("/").trim()
    }


    override fun toString(): String {
        return "{baseUrl: ${validateAndFormatBaseUrl()}, type: $type, token: $token}"
    }
}
