package gg.ingot.iron.sql.scopes.alter

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `ALTER` clause.
 * @author santio
 * @since 2.0
 */
interface AlterScope: Scope {

    infix fun table(table: String): TableAlterScope
    infix fun table(table: SqlTable): TableAlterScope

}