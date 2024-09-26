package gg.ingot.iron.transformer.models

import java.lang.reflect.Constructor

internal data class ConstructorDetails(
    val constructor: Constructor<*>,
    val setParameters: Boolean,
)