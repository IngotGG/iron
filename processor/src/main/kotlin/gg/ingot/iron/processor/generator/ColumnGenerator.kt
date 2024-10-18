package gg.ingot.iron.processor.generator

import com.squareup.kotlinpoet.CodeBlock
import gg.ingot.iron.models.SqlColumn
import gg.ingot.iron.models.bundles.ColumnBundle

/**
 * Generates kotlinpoet code for columns in a table.
 * @author santio
 * @since 2.0
 */
internal object ColumnGenerator {

    /**
     * Generates kotlinpoet code for a column in a table with its static information.
     * @param column The column to generate the code for.
     * @return The property spec for the column.
     */
    fun generate(column: ColumnBundle): CodeBlock {
        return CodeBlock.builder()
            .add("%T(\n", SqlColumn::class)
            .add("  name = %S,\n", column.name)
            .add("  variable = %S,\n", column.variable)
            .add("  field = %S,\n", column.field)
            .add("  clazz = %S,\n", column.boxedClass())
            .add("  nullable = %L,\n", column.nullable)
            .add("  primaryKey = %L,\n", column.primaryKey)
            .add("  autoIncrement = %L,\n", column.autoIncrement)
            .apply {
                // Nullable options
                if (column.enum != null) add("  enum = %L::class.java,\n", column.enum)
                if (column.json)         add("  json = true,\n")
                if (column.timestamp)    add("  timestamp = true,\n")
            }
            .add("  hash = %S\n", column.hash())
            .add(")")
            .build()
    }

}