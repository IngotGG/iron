package gg.ingot.iron.sql.binding.types

import kotlin.reflect.KClass

class JsonType<T: Any> private constructor(
    val type: Class<T>
) {

    companion object {
        @JvmStatic
        @JvmName("ofKType")
        inline fun <reified T : Any> json(type: KClass<T> = T::class): JsonType<T> {
            return json(type.java)
        }

        @JvmStatic
        @JvmName("of")
        fun <T: Any> json(type: Class<T>): JsonType<T> {
            return JsonType(type)
        }
    }

}