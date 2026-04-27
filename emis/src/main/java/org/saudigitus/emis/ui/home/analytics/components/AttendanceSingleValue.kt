package org.saudigitus.emis.ui.home.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.hisp.dhis.mobile.ui.designsystem.theme.Spacing
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor

@Composable
fun TotalAbsenceComponent(
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .background(
                color = Color.White,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth(), // Adjust width as needed
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: "Total Absence" text
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black,
            style = MaterialTheme.typography.bodyLarge
        )

        // Right side: "15" text with blue accent
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = content,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(24.dp)
                    .background(
                        color = SurfaceColor.Primary, // Blue color for the accent
                        shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp)
                    )
            )
        }
    }
}

@Composable
fun AttendanceIndicator(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    indicatorColor: Color = SurfaceColor.Container,
) {
    val backgroundColor = indicatorColor.copy(alpha = 0.1f)

    Row(
        modifier = modifier.fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(Spacing.Spacing8))
            .background(backgroundColor),
    ) {
        Box(
            Modifier.padding(
                horizontal = Spacing.Spacing16,
                vertical = Spacing.Spacing8,
            ).weight(1f),
        ) {
            Row(
                modifier = modifier.fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = content,
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
        }

        Box(Modifier.background(indicatorColor).requiredWidth(Spacing.Spacing16).fillMaxHeight())
    }
}
