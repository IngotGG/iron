package gg.ingot.iron.processor

import gg.ingot.iron.processor.generator.TablesGenerator
import gg.ingot.iron.processor.reader.ModelReader
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.StandardLocation

@SupportedAnnotationTypes("gg.ingot.iron.annotations.Model")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
internal class IronJavaProcessor: AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, env: RoundEnvironment): Boolean {
        val annotation = annotations.firstOrNull()
            ?: return false

        val annotatedElements: Set<Element?> = env.getElementsAnnotatedWith(annotation)
        val classes = annotatedElements.filterIsInstance<TypeElement>()

        val tables = classes.map { ModelReader.read(it) }
        TablesGenerator.findDuplicates(tables)

        // Generate the tables
        TablesGenerator.generate(tables) {
            val file = processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                "main",
                "gg/ingot/iron/generated/Tables.kt"
            )

            file.openWriter().use { writer ->
                writer.write(it.toString())
            }
        }

        return true
    }
}