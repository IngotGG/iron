package gg.ingot.iron.bindings

import gg.ingot.iron.Iron

@Suppress("MemberVisibilityCanBePrivate")
class SqlBindings internal constructor(
    val map: MutableMap<String, Any?> = mutableMapOf()
) {

    val models: MutableList<Bindings> = mutableListOf()

    /**
     * Binds a variable to a value, this is used when you want to dynamically replace
     * ":<variable>" in a query with a value.
     * @param value The value to bind.
     */
    infix fun String.to(value: Any?) {
        map[this] = value
    }

    /**
     * Concatenates the bindings in this object with another bindings object.
     * @param bindings The bindings to concatenate with.
     * @return The concatenated bindings for chaining.
     */
    fun concat(bindings: SqlBindings): SqlBindings {
        map.putAll(bindings.map)
        return this
    }

    /**
     * Concatenates the bindings in this object with another bindings object.
     * @param bindings The bindings to concatenate with.
     */
    operator fun plus(bindings: SqlBindings): SqlBindings {
        return this.concat(bindings)
    }


    /**
     * Add all bindings from a @Model class to this bindings object. This requires models to implement
     * the Bindings interface.
     * @param model The model to add bindings from.
     * @return The concatenated bindings for chaining.
     */
    fun with(model: Bindings): SqlBindings {
        models.add(model)
        return this
    }

    fun parse(iron: Iron): Map<String, Any?> {
        val modelBindings = models
            .map { Bindings.of(it, iron) }
            .fold(SqlBindings()) { acc, binding -> acc.concat(binding) }

        return this.concat(modelBindings).map
    }

    /**
     * @return Checks if a variable is either null or not present in the bindings
     */
    fun isNull(variable: String): Boolean {
        return map.getOrDefault(variable, null) == null
    }

}

fun bind(block: SqlBindings.() -> Unit): SqlBindings {
    return SqlBindings().apply(block)
}

fun bind(vararg bindings: Pair<String, Any?>): SqlBindings {
    return SqlBindings().apply {
        map.putAll(bindings.toMap())
    }
}

fun bind(mappings: Map<String, Any?>): SqlBindings {
    return SqlBindings(mappings.toMutableMap())
}

fun bind(model: Bindings): SqlBindings {
    return SqlBindings().apply {
        models.add(model)
    }
}