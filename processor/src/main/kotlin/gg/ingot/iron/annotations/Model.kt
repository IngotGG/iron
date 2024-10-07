package gg.ingot.iron.annotations

import gg.ingot.iron.stratergies.NamingStrategy

/**
 * Represents a class that is a model for a database entity.
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