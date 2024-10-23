package gg.ingot.iron.sql.scopes.delete

import gg.ingot.iron.sql.scopes.Scope
import gg.ingot.iron.sql.scopes.partial.ReturningPartial

/**
 * The scope for the `DELETE FROM WHERE` clause.
 * @author santio
 * @since 2.0
 */
interface WhereDeleteScope: Scope, ReturningPartial
