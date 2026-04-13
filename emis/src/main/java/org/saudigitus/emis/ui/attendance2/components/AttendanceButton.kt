package org.saudigitus.emis.ui.attendance2.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicator
import org.hisp.dhis.mobile.ui.designsystem.component.ProgressIndicatorType
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonState
import org.saudigitus.emis.utils.Utils.GRAY
import org.saudigitus.emis.utils.Utils.WHITE
import org.saudigitus.emis.utils.Utils.getAttendanceStatusColor
import org.saudigitus.emis.utils.Utils.getIconByName

@Composable
fun AttendanceButton(
    key: String,
    modifier: Modifier = Modifier,
    state: AttendanceButtonState,
    onClick: (AttendanceButtonModel) -> Unit
) {
    if (state.buttons.isEmpty() && !state.isLoading && !state.isEditing) {
        Icon(
            modifier = modifier.then(Modifier.size(45.dp)),
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = null,
            tint = Color.LightGray
        )
    } else if (state.attendanceEvents.isEmpty() && !state.isLoading && !state.isEditing) {
        Icon(
            modifier = modifier.then(Modifier.size(45.dp)),
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = null,
            tint = Color.LightGray
        )
    } else if (state.isLoading) {
        ProgressIndicator(
            modifier = modifier.then(Modifier.size(45.dp)),
            type = ProgressIndicatorType.CIRCULAR_SMALL,
        )
    } else if (state.isEditing) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(state.buttons) { _, item ->
                val event = state.attendanceEvents.find { it.event?.tei == key }

                IconButton(
                    onClick = {
                        onClick.invoke(item)
                    },
                    enabled = item.enabled,
                    modifier = Modifier
                        .border(
                            width = (0.15).dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(100.dp)
                        )
                        .size(45.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (event != null && event.event?.value == item.code) {
                            event.decorator?.containerColor ?: Color.White
                        } else Color.White,
                        contentColor = if (event != null && event.event?.value == item.code) {
                            Color(event.decorator?.contentColor ?: GRAY)
                        } else {
                            item.color
                                ?: getAttendanceStatusColor(item.code.orEmpty())
                        },
                    )
                ) {
                    Icon(
                        imageVector = item.icon
                            ?: ImageVector.vectorResource(getIconByName(item.iconName.orEmpty())),
                        contentDescription = item.name,
                    )
                }
            }
        }
    } else {
        val event = state.attendanceEvents.find { it.event?.tei == key }

        IconButton(
            onClick = {},
            modifier = modifier.then(
                Modifier
                    .border(
                        width = (0.15).dp,
                        color = Color.LightGray,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .size(45.dp)
            ),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = event?.decorator?.containerColor ?: Color.LightGray,
                contentColor = Color(event?.decorator?.contentColor ?: WHITE),
            )
        ) {
            Icon(
                imageVector = event?.icon
                    ?: ImageVector.vectorResource(getIconByName(event?.iconName.orEmpty())),
                contentDescription = event?.iconName,
            )
        }
    }
}
