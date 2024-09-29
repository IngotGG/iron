package gg.ingot.iron.test.models

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Data(
    val name: String = Random.nextInt(100).toString(16),
    val age: Int = Random.nextInt(100)
)

@Model
data class JsonHolder(
    @Column(primaryKey = true)
    val id: Int = Math.random().toInt(),
    @Column(json = true)
    val json: Data = Data()
)
