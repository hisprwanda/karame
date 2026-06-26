package org.saudigitus.emis.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import org.dhis2.commons.date.DateUtils
import org.saudigitus.emis.R
import org.saudigitus.emis.ui.theme.dark_warning
import org.saudigitus.emis.ui.theme.light_error
import org.saudigitus.emis.ui.theme.light_info
import org.saudigitus.emis.utils.DateHelper

@Composable
fun NoResults(
    message: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_empty_folder),
            contentDescription = "",
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = message,
            fontSize = 17.sp,
            color = Color.Black.copy(alpha = 0.38f),
            style = LocalTextStyle.current.copy(
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.rubik_regular)),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePicker(
    show: Boolean = false,
    dismiss: () -> Unit,
    onDatePick: (date: String) -> Unit,
    dateValidator: (Long) -> Boolean = { true },
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = null,
        initialDisplayMode = DisplayMode.Picker,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return dateValidator(utcTimeMillis)
            }
        },
    )

    var selectedDate by remember {
        mutableStateOf(
            DateHelper.formatDate(
                datePickerState.selectedDateMillis ?: DateUtils.getInstance().today.time,
            ) ?: "",
        )
    }

    if (show) {
        DatePickerDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    title = stringResource(R.string.done),
                    containerColor = Color.White,
                    contentColor = light_info,
                ) {
                    selectedDate = DateHelper.formatDate(datePickerState.selectedDateMillis ?: 0) ?: ""
                    onDatePick.invoke(selectedDate)
                    dismiss.invoke()
                }
            },
            dismissButton = {
                TextButton(
                    title = stringResource(R.string.cancel),
                    containerColor = Color.White,
                    contentColor = light_error,
                ) { dismiss.invoke() }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = true,
            ),
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                todayDateBorderColor = Color.Unspecified,
                selectedDayContainerColor = Color(0xFF2C98F0),
                selectedYearContainerColor = Color(0xFF2C98F0),
            ),
        ) {
            DatePicker(
                state = datePickerState,
                title = {},
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    todayDateBorderColor = Color.Unspecified,
                    selectedDayContainerColor = Color(0xFF2C98F0),
                    selectedYearContainerColor = Color(0xFF2C98F0),
                ),
            )
        }
    }
}

@Composable
fun FavoriteAlertDialog(
    onYes: () -> Unit,
    onDismiss: () -> Unit,
    title: String = "",
    message: String = "",
    openDialog: MutableState<Boolean>,
) {
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                Text(text = "$title")
            },
            text = {
                // Text(text = "Would you like to clear the saved favorites?")
                Text(text = "$message")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onYes.invoke()
                        openDialog.value = false
                    },
                    contentColor = Color(0xFF2C98F0),
                    containerColor = Color.White,
                    title = "Yes",
                    /**  TODO("SET String resource") */
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        openDialog.value = false
                    },
                    contentColor = Color(0xFF2C98F0),
                    containerColor = Color.White,
                    title = "Cancel",
                    /**  TODO("SET String resource") */
                )
            },
        )
    }
}

@Composable
fun Info(
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.Default.Info,
    message: String = stringResource(R.string.no_attendance_school_days)
) {
    Row(
        modifier = modifier.then(
            Modifier.background(
                color = Color.LightGray.copy(alpha = .25f),
                shape = RoundedCornerShape(16.dp)
            ).padding(16.dp)
        ),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = stringResource(R.string.no_attendance_school_days),
            tint = dark_warning
        )
        androidx.compose.material3.Text(
            text = message,
            color = dark_warning
        )
    }
}