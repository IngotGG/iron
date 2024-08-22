import gg.ingot.iron.Iron
import gg.ingot.iron.Inflector
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InflectorTest {

    @Test
    fun `test inflection to table names`() = runTest {
        val iron = Iron("jdbc:sqlite::memory:")
        val inflector = Inflector(iron)

        val expected = mapOf(
            "user" to "users",
            "BannedUser" to "banned_users",
            "PersonWithLongName" to "person_with_long_names",
            "Person" to "persons",
            "Hello_World" to "hello_worlds",
        )

        expected.forEach { (input, output) ->
            assertEquals(output, inflector.tableName(input))
        }
    }

}