package gg.ingot.iron.transformer

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.helper.ReflectionHelper.box
import gg.ingot.iron.models.SqlColumn
import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.strategies.EnumTransformation
import gg.ingot.iron.strategies.EnumTransformation.Companion.instance
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.lang.reflect.ParameterizedType
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Handles mapping values from a result set to the requested type.
 * @author santio
 * @since 2.0
 */
class ResultMapper internal constructor(private val iron: Iron) {

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
            if (clazz.annotations.any { it is Model }) {
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
        val value = if (clazz == ByteArray::class.java) {
            try {
                return resultSet.getObject(label) as ByteArray
            } catch (e: Exception) {
                val blob = resultSet.getBlob(label)
                return blob.getBytes(1, blob.length().toInt())
            } catch (e: Exception) {
                error("Failed to convert blob to ByteArray")
            }
        } else if (clazz == ByteArrayInputStream::class.java) {
            val blob = resultSet.getBlob(label)
            val stream = blob.binaryStream

            return if (stream is ByteArrayInputStream) {
                stream
            } else error("Failed to convert blob to ByteArrayInputStream")
        } else if (clazz == InputStream::class.java) {
            val blob = resultSet.getBlob(label)
            return blob.binaryStream
        } else if (clazz.isArray || Collection::class.java.isAssignableFrom(clazz)) {
            try {
                resultSet.getArray(label).array as Array<*>
            } catch (ex: SQLException) {
                try {
                    resultSet.getObject(label) as Array<*>
                } catch (ex: ClassCastException) {
                    throw SQLFeatureNotSupportedException("Failed to get array from ResultSet, this driver might not support arrays, try using an adapter")
                }
            }
        } else {
            resultSet.getObject(label)
        }

        return deserialize(value, label, clazz, json, deserializer, column)
    }

