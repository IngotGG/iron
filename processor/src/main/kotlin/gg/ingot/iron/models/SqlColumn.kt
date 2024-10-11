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

    fun boxedClass(): Class<*> = box(Class.forName(clazz))

    /**
     * Maps a primitive type to its java boxed counterpart.
     * @param type The type to map.
     * @return The boxed type.
     */
    private fun box(type: Class<*>): Class<*> {
        return when (type) {
            Class.forName("java.lang.Boolean") -> Boolean::class.java
            Boolean::class.javaPrimitiveType -> Boolean::class.java
            Byte::class.javaPrimitiveType -> Byte::class.java
            Char::class.javaPrimitiveType -> Char::class.java
            Short::class.javaPrimitiveType -> Short::class.java
            Int::class.javaPrimitiveType -> Int::class.java
            Long::class.javaPrimitiveType -> Long::class.java
            Float::class.javaPrimitiveType -> Float::class.java
            Double::class.javaPrimitiveType -> Double::class.java
            Void::class.javaPrimitiveType -> Void::class.java
            else -> type
        }
    }
}