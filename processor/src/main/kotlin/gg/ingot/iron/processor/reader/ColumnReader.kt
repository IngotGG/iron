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
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass
import javax.lang.model.element.Modifier as JavaModifier

/**
 * Reads all columns in a model and returns their data.
 * @author santio
 * @since 2.0
 */
@Suppress("DuplicatedCode")
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
                .associateWith { it.getAnnotationsByType(Column::class).firstOrNull() }
                .filter { !shouldIgnore(it.key, it.value) }
                .map { (parameter, annotation) ->
                    val type = parameter.type.resolve()
                    val field = parameter.name!!.asString()

                    val name = annotation?.name?.takeIf { it.isNotBlank() }
                        ?: modelAnnotation?.naming?.transform(field)
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
                        autoIncrement = annotation?.autoIncrement ?: false,
                        json = annotation?.json ?: false,
                        timestamp = annotation?.timestamp
                            ?: (type.toClassName().canonicalName == "java.sql.Timestamp")
                    )
                }
        } else {
            // Get all variables in the class
           model.getAllProperties()
               .associateWith { it.getAnnotationsByType(Column::class).firstOrNull() }
               .filter { !shouldIgnore(it.key, it.value) }
               .map { (property, annotation) ->
                    val type = property.type.resolve()
                    val field = property.simpleName.asString()
                    val name = annotation?.name?.takeIf { it.isNotBlank() }
                        ?: modelAnnotation?.naming?.transform(field)
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
                        autoIncrement = annotation?.autoIncrement ?: false,
                        json = annotation?.json ?: false,
                        timestamp = annotation?.timestamp
                            ?: (type.toClassName().canonicalName == "java.sql.Timestamp")
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
        }
            .associateWith { it.getAnnotationsByType(Column::class.java).firstOrNull() }
            .filter { !shouldIgnore(it.key, it.value) }

        return fields.map { (field, annotation) ->
            val fieldName = field.simpleName.toString()
            val name = annotation?.name?.takeIf { it.isNotBlank() }
                ?: modelAnnotation?.naming?.transform(fieldName)
                ?: fieldName

            val enumTransformation = field.getAnnotationClassValue<Column> { enum }
                ?.asTypeName()
                ?.takeIf { it != EnumTransformation::class.asTypeName() }

            ColumnBundle(
                name = name,
                variable = annotation?.variable?.takeIf { it.isNotBlank() } ?: field.simpleName.toString(),
                field = fieldName,
                clazz = field.asType().asTypeName().toString(),
                enum = enumTransformation?.toString(),
                nullable = annotation?.nullable
                    ?: (field.getAnnotation(Nullable::class.java) != null),
                primaryKey = annotation?.primaryKey ?: false,
                autoIncrement = annotation?.autoIncrement ?: false,
                json = annotation?.json ?: false,
                timestamp = annotation?.timestamp
                    ?: (field.asType().asTypeName().toString() == "java.sql.Timestamp")
            )
        }
    }

    @OptIn(KspExperimental::class)
    private fun shouldIgnore(property: Any, annotation: Column?): Boolean {
        if (annotation != null && annotation.ignore) return true

        return when (property) {
            is KSPropertyDeclaration -> {
                property.modifiers.contains(Modifier.JAVA_TRANSIENT)
                    || property.isAnnotationPresent(Transient::class)
            }

            is KSValueParameter -> {
                property.isAnnotationPresent(Transient::class)
            }

            is VariableElement -> {
                property.modifiers.contains(JavaModifier.TRANSIENT)
                    || property.modifiers.contains(JavaModifier.STATIC)
            }

            else -> true
        }
    }

    /**
     * Gets the value of the annotation class.
     * Reference: https://stackoverflow.com/a/58448607
     * @param f The function to get the value from.
     * @return The value of the annotation class.
     */
    private inline fun <reified T : Annotation> Element.getAnnotationClassValue(
        clazz: Class<T> = T::class.java,
        f: T.() -> KClass<*>
    ): TypeMirror? = try {
        getAnnotation(clazz).f()
        throw Exception("Expected to get a MirroredTypeException")
    } catch (e: MirroredTypeException) {
        e.typeMirror
    } catch (e: NullPointerException) {
        null
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