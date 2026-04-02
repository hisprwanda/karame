package org.saudigitus.emis.ui.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.saudigitus.emis.R
import org.saudigitus.emis.ui.attendance.AlertDialogTemplate
import org.saudigitus.emis.ui.attendance.SummaryComponent
import org.saudigitus.emis.ui.components.ActionButtons
import org.saudigitus.emis.ui.theme.light_error
import org.saudigitus.emis.ui.theme.light_success

@Composable
fun PerformanceSummaryDialog(
    title: String,
    data: Pair<String, String>,
    themeColor: Color,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    AlertDialogTemplate {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.Start),
            color = Color.Black.copy(.75f),
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            textAlign = TextAlign.Start,
            style = LocalTextStyle.current.copy(
                fontSize = 20.sp,
                fontFamily = FontFamily(Font(R.font.rubik_regular)),
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.LightGray.copy(.75f),
            thickness = .5.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryComponent(
                summary = data.first,
                containerColor = light_success,
                icon = Icons.Outlined.Check,
            )

            SummaryComponent(
                summary = data.second,
                containerColor = light_error,
                icon = painterResource(R.drawable.not_filled),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp),
            text = stringResource(R.string.reminder_to_sync)
        )
        Divider(
            modifier = Modifier.fillMaxWidth(),
            color = Color.LightGray.copy(.75f),
            thickness = .5.dp,
        )
        ActionButtons(
            modifier = Modifier.align(Alignment.End),
            contentColor = themeColor,
            onCancel = onCancel,
            onDone = onDone,
        )
    }
}
