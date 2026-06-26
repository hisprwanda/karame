package org.saudigitus.emis.data.model


import org.hisp.dhis.android.core.common.ValueTypeRendering
import org.hisp.dhis.android.core.dataelement.DataElement

data class ProgramStageDataElementModel(
    val programStageUid: String? = null,
    val code: String?,
    val displayName: String?,
    val dataElement: DataElement? = null,
    val compulsory: Boolean? = null,
    val renderType: ValueTypeRendering? = null,
    val allowFutureDate: Boolean? = null,
    val sortOrder: Int? = null,
)