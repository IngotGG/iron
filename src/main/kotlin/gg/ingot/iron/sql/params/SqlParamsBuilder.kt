package gg.ingot.iron.sql.params

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model

/**
 * A builder for SQL parameters that allows for easy parameter creation and addition.
 */
data class SqlParamsBuilder internal constructor(
    val values: SqlParams
) {
    /** The models to add to the parameters. */
    private val models = mutableListOf<Any>()

    /**
     * Adds a model to the parameters.
     * @param model The model to add to the parameters.
     * @return The SqlParams instance for chaining.
     */
    internal operator fun plus(model: Any): SqlParamsBuilder {
        if (!model.javaClass.isAnnotationPresent(Model::class.java)) {
            throw IllegalArgumentException("Model must be annotated with @Model")
        }

        models.add(model)
        return this
    }

    /**
     * Adds a [SqlParamsBuilder] to the parameters.
     * @param builder The builder to add to the parameters.
     * @return The SqlParams instance for chaining.
     */
    operator fun plus(builder: SqlParamsBuilder): SqlParamsBuilder {
        models.addAll(builder.models)
        values.putAll(builder.values)
        return this
    }

    /**
     * Builds the SQL parameters using the provided transformer.
     * @param iron The iron instance to use for building the parameters.
     * @return The built SQL parameters.
     */
    internal fun build(iron: Iron): SqlParams {
        val variables = values.toMutableMap()

        for (model in models) {
            val entity = iron.modelReader.read(model::class.java)
            for (field in entity.fields) {
                variables[field.field.variable] = iron.modelReader.getModelValue(model, field)
            }
        }

        return variables
    }
}

/**
 * Creates a new SqlParams instance with the provided values.
 * @param values The values to add to the parameters.
 * @return The SqlParams instance for chaining.
 */
fun sqlParams(vararg values: Pair<String, Any?>): SqlParamsBuilder {
    return SqlParamsBuilder(values.toMap().toMutableMap())
}

/**
 * Creates a new SqlParams instance with the provided values.
 * @param values The values to add to the parameters.
 * @return The SqlParams instance for chaining.
 */
fun sqlParams(values: Map<String, Any?>): SqlParamsBuilder {
    return SqlParamsBuilder(values.toMap().toMutableMap())
}

/**
 * Creates a new SqlParams instance with the provided values.
 * @param models The models to add to the parameters.
 * @return The SqlParams instance for chaining.
 */
internal fun sqlParams(vararg models: Any): SqlParamsBuilder {
    return SqlParamsBuilder(mutableMapOf()).apply {
        models.forEach { this + it }
    }
}

/** The parameters to use in a SQL query. */
typealias SqlParams = MutableMap<String, Any?>