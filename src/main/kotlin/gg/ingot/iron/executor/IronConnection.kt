package gg.ingot.iron.executor

import gg.ingot.iron.executor.impl.BlockingIronExecutor
import gg.ingot.iron.executor.impl.CompletableIronExecutor
import gg.ingot.iron.executor.impl.CoroutineIronExecutor
import gg.ingot.iron.sql.params.SqlParams

/**
 * Represents the entrypoint for executing queries on the database.
 * @author santio
 * @see CoroutineIronExecutor
 * @see BlockingIronExecutor
 * @see CompletableIronExecutor
 */
interface IronConnection {
    fun parseParams(statement: String, params: SqlParams): Pair<String, List<Any?>> {
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
                insertedValues.add(params[name])
                "?"
            }
        }

        return parsedStatement to insertedValues
    }

    private companion object {
        /** The regex for SQL placeholders. */
        val SQL_PLACEHOLDER_REGEX = "'(?:\\\\'|[^'])*'|\"(?:\\\\\"|[^\"])*\"|`(?:\\\\`|[^`])*`|::?(\\w+)".toRegex()

        /** Quote characters that may wrap placeholders. */
        val SURROUNDING_QUOTES = arrayOf('`', '\'', '"')
    }
}