package gg.ingot.iron.annotations

import gg.ingot.iron.strategies.NamingStrategy

annotation class Model(
    val namingStrategy: NamingStrategy = NamingStrategy.SNAKE_CASE
)
