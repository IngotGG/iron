package gg.ingot.iron.sql.scopes.drop

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `DELETE` clause.
 * @author santio
 * @since 2.0
 */
interface DropScope: Scope {

    infix fun table(table: String): TableDropScope
    infix fun table(table: SqlTable): TableDropScope

}