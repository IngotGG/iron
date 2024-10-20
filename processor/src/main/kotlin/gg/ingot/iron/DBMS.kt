package gg.ingot.iron

/**
 * Represents a database management system, which is used to determine the driver to load.
 * @param value The value of the DBMS in the JDBC connection string.
 * @param literalChar The escape character to use for defining literal column and table names (ex: "AUTHOR"."TITLE").
 *                    If 2 characters are specified, the first character is the prefix and the second is the suffix.
 * @param className The class name of the driver to load.
 */
enum class DBMS(val value: String, val literalChar: String, private val className: String) {
    SQLITE("sqlite", "\"", "org.sqlite.JDBC"),
    MYSQL("mysql", "`", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("postgresql", "\"", "org.postgresql.Driver"),
    H2("h2", "\"", "org.h2.Driver"),
    HSQLDB("hsqldb", "\"", "org.hsqldb.jdbc.JDBCDriver"),
    DERBY("derby", "\"", "org.apache.derby.jdbc.ClientDriver"),
    MARIADB("mariadb", "`", "org.mariadb.jdbc.Driver"),
    ORACLE("oracle", "\"", "oracle.jdbc.driver.OracleDriver"),
    SQLSERVER("sqlserver", "[]", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    DB2("db2", "\"", "com.ibm.db2.jcc.DB2Driver"),
    SYBASE("sybase", "[]", "com.sybase.jdbc3.jdbc.SybDriver"),
    INFORMIX("informix", "\"", "com.informix.jdbc.IfxDriver"),
    FIREBIRD("firebird", "\"", "org.firebirdsql.jdbc.FBDriver"),
    INTERBASE("interbase", "\"", "interbase.interclient.Driver"),
    UNKNOWN("?", "", "Unknown Driver"),
    ;

    /**
     * Loads the driver for the DBMS, throwing an exception if the driver is not found.
     */
    fun load() {
        try {
            Class.forName(className)
        } catch (e: ClassNotFoundException) {
            error("Failed to load driver for DBMS $name, make sure the driver is on the classpath or added as a dependency.")
        }
    }

    fun literal(name: String): String {
        return when (literalChar.length) {
            0 -> name
            1 -> "$literalChar$name$literalChar"
            2 -> {
                val prefix = literalChar[0]
                val suffix = literalChar[1]

                "$prefix$name$suffix"
            }
            else -> error("Literal character must be 1 or 2 characters, found ${literalChar.length}")
        }
    }

    companion object {
        fun fromValue(value: String): DBMS? {
            return entries.firstOrNull { it.value == value.lowercase() }
        }
    }
}