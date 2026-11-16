package org.exxlexxlee.data.remote.opendata


/** RFC 4180 reader, including quoted commas, escaped quotes, CRLF and multiline fields. */
internal object Csv {
    fun records(
        text: String,
        requiredColumns: Set<String> = emptySet()
    ): List<Map<String, String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var closed = false
        var index = 0
        val input = text.removePrefix("\uFEFF")
        fun finishField() {
            row += field.toString(); field.setLength(0); closed = false
        }

        fun finishRow() {
            finishField(); rows += row; row = mutableListOf()
        }
        while (index < input.length) {
            val char = input[index++]
            when {
                quoted && char == '"' -> {
                    if (index < input.length && input[index] == '"') {
                        field.append('"'); index++
                    } else {
                        quoted = false; closed = true
                    }
                }

                quoted -> field.append(char)
                char == '"' -> {
                    require(field.isEmpty() && !closed) { "Invalid CSV quote" }; quoted = true
                }

                char == ',' -> finishField()
                char == '\n' || char == '\r' -> {
                    if (char == '\r' && index < input.length && input[index] == '\n') index++
                    finishRow()
                }

                else -> {
                    require(!closed) { "Unexpected text after CSV quote" }; field.append(char)
                }
            }
        }
        require(!quoted) { "Unterminated CSV field" }
        if (field.isNotEmpty() || row.isNotEmpty() || closed) finishRow()
        require(rows.isNotEmpty()) { "Empty CSV response" }
        val header = rows.first().map { it.trim() }
        require(header.all { it.isNotBlank() } && header.distinct().size == header.size) { "Invalid CSV headers" }
        require(header.containsAll(requiredColumns)) { "Missing CSV columns" }
        return rows.drop(1).map {
            require(it.size == header.size) { "Incomplete CSV record" }
            header.zip(it).toMap()
        }
    }
}
