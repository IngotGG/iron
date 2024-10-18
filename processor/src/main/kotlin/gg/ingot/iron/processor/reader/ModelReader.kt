package gg.ingot.iron.processor.reader

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.models.bundles.TableBundle
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.lang.model.element.TypeElement

/**
 * Reads a model from a class and builds it's required generated classes for it to be generated.
 * @author santio
 * @since 2.0
 */
internal object ModelReader {

    private val logger = LoggerFactory.getLogger(ModelReader::class.java)

    val md5 = MessageDigest.getInstance("MD5")
        ?: throw IllegalStateException("Could not create MD5 instance.")

    /**
     * Reads a kotlin model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    @OptIn(KspExperimental::class)
    fun read(environment: SymbolProcessorEnvironment, model: KSClassDeclaration): TableBundle? {
        val annotation = model.getAnnotationsByType(Model::class).firstOrNull()
            ?: error("Models must be annotated with @Model, this is an internal error and should be reported.")

        val tableName = annotation.table.takeIf { it.isNotBlank() }
            ?: model.simpleName.asString()

        if (tableName.equals("all", true)) {
            error("Table name cannot be 'all', this is a reserved name in Iron, please rename model '${model.qualifiedName?.asString()}'.")
        }

        if (tableName.isBlank()) {
            error("Table name cannot be blank, please specify a table name for model '${model.qualifiedName?.asString()}'.")
        }

        if (tableName.contains(' ')) {
            error("Table name cannot contain spaces, please remove spaces from model '${model.qualifiedName?.asString()}'.")
        }

        val columns = ColumnReader.read(annotation, model)
        if (columns.isEmpty()) {
            environment.logger.warn("No columns found for model '${model.qualifiedName?.asString()}', please specify at least one column, model will be skipped.")
            environment.logger.warn("If you are using a java record, please keep in mind that KSP isn't able to read details and you will need to use a regular class instead.")
            return null
        }

        // digest the name and column hashCode() to get a unique hash for the table
        val columnBytes = columns.joinToString("") { column -> column.hash() }.toByteArray()
        val input = tableName.toByteArray() + columnBytes

        val hash = md5.digest(input)
            .joinToString("") { "%02x".format(it) }

        return TableBundle(
            name = tableName,
            columns = columns,
            clazz = model.qualifiedName!!.asString(),
            hash = hash
        )
    }

    /**
     * Reads a java model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    fun read(model: TypeElement): TableBundle? {
        val annotation = model.getAnnotationsByType(Model::class.java).firstOrNull()
            ?: error("Models must be annotated with @Model, this is an internal error and should be reported.")

        val tableName = annotation.table.takeIf { it.isNotBlank() }
            ?: model.simpleName.toString()

        if (tableName.equals("all", true)) {
            error("Table name cannot be 'all', this is a reserved name in Iron, please rename model '${model.qualifiedName}'.")
        }

        if (tableName.isBlank()) {
            error("Table name cannot be blank, please specify a table name for model '${model.qualifiedName}'.")
        }

        if (tableName.contains(' ')) {
            error("Table name cannot contain spaces, please remove spaces from model '${model.qualifiedName}'.")
        }

        val columns = ColumnReader.read(annotation, model)

        if (columns.isEmpty()) {
            logger.warn("No columns found for model '${model.qualifiedName}', please specify at least one column, model will be skipped.")
            return null
        }

        // digest the name and column hashCode() to get a unique hash for the table
        val columnBytes = columns.joinToString("") { column -> column.hash() }.toByteArray()
        val input = tableName.toByteArray() + columnBytes

        val hash = md5.digest(input)
            .joinToString("") { "%02x".format(it) }

        return TableBundle(
            name = tableName,
            columns = columns,
            clazz = model.qualifiedName.toString(),
            hash = hash
        )
    }
}