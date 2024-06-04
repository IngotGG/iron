package gg.ingot.iron.representation

/**
 * Represents a database management system, which is used to determine the driver to load.
 * @param value The value of the DBMS in the JDBC connection string.
 * @param className The class name of the driver to load.
 */
enum class DBMS(val value: String, val className: String) {
    SQLITE("sqlite", "org.sqlite.JDBC"),
    MYSQL("mysql", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("postgresql", "org.postgresql.Driver"),
    H2("h2", "org.h2.Driver"),
    HSQLDB("hsqldb", "org.hsqldb.jdbc.JDBCDriver"),
    DERBY("derby", "org.apache.derby.jdbc.EmbeddedDriver"),
    MARIADB("mariadb", "org.mariadb.jdbc.Driver"),
    ORACLE("oracle", "oracle.jdbc.OracleDriver"),
    SQLSERVER("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    DB2("db2", "com.ibm.db2.jcc.DB2Driver"),
    SYBASE("sybase", "com.sybase.jdbc4.jdbc.SybDriver"),
    INFORMIX("informix", "com.informix.jdbc.IfxDriver"),
    FIREBIRD("firebird", "org.firebirdsql.jdbc.FBDriver"),
    INTERBASE("interbase", "interbase.interclient.Driver"),
    ;

    fun load() {
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            throw IllegalStateException("Failed to load driver for DBMS $name, make sure the driver is on the classpath or added as a dependency.")
        }
    }

    companion object {
        fun fromValue(value: String): DBMS? {
            return entries.firstOrNull { it.value == value.lowercase() }
        }
    }
}

/**
 * Represents a database driver, which is an alias for the DBMS.
 */
typealias DatabaseDriver = DBMS