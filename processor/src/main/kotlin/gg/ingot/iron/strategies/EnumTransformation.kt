package gg.ingot.iron.strategies

/**
 * Transforms an enum value into a string and vice versa.
 *
 * By default [EnumTransformation.Name] is used for serialization and deserialization, which uses the name of the
 * enum value as seen in the code. However, support for ordinal values (position in the enum) can be added by
 * changing to use the [EnumTransformation.Ordinal] transformation. You can also implement your own transformation
 * by implementing the [EnumTransformation] interface.
 *
 * @author santio
 * @since 2.0
 */
abstract class EnumTransformation {
    abstract fun <T: Enum<T>> serialize(value: Enum<T>): Any
    abstract fun <T: Enum<T>> deserialize(value: Any, enum: Class<*>): Enum<T>

    /**
     * Transforms an enum value into a string using the name of the enum value.
     */
    object Name: EnumTransformation() {
        override fun <T: Enum<T>> serialize(value: Enum<T>): Any {
            return value.name
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> deserialize(value: Any, enum: Class<*>): Enum<T> {
            if (value is String) {
                return java.lang.Enum.valueOf(enum as Class<out Enum<*>>, value) as Enum<T>
            } else {
                error("Failed to deserialize enum value, expected a string but got ${value::class.java.name} (value: $value)")
            }
        }
    }

    /**
     * Transforms an enum value into a string using the ordinal position of the enum value.
     */
    object Ordinal: EnumTransformation() {
        override fun <T: Enum<T>> serialize(value: Enum<T>): Any {
            return value.ordinal
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> deserialize(value: Any, enum: Class<*>): Enum<T> {
            return when (value) {
                is String -> deserialize(value.toIntOrNull() ?: false, enum)
                is Int -> enum.enumConstants[value] as Enum<T>
                else -> {
                    error("Failed to deserialize enum value, expected an int but got ${value::class.java.name} (value: $value)")
                }
            }
        }
    }
}