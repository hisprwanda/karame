package org.saudigitus.emis.ui.form.attendance.models

import org.saudigitus.emis.data.model.Option

data class FormFieldData(
    val tei: String,
    val event: String? = null,
    val dataElement: String,
    val value: String? = null,
    val optionModel: Option? = null,
    val isUpdated: Boolean = false
) {
    override fun toString(): String {
        return "{ tei: $tei, value: $value, isUpdated: $isUpdated }"
    }
}