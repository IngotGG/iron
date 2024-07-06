package gg.ingot.iron.annotations

import gg.ingot.iron.strategies.NamingStrategy

/**
 * Represents a class that is a model for a database entity.
 * @param namingStrategy The naming strategy to use for all columns in the model.
 */
annotation class Model(
    val namingStrategy: NamingStrategy = NamingStrategy.NONE,
)