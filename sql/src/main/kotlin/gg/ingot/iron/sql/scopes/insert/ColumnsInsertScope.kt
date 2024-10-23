package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `INSERT INTO ...(COLUMNS)` clause.
 * @author santio
 * @since 2.0
 */
interface ColumnsInsertScope: Scope {

    infix fun values(size: Int): ValuesInsertScope
    fun values(vararg values: Any?): ValuesInsertScope

}
