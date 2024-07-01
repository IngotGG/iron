package gg.ingot.iron.sql.controller

import gg.ingot.iron.representation.ExplodingModel
import gg.ingot.iron.serialization.SerializationAdapter
import gg.ingot.iron.sql.IronResultSet
import gg.ingot.iron.sql.params.SqlParams
import gg.ingot.iron.transformer.ModelTransformer
import gg.ingot.iron.transformer.PlaceholderTransformer
import gg.ingot.iron.transformer.ResultTransformer
import org.slf4j.LoggerFactory
import java.sql.Connection

/**
 * Implementation of the [TransactionController] interface.
 * @since 1.4
 * @author DebitCardz
 */
internal class ControllerImpl(
    private val connection: Connection,
    private val modelTransformer: ModelTransformer,
    private val resultTransformer: ResultTransformer,
    private val serializationAdapter: SerializationAdapter? = null
) : TransactionController {
    override fun <T> transaction(block: TransactionActionableController.() -> T): T {
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
        return IronResultSet(resultSet, serializationAdapter, resultTransformer)
    }

    override fun prepare(statement: String, vararg values: Any?): IronResultSet {
        val preparedStatement = connection.prepareStatement(statement)

        require(preparedStatement.parameterMetaData.parameterCount == values.size) {
            "The number of parameters provided does not match the number of parameters in the prepared statement."
        }

        logger.trace("Preparing Statement\n{}", statement)

        for((index, value) in values.withIndex()) {
            preparedStatement.setObject(
                index + 1,
                PlaceholderTransformer.convert(value, serializationAdapter)
            )
        }

        val resultSet = if (preparedStatement.execute()) {
            preparedStatement.resultSet
        } else {
            null
        }

        return IronResultSet(resultSet, serializationAdapter, resultTransformer)
    }

    override fun prepare(statement: String, model: ExplodingModel): IronResultSet {
        val entity = modelTransformer.transform(model::class)
        return this.prepare(statement, *entity.fields.map {
            modelTransformer.getModelValue(model, it)
        }.toTypedArray())
    }

    override fun prepare(statement: String, model: SqlParams): IronResultSet {
        return prepare(statement, model.build(modelTransformer))
    }

    override fun execute(statement: String): Boolean {
        logger.trace("Executing Statement\n{}", statement)

        return connection.createStatement()
            .execute(statement)
    }

    private companion object {
        /** The logger for this class. */
        private val logger = LoggerFactory.getLogger(ControllerImpl::class.java)
    }
}