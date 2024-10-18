package gg.ingot.iron.models.bundles

import gg.ingot.iron.annotations.Model
import gg.ingot.iron.models.SqlTable

/**
 * This is a processor class that is used to build [SqlTable] objects.
 * @author santio
 * @since 2.0
 */
internal data class TableBundle(
    /** The name of the table in the database. */
    val name: String,
    /** The class which is annotated with [Model] to represent this table. */
    val clazz: String,
    /** The column bundles which need to be generated for this table. */
    val columns: List<ColumnBundle>,
    /** The hash of the table which changes when any details of the table change. */
    val hash: String,
)
