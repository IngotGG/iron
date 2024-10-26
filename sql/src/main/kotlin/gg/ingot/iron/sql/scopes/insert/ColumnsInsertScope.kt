package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `INSERT INTO ...(COLUMNS)` clause.
 * @author santio
 * @since 2.0
 */
interface ColumnsInsertScope: Scope {

    fun values(vararg values: Any?): ValuesInsertScope

}
