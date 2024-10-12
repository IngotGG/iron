package gg.ingot.iron.processor.generator

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import gg.ingot.iron.models.SqlTable
import kotlin.reflect.KClass

/**
 * Generates a repository for all models in the project.
 * @author santio
 * @since 2.0
 */
internal object TablesGenerator {

    /**
     * Generates a repository for all models in the project.
     * @param models The models to generate the repository for.
     * @param response The callback to call when the file is made.
     */
    fun generate(models: List<SqlTable>, response: (FileSpec) -> Unit = {}) {
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

        val type = TypeSpec.objectBuilder("Tables")
            .addProperties(tables)
            .addProperty(PropertySpec.builder("ALL", Map::class.asTypeName()
                .parameterizedBy(Class::class.asTypeName().parameterizedBy(STAR), SqlTable::class.asTypeName()))
                .addAnnotation(AnnotationSpec.builder(JvmName::class)
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.GET)
                    .addMember("%S", "ALL")
                    .build())
                .addAnnotation(AnnotationSpec.builder(JvmStatic::class)
                    .build())
                .initializer("mapOf(\n${tableProperties.entries.joinToString(", \n") { "    ${it.key}::class.java to ${it.value}" }}\n)")
                .build())

        val file = FileSpec.builder("gg.ingot.iron.generated", "Tables")
            .addType(type.build())
            .build()

        try {
            response.invoke(file)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * Finds any duplicate table names in the list of tables and throws an error.
     * @param tables The tables to check for duplicates.
     */
    fun findDuplicates(tables: List<SqlTable>) {
        // Check for duplicate table names
        val duplicates = tables.groupBy { it.name }.filter { it.value.size > 1 }
        if (duplicates.isNotEmpty()) {
            var error = "Found duplicate table names: ${duplicates.keys.joinToString(", ")}"

            for (table in duplicates.values.flatten().groupBy { it.name }) {
                error += "\n - ${table.key}"
                for (model in table.value) {
                    error += "\n   └ ${model.clazz}"
                }
            }

            error += "\nPlease rename the tables to be unique."
            error(error)
        }
    }

}