package gg.ingot.iron.models

import gg.ingot.iron.annotations.Model
import org.slf4j.LoggerFactory
import java.lang.reflect.Field
import kotlin.reflect.KClass

/**
 * Represents a table in the database that has all possible information about it.
 * @author santio
 * @since 2.0
 */
data class SqlTable(
    /** The name of the table in the database. */
    val name: String,
    /** The class which is annotated with [Model] to represent this table. */
    val clazz: String,
    /** The column bundles which need to be generated for this table. */
    val columns: List<SqlColumn>,
    /** The hash of the table which changes when any details of the table change. */
    val hash: String,
) {

    fun clazz(): Class<*> {
        return Class.forName(clazz)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SqlTable::class.java)

        private var tablesObject: Any? = null
        private var mappingsField: Field? = null

        /**
         * Get the table data from a class.
         *
         * Note: This method uses reflection, if you are calling this method, try looking at
         * using the Tables global object instead as that object is generated at compile time.
         *
         * @param clazz The class to get the table data from.
         * @return The table data for the class.
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        @JvmName("get")
        fun get(clazz: Class<*>): SqlTable? {
            // Get the tables global object that was generated at compile time
            return try {
                if (mappingsField == null) {
                    val tables = Class.forName("gg.ingot.iron.generated.Tables")
                    val field = tables.getDeclaredField("ALL")
                    field.isAccessible = true

                    tablesObject = tables.kotlin.objectInstance
                    mappingsField = field
                }

                val mappings = mappingsField!!.get(tablesObject) as Map<Class<*>, SqlTable>
                mappings[clazz]
            } catch (ex: ClassNotFoundException) {
                logger.error("Failed to find Tables class, make sure you have the Iron processor plugin installed.")
                null
            } catch (e: Exception) {
                logger.error("Failed to get model table data, make sure you have the Iron processor plugin installed.", e)
                null
            }
        }

        /**
         * Get the table data from a class.
         *
         * Note: This method uses reflection, if you are calling this method, try looking at
         * using the Tables global object instead as that object is generated at compile time.
         *
         * @param T The class to get the table data from.
         * @return The table data for the class.
         */
        fun KClass<*>.table(): SqlTable? {
            return get(this.java)
        }
    }
}