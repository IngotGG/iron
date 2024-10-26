package gg.ingot.iron.sql.builder

import gg.ingot.iron.DBMS

/**
 * A quick builder for concatenating keywords together.
 * @author santio
 * @since 2.0
 */
internal class SqlBuilder(
    internal val driver: DBMS,
    private val components: MutableList<String> = mutableListOf(),
    private val statements: MutableList<SqlStatement> = mutableListOf(),
    private val values: MutableList<Any?> = mutableListOf()
) {

    /**
     * Creates a SQL builder from an SQL string. The resulting builder will likely not be
     * nice to work with as it's added as a single component to the query.
     * @param string The SQL string to parse.
     */
    constructor(string: String): this(DBMS.UNKNOWN, mutableListOf(string))

    /**
     * Get the context of the query. This is useful for understanding parts of the query. (ex: the table name
     * being affected by the query)
     * @return The context of the query.
     */
    val context: SqlContext = SqlContext(this)

    /**
     * Converts all the components in the builder into a single statement and adds it to the builder.
     * Any components after this method will be made into a new statement.
     * @return The statement that was created.
     */
    fun next(): SqlStatement {
        val statement = SqlStatement(
            sql = toString(),
            values = values.toList()
        )
        statements.add(statement)

        values.clear()
        components.clear()

        return statement
    }

    /**
     * Get all the statements in the builder.
     * @return The statements in the builder.
     */
    fun statements(): List<SqlStatement> {
        if (components.isNotEmpty()) next()
        return statements
    }

    /**
     * Get all the values for this statement.
     * @return The values for this statement.
     */
    fun values(): List<Any?> {
        return values
    }

    /**
     * Add a value to the builder
     * @param value The values to add
     */
    fun addValue(vararg value: Any?) {
        values.addAll(value)
    }

    /**
     * Wrap the builder with a prefix and suffix component.
     * @param prefix The prefix to wrap the builder with.
     * @param suffix The suffix to wrap the builder with.
     * @return The wrapped builder.
     */
    fun wrap(prefix: String, suffix: String): SqlBuilder {
        append(0, prefix)
        append(components.size, suffix)
        return this
    }

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
     * @param index The index to append the components at.
     * @param values The components to append.
     */
    fun append(index: Int, vararg values: String) {
        for (value in values) {
            components.add(index, value.trim())
        }
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