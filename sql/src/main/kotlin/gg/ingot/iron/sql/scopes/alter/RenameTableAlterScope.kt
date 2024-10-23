package gg.ingot.iron.sql.scopes.alter

import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `ALTER TABLE RENAME` clause.
 * @author santio
 * @since 2.0
 */
interface RenameTableAlterScope: Scope, TableAlterScope