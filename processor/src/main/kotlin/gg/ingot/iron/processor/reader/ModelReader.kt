package gg.ingot.iron.processor.reader

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.models.SqlTable
import java.security.MessageDigest

/**
 * Reads a model from a class and builds it's required generated classes for it to be generated.
 * @author santio
 * @since 2.0
 */
object ModelReader {

    private val md5 = MessageDigest.getInstance("MD5")
        ?: throw IllegalStateException("Could not create MD5 instance.")

    /**
     * Reads a model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    @OptIn(KspExperimental::class)
    fun read(model: KSClassDeclaration): SqlTable {
        val annotation = model.getAnnotationsByType(Model::class).firstOrNull()
            ?: error("Models must be annotated with @Model, this is an internal error and should be reported.")

        val tableName = annotation.table.takeIf { it.isNotBlank() }
            ?: model.simpleName.asString()

        val columns = ColumnReader.read(model)

        // digest the name and column hashCode() to get a unique hash for the table
        val columnBytes = columns.joinToString("") { column -> column.hash() }.toByteArray()
        val input = tableName.toByteArray() + columnBytes

        val hash = md5.digest(input)
            .joinToString("") { "%02x".format(it) }

        return SqlTable(
            name = tableName,
            columns = columns,
            clazz = model.qualifiedName!!.asString(),
            hash = hash
        )
    }
}