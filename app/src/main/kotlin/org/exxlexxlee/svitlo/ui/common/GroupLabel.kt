package org.exxlexxlee.svitlo.ui.common

internal fun groupLabel(name: String): String {
    val normalized = name.trim()
    return if (
        normalized.startsWith("Група", ignoreCase = true) ||
        normalized.startsWith("Черга", ignoreCase = true)
    ) {
        normalized
    } else {
        "Група $normalized"
    }
}
