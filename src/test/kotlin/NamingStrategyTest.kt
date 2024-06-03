import gg.ingot.iron.strategies.NamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals

class NamingStrategyTest {

    private class TestClass {
        val helloWorldFooBar: String = ""
    }

    @Test
    fun testNamingStrategies() {
        val fieldName = TestClass::helloWorldFooBar.name

        assertEquals("hello_world_foo_bar", NamingStrategy.SNAKE_CASE.transform(fieldName))
        assertEquals("helloWorldFooBar", NamingStrategy.CAMEL_CASE.transform(fieldName))
        assertEquals("hello-world-foo-bar", NamingStrategy.KEBAB_CASE.transform(fieldName))
        assertEquals("HelloWorldFooBar", NamingStrategy.UPPER_CAMEL_CASE.transform(fieldName))
        assertEquals("HELLO_WORLD_FOO_BAR", NamingStrategy.UPPER_SNAKE_CASE.transform(fieldName))
        assertEquals("HELLO-WORLD-FOO-BAR", NamingStrategy.UPPER_KEBAB_CASE.transform(fieldName))
    }

}