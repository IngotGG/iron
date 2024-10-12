package gg.ingot.iron.models.bundles

import gg.ingot.iron.processor.reader.ModelReader
import gg.ingot.iron.strategies.EnumTransformation
import java.io.ByteArrayOutputStream

/**
 * This is a processor class that is used to build [SqlColumn] objects.
 * @author santio
 * @since 2.0
 */
internal data class ColumnBundle(
    /** The name of the column in the database. */
    val name: String,
    /** The named variable to reference this column in the model. (ex: 'id' will replace :id in queries) */
    val variable: String,
    /** The name of the field in the model. */
    val field: String,
    /** The (boxed) type of the column. References a class name. */
    val clazz: String,
    /**
     * The literal pointing to the qualified name of a [EnumTransformation] to use for this column.
     * Example: `my.package.transformation.MyTransformation`
     */
    val enum: String?,
    /** Whether the column is nullable. */
    val nullable: Boolean,
    /** Whether the column is a primary key. */
    val primaryKey: Boolean,
    /** Whether the column is an auto increment. */
    val autoIncrement: Boolean
) {

    /**
     * Generates a hash of the column which changes when any details of the column change.
     * @return The hash of the column.
     */
    fun hash(): String {
        val stream = ByteArrayOutputStream()

        stream.write(name.toByteArray())
        stream.write(variable.toByteArray())
        stream.write(clazz.toByteArray())
        stream.write(if (nullable) 1 else 0)
        stream.write(if (primaryKey) 1 else 0)
        stream.write(if (autoIncrement) 1 else 0)
        if (enum != null) stream.write(enum.toByteArray())
        else stream.write(-1)

        val bytes = stream.toByteArray()
        stream.close()

        return ModelReader.md5.digest(bytes).joinToString("") { "%02x".format(it) }
    }

    /**
     * Maps the column's class to its java boxed counterpart.
     * @return The boxed type.
     */
    fun boxedClass(): String {
        return when (clazz) {
            "kotlin.Boolean", "bool" -> "java.lang.Boolean"
            "kotlin.Byte", "byte" -> "java.lang.Byte"
            "kotlin.Char", "char" -> "java.lang.Character"
            "kotlin.Short", "short" -> "java.lang.Short"
            "kotlin.Int", "int" -> "java.lang.Integer"
            "kotlin.Long", "long" -> "java.lang.Long"
            "kotlin.Float", "float" -> "java.lang.Float"
            "kotlin.Double", "double" -> "java.lang.Double"
            "kotlin.String" -> "java.lang.String"
            else -> clazz
        }
    }

}
