package org.saudigitus.emis.ui.teis

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hisp.dhis.android.core.enrollment.EnrollmentStatus
import org.hisp.dhis.mobile.ui.designsystem.component.AdditionalInfoItem
import org.hisp.dhis.mobile.ui.designsystem.component.ListCard
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardDescriptionModel
import org.hisp.dhis.mobile.ui.designsystem.component.ListCardTitleModel
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberAdditionalInfoColumnState
import org.hisp.dhis.mobile.ui.designsystem.component.state.rememberListCardState
import org.saudigitus.emis.R
import org.saudigitus.emis.data.model.mapper.map
import org.saudigitus.emis.ui.components.NoResults
import org.saudigitus.emis.ui.components.ShowCard
import org.saudigitus.emis.ui.components.Toolbar
import org.saudigitus.emis.ui.components.ToolbarActionState
import org.saudigitus.emis.ui.home.HomeViewModel
import org.saudigitus.emis.ui.teis.mapper.TEICardMapper

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeiScreen(
    viewModel: HomeViewModel,
    teiCardMapper: TEICardMapper,
    onBack: () -> Unit,
    onSync: () -> Unit,
    onCardClick: (tei: String, enrollment: String) -> Unit,
) {
    val students by viewModel.teis.collectAsStateWithLifecycle()
    val toolbarHeaders by viewModel.toolbarHeaders.collectAsStateWithLifecycle()
    val infoCard by viewModel.infoCard.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Toolbar(
                headers = toolbarHeaders,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C98F0),
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                navigationAction = { onBack.invoke() },
                disableNavigation = false,
                actionState = ToolbarActionState(
                    syncVisibility = true,
                    showFavorite = false,
                    filterVisibility = false,
                ),
                syncAction = onSync,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFF2C98F0))
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                            .copy(
                                topStart = CornerSize(16.dp),
                                topEnd = CornerSize(16.dp),
                                bottomStart = CornerSize(0.dp),
                                bottomEnd = CornerSize(0.dp),
                            ),
                    ),
                verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                if (students.isEmpty()) {
                    NoResults(message = stringResource(R.string.search_no_results))
                } else {
                    ShowCard(infoCard)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(students) { student ->
                            val card = student.map(teiCardMapper, showSync = false, onCardClick = onCardClick)
                            val isInactive = student.enrollments.getOrNull(0)?.status() == EnrollmentStatus.CANCELLED

                            ListCard(
                                modifier = Modifier.fillMaxWidth(),
                                listCardState = rememberListCardState(
                                    title = ListCardTitleModel(
                                        text = card.title,
                                        allowOverflow = false
                                    ),
                                    description = card.description?.let {
                                        ListCardDescriptionModel(
                                            text = it,
                                        )
                                    },
                                    lastUpdated = card.lastUpdated,
                                    additionalInfoColumnState = rememberAdditionalInfoColumnState(
                                        additionalInfoList = card.additionalInfo,
                                        syncProgressItem = AdditionalInfoItem(
                                            key = stringResource(id = R.string.syncing),
                                            value = "",
                                        ),
                                        expandLabelText = stringResource(id = R.string.show_more),
                                        shrinkLabelText = stringResource(id = R.string.show_less),
                                        scrollableContent = true,
                                    ),
                                ),
                                onCardClick = card.onCardCLick,
                                listAvatar = card.avatar, actionButton = card.actionButton,
                            )
                        }
                    }
                }
            }
        }
    }
}
