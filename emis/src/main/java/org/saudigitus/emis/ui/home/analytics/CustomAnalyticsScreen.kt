package org.saudigitus.emis.ui.home.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hisp.dhis.mobile.ui.designsystem.theme.SurfaceColor
import org.saudigitus.emis.R
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ShowCard
import org.saudigitus.emis.ui.home.analytics.components.AttendanceIndicator
import org.saudigitus.emis.ui.home.analytics.components.AttendanceRate
import org.saudigitus.emis.ui.theme.light_success
import org.saudigitus.emis.utils.Constants
import org.saudigitus.emis.utils.getByType
import org.saudigitus.emis.utils.isTypeEmpty


@Composable
fun CustomAnalyticsScreen(
    viewModel: AnalyticsViewModel,
    orgUnit: String,
    academicYear: String,
    teiName: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CustomAnalyticsScreen(uiState, orgUnit, academicYear, teiName)
}


@Composable
fun CustomAnalyticsScreen(
    uiState: AnalyticsUiState,
    orgUnit: String,
    academicYear: String,
    teiName: String,
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .padding(16.dp),
    ) {
        ShowCard(
            modifier = Modifier.fillMaxWidth(),
            infoCard = InfoCard(
                grade = teiName,
                academicYear = academicYear,
                orgUnitName = orgUnit,
            ),
        )

        Spacer(modifier = Modifier.padding(16.dp))
        if (uiState.analyticsGroup.isTypeEmpty(Constants.CARD_VALUE)
            || uiState.analyticsGroup.isTypeEmpty(Constants.SINGLE_VALUE)) {

            val cardValues = uiState.analyticsGroup.getByType(Constants.CARD_VALUE)
            val singleValues = uiState.analyticsGroup.getByType(Constants.SINGLE_VALUE)

            AnimatedVisibility(visible = cardValues?.attendanceIndicators?.isNotEmpty() == true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start)) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_sliders),
                            contentDescription = null,
                            tint = SurfaceColor.Primary,
                        )
                        Text(
                            text = cardValues?.displayName.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    }
                    LazyVerticalGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        columns = GridCells.Adaptive(128.dp),
                        contentPadding = PaddingValues(vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                        horizontalArrangement = Arrangement.spacedBy(
                            16.dp,
                            Alignment.CenterHorizontally,
                        ),
                    ) {
                        items(cardValues?.attendanceIndicators ?: emptyList(), key = { it.uid }) {
                            AttendanceRate(
                                title = it.value.orEmpty(),
                                content = it.name.orEmpty(),
                                indicatorColor = it.color ?: light_success.copy(.2f),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.padding(16.dp))
            AnimatedVisibility(visible = singleValues?.attendanceIndicators?.isNotEmpty() == true) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_sliders),
                            contentDescription = null,
                            tint = SurfaceColor.Primary
                        )
                        Text(
                            text = singleValues?.displayName.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        contentPadding = PaddingValues(vertical = 5.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        items(singleValues?.attendanceIndicators ?: emptyList(), key = { it.uid }) {
                            AttendanceIndicator(
                                title = it.name.orEmpty(),
                                content = it.value.orEmpty()
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = Color.LightGray.copy(.5f),
                    modifier = Modifier.size(50.dp)
                )
                Text(
                    text = "No data available",
                    fontWeight = FontWeight.Bold,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    color = Color.LightGray.copy(.5f)
                )
            }
        }
    }
}