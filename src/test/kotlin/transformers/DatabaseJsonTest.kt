package transformers
import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.annotations.Model
import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.params.jsonField
import gg.ingot.iron.strategies.NamingStrategy
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseJsonTest {
    private val connection = Iron("jdbc:sqlite::memory:", IronSettings(
        namingStrategy = NamingStrategy.CAMEL_CASE,
        serialization = SerializationAdapter.Gson(Gson())
    )).connect()

    @Model
    data class User(
        val id: Int,
        val metadata: UserMetadata
    ): ExplodingModel

    @Serializable
    data class UserMetadata(
        val name: String,
        val age: Int
    )

    @Test
    fun `retrieve as json`() = runTest {
        val metadata = connection.transaction {
            prepare(
                "CREATE TABLE user (id INTEGER PRIMARY KEY, metadata TEXT)"
            )

            repeat (10) {
                prepare(
                    "INSERT INTO user VALUES (?, ?)",
                    it, jsonField(UserMetadata("a", it))
                )
            }

            prepare("SELECT metadata FROM user")

        }

        assertEquals(10, metadata.allJson<UserMetadata>().size)
    }

}