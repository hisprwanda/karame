package org.saudigitus.emis.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun <T>DetailsWithOptions(
    modifier: Modifier = Modifier,
    infoCard: InfoCard,
    placeholder: String,
    leadingIcon: ImageVector,
    trailingIcon: ImageVector? = null,
    data: List<T>,
    defaultSelection: String = "",
    onItemClick: (T) -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        ShowCard(infoCard = infoCard)
        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            color = Color.LightGray.copy(.85f),
            thickness = .9.dp,
        )
        DropDown(
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            data = data,
            elevation = 1.dp,
            selectedItemName = defaultSelection,
            onItemClick = onItemClick,
        )
        Spacer(modifier = Modifier.size(5.dp))
    }
}
