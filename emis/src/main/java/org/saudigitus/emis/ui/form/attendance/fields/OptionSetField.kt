package org.saudigitus.emis.ui.form.attendance.fields

import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState
@Composable
fun OptionSetField(
    field: FormFieldState,
    formFieldData: FormFieldData? = null,
    enabled: Boolean? = null,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onItemClick: (code: String) -> Unit
) {
    DropdownField(
        label = field.label + if (field.mandatory) " *" else "",
        placeholder = field.label,
        supportingText =field.errorMessage,
        isError = field.hasError,
        data = field.optionSet ?: emptyList(),
        selectedItem = formFieldData?.optionModel,
        enabled = enabled ?: field.enabled,
        colors = colors,
        onClick = {
            onItemClick.invoke(it.code.orEmpty())
        },
    )
}