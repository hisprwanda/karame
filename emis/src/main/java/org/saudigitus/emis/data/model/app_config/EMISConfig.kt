package org.saudigitus.emis.data.model.app_config

import android.util.Log
import kotlinx.serialization.builtins.ListSerializer
import org.saudigitus.emis.utils.JsonMapper

class EMISConfig {
    companion object {
        fun fromJson(json: String?): List<EMISConfigItem>? {
            if (json.isNullOrEmpty()) return null

            return try {
                JsonMapper.json.decodeFromString(
                    ListSerializer(EMISConfigItem.serializer()),
                    json
                )
            } catch (e: Exception) {
                Log.e("EMISConfig", "fromJson: ${e.message}")
                null
            }
        }

        inline fun <reified T> translateFromJson(json: String?): T? =
            if (json != null) {
                JsonMapper.json.decodeFromString<T>(json)
            } else {
                null
            }
    }
}