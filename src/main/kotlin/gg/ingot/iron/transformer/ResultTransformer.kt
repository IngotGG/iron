package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * Transforms a result set into a model representation, allowing for the model to be easily built. You shouldn't
 * call this method directly, instead use the mapper variant of the methods in [Iron] to automatically map the
 * result set to a model.
 * @author Santio
 * @since 1.0
 */
internal class ResultTransformer(
    private val valueTransformer: ValueTransformer
) {
    fun <T: Any> read(result: ResultSet, clazz: KClass<T>): T {
        val entity = ModelTransformer.transform(clazz)

        val emptyConstructor = clazz.constructors.firstOrNull { it.parameters.isEmpty() }
        val fullConstructor = clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }

        if (emptyConstructor != null) {
            emptyConstructor.isAccessible = true
            val model = emptyConstructor.call()

            for (field in entity.fields) {
                val value = valueTransformer.convert(result, field)
                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                field.javaField.setAccessible(true)
                field.javaField.set(model, value)
            }

            return model
        } else if (fullConstructor != null) {
            fullConstructor.isAccessible = true

            val fields = entity.fields.map { field ->
                val value = valueTransformer.convert(result, field)
                if (value == null && !field.nullable) {
                    error("Field '${field.field.name}' is not nullable but the associated column '${field.columnName}' was null for model: $clazz")
                }

                value
            }

            return fullConstructor.call(*fields.toTypedArray())
        } else {
            error("No empty or full constructor found for model: $clazz")
        }
    }
}