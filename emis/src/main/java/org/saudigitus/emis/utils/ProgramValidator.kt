package org.saudigitus.emis.utils

import org.hisp.dhis.android.core.D2
import org.saudigitus.emis.data.model.app_config.EMISConfig
import javax.inject.Inject

class ProgramValidator @Inject constructor(private val d2: D2) {
    fun isSEMIS(program: String): Boolean {
        val dataStore = d2.dataStoreModule()
            .dataStore()
            .byNamespace().eq("semis")
            .byKey().eq(Constants.KEY)
            .one().blockingGet()

        val config = EMISConfig.fromJson(decodeJson(dataStore?.value())) ?: emptyList()
        return config.find { it.program == program } != null
    }
}