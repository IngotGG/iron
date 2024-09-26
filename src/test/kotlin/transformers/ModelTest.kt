package transformers

import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.model.ModelReader
import gg.ingot.iron.strategies.NamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class ModelTest {

    @Model(namingStrategy = NamingStrategy.KEBAB_CASE)
    private data class User(
        @Column(name = "_id")
        val id: Int = 0,
        val name: String = "",
        val age: Int = 0,
        @Column(name = "first_jointime")
        val firstJoinTime: Int = 0,
        @Column(ignore = true)
        var ignored: String = ""
    )

    @Test
    fun `generate entity model`() {
        val iron = Iron("jdbc:sqlite::memory:")
        val modelReader = ModelReader(iron)

        val entity = modelReader.read(User::class)
        assertEquals(User::class, entity.clazz.kotlin)
        assertEquals(4, entity.fields.size)
    }

    @Test
    fun `proper parameter order`() {
        val iron = Iron("jdbc:sqlite::memory:")
        val modelReader = ModelReader(iron)

        val entity = modelReader.read(User::class)
        val fields = entity.fields.map { it.field }

        assertEquals(User::id, fields[0].kotlin)
        assertEquals(User::name, fields[1].kotlin)
        assertEquals(User::age, fields[2].kotlin)
        assertEquals(User::firstJoinTime, fields[3].kotlin)
    }

}