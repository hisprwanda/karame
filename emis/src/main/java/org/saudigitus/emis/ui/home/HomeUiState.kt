package org.saudigitus.emis.ui.home

import android.os.Bundle
import androidx.compose.runtime.Stable
import org.saudigitus.emis.data.model.Module
import org.saudigitus.emis.data.model.OU
import org.saudigitus.emis.ui.components.DropdownItem
import org.saudigitus.emis.ui.components.DropdownState
import org.saudigitus.emis.ui.components.InfoCard
import org.saudigitus.emis.ui.components.ToolbarHeaders
import org.saudigitus.emis.utils.Constants

@Stable
data class HomeUiState(
    val isLoading: Boolean = true,
    val displayFilters: Boolean = true,
    val academicYear: DropdownItem? = null,
    val school: OU? = null,
    val grade: DropdownItem? = null,
    val section: DropdownItem? = null,
    val key: String? = null,
    val trackedEntityType: String = "",
    val academicYearState: DropdownState? = null,
    val dataElementFilters: List<DropdownState> = emptyList(),
    val toolbarHeaders: ToolbarHeaders = ToolbarHeaders("", subtitle = "Ministry of Education"),
    val programSettings: Bundle? = null,
    val infoCard: InfoCard = InfoCard(),
    val modules: List<Module> = emptyList(),
) {
    val isNull: Boolean
        get() = academicYear == null && school == null

    val isStaff: Boolean
        get() = key == Constants.STAFF

    val options: List<String>
        get() = if (academicYear != null && school != null) {
            listOfNotNull(
                academicYear.code,
                grade?.code,
                section?.code,
            )
        } else {
            emptyList()
        }

    val filterSelection: Triple<DropdownItem?, DropdownItem?, DropdownItem?>
        get() = Triple(academicYear, grade, section)

    fun hasModules(key: String): Boolean  = modules.any { it.key == key && it.display }
}
