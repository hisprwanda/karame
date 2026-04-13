package org.saudigitus.emis.ui.attendance2.models

data class AttendanceEvent(
    val tei: String,
    val enrollment: String,
    val event: String? = null,
    val dataElement: String,
    val reasonDataElement: String? = null,
    val reasonOfAbsence: String? = null,
    val value: String,
    val date: String,
)