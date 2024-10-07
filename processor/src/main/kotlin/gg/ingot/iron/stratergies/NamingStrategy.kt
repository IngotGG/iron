package gg.ingot.iron.stratergies

/**
 * Naming strategy for transforming field names, since the string we're given is a field name we'll assume it's
 * originally in camel case.
 * @author Santio
 * @since 1.0
 */
@Suppress("unused")
enum class NamingStrategy {
    NONE {
        override fun transform(name: String): String = name
    },
    SNAKE_CASE {
        override fun transform(name: String): String {
            return buildString {
                var wasUnderscore = name.first() == '_'
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0 && !wasUnderscore) {
                        append('_')
                        wasUnderscore = true
                    }

                    wasUnderscore = c == '_'
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
            var wasDash = name.first() == '-'
            return buildString {
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0 && !wasDash) {
                        append('-')
                    }

                    wasDash = c == '-'
                    append(c.lowercase())
                }
            }
        }
    },
    UPPER_SNAKE_CASE {
        override fun transform(name: String): String {
            return buildString {
                var wasUnderscore = name.first() == '_'
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0 && !wasUnderscore) {
                        append('_')
                    }

                    append(c.uppercase())
                    wasUnderscore = c == '_'
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
                var wasDash = name.first() == '-'
                name.forEachIndexed { index, c ->
                    if (c.isUpperCase() && index != 0 && !wasDash) {
                        append('-')
                    }

                    append(c.uppercase())
                    wasDash = c == '-'
                }
            }
        }
    },
    ;

    abstract fun transform(name: String): String
}