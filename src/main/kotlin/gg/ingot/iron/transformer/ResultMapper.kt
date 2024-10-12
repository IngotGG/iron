package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.models.SqlColumn
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.serialization.ColumnDeserializer
import java.lang.reflect.ParameterizedType
import java.sql.ResultSet
import java.util.*
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor

/**
 * Handles mapping values from a result set to the requested type.
 * @author santio
 * @since 2.0
 */
internal class ResultMapper(private val iron: Iron) {

    private val trueValues = listOf("1", "true")
    private val falseValues = listOf("0", "false")

    /**
     * Takes a value from a [ResultSet] and converts it's requested java type.
     * @param resultSet The result set to retrieve the value from.
     * @param label The column label to retrieve the value for.
     * @param clazz The class to convert the value to.
     * @param deserializer The deserializer to use for the value.
     * @param json Whether the value is JSON (requires serialization setup on the iron instance).
     * @return The value from the result set.
     */
    @Suppress("UNCHECKED_CAST")
    fun read(
        resultSet: ResultSet,
        label: String?,
        clazz: Class<*>,
        deserializer: ColumnDeserializer<*, *>? = null,
        json: Boolean = false,
        column: SqlColumn? = null
    ): Any? {
        // If no label is provided, see if we were given one column back, if not we'll map to a model
        if (label == null) {
            val columns = resultSet.metaData.columnCount
            if (columns == 1) {
                return read(resultSet, resultSet.metaData.getColumnLabel(1), clazz, deserializer, json)
            }

            // Map to a model
            if(clazz.annotations.any { it is Model }) {
                error("The result returned multiple columns, however either need to specify that '${clazz.name}' is a model, make sure you only return one column, or use IronResultSet#get instead")
            }

            return mapModel(resultSet, clazz)
        }

        // Check if we're requesting an Optional, if so we'll parse with the type we want inside
        // the optional, and then wrap it in an Optional
        if (clazz.isAssignableFrom(Optional::class.java)) {
            val type = clazz.genericSuperclass as ParameterizedType
            val innerType = type.getUnderlyingType() ?: error("Could not get the underlying type of optional")

            return Optional.ofNullable(read(resultSet, label, innerType, deserializer, json))
        }

        // Get the value from the result set
        val value = if (clazz.isArray || Collection::class.java.isAssignableFrom(clazz)) {
            resultSet.getArray(label).array as Array<*>
        } else {
            resultSet.getObject(label)
        }

        // Handle null values
        if (value == null) return null

        // Handle transformations
        if (deserializer != null) {
            // User provided deserializer
            @Suppress("NAME_SHADOWING")
            val deserializer = deserializer as ColumnDeserializer<Any, *>
            return deserializer.fromDatabaseValue(value)
        }

        // Handle JSON
        if (json) {
            return iron.settings.serialization?.deserialize(value, clazz)
                ?: error("Tried to deserialize JSON for column '$label', but no serialization was setup on the iron instance.")
        }

        // Parse collections
        if (Collection::class.java.isAssignableFrom(clazz)) {
            return (value as Array<*>).toMutableList()
        }

        // Parse booleans
        if (clazz == java.lang.Boolean::class.java) {
            return if (trueValues.contains(value.toString())) true
            else if (falseValues.contains(value.toString())) false
            else error("Failed to convert boolean value for field '$label', found '$value'")
        }

        // Handle model specific parsing
        if (column != null) {
            val type = column.clazz

            // Parse enums
            if (type.isEnum) {
                val transformation = column.enum.kotlin
                val instance = transformation.objectInstance
                    ?: transformation.createInstance()

                return instance.deserialize(value, type)
            }
        }

        // Otherwise return the value
        return value
    }

    private fun mapModel(resultSet: ResultSet, clazz: Class<*>): Any {
        val table = SqlTable.get(clazz)
            ?: error("Failed to get table data for class '${clazz.name}', make sure your annotation processor is setup correctly.")

        val mapping = table.columns.associate {
            it.field to read(resultSet, it.name, it.clazz, column = it)
        }

        // Instantiate the model
        val constructors = clazz.declaredConstructors

        // We want to prefer the primary constructor, if it exists, otherwise we'll use the no-arg constructor
        val primaryConstructor = clazz.kotlin.primaryConstructor
            ?.takeIf { it.parameters.size == mapping.size }
            ?: clazz.kotlin.constructors.firstOrNull { it.parameters.size == mapping.size }

        if (primaryConstructor != null) {
            try {
                return primaryConstructor.callBy(mapping.map { (field, value) ->
                    val parameter = primaryConstructor.parameters.firstOrNull { it.name == field }
                        ?: error("Failed to find parameter for field '$field' in primary constructor of '${clazz.name}', make sure parameter names match the backing field name.")

                    parameter to value
                }.toMap())
            } catch (e: IllegalArgumentException) {
                val requiredTypes = primaryConstructor.parameters.map { it.type.toString() }
                val providedTypes = mapping.map {
                    if (it.value == null) "null" else it.value!!::class.java.name
                }

                throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor, expected types: $requiredTypes, but provided: $providedTypes", e)
            } catch (e: Exception) {
                throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor", e)
            }
        } else if (constructors.firstOrNull { it.parameterCount == 0 } != null) {
            try {
                val instance = constructors.first { it.parameterCount == 0 }.newInstance()

                mapping.forEach { (name, value) ->
                    val field = instance::class.java.getDeclaredField(name)

                    field.isAccessible = true
                    field.set(instance, value)
                }

                return instance
            } catch (e: Exception) {
                throw RuntimeException("Failed to instantiate model '${clazz.name}' with no-arg constructor", e)
            }
        } else {
            error("Failed to instantiate model '${clazz.name}', make sure it has either a no-arg constructor or a constructor with all parameters.")
        }
    }

    private fun Any.getUnderlyingType(): Class<*>? {
        val clazz = this::class.java

        return if (clazz.isArray) {
            clazz.componentType
        } else if (clazz.genericSuperclass is ParameterizedType) {
            val parameterizedType = clazz.genericSuperclass as ParameterizedType
            parameterizedType.actualTypeArguments[0] as Class<*>
        } else null
    }
}