package gg.ingot.iron.processor.generator

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import gg.ingot.iron.models.SqlTable

/**
 * Generates a repository for all models in the project.
 * @author santio
 * @since 2.0
 */
object TablesGenerator {

    /**
     * Generates a repository for all models in the project.
     * @param environment The environment to write the file to.
     * @param models The models to generate the repository for.
     */
    fun generate(environment: SymbolProcessorEnvironment, models: List<SqlTable>) {
        val tableProperties = mutableMapOf<String, String>()
        val tables = models.map {
            tableProperties[it.clazz] = it.name.uppercase()

            PropertySpec.builder(tableProperties[it.clazz]!!, SqlTable::class)
                .initializer(
                    "%T(\nname = %S, \nclazz = %S, \ncolumns = listOf(%L), \nhash = %S\r  )",
                    SqlTable::class,
                    it.name,
                    it.clazz,
                    it.columns.joinToString(", ") { column -> ColumnGenerator.generate(column).toString() },
                    it.hash
                )
                .addAnnotation(AnnotationSpec.builder(JvmName::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    .addMember("%S", it.name.uppercase())
                    .build())
                .addAnnotation(AnnotationSpec.builder(JvmStatic::class)
                    .build())
                .build()
        }

        val file = FileSpec.builder("gg.ingot.iron.generated", "Tables")
            .addType(TypeSpec.objectBuilder("Tables")
                .addProperties(tables)
                .addProperty(PropertySpec.builder("ALL", Map::class.asTypeName()
                    .parameterizedBy(Class::class.asTypeName().parameterizedBy(STAR), SqlTable::class.asTypeName()))
                    .addAnnotation(AnnotationSpec.builder(JvmName::class)
                        .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                        .addMember("%S", "ALL")
                        .build())
                    .addAnnotation(AnnotationSpec.builder(JvmStatic::class)
                        .build())
                    .initializer("mapOf(${tableProperties.entries.joinToString { "${it.key}::class.java to ${it.value}" }})")
                    .build())
                .build())
            .build()

        try {
            file.writeTo(environment.codeGenerator, Dependencies.ALL_FILES)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}