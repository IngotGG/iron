package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron
import java.lang.reflect.ParameterizedType

abstract class ValueAdapter<T: Any> {
    abstract fun fromDatabaseValue(value: Any, iron: Iron): T
    abstract fun toDatabaseValue(value: T, iron: Iron): Any

    internal fun Any.getUnderlyingType(): Class<*>? {
        val clazz = this::class.java

        return if (clazz.isArray) {
            clazz.componentType
        } else if (clazz.genericSuperclass is ParameterizedType) {
            val parameterizedType = clazz.genericSuperclass as ParameterizedType
            parameterizedType.actualTypeArguments[0] as Class<*>
        } else null
    }

}