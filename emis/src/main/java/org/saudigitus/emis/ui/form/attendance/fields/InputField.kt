package org.saudigitus.emis.ui.form.attendance.fields

import android.content.Intent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import org.dhis2.composetable.model.extensions.keyboardCapitalization
import org.dhis2.composetable.model.extensions.toKeyboardType
import org.saudigitus.emis.ui.form.attendance.models.FormFieldData
import org.saudigitus.emis.ui.form.attendance.models.FormFieldState
import org.saudigitus.emis.ui.form.fields.IntentAction
import org.saudigitus.emis.utils.toKeyBoardInputType

@Composable
fun InputField(
    modifier: Modifier = Modifier,
    field: FormFieldState,
    formFieldData: FormFieldData? = null,
    enable: Boolean? = null,
    onValueChange: (String) -> Unit,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    ),
) {
    var action by remember { mutableStateOf("") }

    if (action.isNotEmpty()) {
        IntentAction(action = action, value = field.value.orEmpty())
    }

    TextField(
        modifier = modifier,
        enabled = enable ?: field.enabled,
        value = field.value ?: formFieldData?.value.orEmpty(),
        onValueChange = onValueChange,
        label = { Text(text = field.label + if (field.mandatory) " *" else "") },
        placeholder = { Text(text = field.label) },
        leadingIcon = {
            when (field.valueType.toKeyBoardInputType()?.toKeyboardType()) {
                KeyboardType.Email -> {
                    IconButton(onClick = { action = Intent.ACTION_SENDTO }) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = field.label,
                        )
                    }
                }

                KeyboardType.Phone -> {
                    IconButton(onClick = { action = Intent.ACTION_DIAL }) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = field.label,
                        )
                    }
                }
            }
        },
        isError = field.hasError,
        supportingText = {
            field.errorMessage?.let { Text(text = it) }
        },
        singleLine = field.valueType.toKeyBoardInputType()?.multiline == true,
        maxLines = if (field.valueType.toKeyBoardInputType()?.multiline == false) 1 else Int.MAX_VALUE,
        keyboardOptions = KeyboardOptions(
            keyboardType = field.valueType.toKeyBoardInputType()?.toKeyboardType()
                ?: KeyboardType.Text,
            capitalization = field.valueType.toKeyBoardInputType()?.keyboardCapitalization()
                ?: KeyboardCapitalization.None,
            imeAction = ImeAction.Done,
        ),
        colors = colors,
        visualTransformation = if (field.valueType.toKeyBoardInputType()
                ?.toKeyboardType() == KeyboardType.Password
        ) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
    )
}