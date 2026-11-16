package org.exxlexxlee.data.mapper.json

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal fun Json.objectOf(raw: String) = parseToJsonElement(raw).jsonObject

internal fun JsonObject.text(key: String) = get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

internal fun date(value: String, zone: String): Instant? = if (value.isBlank()) null else
    runCatching { Instant.parse(value) }.getOrElse {
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrElse {
            LocalDateTime.parse(value.replace(' ', 'T')).atZone(ZoneId.of(zone)).toInstant()
        }
    }
