package gg.ingot.iron.representation

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.strategies.NamingStrategy
import java.lang.reflect.Field

/**
 * Represents a field in an entity.
 * @author Santio
 * @since 1.0
 * @see EntityModel
 */
data class EntityField(
    val field: Field,
    val columnName: String,
    val variableName: String,
    val nullable: Boolean,
    val isJson: Boolean,
    val isPrimaryKey: Boolean,
    val serializer: ColumnSerializer<*, *>?,
    val deserializer: ColumnDeserializer<*, *>?,
) {
    val isBoolean get() = box(field.type) == Boolean::class.java
    val isArray get() = field.type.isArray
    val isEnum get() = field.type.isEnum || isArray && field.type.componentType.isEnum
    val isCollection get() = Collection::class.java.isAssignableFrom(field.type)

    fun value(instance: Any): Any? {
        return field.get(instance)
    }

    /**
     * Maps a primitive type to its java boxed counterpart.
     * @param type The type to map.
     * @return The boxed type.
     */
    private fun box(type: Class<*>): Class<*> {
        return when (type) {
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

    /** Transforms the name of the column using the naming strategy. */
    fun convertedName(namingStrategy: NamingStrategy): String = namingStrategy.transform(columnName)
}
