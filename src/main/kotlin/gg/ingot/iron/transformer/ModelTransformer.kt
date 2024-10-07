package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.representation.EntityModel
import gg.ingot.iron.transformer.models.ConstructorDetails
import java.sql.ResultSet
import kotlin.jvm.internal.DefaultConstructorMarker
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor

/**
 * Converts a result set into a model representation, allowing for the model to be easily built.
 * @author santio
 * @since 2.0
 */
internal class ModelTransformer(
    private val iron: Iron
) {

    /**
     * Reads the result set and transforms it into a model.
     * @param result The result set to read from.
     * @param clazz The class to transform the result to.
     * @return The model from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> readModel(result: ResultSet, clazz: Class<T>): T {
        val entity = iron.modelReader.read(clazz)
        val details = getConstructor(clazz, entity)

        val accessible = details.constructor.trySetAccessible()
        if (!accessible) error("Failed to make constructor for '${clazz.simpleName}' accessible")

        if (details.setParameters) {
            val model: T = details.constructor.newInstance() as T

            for (column in entity.fields) {
                val value = iron.valueTransformer.fromResultSet(result, column)
                column.field.java.set(model, value)
            }

            return model
        } else {
            val arguments = entity.fields.map { field ->
                iron.valueTransformer.fromResultSet(result, field)
            }.toMutableList()

            // Handle default constructor marker (for kotlin data classes)
            val parameters = details.constructor.parameters
            if (parameters.isNotEmpty() && parameters.last().type == DefaultConstructorMarker::class.java) {
                arguments.add(null)
            }

            try {
                return details.constructor.newInstance(*arguments.toTypedArray()) as T
            } catch (ex: IllegalArgumentException) {

                val expectedTypes = details.constructor.parameters.joinToString(", ") { it.type.simpleName }
                val argTypes = arguments.joinToString(", ") { it?.javaClass?.simpleName ?: "null" }

                throw RuntimeException("Invalid arguments passed for query, expected '[${expectedTypes}]' but got '[${argTypes}]'", ex)

            } catch (ex: Exception) {
                throw RuntimeException("Failed to create instance for model '${clazz.simpleName}'", ex)
            }
        }
    }

    /**
     * Gets the constructor to use for the model.
     * @param clazz The class to get the constructor for.
     * @return The constructor details.
     */
    private fun getConstructor(clazz: Class<*>, entity: EntityModel): ConstructorDetails {
        // Try to use a constructor with all parameters, since that's the easiest to work with
        val fullConstructor = if (clazz.isRecord) {
            clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }
        } else if (clazz.kotlin.isData) {
            clazz.kotlin.primaryConstructor?.javaConstructor
        } else {
            clazz.constructors.firstOrNull { it.parameters.size == entity.fields.size }
        }

        if (fullConstructor != null) {
            return ConstructorDetails(fullConstructor, false)
        }

        // Try to use an empty constructor, and we'll use reflection to set the parameters
        val emptyConstructor = clazz.constructors.firstOrNull { it.parameters.isEmpty() }
        if (emptyConstructor != null) {
            return ConstructorDetails(emptyConstructor, true)
        }

        error("Models require either a constructor with all parameters or an empty constructor, but found none for '${clazz.simpleName}'")
    }
}