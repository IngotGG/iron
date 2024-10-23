package gg.ingot.iron.sql.scopes.partial

import gg.ingot.iron.DBMS.MYSQL
import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * A partial containing the methods for the `returning` clause.
 * @author santio
 * @since 2.0
 */
interface ReturningPartial {

    /**
     * Non-supported databases: [MYSQL]
     */
    fun returning(vararg columns: String): Scope

    /**
     * Non-supported databases: [MYSQL]
     */
    fun returning(vararg columns: Expression): Scope

    /**
     * Non-supported databases: [MYSQL]
     */
    fun returning(): Scope

}