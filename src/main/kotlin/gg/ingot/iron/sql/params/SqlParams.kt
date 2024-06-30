package gg.ingot.iron.sql.params

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.transformer.ModelTransformer

class SqlParams internal constructor(
    val values: Parameters
) {
    private val models = mutableListOf<ExplodingModel>()

    operator fun plus(model: ExplodingModel): SqlParams {
        models.add(model)
        return this
    }

    internal fun build(transformer: ModelTransformer): Parameters {
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

fun sqlParams(vararg values: Pair<String, Any?>): SqlParams {
    return SqlParams(values.toMap().toMutableMap())
}

typealias Parameters = MutableMap<String, Any?>