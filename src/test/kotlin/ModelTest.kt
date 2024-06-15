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
    fun testGenerateEntityModel() {
        val modelTransformer = ModelTransformer(NamingStrategy.SNAKE_CASE)

        val entity = modelTransformer.transform(User::class)
        assertEquals(User::class, entity.clazz)
        assertEquals(4, entity.fields.size)
    }

}