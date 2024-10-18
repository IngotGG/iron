package gg.ingot.iron.test.models

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.bindings.Bindings
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Data(
    val name: String = Random.nextInt(100000).toString(16),
    val age: Int = Random.nextInt(100)
)

@Model
@Serializable
data class JsonHolder(
    @Column(primaryKey = true)
    val id: Int = Random.nextInt(100),
    @Column(json = true)
    val json: Data = Data()
): Bindings
