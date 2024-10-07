package gg.ingot.iron.models

import com.google.devtools.ksp.symbol.KSTypeReference
import gg.ingot.iron.models.ColumnType.Custom

/**
 * A representation of all common types of columns in a database. Please use [Custom] for specific types.
 * @author santio
 * @since 2.0
 */
sealed class ColumnType {
    /** Represents an integer column, for BIGINT or SMALLINT please use [Custom] instead */
    class INT(val size: Int? = 0): ColumnType()
    /** Represents a VARARG column (or whatever the DBMS uses) */
    class STRING(val size: Int? = 0): ColumnType()
    /** Represents a boolean column */
    data object BOOLEAN : ColumnType()
    /** Represents a float column */
    class FLOAT(val size: Int? = 0): ColumnType()
    /** Represents a double column */
    class DOUBLE(val size: Int? = 0): ColumnType()
    /** Represents a blob column */
    class BLOB(val size: Int? = 0): ColumnType()
    /** Represents a json column */
    data object JSON : ColumnType()
    /** Represents a custom column, where the type specified will be treated as a literal data type in SQL. */
    class Custom(val type: String): ColumnType()

    companion object {
        //todo: change to dbms specific handling
        fun from(type: KSTypeReference): ColumnType {
            return STRING(0)
        }
    }
}