package gg.ingot.iron.sql.params

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.transformer.ModelTransformer
import kotlin.random.Random

class SqlParams internal constructor(
    val values: MutableMap<String, Any?>
) {

    private val models: MutableList<ExplodingModel> = mutableListOf()

    operator fun plus(model: ExplodingModel): SqlParams {
        this.models.plus(model)
        return this
    }

    internal fun build(transformer: ModelTransformer): MutableMap<String, Any?> {
        val variables = this.values.toMutableMap()

        for (model in models) {
            val entity = transformer.transform(model::class)
            for (field in entity.fields) {
                variables[field.columnName] = transformer.getModelValue(model, field)
            }
        }

        return variables
    }

}

fun sqlParams(vararg variables: Pair<String, Any?>) = SqlParams(variables.toMap().toMutableMap())

suspend fun main() {
    val model: ExplodingModel = null!!
    val iron: Iron = null!!
    val UUIDSerializer: ColumnSerializer<String, *> = null!!

    iron.prepare("INSERT INTO nerds VALUES(?, ?, ?)", sqlParams(
        "id" to Random.nextInt(),
        "name" to "bob",
        "json" to jsonField("{'a': 'b'}"),
        "hi" to serializedField("0bfea-...", UUIDSerializer)
    ) + model)

}