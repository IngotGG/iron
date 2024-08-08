package gg.ingot.iron.sql.params

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.transformer.ModelTransformer

/**
 * A builder for SQL parameters that allows for easy parameter creation and addition.
 */
class SqlParamsBuilder internal constructor(
    val values: SqlParams
) {
    /** The models to add to the parameters. */
    private val models = mutableListOf<ExplodingModel>()

    /**
     * Adds a model to the parameters.
     * @param model The model to add to the parameters.
     * @return The SqlParams instance for chaining.
     */
    operator fun plus(model: ExplodingModel): SqlParamsBuilder {
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
     * @param transformer The transformer to use to build the parameters.
     * @return The built SQL parameters.
     */
    internal fun build(transformer: ModelTransformer): SqlParams {
        val variables = values.toMutableMap()

        for (model in models) {
            val entity = transformer.transform(model::class)
            for (field in entity.fields) {
                variables[field.columnName] = transformer.getModelValue(model, field)
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
 * @param models The models to add to the parameters.
 * @return The SqlParams instance for chaining.
 */
fun sqlParams(vararg models: ExplodingModel): SqlParamsBuilder {
    return SqlParamsBuilder(mutableMapOf()).apply {
        models.forEach { this + it }
    }
}

/**
 * Convert a list of models into a SqlParams instance.
 * @param models The models to convert into a SqlParams instance.
 * @return The SqlParams instance for chaining.
 */
fun namedSqlParams(vararg models: ExplodingModel): SqlParamsBuilder {
    return SqlParamsBuilder(mutableMapOf()).apply {
        models.forEach { this + it }
    }
}

/** The parameters to use in a SQL query. */
typealias SqlParams = MutableMap<String, Any?>