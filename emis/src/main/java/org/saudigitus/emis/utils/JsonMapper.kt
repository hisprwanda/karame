package org.saudigitus.emis.utils

import kotlinx.serialization.json.Json

object JsonMapper {
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
        explicitNulls = false
    }
}