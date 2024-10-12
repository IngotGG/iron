package gg.ingot.iron.models

import gg.ingot.iron.strategies.EnumTransformation

/**
 * Represents a column a table, this should contain all possible information about the column.
 * This is built during the compile-time using information from [ColumnBundle].
 * @author santio
 * @since 2.0
 */
data class SqlColumn(
    /** The name of the column in the database. */
    val name: String,
    /** The named variable to reference this column in the model. (ex: 'id' will replace :id in queries) */
    val variable: String,
    /** The name of the field in the model. */
    val field: String,
    /** The (boxed) type of the column. References a class name. */
    val clazz: Class<*>,
    /** How enums are stored in the database. References a class name. If not specified, the Iron defaults are used. */
    val enum: Class<out EnumTransformation> = EnumTransformation.Name::class.java,
    /** Whether the column is nullable. */
    val nullable: Boolean,
    /** Whether the column is a primary key. */
    val primaryKey: Boolean,
    /** Whether the column is an auto increment. */
    val autoIncrement: Boolean,
    /** The hash of the column which changes when any details of the column change. */
    val hash: String
) {
    /**
     * Get the value of the column from the instance.
     * @param instance The instance to get the value from.
     * @return The value of the column.
     */
    fun value(instance: Any): Any? {
        val field = instance::class.java.getDeclaredField(variable)
        field.isAccessible = true

        return field.get(instance)
    }
}