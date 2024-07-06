import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy
import gg.ingot.iron.transformer.ModelTransformer
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
        val modelTransformer = ModelTransformer(NamingStrategy.SNAKE_CASE)

        val entity = modelTransformer.transform(User::class)
        assertEquals(User::class, entity.clazz)
        assertEquals(4, entity.fields.size)
    }

    @Test
    fun `proper parameter order`() {
        val modelTransformer = ModelTransformer(NamingStrategy.NONE)
        val entity = modelTransformer.transform(User::class)
        val fields = entity.fields.map { it.field }

        assertEquals(User::id, fields[0])
        assertEquals(User::name, fields[1])
        assertEquals(User::age, fields[2])
        assertEquals(User::firstJoinTime, fields[3])
    }

}