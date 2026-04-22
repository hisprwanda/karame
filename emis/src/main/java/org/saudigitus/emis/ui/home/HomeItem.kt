package org.saudigitus.emis.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HomeItem(
    modifier: Modifier = Modifier,
    icon: Painter,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        border = BorderStroke(width = 0.85.dp, color = Color.LightGray.copy(.85f)),
        elevation = CardDefaults.cardElevation(3.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        enabled = enabled,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.size(64.dp),
                    painter = icon,
                    contentDescription = label,
                )
            }
            Divider(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 2.dp),
                color = Color.LightGray.copy(.85f),
                thickness = .9.dp,
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(.75f),
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    maxLines = 2,
                    softWrap = true,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
