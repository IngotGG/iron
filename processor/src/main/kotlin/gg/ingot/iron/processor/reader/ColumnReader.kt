package gg.ingot.iron.processor.reader

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.DelicateKotlinPoetApi
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.models.bundles.ColumnBundle
import gg.ingot.iron.strategies.EnumTransformation
import org.jetbrains.annotations.Nullable
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.element.Modifier as JavaModifier

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
    fun read(modelAnnotation: Model?, model: KSClassDeclaration): List<ColumnBundle> {
        if (model.classKind != ClassKind.CLASS) {
            error("Models must be classes, please make sure you unmark '${model.simpleName}' as a model or make it a class.")
        }

        // Based on the kind of model, we'll pull the columns from different sources
        val columns: List<ColumnBundle> = if (model.modifiers.contains(Modifier.DATA)) {
            val constructor = model.primaryConstructor
                ?: error("Data classes must have a primary constructor, please give '${model.simpleName}' a primary constructor.")

            constructor.parameters
                .filter { !shouldIgnore(it, it.getAnnotationsByType(Column::class).firstOrNull()) }
                .map { parameter ->
                    val annotation = parameter.getAnnotationsByType(Column::class).firstOrNull()
                    val type = parameter.type.resolve()
                    val field = parameter.name!!.asString()
                    val name = annotation?.name?.takeIf { it.isNotBlank() }
                        ?: modelAnnotation?.namingStrategy?.transform(field)
                        ?: field

                    val ksAnnotation = getKSAnnotation(Column::class.java, parameter)
                    val enumTransformationType = ksAnnotation?.getKClass("enum")
                    val enumTransformation = enumTransformationType?.declaration
                        ?.qualifiedName?.asString()

                    return@map ColumnBundle(
                        name = name,
                        variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: parameter.name!!.asString(),
                        clazz = type.toClassName().canonicalName,
                        field = field,
                        enum = enumTransformation,
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
                .filter { !shouldIgnore(it, it.getAnnotationsByType(Column::class).firstOrNull()) }
                .map { property ->
                    val annotation = property.getAnnotationsByType(Column::class).firstOrNull()
                    val type = property.type.resolve()
                    val field = property.simpleName.asString()
                    val name = annotation?.name?.takeIf { it.isNotBlank() }
                        ?: modelAnnotation?.namingStrategy?.transform(field)
                        ?: field

                    val ksAnnotation = getKSAnnotation(Column::class.java, property)
                    val enumTransformationType = ksAnnotation?.getKClass("enum")
                    val enumTransformation = enumTransformationType?.declaration
                        ?.qualifiedName?.asString()

                    return@map ColumnBundle(
                        name = name,
                        variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: property.simpleName.asString(),
                        field = field,
                        clazz = type.toClassName().canonicalName,
                        enum = enumTransformation,
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

    /**
     * Reads a java model from a class and builds it's required generated classes for it to be generated.
     * @param model The class to read the model from.
     * @return The table representation of the model.
     */
    @OptIn(DelicateKotlinPoetApi::class)
    fun read(modelAnnotation: Model?, model: TypeElement): List<ColumnBundle> {
        val fields = if (model.kind == ElementKind.RECORD) {
            model.enclosedElements
                .filterIsInstance<VariableElement>()
                .distinctBy { it.simpleName } // Records can have duplicate fields, so we need to filter them out
        } else {
            model.enclosedElements.filterIsInstance<VariableElement>()
        }.filter { !shouldIgnore(it, it.getAnnotationsByType(Column::class.java).firstOrNull()) }

        return fields.map { field ->
            val annotation = field.getAnnotationsByType(Column::class.java).firstOrNull()
            val fieldName = field.simpleName.toString()
            val name = annotation?.name?.takeIf { it.isNotBlank() }
                ?: modelAnnotation?.namingStrategy?.transform(fieldName)
                ?: fieldName

            val enumTransformation = annotation?.enum?.takeIf { it != EnumTransformation::class }

            ColumnBundle(
                name = name,
                variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: field.simpleName.toString(),
                field = fieldName,
                clazz = field.asType().asTypeName().toString(),
                enum = enumTransformation?.qualifiedName,
                nullable = annotation?.nullable
                    ?: (field.getAnnotation(Nullable::class.java) != null),
                primaryKey = annotation?.primaryKey ?: false,
                autoIncrement = annotation?.autoIncrement ?: false
            )
        }
    }

    @OptIn(KspExperimental::class)
    private fun shouldIgnore(property: Any, annotation: Column?): Boolean {
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

            is VariableElement -> {
                (annotation != null && annotation.ignore)
                    || property.modifiers.contains(JavaModifier.TRANSIENT)
                    || property.modifiers.contains(JavaModifier.STATIC)
            }

            else -> true
        }
    }

    private fun getKSAnnotation(type: Class<out Annotation>,annotated: KSAnnotated): KSAnnotation? {
        return annotated.annotations
            .firstOrNull {
                it.annotationType.resolve().declaration.qualifiedName?.asString() == type.name
            }
    }

    private fun KSAnnotation.getKClass(field: String): KSType? {
        return this.arguments
            .firstOrNull { it.name?.asString() == field }
            ?.value as KSType?
    }

}