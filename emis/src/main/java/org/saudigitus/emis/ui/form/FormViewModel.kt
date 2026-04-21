package org.saudigitus.emis.ui.form

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.hisp.dhis.android.core.common.ValueType
import javax.inject.Inject

class FormViewModel
@Inject constructor() : ViewModel() {

    private val _formState = MutableStateFlow<List<Field>>(emptyList())
    val formState: StateFlow<List<Field>> = _formState

    fun setFormState(
        key: String,
        dataElement: String,
        value: String,
        valueType: ValueType? = null,
    ) {
        val junkData = mutableListOf<Field>()

        if (formState.value.isNotEmpty()) {
            junkData.addAll(formState.value)
        }

        val index = junkData.indexOfFirst { it.key == key && it.dataElement == dataElement }

        if (index >= 0) {
            junkData.removeAt(index)
            junkData.add(
                index,
                Field(
                    key = key,
                    "",
                    dataElement = dataElement,
                    value = value,
                    valueType = valueType,
                ),
            )
        } else {
            junkData.add(
                Field(
                    key = key,
                    "",
                    dataElement = dataElement,
                    value = value,
                    valueType = valueType,
                ),
            )
        }

        _formState.update {
            junkData
        }
    }
}
