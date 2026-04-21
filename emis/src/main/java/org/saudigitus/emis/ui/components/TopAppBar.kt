package org.saudigitus.emis.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.saudigitus.emis.R
import java.time.LocalDate

data class ToolbarHeaders(
    val title: String,
    val subtitle: String? = null,
)

data class ToolbarActionState(
    val syncVisibility: Boolean = true,
    val filterVisibility: Boolean = true,
    val showCalendar: Boolean = false,
    val showFavorite: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    headers: ToolbarHeaders,
    modifier: Modifier = Modifier,
    navigationAction: () -> Unit,
    disableNavigation: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
) {
    TopAppBar(
        title = {
            Column(
                modifier = Modifier.offset(x = (-16).dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = headers.title,
                    maxLines = 1,
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    fontWeight = FontWeight.Bold,
                )
                headers.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (disableNavigation) {
                IconButton(onClick = { navigationAction.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(
    headers: ToolbarHeaders,
    modifier: Modifier = Modifier,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    navigationAction: () -> Unit,
    disableNavigation: Boolean = true,
    actionState: ToolbarActionState = ToolbarActionState(),
    calendarAction: (date: String) -> Unit = {},
    dateValidator: (Long) -> Boolean = { true },
    syncAction: () -> Unit = {},
    filterAction: () -> Unit = {},
) {
    var isCalendarShown by remember { mutableStateOf(false) }

    CustomDatePicker(
        show = isCalendarShown,
        dismiss = { isCalendarShown = !isCalendarShown },
        onDatePick = { calendarAction.invoke(it) },
        dateValidator = dateValidator,
    )

    TopAppBar(
        title = {
            Column(
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = headers.title,
                    maxLines = 1,
                    fontSize = 17.sp,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    style = LocalTextStyle.current.copy(
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.rubik_medium)),
                    ),
                )
                headers.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                        style = LocalTextStyle.current.copy(
                            fontFamily = FontFamily(Font(R.font.rubik_regular)),
                        ),
                        color = Color.White.copy(.5f),
                    )
                }
            }
        },
        modifier = modifier,
        navigationIcon = {
            if (!disableNavigation) {
                IconButton(onClick = { navigationAction.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
        },
        actions = {
            if (actionState.showCalendar) {
                IconButton(onClick = { isCalendarShown = !isCalendarShown }) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = stringResource(R.string.calendar),
                    )
                }
            }
            if (actionState.syncVisibility) {
                IconButton(onClick = { syncAction.invoke() }) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = stringResource(R.string.sync),
                    )
                }
            }
            if (actionState.filterVisibility) {
                IconButton(onClick = { filterAction.invoke() }) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_filter),
                        contentDescription = stringResource(R.string.filter),
                    )
                }
            }
        },
        colors = colors,
    )
}
