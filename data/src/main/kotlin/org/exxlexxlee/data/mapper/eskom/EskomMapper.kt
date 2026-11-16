package org.exxlexxlee.data.mapper.eskom

import java.time.Instant
import org.exxlexxlee.data.dto.eskom.EskomOutageDto
import org.exxlexxlee.data.remote.opendata.Csv
import org.exxlexxlee.domain.model.InternationalOutage

internal object EskomMapper {
    fun parse(raw: String): List<InternationalOutage> = Csv.records(
        raw, setOf("area_name", "start", "finsh", "stage", "source"),
    ).map { row ->
        EskomOutageDto(
            areaName = row.getValue("area_name"), start = row.getValue("start"),
            finish = row.getValue("finsh"), stage = row.getValue("stage"), source = row.getValue("source"),
        )
    }.map { row ->
        val area = row.areaName.also { require(it.isNotBlank()) }
        val start = Instant.parse(row.start)
        // Upstream's published header intentionally spells this column 'finsh'.
        val end = Instant.parse(row.finish)
        val stage = row.stage.toInt().also { require(it in 1..8) }
        require(end > start) { "Invalid Eskom interval" }
        InternationalOutage(
            "eskom:$area:$start:$stage", area,
            "Stage $stage · ${row.source}", start, end,
            kind = org.exxlexxlee.domain.model.OutageKind.PLANNED,
        )
    }
}
