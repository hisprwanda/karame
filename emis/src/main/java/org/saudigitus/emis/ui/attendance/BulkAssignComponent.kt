package org.saudigitus.emis.ui.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.saudigitus.emis.R
import org.saudigitus.emis.ui.attendance2.models.AttendanceButtonModel
import org.saudigitus.emis.utils.Utils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAssignComponent(
    onDismissRequest: () -> Unit,
    attendanceOptions: List<AttendanceOption>,
    onAttendanceStatus: (Triple<Int, String, Color?>, key: String?) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Rocket,
                contentDescription = null,
                tint = Color(0xFF2C98F0),
            )

            Text(
                text = stringResource(id = R.string.bulk_assing),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                itemsIndexed(attendanceOptions) { index, option ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = option.color ?: Color.LightGray.copy(0.25f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                        ),
                        onClick = { onAttendanceStatus.invoke(Triple(index, option.code.orEmpty(), option.color), option.key) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = option.icon ?: ImageVector
                                    .vectorResource(Utils.getIconByName("${option.iconName}")),
                                contentDescription = "${option.name}",
                                tint = option.color ?: Color.LightGray,
                            )
                            Text(
                                text = "${option.name}",
                                maxLines = 1,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 17.sp,
                                color = option.color ?: Color.LightGray,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray.copy(.85f),
                thickness = .9.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(width = 1.dp, color = Color.LightGray.copy(0.3f)),
                    onClick = onClear::invoke,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = stringResource(R.string.clear_all),
                        )
                        Text(
                            text = stringResource(R.string.clear),
                            color = Color.Gray,
                        )
                    }
                }

                Spacer(modifier = Modifier.size(24.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 3.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2C98F0),
                    ),
                    onClick = onCancel::invoke,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAssignComponent(
    onDismissRequest: () -> Unit,
    attendanceButtonModels: List<AttendanceButtonModel>,
    onAttendanceStatus: (AttendanceButtonModel) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Rocket,
                contentDescription = null,
                tint = Color(0xFF2C98F0),
            )

            Text(
                text = stringResource(id = R.string.bulk_assing),
                maxLines = 1,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
                fontSize = 20.sp,
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            ) {
                items(attendanceButtonModels) { option ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = option.color ?: Color.LightGray.copy(0.25f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White,
                        ),
                        onClick = { onAttendanceStatus.invoke(option) },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = option.icon ?: ImageVector
                                    .vectorResource(Utils.getIconByName(option.iconName.orEmpty())),
                                contentDescription = option.name.orEmpty(),
                                tint = option.color ?: Color.LightGray,
                            )
                            Text(
                                text = option.name.orEmpty(),
                                maxLines = 1,
                                softWrap = true,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 17.sp,
                                color = option.color ?: Color.LightGray,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = Color.LightGray.copy(.85f),
                thickness = .9.dp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(width = 1.dp, color = Color.LightGray.copy(0.3f)),
                    onClick = onClear::invoke,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = stringResource(R.string.clear_all),
                        )
                        Text(
                            text = stringResource(R.string.clear),
                            color = Color.Gray,
                        )
                    }
                }

                Spacer(modifier = Modifier.size(24.dp))

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(32.dp),
                    elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 3.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2C98F0),
                    ),
                    onClick = onCancel::invoke,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                    )
                }
            }
        }
    }
}
