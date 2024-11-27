package gg.ingot.iron.sql.expressions.queries

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.Sql
import gg.ingot.iron.sql.scopes.alter.*
import gg.ingot.iron.sql.types.ColumnData
import gg.ingot.iron.sql.types.ExpColumn
import gg.ingot.iron.sql.types.Expression
import gg.ingot.iron.sql.types.column
import java.util.function.Consumer

internal class AlterQuery(private val sql: Sql): Sql(sql.driver, sql.builder),
    AlterScope, TableAlterScope, AddColumnAlterScope, DropConstraintAlterScope, ForeignKeyAlterScope,
    ModifyColumnAlterScope, PrimaryKeyAlterScope, RemoveColumnAlterScope, RenameTableAlterScope {
    override fun first(): AddColumnAlterScope {
        return modify(this) {
            append("FIRST")
        }
    }

    override fun after(column: String): AddColumnAlterScope {
        return after(column(column))
    }

    override fun after(column: Expression): AddColumnAlterScope {
        return modify(this) {
            append("AFTER", column.asString(sql))
        }
    }

    override fun rename(to: String): RenameTableAlterScope {
       return modify(this) {
           if (count() > 2) replace(-1, "${get(-1)},")
           append("RENAME TO", sql.driver.literal(to))
       }
    }

    override fun rename(to: SqlTable): RenameTableAlterScope {
        return rename(to.name)
    }

    override fun add(column: ColumnData): AddColumnAlterScope {
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append("ADD COLUMN", column.create(sql))
        }
    }

    override fun add(name: String?, type: String?, builder: ColumnData.() -> Unit): AddColumnAlterScope {
        val column = ColumnData(name ?: "UNDEFINED", type ?: "UNDEFINED")
        builder(column)
        return add(column)
    }

    override fun remove(column: String): RemoveColumnAlterScope {
        return remove(column(column))
    }

    override fun remove(column: Expression): RemoveColumnAlterScope {
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append("DROP COLUMN", column.asString(sql))
        }
    }

    override fun modify(column: String, builder: ColumnData.() -> Unit): ModifyColumnAlterScope {
        TODO("Not yet implemented")
    }

    override fun modify(column: Expression, builder: ColumnData.() -> Unit): ModifyColumnAlterScope {
        TODO("Not yet implemented")
    }

    override fun modify(column: String, builder: Consumer<ColumnData>): ModifyColumnAlterScope {
        TODO("Not yet implemented")
    }

    override fun modify(column: Expression, builder: Consumer<ColumnData>): ModifyColumnAlterScope {
        TODO("Not yet implemented")
    }

    override fun drop(constraint: String): DropConstraintAlterScope {
        return drop(column(constraint))
    }

    override fun drop(constraint: Expression): DropConstraintAlterScope {
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append("DROP CONSTRAINT", constraint.asString(sql))
        }
    }

    override fun primaryKey(column: String): PrimaryKeyAlterScope {
        return primaryKey(column(column))
    }

    override fun primaryKey(column: Expression): PrimaryKeyAlterScope {
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append("ADD CONSTRAINT", column.asString(sql), "PRIMARY KEY")
        }
    }

    override fun foreignKey(column: String, ref: ExpColumn): ForeignKeyAlterScope {
        return foreignKey(column(column), ref)
    }

    override fun foreignKey(column: Expression, ref: ExpColumn): ForeignKeyAlterScope {
        if (ref.table == null) error("Attempted to add a foreign key but the reference column wasn't given a table")
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append(
                "ADD CONSTRAINT",
                column.asString(sql),
                "FOREIGN KEY",
                "REFERENCES",
                ref.qualified(sql),
            )
        }
    }

    override fun table(table: String): TableAlterScope {
        return modify(this) {
            if (count() > 2) replace(-1, "${get(-1)},")
            append(sql.driver.literal(table))
        }
    }

    override fun table(table: SqlTable): TableAlterScope {
        return table(table.name)
    }
}