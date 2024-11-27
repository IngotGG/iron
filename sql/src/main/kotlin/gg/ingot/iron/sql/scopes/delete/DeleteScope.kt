package gg.ingot.iron.sql.scopes.delete

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `DELETE` clause.
 * @author santio
 * @since 2.0
 */
interface DeleteScope: Scope {

    infix fun from(table: String): FromDeleteScope
    infix fun from(table: SqlTable): FromDeleteScope

}