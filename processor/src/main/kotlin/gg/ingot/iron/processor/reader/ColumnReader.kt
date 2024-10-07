package gg.ingot.iron.processor.reader

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.models.ColumnType
import gg.ingot.iron.models.SqlColumn

/**
 * Reads all columns in a model and returns their data.
 * @author santio
 * @since 2.0
 */
object ColumnReader {

    /**
     * Reads a model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    fun read(model: KSClassDeclaration): List<SqlColumn> {
        if (model.classKind != ClassKind.CLASS) {
            error("Models must be classes, please make sure you unmark '${model.simpleName}' as a model or make it a class.")
        }

        // Based on the kind of model, we'll pull the columns from different sources
        if (model.modifiers.contains(Modifier.DATA)) {
            val constructor = model.primaryConstructor
                ?: error("Data classes must have a primary constructor, please give '${model.simpleName}' a primary constructor.")

            return constructor.parameters
                .filter { !shouldIgnore(it) }
                .map { SqlColumn(
                    name = it.name!!.asString(),
                    variable = it.name!!.asString(),
                    type = ColumnType.from(it.type),
                    nullable = false, // TODO: add support for nullability
                    primaryKey = false,
                    autoIncrement = false,
                    defaultValue = null
                ) }
        } else {
            // Get all variables in the class
            val properties = model.getAllProperties()
                .filter { !shouldIgnore(it) }

            return emptyList()
        }
    }

    @OptIn(KspExperimental::class)
    private fun shouldIgnore(property: KSAnnotated): Boolean {
        val annotation = property.getAnnotationsByType(Column::class).firstOrNull()

        return when (property) {
            is KSPropertyDeclaration -> {
                property.modifiers.contains(Modifier.JAVA_TRANSIENT)
                    || property.hasBackingField
                    || (annotation != null && annotation.ignore)
                    || property.isAnnotationPresent(Transient::class)
            }

            is KSValueParameter -> {
                (annotation != null && annotation.ignore)
                    || property.isAnnotationPresent(Transient::class)
            }

            else -> true
        }
    }
}