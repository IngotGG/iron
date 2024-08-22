import gg.ingot.iron.Iron
import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.strategies.NamingStrategy
import gg.ingot.iron.transformer.ModelTransformer
import kotlin.reflect.jvm.kotlinProperty
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
        val modelTransformer = ModelTransformer(iron.settings, iron.inflector)

        val entity = modelTransformer.transform(User::class)
        assertEquals(User::class, entity.clazz.kotlin)
        assertEquals(4, entity.fields.size)
    }

    @Test
    fun `proper parameter order`() {
        val iron = Iron("jdbc:sqlite::memory:")
        val modelTransformer = ModelTransformer(iron.settings, iron.inflector)

        val entity = modelTransformer.transform(User::class)
        val fields = entity.fields.map { it.field }

        assertEquals(User::id, fields[0].kotlinProperty)
        assertEquals(User::name, fields[1].kotlinProperty)
        assertEquals(User::age, fields[2].kotlinProperty)
        assertEquals(User::firstJoinTime, fields[3].kotlinProperty)
    }

}