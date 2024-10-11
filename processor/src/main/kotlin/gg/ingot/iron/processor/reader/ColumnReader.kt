package gg.ingot.iron.processor.reader

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.models.SqlColumn
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement

/**
 * Reads all columns in a model and returns their data.
 * @author santio
 * @since 2.0
 */
internal object ColumnReader {

    /**
     * Reads a kotlin model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    @OptIn(KspExperimental::class)
    fun read(model: KSClassDeclaration): List<SqlColumn> {
        if (model.classKind != ClassKind.CLASS) {
            error("Models must be classes, please make sure you unmark '${model.simpleName}' as a model or make it a class.")
        }

        // Based on the kind of model, we'll pull the columns from different sources
        val columns: List<SqlColumn> = if (model.modifiers.contains(Modifier.DATA)) {
            val constructor = model.primaryConstructor
                ?: error("Data classes must have a primary constructor, please give '${model.simpleName}' a primary constructor.")

            constructor.parameters
                .filter { !shouldIgnore(it) }
                .map { parameter ->
                    val annotation = parameter.getAnnotationsByType(Column::class).firstOrNull()
                    val type = parameter.type.resolve()

                    return@map SqlColumn(
                        name = annotation?.name?.takeIf { it.isNotBlank() } ?: parameter.name!!.asString(),
                        variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: parameter.name!!.asString(),
                        clazz = type.toClassName().canonicalName,
                        nullable = annotation?.nullable
                            ?: type.isMarkedNullable.takeIf { it }
                            ?: parameter.isAnnotationPresent(Nullable::class),
                        primaryKey = annotation?.primaryKey ?: false,
                        autoIncrement = annotation?.autoIncrement ?: false
                    )
                }
        } else {
            // Get all variables in the class
           model.getAllProperties()
                .filter { !shouldIgnore(it) }
                .map { property ->
                    val annotation = property.getAnnotationsByType(Column::class).firstOrNull()
                    val type = property.type.resolve()

                    return@map SqlColumn(
                        name = annotation?.name?.takeIf { it.isNotBlank() } ?: property.simpleName.asString(),
                        variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: property.simpleName.asString(),
                        clazz = type.toClassName().canonicalName,
                        nullable = annotation?.nullable
                            ?: type.isMarkedNullable.takeIf { it }
                            ?: property.isAnnotationPresent(Nullable::class),
                        primaryKey = annotation?.primaryKey ?: false,
                        autoIncrement = annotation?.autoIncrement ?: false
                    )
                }.toList()
        }

        return columns
    }

    @OptIn(KspExperimental::class)
    private fun shouldIgnore(property: KSAnnotated): Boolean {
        val annotation = property.getAnnotationsByType(Column::class).firstOrNull()

        return when (property) {
            is KSPropertyDeclaration -> {
                property.modifiers.contains(Modifier.JAVA_TRANSIENT)
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

    /**
     * Reads a java model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    @OptIn(DelicateKotlinPoetApi::class)
    fun read(model: TypeElement): List<SqlColumn> {
        val fields = if (model.kind == ElementKind.RECORD) {
            model.enclosedElements
                .filterIsInstance<VariableElement>()
                .distinctBy { it.simpleName } // Records can have duplicate fields, so we need to filter them out
        } else {
            model.enclosedElements.filterIsInstance<VariableElement>()
        }

        return fields.map { field ->
            val annotation = field.getAnnotationsByType(Column::class.java).firstOrNull()

            SqlColumn(
                name = annotation?.name?.takeIf { it.isNotBlank() } ?: field.simpleName.toString(),
                variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: field.simpleName.toString(),
                clazz = field.asType().asTypeName().toString(),
                nullable = annotation?.nullable
                    ?: (field.getAnnotation(Nullable::class.java) != null),
                primaryKey = annotation?.primaryKey ?: false,
                autoIncrement = annotation?.autoIncrement ?: false
            )
        }
    }

}