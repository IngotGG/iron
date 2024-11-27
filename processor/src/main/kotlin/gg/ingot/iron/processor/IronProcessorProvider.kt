package gg.ingot.iron.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * The entrypoint for the Iron processor.
 * @author santio
 * @since 2.0
 */
internal class IronProcessorProvider: SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return IronKspProcessor(environment)
    }
}