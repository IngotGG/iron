package gg.ingot.iron.test.models

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import kotlin.random.Random

@Model
class Application(
    @Column(primaryKey = true)
    val name: String,
    val description: String,
) {
    val clientId: String = Random.nextInt(100).toString(16)
}
