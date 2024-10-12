package gg.ingot.iron.models

import java.io.ByteArrayOutputStream

/**
 * Represents a column a table, this should contain all possible information about the column.
 * @author santio
 * @since 2.0
 */
data class SqlColumn(
    /** The name of the column in the database. */
    val name: String,
    /** The named variable to reference this column in the model. (ex: 'id' will replace :id in queries) */
    val variable: String,
    /** The name of the field in the model. */
    val field: String,
    /** The (boxed) type of the column. */
    val clazz: String,
    /** Whether the column is nullable. */
    val nullable: Boolean,
    /** Whether the column is a primary key. */
    val primaryKey: Boolean,
    /** Whether the column is an auto increment. */
    val autoIncrement: Boolean
) {
    fun hash(): String {
        val stream = ByteArrayOutputStream()

        stream.write(name.toByteArray())
        stream.write(variable.toByteArray())
        stream.write(clazz.toByteArray())
        stream.write(if (nullable) 1 else 0)
        stream.write(if (primaryKey) 1 else 0)
        stream.write(if (autoIncrement) 1 else 0)

        val bytes = stream.toByteArray()
        stream.close()

        return bytes.joinToString { "%02x".format(it) }
    }

    fun boxedClass(): Class<*> = box(clazz)

    /**
     * Maps a primitive type to its java boxed counterpart.
     * @param type The type to map.
     * @return The boxed type.
     */
    private fun box(className: String): Class<*> {
        return when (className) {
            "kotlin.Boolean", "java.lang.Boolean" -> Boolean::class.java
            "kotlin.Byte", "java.lang.Byte" -> Byte::class.java
            "kotlin.Char", "java.lang.Character" -> Char::class.java
            "kotlin.Short", "java.lang.Short" -> Short::class.java
            "kotlin.Int", "java.lang.Integer" -> Int::class.java
            "kotlin.Long", "java.lang.Long" -> Long::class.java
            "kotlin.Float", "java.lang.Float" -> Float::class.java
            "kotlin.Double", "java.lang.Double" -> Double::class.java
            "kotlin.String", "java.lang.String" -> String::class.java
            else -> Class.forName(className)
        }
    }

    /**
     * Get the value of the column from the instance.
     * @param instance The instance to get the value from.
     * @return The value of the column.
     */
    fun value(instance: Any): Any? {
        val field = instance::class.java.getDeclaredField(variable)
        field.isAccessible = true

        return field.get(instance)
    }
}