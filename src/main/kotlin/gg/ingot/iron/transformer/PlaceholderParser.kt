package gg.ingot.iron.transformer

/**
 * Transforms a placeholder value into a value that can be used in a SQL query. This is useful for
 * transforming serialized fields and JSON fields into a format that can be used in a query.
 * @since 1.3
 * @author DebitCardz
 */
internal object PlaceholderParser {
    /** The regex for SQL placeholders. */
    private val SQL_PLACEHOLDER_REGEX = "'(?:\\\\'|[^'])*'|\"(?:\\\\\"|[^\"])*\"|`(?:\\\\`|[^`])*`|::?(\\w+)".toRegex()

    /** Quote characters that may wrap placeholders. */
    private val SURROUNDING_QUOTES = arrayOf('`', '\'', '"')

    fun getVariables(statement: String): List<String> {
        return SQL_PLACEHOLDER_REGEX.findAll(statement).map { it.groupValues[1] }
            .filter { it.isNotBlank() }
            .toList()
    }

    fun parseParams(statement: String, params: Map<String, Any?>): Pair<String, List<Any?>> {
        val insertedValues = mutableListOf<Any?>()

        val parsedStatement = SQL_PLACEHOLDER_REGEX.replace(statement) { matchResult ->
            val group = matchResult.groupValues.first()

            // cast
            if(group.startsWith("::")) {
                group
                // wrapped in text or something
            } else if(SURROUNDING_QUOTES.any { it == group.first() && it == group.last() }) {
                group
            } else {
                val name = matchResult.groupValues[1]
                val value = params[name]?.takeIf { params.containsKey(name) }
                    ?: error("No value found for variable '$name', did you forget to bind it or misname it?")

                insertedValues.add(value)
                "?"
            }
        }

        return parsedStatement to insertedValues
    }
}