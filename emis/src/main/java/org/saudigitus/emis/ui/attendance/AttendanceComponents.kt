package org.saudigitus.emis.ui.attendance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.saudigitus.emis.R
import org.saudigitus.emis.data.model.dto.AttendanceEntity
import org.saudigitus.emis.utils.Utils.WHITE
import org.saudigitus.emis.utils.Utils.getIconByName

@Composable
fun AttendanceItemState(
    tei: String,
    attendanceState: List<AttendanceEntity>,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (attendanceState.isEmpty()) {
            Icon(
                imageVector = Icons.Filled.Help,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.LightGray,
            )
        } else {
            val attendance = attendanceState.find { it.tei == tei }

            if (attendance != null) {
                val icon = getIconByName("${attendance.setting?.iconName}")

                if (icon != R.drawable.ic_empty) {
                    Icon(
                        imageVector = attendance.setting?.icon ?: ImageVector.vectorResource(icon),
                        contentDescription = attendance.value,
                        modifier = Modifier.size(40.dp),
                        tint = attendance.setting?.iconColor ?: Color.Black,
                    )
                } else {
                    ProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        type = ProgressIndicatorType.CIRCULAR_SMALL
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Filled.Help,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.LightGray,
                )
            }
        }
    }
}

@Composable
fun AttendanceButtons(
    tei: String,
    isEnabled: Boolean = true,
    btnState: List<AttendanceActionButtonState>,
    actions: List<AttendanceOption>,
    onClick: (
        index: Int,
        key: String,
        tei: String?,
        attendanceState: String,
        color: Color,
    ) -> Unit,
) {
    var btnCode by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableIntStateOf(-1) }

    Row(
        modifier = Modifier.layoutId(layoutId = tei)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions.forEachIndexed { index, action ->
            IconButton(
                onClick = {
                    btnCode = action.code ?: ""
                    selectedIndex = index

                    onClick.invoke(
                        index,
                        action.key ?: "",
                        tei,
                        action.code ?: "",
                        action.color ?: Color.LightGray,
                    )
                },
                enabled = isEnabled,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor =
                    getContainerColor(btnState, tei, action.code.toString(), selectedIndex, index),
                    contentColor =
                    if (selectedIndex == index || btnState.isNotEmpty()) {
                        getContentColor(btnState, tei, action.code.toString(), selectedIndex, index)?.let {
                            Color(it)
                        }
                            ?: action.color ?: Color.White
                    } else {
                        action.color ?: Color.White
                    },

                ),
            ) {
                Icon(
                    imageVector = action.icon ?: ImageVector.vectorResource(getIconByName("${action.iconName}")),
                    contentDescription = action.name,
                )
            }
        }
    }
}

private fun getContainerColor(
    btnState: List<AttendanceActionButtonState>,
    tei: String,
    code: String,
    selectedIndex: Int,
    itemIndex: Int,
): Color {
    val attendance = btnState.find { it.btnId == tei }

    return if (attendance != null &&
        attendance.buttonState?.buttonType == code &&
        (attendance.btnIndex == selectedIndex || attendance.btnIndex == itemIndex)
    ) {
        attendance.buttonState.containerColor ?: Color.Black
    } else {
        Color.White
    }
}

private fun getContentColor(
    btnState: List<AttendanceActionButtonState>,
    tei: String,
    code: String,
    selectedIndex: Int,
    itemIndex: Int,
): Long? {
    val attendance = btnState.find { it.btnId == tei }

    return if (attendance != null &&
        attendance.buttonState?.buttonType == code &&
        (attendance.btnIndex == selectedIndex || attendance.btnIndex == itemIndex)
    ) {
        attendance.buttonState.contentColor ?: WHITE
    } else {
        null
    }
}
