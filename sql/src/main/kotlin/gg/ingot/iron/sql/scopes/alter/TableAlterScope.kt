package gg.ingot.iron.sql.scopes.alter

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.ColumnData
import gg.ingot.iron.sql.types.ExpColumn
import gg.ingot.iron.sql.types.Expression
import java.util.function.Consumer

/**
 * The scope for the `ALTER TABLE` clause.
 * @author santio
 * @since 2.0
 */
interface TableAlterScope: Scope {

    infix fun rename(to: String): RenameTableAlterScope
    infix fun rename(to: SqlTable): RenameTableAlterScope

    infix fun add(column: ColumnData): AddColumnAlterScope
    fun add(name: String? = null, type: String? = null, builder: (ColumnData.() -> Unit) = {}): AddColumnAlterScope

    infix fun remove(column: String): RemoveColumnAlterScope
    infix fun remove(column: Expression): RemoveColumnAlterScope

    fun modify(column: String, builder: ColumnData.() -> Unit): ModifyColumnAlterScope
    fun modify(column: Expression, builder: ColumnData.() -> Unit): ModifyColumnAlterScope
    fun modify(column: String, builder: Consumer<ColumnData>): ModifyColumnAlterScope
    fun modify(column: Expression, builder: Consumer<ColumnData>): ModifyColumnAlterScope

    infix fun drop(constraint: String): DropConstraintAlterScope
    infix fun drop(constraint: Expression): DropConstraintAlterScope

    infix fun primaryKey(column: String): PrimaryKeyAlterScope
    infix fun primaryKey(column: Expression): PrimaryKeyAlterScope

    fun foreignKey(column: String, ref: ExpColumn): ForeignKeyAlterScope
    fun foreignKey(column: Expression, ref: ExpColumn): ForeignKeyAlterScope
}
