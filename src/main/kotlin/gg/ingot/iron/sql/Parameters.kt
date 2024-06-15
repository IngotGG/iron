package gg.ingot.iron.sql

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnSerializer
import kotlin.reflect.KClass

/**
 * Creates an array of parameters for a SQL query.
 * Helper function to make more concise prepared statements.
 * @param values The values to include in the array.
 * @return The array of parameters.
 */
fun sqlParams(vararg values: Any): Array<out Any> {
    return values
}

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
 * Creates a serialized field for a SQL query, this will automatically
 * be serialized by the provided [gg.ingot.iron.serialization.ColumnSerializer].
 * @param value The value to include in the serialized field.
 * @param serializer The serializer to use for the value.
 */
fun serializedField(value: Any, serializer: KClass<out ColumnSerializer<*, *>>) =
    ColumnSerializedField(value, serializer)

data class ColumnSerializedField(val value: Any, val serializer: KClass<out ColumnSerializer<*, *>>)

/**
 * Creates a JSON field for a SQL query, this will automatically
 * be serialized into a JSON object by the provided [gg.ingot.iron.serialization.SerializationAdapter].
 * @param value The value to include in the JSON field.
 */
fun jsonField(value: Any) = ColumnJsonField(value)

data class ColumnJsonField(val value: Any)

/**
 * A map of parameters for a SQL query.
 */
typealias SqlParameters = Map<String, Any>