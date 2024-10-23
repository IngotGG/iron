package gg.ingot.iron.sql.builder

/**
 * A quick builder for concatenating keywords together.
 * @author santio
 * @since 2.0
 */
internal class SqlBuilder(
    private val components: MutableList<String> = mutableListOf()
) {

    /**
     * Creates a SQL builder from an SQL string. The resulting builder will likely not be
     * nice to work with as it's added as a single component to the query.
     * @param string The SQL string to parse.
     */
    constructor(string: String): this(mutableListOf(string))

    /**
     * @return The number of components in the builder.
     */
    fun count(): Int {
        return components.size
    }

    /**
     * Appends a component to the builder.
     * @param values The strings to append.
     */
    fun append(vararg values: String) {
        components.addAll(values.map { it.trim() })
    }

    /**
     * Appends a component to the builder at the specified index.
     * @param value The component to append.
     * @param index The index to append the component at.
     */
    fun append(value: String, index: Int) {
        components.add(index, value.trim())
    }

    /**
     * Appends a sql builder to the builder. We don't want to cause any issues with
     * sub-queries, so we'll add the entire sub-query as a component to the builder.
     * @param builder The builder to append.
     */
    fun append(builder: SqlBuilder) {
        components.add("(${builder.toString().trim()})")
    }

    /**
     * Gets the index of the last occurrence of a component.
     * @param component The component to search for.
     * @return The index of the last occurrence of the component, or -1 if it doesn't exist.
     */
    fun lastIndexOf(component: String): Int {
        return components.lastIndexOf(component.trim())
    }

    /**
     * Gets the index of the first occurrence of a component.
     * @param component The component to search for.
     * @return The index of the first occurrence of the component, or -1 if it doesn't exist.
     */
    fun firstIndexOf(component: String): Int {
        return components.indexOf(component.trim())
    }

    /**
     * Removes a component from the builder.
     * @param index The index of the component to remove.
     */
    fun remove(index: Int) {
        components.removeAt(index)
    }

    /**
     * Checks if the builder contains a component.
     * @param component The component to check for.
     * @return Whether the builder contains the component.
     */
    fun contains(component: String): Boolean {
        return components.contains(component.trim())
    }

    /**
     * Gets the component from the specified index
     * @param index The index of the component to get, or a negative index to get from the end.
     * @return The component at the specified index.
     */
    fun get(index: Int): String? {
        return if (index < 0) components.getOrNull(components.size + index)
        else components.getOrNull(index)
    }

    /**
     * Replace a component at the specified index with a new component.
     * @param index The index of the component to replace.
     * @param value The new component to replace the old one with.
     */
    fun replace(index: Int, value: String) {
        if (index < 0) components[components.size + index] = value
        else components[index] = value
    }

    override fun toString(): String {
        return components.joinToString(" ")
    }

}