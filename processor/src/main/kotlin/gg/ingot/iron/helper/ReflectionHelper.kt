package gg.ingot.iron.helper

import kotlin.reflect.KClass

/**
 * Holds internal helper methods for reflection. This class is meant for internal usage only.
 * @author santio
 * @since 2.0
 */
@Suppress("MemberVisibilityCanBePrivate")
object ReflectionHelper {
    /**
     * Maps a class name to its boxed counterpart.
     * @param className The class name to map.
     * @return The boxed class name.
     */
    fun box(className: String): String {
        return when (className) {
            "kotlin.Boolean", "bool" -> "java.lang.Boolean"
            "kotlin.Byte", "byte" -> "java.lang.Byte"
            "kotlin.Char", "char" -> "java.lang.Character"
            "kotlin.Short", "short" -> "java.lang.Short"
            "kotlin.Int", "int" -> "java.lang.Integer"
            "kotlin.Long", "long" -> "java.lang.Long"
            "kotlin.Float", "float" -> "java.lang.Float"
            "kotlin.Double", "double" -> "java.lang.Double"
            "kotlin.String" -> "java.lang.String"
            "kotlin.collections.List", "kotlin.collections.MutableList", "kotlin.collections.ArrayList" -> "java.util.ArrayList"
            "kotlin.collections.Set", "kotlin.collections.MutableSet", "kotlin.collections.HashSet" -> "java.util.HashSet"
            "kotlin.collections.Map", "kotlin.collections.MutableMap", "kotlin.collections.HashMap" -> "java.util.HashMap"
            else -> className
        }
    }

    /**
     * Maps a class to its boxed counterpart.
     * @return The boxed class.
     */
    fun Class<*>.box(): Class<out Any> {
        return Class.forName(box(this.name))
    }

    /**
     * Maps a kotlin class to its boxed counterpart.
     * @return The boxed kotlin class.
     */
    fun KClass<*>.box(): KClass<out Any> {
        val qualifiedName = this.qualifiedName
            ?: error("Failed to get qualified name of class '${this.java.name}'")

        return Class.forName(box(qualifiedName)).kotlin
    }
}