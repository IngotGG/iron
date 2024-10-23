package gg.ingot.iron.sql.types

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.builder.SqlBuilder

/**
 * Represents a column definition, unlike [ExpColumn] this class holds the definition of a column
 * and is not used to reference an existing column.
 * @param name The name of the column
 * @param type The type of the column
 * @param nullable Whether the column is nullable
 * @param autoIncrement Whether the column is an auto increment column
 * @param primaryKey Whether the column is a primary key
 * @param foreignKey Whether the column is a foreign key
 * @param defaultValue The default value of the column
 */
data class ColumnData internal constructor(
    var name: String,
    var type: String,
    var nullable: Boolean = true,
    var autoIncrement: Boolean = false,
    var primaryKey: Boolean = false,
    var foreignKey: Column? = null,
    var defaultValue: String? = null,
): Expression() {

    override fun asString(sql: Sql): String {
        return ExpColumn(name, null).asString(sql)
    }

    fun create(sql: Sql): String {
        val builder = SqlBuilder()

        builder.append(sql.driver.literal(name))
        builder.append(type)
        if (!nullable) builder.append(" NOT NULL")
        if (autoIncrement) builder.append(" AUTO_INCREMENT")
        if (primaryKey) builder.append(" PRIMARY KEY")

        return builder.toString()
    }

}

@Suppress("FunctionName")
fun Column(name: String? = null, type: String? = null, builder: ColumnData.() -> Unit): ColumnData {
    val column = ColumnData(name ?: "UNDEFINED", type ?:"UNDEFINED")
    builder(column)
    return column
}