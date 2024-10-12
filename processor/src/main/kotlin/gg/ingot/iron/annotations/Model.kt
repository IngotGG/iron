package gg.ingot.iron.annotations

import gg.ingot.iron.stratergies.NamingStrategy

/**
 * Represents a class that is a model for a database entity.
 *
 * Unlike v1 of Iron, models are iron-agnostic and can be used with any instance of Iron.
 * This means that they are no longer tied to a runtime configuration and are now resolvable
 * at compile time. Because of this change, you can now get all models from the generated
 * Tables global object.
 *
 * Notice: Using the Tables object will show an error for developers who have not yet built
 * the project, that's because the object is not normally committed to the project. Make sure
 * to inform developers that they need to build the project before their IDE will recognize
 * the Tables object.
 *
 * You sometimes may need to add models to sources in gradle, to do that you can specify the directory
 * to look for generated content. (This is mainly an issue when using Java rather than Kotlin)
 *
 * ```kotlin
 * // build.gradle.kts
 * sourceSets {
 *     main {
 *         java {
 *             srcDir("build/generated")
 *         }
 *     }
 * }
 * ```
 *
 * @author santio
 * @since 2.0
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Model(
    /**
     * The name of the table in the database.
     */
    val table: String = "",

    /**
     * The default naming strategy to use for all columns in the model.
     * Use [NamingStrategy.NONE] to disable any automatic column name transformation.
     * We recommend using [NamingStrategy.SNAKE_CASE] for most cases.
     */
    val namingStrategy: NamingStrategy = NamingStrategy.NONE,
)