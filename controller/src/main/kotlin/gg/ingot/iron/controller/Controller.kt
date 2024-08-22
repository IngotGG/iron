package gg.ingot.iron.controller

/**
 * Marks a model as a controller allowing for Iron to handle basic crud operations on
 * it. Use [Iron#controller] to grab the controller for this model.
 */
annotation class Controller(
    val table: String = ""
)