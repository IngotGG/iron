package gg.ingot.iron.test

import com.google.gson.Gson
import gg.ingot.iron.Iron
import gg.ingot.iron.IronSettings
import gg.ingot.iron.stratergies.NamingStrategy
import gg.ingot.iron.serialization.SerializationAdapter
import io.kotest.core.test.TestScope
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.time.Duration.Companion.seconds


/**
 * Core utilities for testing the Iron library.
 */
object IronTest {

    private val baseSettings = IronSettings().apply {
        namingStrategy = NamingStrategy.SNAKE_CASE
        connectionPollTimeout = 3.seconds
    }

    /**
     * Build a new in-memory SQLite database.
     * @return The iron instance.
     */
    fun sqlite(settings: IronSettings = baseSettings): Iron {
        return Iron("jdbc:sqlite::memory:", settings).connect()
    }

    /**
     * Build a new in-memory H2 database.
     * @return The iron instance.
     */
    fun h2(settings: IronSettings = baseSettings): Iron {
        return Iron("jdbc:h2:mem:test", settings).connect()
    }

    /**
     * Build a new postgres database with TestContainers.
     * @return The iron instance.
     */
    fun postgres(settings: IronSettings = baseSettings): Iron {
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
        postgres.start()

        return Iron("jdbc:tc:postgresql:9.6.8:///iron", settings).onClose {
            postgres.stop()
        }.connect()
    }

    /**
     * The logger for the current test case.
     */
    val TestScope.logger: Logger
        get() = run {
            val name = if (testCase.parent != null) {
                "${testCase.parent!!.name.testName}@${testCase.name.testName}"
            } else testCase.name.testName

            LoggerFactory.getLogger(name)
        }

    /**
     * Build a pooled Iron setting configuration.
     * @return The iron settings required for a pooled iron instance.
     */
    fun pooled(): IronSettings {
        return IronSettings().apply {
            maximumConnections = 3
            namingStrategy = NamingStrategy.SNAKE_CASE
            connectionPollTimeout = 3.seconds
        }
    }

    /**
     * Build a setting configuration with json serialization enabled.
     * @return The iron settings required for a json iron instance.
     */
    fun json(): IronSettings {
        return IronSettings().apply {
            serialization = SerializationAdapter.Gson(Gson())
            namingStrategy = NamingStrategy.SNAKE_CASE
            connectionPollTimeout = 3.seconds
        }
    }
}