package gg.ingot.iron.sql

import gg.ingot.iron.representation.ExplodingModel

/**
 * Creates a map of parameters for a SQL query.
 * @param params The parameters to include in the map.
 * @return The map of parameters.
 * @throws IllegalArgumentException If there are duplicate parameter names.
 */
fun sqlParams(vararg params: Pair<String, Any>): SqlParameters {
    require(params.map { it.first }.toSet().size == params.size) { "Duplicate parameter names" }

    return mapOf(*params)
}

/**
 * Creates a map of parameters for a SQL query from an [ExplodingModel].
 * @param model The model to create the parameters from.
 * @return The map of parameters.
 */
fun sqlParams(model: ExplodingModel): SqlParameters =
    model.toSqlParams()

/**
 * A map of parameters for a SQL query.
 */
typealias SqlParameters = Map<String, Any>