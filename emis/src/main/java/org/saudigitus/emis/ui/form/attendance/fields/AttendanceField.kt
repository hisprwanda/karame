package org.saudigitus.emis.ui.form.attendance.fields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.hisp.dhis.android.core.common.ObjectWithUid.uid
import org.hisp.dhis.mobile.ui.designsystem.component.InputShellState
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState

@Composable
fun AttendanceField(
    key: String,
    field: FormFieldState? = null,
    enabled: Boolean? = null,
    fieldsData: List<FormFieldData> = emptyList(),
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = InputShellState.FOCUSED.color,
        unfocusedIndicatorColor = InputShellState.UNFOCUSED.color,
        disabledIndicatorColor = InputShellState.DISABLED.color,
    ),
    onValueChange: (key: String, fieldUid: String, value: String) -> Unit
) {
    val fieldData = fieldsData.find { it.tei == key }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when {
            field == null -> Unit
            !field.optionSet.isNullOrEmpty() -> {
                OptionSetField(
                    field,
                    formFieldData = fieldData,
                    enabled = enabled,
                    colors =
                        TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = InputShellState.UNFOCUSED.color,
                            disabledIndicatorColor = InputShellState.DISABLED.color,
                        ),
                    onItemClick = {
                        onValueChange.invoke(key, field.dataElementUid, it)
                    }
                )
            }

            else -> InputField(
                field = field,
                formFieldData = fieldData,
                enable = enabled,
                colors = colors,
                onValueChange = {
                    onValueChange.invoke(key, field.dataElementUid, it)
                }
            )
        }
    }
}