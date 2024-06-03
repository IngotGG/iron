package gg.ingot.iron.repository

import gg.ingot.iron.representation.EntityModel
import kotlin.reflect.KClass

/**
 * Holds a cache of all the [EntityModel]s in the application, allowing for no reflection to be done more than once.
 * @author Santio
 * @since 1.0
 */
internal object ModelRepository {

    val models = HashMap<KClass<*>, EntityModel>()

}