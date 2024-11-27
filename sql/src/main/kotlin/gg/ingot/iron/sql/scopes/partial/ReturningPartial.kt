package gg.ingot.iron.sql.scopes.partial

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * A partial containing the methods for the `returning` clause.
 * @author santio
 * @since 2.0
 */
interface ReturningPartial {

    fun returning(vararg columns: String): Scope
    fun returning(vararg columns: Expression): Scope
    fun returning(): Scope

}