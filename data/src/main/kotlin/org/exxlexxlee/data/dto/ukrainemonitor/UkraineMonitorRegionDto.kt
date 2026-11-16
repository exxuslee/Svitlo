package org.exxlexxlee.data.dto.ukrainemonitor

import kotlinx.serialization.Serializable

@Serializable
data class UkraineMonitorRegionDto(
    val cpu: String,
    val schedule: Map<String, Map<String, Map<String, Int>>>,
    val emergency: Boolean,
)
