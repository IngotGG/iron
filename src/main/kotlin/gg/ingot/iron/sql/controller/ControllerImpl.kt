package gg.ingot.iron.sql.controller

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.ColumnSerializer
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.ColumnJsonField
import gg.ingot.iron.sql.params.ColumnSerializedField
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.transformer.ModelTransformer
import gg.ingot.iron.transformer.ResultTransformer
import gg.ingot.iron.transformer.isEnum
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * Controller implementation for handling database transactions and queries.
 * @author DebitCardz
 * @since 1.3
 */
internal open class ControllerImpl(
    private val connection: Connection,
    private val modelTransformer: ModelTransformer,
    private val resultTransformer: ResultTransformer,
    private val serializationAdapter: SerializationAdapter? = null
) : Controller {
    private val logger = LoggerFactory.getLogger(ControllerImpl::class.java)

    override fun <T> transaction(block: TransactionController.() -> T): T {
        val transactionController = TransactionControllerImpl(this)

        return try {
            connection.autoCommit = false

            val result = block(transactionController)

            connection.commit()
            transactionController.commit()

            result
        } catch(ex: Exception) {
            connection.rollback()
            transactionController.rollback()

            throw ex
        } finally {
            connection.autoCommit = true
        }
    }

    override fun query(query: String): IronResultSet {
        logger.trace("Executing Query\n{}", query)

        val resultSet = connection.createStatement()
            .executeQuery(query)

        return IronResultSet(resultSet, resultTransformer)
    }

    override fun execute(statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return connection.createStatement()
            .execute(statement)
    }

    @Suppress("UNCHECKED_CAST")
    override fun prepare(statement: String, vararg values: Any?): IronResultSet {
        val preparedStatement = connection.prepareStatement(statement)

        require(preparedStatement.parameterMetaData.parameterCount == values.size) {
            "The number of parameters provided does not match the number of parameters in the prepared statement."
        }

        logger.trace("Preparing Statement\n{}", statement)

        for ((index, value) in values.withIndex()) {
            val paramIndex = index + 1

            if(value == null) {
                logger.trace("Setting parameter {} to null.", paramIndex)
                preparedStatement.setObject(paramIndex, null)
                continue
            }

            val kClass = value::class

            if(value is ColumnSerializedField) {
                logger.trace("Deserializing parameter {} as a serialized field {}.", paramIndex, value.serializer::class.simpleName)

                val innerValue = value.value
                if(innerValue == null) {
                    preparedStatement.setObject(paramIndex, null)
                    continue
                }
                value.serializer as ColumnSerializer<Any, *>

                preparedStatement.setObject(paramIndex, value.serializer.toDatabaseValue(innerValue))
                continue
            } else if(value is ColumnJsonField) {
                requireNotNull(serializationAdapter) { "A serialization adapter must be provided to serialize JSON values." }

                logger.trace("Deserializing parameter {} as a JSON field.", paramIndex)

                val innerJson = value.value
                if(innerJson == null) {
                    preparedStatement.setObject(paramIndex, null)
                    continue
                }

                preparedStatement.setObject(paramIndex, serializationAdapter.serialize(innerJson, value.value::class.java))
                continue
            }

            // parse enum values to db
            if(isEnum(kClass)) {
                logger.trace("Deserializing parameter {} as an enum.", paramIndex)

                preparedStatement.setObject(paramIndex, (value as Enum<*>).name)
                continue
            }

            logger.trace("Setting parameter {} to value {}", paramIndex, value)
            preparedStatement.setObject(paramIndex, value)
        }

        val resultSet = if (preparedStatement.execute()) {
            preparedStatement.resultSet
        } else {
            null
        }

        return IronResultSet(resultSet, resultTransformer)
    }

    override fun prepare(statement: String, model: ExplodingModel): IronResultSet {
        val entity = modelTransformer.transform(model::class)
        return this.prepare(statement, *entity.fields.map {
            modelTransformer.getModelValue(model, it)
        }.toTypedArray())
    }

    override fun prepare(statement: String, model: SqlParams): IronResultSet {
        return this.prepare(statement, *model.build(this.modelTransformer))
    }

    private companion object {
        val VARIABLE_REGEX = Regex(""":(\w+)""")
    }
}