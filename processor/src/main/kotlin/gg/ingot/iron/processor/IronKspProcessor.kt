package gg.ingot.iron.processor

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ksp.writeTo
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.processor.generator.TablesGenerator
import gg.ingot.iron.processor.reader.ModelReader

internal class IronKspProcessor(private val environment: SymbolProcessorEnvironment): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Model::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: return emptyList()

        val tables = annotated.mapNotNull { ModelReader.read(environment, it) }
        TablesGenerator.findDuplicates(tables)

        // Generate the tables
        TablesGenerator.generate(tables) {
            it.writeTo(environment.codeGenerator, Dependencies.ALL_FILES)
        }

        return annotated.toList()
    }
}