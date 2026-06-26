package org.saudigitus.emis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.saudigitus.emis.R

@Composable
fun MetadataIcon(
    modifier: Modifier = Modifier,
    cornerShape: Dp = 4.dp,
    backgroundColor: Color,
    size: Dp = 40.dp,
    paddingAll: Dp = 0.dp,
    painter: Painter? = null,
    contentDescription: String? = null,
    colorFilter: ColorFilter? = null,
) {
    if (painter != null) {
        Image(
            modifier = modifier
                .clip(RoundedCornerShape(cornerShape))
                .background(color = backgroundColor)
                .size(size)
                .padding(paddingAll),
            painter = painter,
            contentDescription = contentDescription,
            colorFilter = colorFilter,
        )
    }
}

@Composable
fun MetadataItem(
    displayName: String,
    attrValue: String? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .clickable { onClick.invoke() },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundedIcon(label = "${displayName[0]}")
            Spacer(modifier = Modifier.size(15.dp))
            TitleSubtitleComponent(
                modifier = Modifier.weight(1f, true),
                title = displayName,
                subtitle = "$attrValue",
                fontDefaults = TitleSubtitleDefaults(
                    titleColor = Color.Black.copy(.75f),
                ),
            )
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth(.83f)
                .align(Alignment.End)
                .wrapContentWidth(Alignment.End, false)
                .padding(end = 5.dp),
            thickness = .75.dp,
        )
    }
}

@Composable
fun MetadataItem(
    displayName: String,
    attrValue: String? = null,
    enableClickAction: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.White)
            .clickable(enabled = enableClickAction) { onClick.invoke() },
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundedIcon(label = "${displayName[0]}")
                Spacer(modifier = Modifier.size(15.dp))
                TitleSubtitleComponent(
                    modifier = Modifier.weight(1f),
                    title = displayName,
                    subtitle = "$attrValue",
                    fontDefaults = TitleSubtitleDefaults(
                        titleColor = Color.Black.copy(.75f),
                    ),
                )
            }

            content.invoke()
        }
        Divider(
            modifier = Modifier
                .fillMaxWidth(.83f)
                .align(Alignment.End)
                .wrapContentWidth(Alignment.End, false)
                .padding(end = 5.dp),
            thickness = .75.dp,
        )
    }
}

@Composable
fun RoundedIcon(
    painter: Painter? = null,
    label: String? = null,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color = colorResource(R.color.colorPrimary))
            .size(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        MetadataIcon(
            cornerShape = 100.dp,
            backgroundColor = colorResource(R.color.colorPrimary),
            size = 48.dp,
            paddingAll = 5.dp,
            painter = painter,
            colorFilter = ColorFilter.tint(Color.White),
        )
        if (painter == null) {
            Text(text = "$label", color = Color.White)
        }
    }
}

@Composable
fun TEICountComponent(
    teiCount: Int = 0,
    imageVector: ImageVector = Icons.Outlined.Person,
) {
    Row(
        modifier = Modifier
            .background(
                color = Color.LightGray.copy(.35f),
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "Image",
            tint = Color.Black.copy(.5f),
        )
        Text(
            text = "$teiCount",
            color = Color.Black.copy(.5f),
            maxLines = 1,
            softWrap = true,
            overflow = TextOverflow.Ellipsis,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

data class InfoCard(
    val grade: String = "",
    val section: String = "",
    val academicYear: String = "",
    val orgUnitName: String = "",
    val teiCount: Int = 0,
    val isStaff: Boolean = false,
    val displayBoolean: Boolean = true,
) {
    fun hasData(): Boolean {
        return teiCount > 0
    }
}

@Composable
fun ShowCard(
    infoCard: InfoCard,
    enableTEICount: Boolean = true,
    enabledIconButton: Boolean = true,
    onClick: () -> Unit = {},
    onIconClick: () -> Unit = {},
) {
    AnimatedVisibility(infoCard.displayBoolean) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f),
                    ) {
                        Icon(
                            Icons.Rounded.School,
                            tint = Color(0xFF2C98F0),
                            contentDescription = "Icon",
                        )
                        Spacer(modifier = Modifier.size(10.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            val hasGradeOrSection =
                                infoCard.grade.isNotEmpty() || infoCard.section.isNotEmpty()

                            if (hasGradeOrSection) {
                                val gradeLabel = listOfNotNull(infoCard.grade, infoCard.section)
                                    .filter { it.isNotEmpty() }
                                    .joinToString(separator = ", ")

                                Text(
                                    text = gradeLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "${infoCard.academicYear} | ${infoCard.orgUnitName}",
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (enableTEICount) {
                        TEICountComponent(teiCount = infoCard.teiCount)
                    } else {
                        IconButton(
                            onClick = onIconClick,
                            enabled = enabledIconButton,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Rocket,
                                contentDescription = null,
                                tint = Color(0xFF2C98F0),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShowCard(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
    infoCard: InfoCard,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f),
                ) {
                    Icon(
                        Icons.Rounded.School,
                        tint = Color(0xFF2C98F0),
                        contentDescription = "Icon",
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = String.format("%s, %s", infoCard.grade, infoCard.section),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Text(
                            text = String.format("%s | %s", infoCard.academicYear, infoCard.orgUnitName),
                            fontSize = 14.sp,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = true,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

            }
        }
    }
}