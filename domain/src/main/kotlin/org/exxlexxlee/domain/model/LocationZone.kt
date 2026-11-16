package org.exxlexxlee.domain.model

import java.time.ZoneId
import java.time.zone.ZoneRulesException

/** Android releases with older tzdata only know the historic Kyiv identifier. */
fun locationZone(id: String): ZoneId = try {
    ZoneId.of(id)
} catch (failure: ZoneRulesException) {
    if (id == "Europe/Kyiv") ZoneId.of("Europe/Kiev") else throw failure
}