    private fun mapModel(resultSet: ResultSet, clazz: Class<*>): Any {
        val table = SqlTable.get(clazz)
            ?: error("Failed to get table data for class '${clazz.name}', make sure your annotation processor is setup correctly.")

        val mapping = table.columns.associate {
            it.field to read(resultSet, it.name, it.clazz(), column = it)
        }.toMutableMap()

        // We want to prefer the primary constructor, if it exists, otherwise we'll use the no-arg constructor
        val primaryConstructor = clazz.kotlin.primaryConstructor
            ?.takeIf { it.parameters.isNotEmpty() }
            ?: clazz.kotlin.constructors.firstOrNull { it.parameters.size == mapping.size }

        if (primaryConstructor != null) {
            primaryConstructor.isAccessible = true

            try {
                // todo: java support
                return primaryConstructor.callBy(mapping.map { (field, value) ->
                    val parameter = primaryConstructor.parameters.firstOrNull { it.name == field }
                        ?: error("Failed to find parameter for field '$field' in primary constructor of '${clazz.name}', make sure parameter names match the backing field name.")

                    parameter to value
                }.toMap())
            } catch (e: IllegalArgumentException) {
                val missingParameters = primaryConstructor.parameters.filter { !it.isOptional }
                    .filter { it.name !in mapping.map { m -> m.key } }

                val mismatchedParameters = primaryConstructor.parameters
                    .filter { it.name in mapping.map { m -> m.key } }
                    .filter {
                        val type = (it.type.classifier as KClass<*>).java.box()
                        val providedType = mapping[it.name]!!.javaClass.box()

                        type != providedType
                    }.associateWith { mapping[it.name]!! }

                if (missingParameters.isNotEmpty()) {
                    throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor, " +
                        "missing parameters: ${missingParameters.joinToString { "${it.type} ${it.name}" }}", e
                    )
                } else if (mismatchedParameters.isNotEmpty()) {
                    throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor, " +
                        "mismatched parameters: ${mismatchedParameters.map { "${it.key.type} ${it.key.name} (was ${it.value::class.java})" }}", e
                    )
                } else {
                    throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor", e)
                }
            } catch (e: Exception) {
                throw RuntimeException("Failed to instantiate model '${clazz.name}' with primary/full constructor", e)
            }
        } else {
            try {
                val instance = clazz.kotlin.createInstance()

                mapping.forEach { (name, value) ->
                    val field = instance::class.java.getDeclaredField(name)

                    field.isAccessible = true
                    field.set(instance, value)
                }

                return instance
            } catch (e: Exception) {
                throw RuntimeException("Failed to instantiate model '${clazz.name}' with no-arg constructor", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserialize(
        value: Any?,
        label: String,
        clazz: Class<*>,
        json: Boolean = false,
        deserializer: ColumnDeserializer<*, *>? = null,
        column: SqlColumn? = null
    ): Any? {
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
        if (Collection::class.java.isAssignableFrom(clazz) || clazz.isArray) {
            if (value !is Array<*>) error("Failed to convert collection for field '$label', found '$value'")

            val type = clazz.getUnderlyingType()
                ?: Object::class.java

            val typed = value.map {
                if (it == null) null
                else deserialize(it, label, type)
            }

            return when {
                List::class.java.isAssignableFrom(clazz) -> ArrayList(typed)
                clazz == LinkedList::class.java -> LinkedList(typed)
                clazz == TreeSet::class.java -> TreeSet(typed)
                Set::class.java.isAssignableFrom(clazz) -> HashSet(typed)
                clazz.isArray -> convertListToArray(typed, type)
                else -> error("Failed to convert collection for field '$label', found '${clazz.name}'")
            }
        }

        // Parse booleans
        if (clazz == java.lang.Boolean::class.java) {
            return if (trueValues.contains(value.toString())) true
            else if (falseValues.contains(value.toString())) false
            else error("Failed to convert boolean value for field '$label', found '$value'")
        }

        // Handle model specific parsing
        if (column != null) {
            val type = column.clazz()

            // Parse enums
            if (type.isEnum) {
                val instance = column.enum.instance()
                return instance.deserialize(value, type)
            }

            // Parse json
            if (column.json) {
                val serialization = iron.settings.serialization
                    ?: error("Tried to deserialize JSON for column '${column.name}', but no serialization was setup on the iron instance.")

                return serialization.deserialize(value, type)
            }
        }

        // Handle timestamps
        if (column?.timestamp == true) {
            val timestamp = when (value) {
                is Timestamp -> value
                is Long -> Timestamp(value)
                is String -> Timestamp.valueOf(value)
                else -> error("Expected a timestamp, but database returned '${value::class.java.name}' ($value)")
            }

            return when (clazz) {
                java.lang.Long::class.java -> timestamp.time
                java.lang.Integer::class.java -> timestamp.time.toInt()
                java.sql.Date::class.java -> java.sql.Date(timestamp.time)
                java.util.Date::class.java -> java.util.Date(timestamp.time)
                Instant::class.java -> Instant.ofEpochMilli(timestamp.time)
                LocalDateTime::class.java -> LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp.time), ZoneId.systemDefault())
                String::class.java -> timestamp.toString()
                Timestamp::class.java -> timestamp
                else -> error("Failed to convert timestamp for field '$label', found '${clazz.name} which can't be deserialized into by default'")
            }
        }

        // Otherwise return the value
        return value
    }

    /**
     * Prepares the value to be transformed into a database-acceptable value.
     * @param value The value to prepare.
     * @return The prepared value.
     */
    fun serialize(column: SqlColumn?, value: Any?): Any? {
        if (value == null) return null
        if (value is Byte) return value

        // Handle enums
        if (value is Enum<*>) {
            val transformer = column?.enum?.instance()
                ?: EnumTransformation.Name

            return transformer.serialize(value)
        }

        // Handle collections
        if (value is Collection<*> || value is Array<*>) {
            if (value is ByteArray) {
                return value
            }

            val list = if (value is Collection<*>) value
            else (value as Array<*>).toList()

            val type = list.firstOrNull()?.javaClass
                ?: Object::class.java

            return convertListToArray(list.map {
                serialize(null, it)
            }, type)
        }

        // Handle JSON
        if (column?.json == true) {
            val serialization = iron.settings.serialization
                ?: error("Tried to serialize JSON for column '${column.name}', but no serialization was setup on the iron instance.")

            return serialization.serialize(value, column.clazz())
        }

        // Handle timestamps
        if (column?.timestamp == true) {
            return when (value) {
                is Timestamp -> value
                is java.util.Date -> Timestamp(value.time)
                is java.time.Instant -> Timestamp(value.toEpochMilli())
                is LocalDateTime -> Timestamp(value.toInstant(ZoneOffset.UTC).toEpochMilli())
                is Long -> Timestamp(value)
                is Int -> Timestamp(value.toLong())
                is String -> Timestamp(value.toLong())
                else -> error("Failed to convert timestamp for field '${column.name}', found '${value::class.java.name} which can't be serialized into by default'")
            }
        }

        return value
    }

    private fun Any.getUnderlyingType(): Class<*>? {
        val clazz = if (this is Class<*>) this else this::class.java

        return if (clazz.isArray) {
            clazz.componentType
        } else if (clazz.genericSuperclass is ParameterizedType) {
            val parameterizedType = clazz.genericSuperclass as ParameterizedType
            val typeArgument = parameterizedType.actualTypeArguments[0]

            if (typeArgument is Class<*>) typeArgument
            else null
        } else null
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertListToArray(list: List<*>, type: Class<*>): Array<*> {
            val array = java.lang.reflect.Array.newInstance(type, list.size) as Array<Any?>

            for (i in list.indices) {
                try {
                    array[i] = list[i]
                } catch (e: ArrayStoreException) {
                    error("Failed to add element to typed array, value '${list[i]}' is not assignable to type '$type', was ${list[i]?.javaClass?.name ?: "null"}")
                }
            }

            return array
    }
}