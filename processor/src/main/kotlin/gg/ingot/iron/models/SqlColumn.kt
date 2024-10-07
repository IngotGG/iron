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
    /** The type of the column. */
    val type: ColumnType,
    /** Whether the column is nullable. */
    val nullable: Boolean,
    /** Whether the column is a primary key. */
    val primaryKey: Boolean,
    /** Whether the column is an auto increment. */
    val autoIncrement: Boolean,
    /** The default value of the column, or null if there is no default value. */
    val defaultValue: String?        // if data class, we'll use the primary constructor to get the values

) {
    fun hash(): String {
        val stream = ByteArrayOutputStream()

        stream.write(name.toByteArray())
        stream.write(variable.toByteArray())
        stream.write(type::class.simpleName!!.toByteArray())
        stream.write(if (nullable) 1 else 0)
        stream.write(if (primaryKey) 1 else 0)
        stream.write(if (autoIncrement) 1 else 0)
        stream.write(defaultValue?.toByteArray() ?: ByteArray(0))

        val bytes = stream.toByteArray()
        stream.close()

        return bytes.joinToString { "%02x".format(it) }
    }
}