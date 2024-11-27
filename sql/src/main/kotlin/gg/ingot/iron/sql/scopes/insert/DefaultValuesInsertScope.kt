package gg.ingot.iron.sql.scopes.insert

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.partial.ReturningPartial

/**
 * The scope for the `INSERT INTO DEFAULT VALUES` clause.
 * @author santio
 * @since 2.0
 */
interface DefaultValuesInsertScope: Scope, ReturningPartial
