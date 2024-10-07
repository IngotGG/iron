package gg.ingot.iron.processor.generator

import com.squareup.kotlinpoet.CodeBlock
import gg.ingot.iron.models.ColumnType
import gg.ingot.iron.models.SqlColumn

/**
 * Generates kotlinpoet code for columns in a table.
 * @author santio
 * @since 2.0
 */
object ColumnGenerator {

    /**
     * Generates kotlinpoet code for a column in a table with its static information.
     * @param column The column to generate the code for.
     * @return The property spec for the column.
     */
    fun generate(column: SqlColumn): CodeBlock {
        return CodeBlock.builder()
            .add("%T(\n", SqlColumn::class)
            .add("  name = %S,\n", column.name)
            .add("  variable = %S,\n", column.variable)
            .add("  type = %L,\n", generateType(column.type))
            .add("  nullable = %L,\n", column.nullable)
            .add("  primaryKey = %L,\n", column.primaryKey)
            .add("  autoIncrement = %L,\n", column.autoIncrement)
            .add("  defaultValue = %L,\n", column.defaultValue)
            .add(")")
            .build()
    }

    private fun generateType(type: ColumnType): CodeBlock {
        val clazz = ColumnType::class.qualifiedName!!

        return when(type) {
            is ColumnType.INT -> CodeBlock.of("$clazz.INT(%L)", type.size)
            is ColumnType.STRING -> CodeBlock.of("$clazz.STRING(%L)", type.size)
            is ColumnType.BOOLEAN -> CodeBlock.of("$clazz.BOOLEAN")
            is ColumnType.FLOAT -> CodeBlock.of("$clazz.FLOAT(%L)", type.size)
            is ColumnType.DOUBLE -> CodeBlock.of("$clazz.DOUBLE(%L)", type.size)
            is ColumnType.BLOB -> CodeBlock.of("$clazz.BLOB(%L)", type.size)
            is ColumnType.JSON -> CodeBlock.of("$clazz.JSON")
            is ColumnType.Custom -> CodeBlock.of("$clazz.Custom(%S)", type.type)
        }
    }

}