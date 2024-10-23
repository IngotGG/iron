package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.types.Expression

/**
 * The scope for the `INSERT INTO` clause.
 * @author santio
 * @since 2.0
 */
interface IntoInsertScope: Scope {

    fun columns(vararg columns: String): ColumnsInsertScope
    fun columns(vararg columns: Expression): ColumnsInsertScope

    fun defaultValues(): DefaultValuesInsertScope

    infix fun values(size: Int): ValuesInsertScope
    fun values(vararg values: Any?): ValuesInsertScope
    fun values(vararg values: Expression): ValuesInsertScope

}
