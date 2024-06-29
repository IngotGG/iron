package gg.ingot.iron.sql

/**
 * Creates an array of parameters for a SQL query.
 * Helper function to make more concise prepared statements.
 * @param values The values to include in the array.
 * @return The array of parameters.
 */
fun sqlParams(vararg values: Any?): Array<out Any?> {
    return values
}

/**
 * Creates a map of parameters for a SQL query.
 * @param params The parameters to include in the map.
 * @return The map of parameters.
 * @throws IllegalArgumentException If there are duplicate parameter names.
 */
fun sqlParams(vararg params: Pair<String, Any?>): SqlParameters {
    require(params.map { it.first }.toSet().size == params.size) { "Duplicate parameter names" }

    return mapOf(*params)
}

/**
 * A map of parameters for a SQL query.
 */
typealias SqlParameters = Map<String, Any?>