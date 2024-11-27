package gg.ingot.iron.sql.scopes.alter

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `ALTER TABLE ADD COLUMN` clause.
 * @author santio
 * @since 2.0
 */
interface AddColumnAlterScope: Scope, TableAlterScope {

    fun first(): AddColumnAlterScope
    infix fun after(column: String): AddColumnAlterScope
    infix fun after(column: Expression): AddColumnAlterScope

}
