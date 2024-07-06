package gg.ingot.iron.representation

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.strategies.NamingStrategy
import gg.ingot.iron.transformer.isArray
import gg.ingot.iron.transformer.isCollection
import gg.ingot.iron.transformer.isEnum
import java.lang.reflect.Field
import kotlin.reflect.KProperty

/**
 * Represents a field in an entity.
 * @author Santio
 * @since 1.0
 * @see EntityModel
 */
data class EntityField(
    val field: KProperty<*>,
    val javaField: Field,
    val columnName: String,
    val nullable: Boolean,
    val isJson: Boolean,
    val serializer: ColumnSerializer<*, *>?,
    val deserializer: ColumnDeserializer<*, *>?,
) {
    val isArray get() = isArray(field)

    val isCollection get() = isCollection(field)

    val isEnum get() = isEnum(field)

    /** Transforms the name of the column using the naming strategy. */
    fun convertedName(namingStrategy: NamingStrategy): String = namingStrategy.transform(columnName)
}
