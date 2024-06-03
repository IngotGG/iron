package gg.ingot.iron.representation

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
    val nullable: Boolean
)
