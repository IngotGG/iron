package gg.ingot.iron.representation

import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.strategies.NamingStrategy
import java.lang.reflect.Field
import kotlin.reflect.KProperty

/**
 * Represents a field in an entity.
 * @author Santio
 * @since 1.0
 * @see EntityModel
 */
internal data class EntityField(
    val field: KProperty<*>,
    val javaField: Field,
    val columnName: String,
    val nullable: Boolean,
    val isJson: Boolean,

    val isArray: Boolean,
    val isCollection: Boolean,
    val isEnum: Boolean,

    val deserializer: ColumnDeserializer<*, *>?,
) {
    /** Transforms the name of the column using the naming strategy. */
    fun convertedName(namingStrategy: NamingStrategy): String = namingStrategy.transform(columnName)
}
