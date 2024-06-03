package gg.ingot.iron.strategies

/**
 * Naming strategy for transforming field names, since the string we're given is a field name we'll assume it's
 * originally in camel case.
 * @author Santio
 * @since 1.0
 */
@Suppress("unused")
enum class NamingStrategy {
    SNAKE_CASE {
        override fun transform(name: String): String {
            return buildString {
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0) {
                        append('_')
                    }
                    append(c.lowercase())
                }
            }
        }
    },
    CAMEL_CASE {
        override fun transform(name: String): String {
            return name.replaceFirstChar { it.lowercase() }
        }
    },
    KEBAB_CASE {
        override fun transform(name: String): String {
            return buildString {
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0) {
                        append('-')
                    }
                    append(c.lowercase())
                }
            }
        }
    },
    UPPER_SNAKE_CASE {
        override fun transform(name: String): String {
            return buildString {
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0) {
                        append('_')
                    }
                    append(c.uppercase())
                }
            }
        }
    },
    UPPER_CAMEL_CASE {
        override fun transform(name: String): String {
            return name.replaceFirstChar { it.uppercase() }
        }
    },
    UPPER_KEBAB_CASE {
        override fun transform(name: String): String {
            return buildString {
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0) {
                        append('-')
                    }
                    append(c.uppercase())
                }
            }
        }
    },
    ;

    abstract fun transform(name: String): String
}