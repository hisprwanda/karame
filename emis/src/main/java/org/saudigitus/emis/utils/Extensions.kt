package org.saudigitus.emis.utils

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.dhis2.composetable.ui.displayName
import org.hisp.dhis.android.core.D2
import org.hisp.dhis.android.core.arch.repositories.scope.RepositoryScope
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.option.Option
import org.saudigitus.emis.R
import org.saudigitus.emis.data.model.AnalyticGroup
import org.saudigitus.emis.data.model.OU
import org.saudigitus.emis.data.model.dto.AttendanceEntity
import org.saudigitus.emis.ui.attendance.AttendanceOption
import org.saudigitus.emis.ui.components.DropdownItem
import org.saudigitus.emis.ui.components.DropdownState
import org.saudigitus.emis.ui.form.Field
import org.saudigitus.emis.ui.form.FormData
import org.saudigitus.emis.ui.form.FormField
import org.saudigitus.emis.ui.teis.FilterType

fun D2.eventsWithTrackedDataValues(
    ou: String,
    program: String,
    stage: String,
    tei: String,
): List<Event> = eventModule().events()
    .byOrganisationUnitUid().eq(ou)
    .byProgramUid().eq(program)
    .byProgramStageUid().eq(stage)
    .byTrackedEntityInstanceUids(listOf(tei))
    .byDeleted().isFalse
    .withTrackedEntityDataValues()
    .blockingGet()

fun D2.optionByOptionSet(
    optionSet: String?,
): List<Option> = optionModule()
    .options()
    .byOptionSetUid().eq(optionSet)
    .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
    .blockingGet()

fun D2.optionsNotInOptionsSets(
    options: List<String>,
    optionSet: String?,
): List<Option> = optionModule()
    .options()
    .byUid().notIn(options)
    .byOptionSetUid().eq(optionSet)
    .orderByDisplayName(RepositoryScope.OrderByDirection.ASC)

    .blockingGet()

fun D2.optionsNotInOptionGroup(
    optionGroups: List<String>,
    optionSet: String?,
): List<Option> = optionModule()
    .optionGroups()
    .byUid().notIn(optionGroups)
    .byOptionSetUid().eq(optionSet)
    .withOptions()
    .orderByDisplayName(RepositoryScope.OrderByDirection.ASC)
    .blockingGet()
    .flatMap {
        it.options() ?: emptyList()
    }.flatMap {
        optionModule()
            .options()
            .byUid().eq(it.uid())
            .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
            .blockingGet()
    }

fun D2.optionsByOptionSetAndCode(
    optionSet: String?,
    codes: List<String>,
): List<Option> = optionModule()
    .options()
    .byCode().`in`(codes)
    .byOptionSetUid().eq(optionSet)
    .orderBySortOrder(RepositoryScope.OrderByDirection.ASC)
    .blockingGet()

fun List<DropdownState>.getByType(type: FilterType): DropdownState =
    find { it.filterType == type } ?: DropdownState()

fun DropdownState.icon() = when (this.filterType) {
    FilterType.ACADEMIC_YEAR -> R.drawable.ic_book
    FilterType.GRADE -> R.drawable.ic_school
    FilterType.SECTION -> R.drawable.ic_category
    FilterType.SCHOOL -> R.drawable.ic_location_on
    FilterType.NONE -> R.drawable.filter_none
}

fun DropdownState.placeholder() = when (this.filterType) {
    FilterType.ACADEMIC_YEAR -> R.string.academic_year
    FilterType.GRADE -> R.string.grade
    FilterType.SECTION -> R.string.cls
    FilterType.SCHOOL -> R.string.school
    FilterType.NONE -> R.string.none
}

fun DropdownItem.toOu() = OU(
    uid = this.id,
    displayName = this.itemName,
)

fun Modifier.expandedFormSize(isExpanded: Boolean) = if (isExpanded) {
    this.width(170.dp)
        .height(90.dp)
} else {
    this.width(150.dp)
        .height(90.dp)
}

fun List<AnalyticGroup>.isTypeEmpty(type: String) =
    this.any { it.type == type }

fun List<AnalyticGroup>.getByType(type: String) =
    this.find { it.type == type }

fun List<org.saudigitus.emis.data.model.Option>.findByCode(code: String) =
    this.find { it.code == code }

fun List<AttendanceEntity>.getReasonByTei(tei: String) =
    this.find { it.tei == tei }
        ?.reasonOfAbsence


fun  List<FormData>.isVisible(tei: String) =
    this.any { it.tei == tei }


fun FormField.getOption(reason: String): org.saudigitus.emis.data.model.Option {
    val option =  this.options?.find { it.code == reason }

    return org.saudigitus.emis.data.model.Option(
        uid = option?.uid.orEmpty(),
        code = option?.code,
        displayName = option?.displayName,
    )
}