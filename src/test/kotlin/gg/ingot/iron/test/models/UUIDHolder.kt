package gg.ingot.iron.test.models

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.generated.Tables
import gg.ingot.iron.serialization.ColumnAdapter
import java.util.*

@Model
data class UUIDHolder(
    @Column(adapter = UUIDAdapter::class)
    val id: UUID = UUID.randomUUID(),
    val value: Double,
)

object UUIDAdapter: ColumnAdapter<String, UUID> {
    override fun toDatabaseValue(value: UUID): String {
        Tables
        return value.toString()
    }

    override fun fromDatabaseValue(value: String): UUID {
        return UUID.fromString(value)
    }
}
