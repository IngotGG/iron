package gg.ingot.iron.representation

import gg.ingot.iron.model.ModelField
import gg.ingot.iron.serialization.ColumnDeserializer
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.stratergies.EnumTransformation
import gg.ingot.iron.stratergies.NamingStrategy
import java.lang.reflect.ParameterizedType

/**
 * Represents a field in an entity.
 * @author Santio
 * @since 1.0
 * @see EntityModel
 */
data class EntityField(
    val field: ModelField,
    val nullable: Boolean,
    val isJson: Boolean,
    val serializer: ColumnSerializer<*, *>?,
    val deserializer: ColumnDeserializer<*, *>?,
    val enumTransformation: EnumTransformation?,

    // Computed
    val isArray: Boolean = field.java.type.isArray,
    val isCollection: Boolean = Collection::class.java.isAssignableFrom(field.java.type),
    val isSet: Boolean = Set::class.java.isAssignableFrom(field.java.type),
) {

    val isOptional: Boolean = field.java.type == java.util.Optional::class.java
    val name: String = field.java.name

    internal fun getUnderlyingType(): Class<*> {
        return if (field.java.type.isArray) {
            field.java.type.componentType
        } else if (field.java.genericType is ParameterizedType) {
            val parameterizedType = field.java.genericType as ParameterizedType
            parameterizedType.actualTypeArguments[0] as Class<*>
        } else field.java.type
    }

    fun value(instance: Any): Any? {
        return field.java.get(instance)
    }

    /** Transforms the name of the column using the naming strategy. */
    fun convertedName(namingStrategy: NamingStrategy): String = namingStrategy.transform(field.column)
}
