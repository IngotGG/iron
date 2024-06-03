
import gg.ingot.iron.Iron
import gg.ingot.iron.transformer.ResultTransformer.model
import kotlinx.coroutines.runBlocking
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {

    @Test
    fun testIronUse(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val success = connection.use {
            it.createStatement().execute("SELECT 1 + 1")
        }

        assert(success)
    }

    @Test
    fun testIronQuery(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val result = connection.query("SELECT 1 + 1").getInt(1)
        assertEquals(2, result)
    }

    @Test
    fun testIronTransaction(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
            execute("INSERT INTO test VALUES (1)")
        }

        val result = connection.query("SELECT * FROM test").getInt(1)
        assertEquals(1, result)
    }

    @Test
    fun testIronBrokenTransaction(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        try {
            connection.transaction {
                execute("CREATE TABLE test (id INTEGER PRIMARY KEY)")
                execute("INSERT INTO test VALUES (1)")
                throw IllegalStateException("This is a test")
            }
        } catch (e: IllegalStateException) {
            // expected
        }

        try {
            connection.query("SELECT * FROM test")
        } catch (e: Exception) {
            // expected
            assert(e is SQLException)
        }
    }

    @Test
    fun testIronPrepared(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        val result = connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")

            prepare(
                "INSERT INTO test VALUES (?, ?)",
                1, "test"
            )

            prepare("SELECT * FROM test")
        }

        assertEquals(1, result?.getInt(1))
        assertEquals("test", result?.getString(2))
    }

    private data class TestModel(val id: Int, val name: String)
    private class TestModel2 {
        var id: Int = 0
        var name: String = ""
    }

    @Test
    fun testMapper(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test')")
        }

        val result = connection.query("SELECT * FROM test").model(TestModel::class)
        assertEquals(1, result.id)
        assertEquals("test", result.name)

        val result2 = connection.query("SELECT * FROM test").model(TestModel2::class)
        assertEquals(1, result.id)
        assertEquals("test", result.name)
    }

    @Test
    fun testMapperAll(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test1')")
            execute("INSERT INTO test VALUES (2, 'test2')")
            execute("INSERT INTO test VALUES (3, 'test3')")
            execute("INSERT INTO test VALUES (4, 'test4')")
            execute("INSERT INTO test VALUES (5, 'test5')")
            execute("INSERT INTO test VALUES (6, 'test6')")
        }

        val results = connection.prepare<TestModel>("SELECT * FROM test")
        results.all().forEachIndexed { index, result ->
            assertEquals(index + 1, result.id)
            assertEquals("test${index + 1}", result.name)
        }
    }

    @Test
    fun testMapperNext(): Unit = runBlocking {
        val connection = Iron("jdbc:sqlite::memory:").connect()

        connection.transaction {
            execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
            execute("INSERT INTO test VALUES (1, 'test1')")
            execute("INSERT INTO test VALUES (2, 'test2')")
            execute("INSERT INTO test VALUES (3, 'test3')")
            execute("INSERT INTO test VALUES (4, 'test4')")
            execute("INSERT INTO test VALUES (5, 'test5')")
            execute("INSERT INTO test VALUES (6, 'test6')")
        }

        val result = connection.prepare<TestModel>("SELECT * FROM test").getNext()
        assertEquals(1, result?.id)
        assertEquals("test1", result?.name)
    }

}