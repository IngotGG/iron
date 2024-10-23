package gg.ingot.iron.sql.scopes.alter

import gg.ingot.iron.sql.scopes.Scope

/**
 * The scope for the `ALTER TABLE ALTER COLUMN` clause.
 * @author santio
 * @since 2.0
 */
interface PrimaryKeyAlterScope: Scope, TableAlterScope
