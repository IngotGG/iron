package gg.ingot.iron.sql.params

import java.sql.PreparedStatement

internal object VariableParser {

    /**
     * Finds all variables *(in format `:id`)* and replaces them properly, this makes sure that we aren't
     * replacing any in strings to prevent any weird issues, however this does **not** check the placement of
     * variables, meaning a statement like `SELECT :column FROM table` will actually fail. While having its
     * shortcomings, this shouldn't expose any security vulnerabilities since the values are passed through
     * a prepared statement.
     * @param statement The statement to execute which contains named variables
     * @param variables The mapping of variables to their respective values
     * @return A prepared statement with variables replaced with `?` and set for you
     */
    fun parse(statement: String, variables: Map<String, Any?>): PreparedStatement {

    }

    private fun findVariables(statement: String): Map<IntRange, String> {
        val variables = mutableMapOf<IntRange, String>()

        var index = 0
        var escaping = false
        var inQuotes = false
        var quote: Char? = null

        while (index != statement.length) {
            index++



        }

        return variables
    }

}