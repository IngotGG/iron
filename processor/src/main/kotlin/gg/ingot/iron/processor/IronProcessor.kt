package gg.ingot.iron.processor

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.processor.generator.TablesGenerator
import gg.ingot.iron.processor.reader.ModelReader

class IronProcessor(private val environment: SymbolProcessorEnvironment): SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val annotated = resolver.getSymbolsWithAnnotation(Model::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .also { println(it) }
            .toList()
            .takeIf { it.isNotEmpty() }
            ?: return emptyList()

        val tables = annotated.map { ModelReader.read(it) }
        TablesGenerator.generate(environment, tables)

        return annotated.toList()
    }
}