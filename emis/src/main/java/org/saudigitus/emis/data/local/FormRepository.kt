package org.saudigitus.emis.data.local

import kotlinx.coroutines.flow.Flow
import org.saudigitus.emis.data.model.EventTuple
import org.saudigitus.emis.data.model.Option
import org.saudigitus.emis.ui.form.FormData
import org.saudigitus.emis.ui.form.FormField
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState

interface FormRepository {
    suspend fun save(eventTuple: EventTuple): Boolean
    suspend fun keyboardInputTypeByStage(program: String, stage: String, dl: String): List<FormField>

    suspend fun getFormFields(
        program: String,
        stage: String,
        dl: String? = null
    ): List<FormFieldState>

    suspend fun getOptions(program: String, dataElement: String): List<Option>
    fun getEvents(
        ou: String,
        program: String,
        programStage: String,
        dataElement: String,
        teis: List<String>,
    ): Flow<List<FormData>>
}
