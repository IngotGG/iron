package gg.ingot.iron.strategies

import gg.ingot.iron.IronSettings

/**
 * Transforms an enum value into a string and vice versa.
 *
 * By default [EnumTransformation.Name] is used for serialization and deserialization, which uses the name of the
 * enum value as seen in the code. However, support for ordinal values (position in the enum) can be added by
 * changing to use the [EnumTransformation.Ordinal] transformation. You can also implement your own transformation
 * by implementing the [EnumTransformation] interface.
 *
 * @see IronSettings.enumTransformation
 * @author santio
 * @since 2.0
 */
abstract class EnumTransformation {
    abstract fun <T: Enum<T>> serialize(value: Enum<T>): String
    abstract fun <T: Enum<T>> deserialize(value: String, enum: Class<*>): Enum<T>

    /**
     * Transforms an enum value into a string using the name of the enum value.
     */
    object Name: EnumTransformation() {
        override fun <T: Enum<T>> serialize(value: Enum<T>): String {
            return value.name
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> deserialize(value: String, enum: Class<*>): Enum<T> {
            return java.lang.Enum.valueOf(enum as Class<out Enum<*>>, value) as Enum<T>
        }
    }

    /**
     * Transforms an enum value into a string using the ordinal position of the enum value.
     */
    object Ordinal: EnumTransformation() {
        override fun <T: Enum<T>> serialize(value: Enum<T>): String {
            return value.ordinal.toString()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> deserialize(value: String, enum: Class<*>): Enum<T> {
            return enum.enumConstants[value.toInt()] as Enum<T>
        }
    }
}