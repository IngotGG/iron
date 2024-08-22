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
    val isBoolean get() = field.type == Boolean::class.java || field.type == Boolean::class.javaPrimitiveType
    val isArray get() = field.type.isArray
    val isEnum get() = field.type.isEnum || isArray && field.type.componentType.isEnum
    val isCollection get() = Collection::class.java.isAssignableFrom(field.type)

    fun value(instance: Any): Any? {
        return field.get(instance)
    }

    /** Transforms the name of the column using the naming strategy. */
    fun convertedName(namingStrategy: NamingStrategy): String = namingStrategy.transform(columnName)
}
