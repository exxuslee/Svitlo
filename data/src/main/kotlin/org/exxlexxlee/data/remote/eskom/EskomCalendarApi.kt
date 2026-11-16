package org.exxlexxlee.data.remote.eskom

import org.exxlexxlee.data.mapper.eskom.EskomMapper
import org.exxlexxlee.data.remote.http.ConditionalHttpClient
import org.exxlexxlee.domain.model.InternationalOutage

/** Historical import only: the upstream project is archived, so the catalogue must not mark it LIVE. */
class EskomCalendarApi(private val http: ConditionalHttpClient) {
    suspend fun outages(): List<InternationalOutage> = http.get(
        "https://github.com/beyarkay/eskom-calendar/releases/download/latest/machine_friendly.csv",
        "eskom_calendar", decode = ::parse,
    )

    fun parse(raw: String): List<InternationalOutage> = EskomMapper.parse(raw)
}
