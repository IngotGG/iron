package gg.ingot.iron.representation

import gg.ingot.iron.strategies.NamingStrategy
import kotlin.reflect.KClass

/**
 * A representation of a class (of which that class is a database entity). This holds information retrieved from
 * reflection about the class, allowing for the entity to be easily built.
 * @author Santio
 * @since 1.0
 * @see gg.ingot.iron.transformer.ModelTransformer
 */
internal data class EntityModel(
    val clazz: KClass<*>,
    val fields: List<EntityField>,
    val namingStrategy: NamingStrategy
)