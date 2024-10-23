package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.partial.ReturningPartial
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `INSERT INTO VALUES` clause.
 * @author santio
 * @since 2.0
 */
interface ValuesInsertScope: Scope, ReturningPartial {

    infix fun values(size: Int): ValuesInsertScope
    fun values(vararg values: Any?): ValuesInsertScope
    fun values(vararg values: Expression): ValuesInsertScope

}