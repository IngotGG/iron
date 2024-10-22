package gg.ingot.iron.sql.scopes.select

import gg.ingot.iron.models.SqlTable
import gg.ingot.iron.sql.expressions.Entrypoint
import gg.ingot.iron.sql.scopes.Scope
import java.util.function.Consumer

/**
 * The scope for the `SELECT DISTINCT` clause.
 * @author santio
 * @since 2.0
 */
interface DistinctSelectScope: Scope {

    infix fun from(table: String): FromSelectScope
    infix fun from(table: SqlTable): FromSelectScope
    infix fun from(subquery: Consumer<Entrypoint>): FromSelectScope

}