package gg.ingot.iron.sql.scopes.select.partial

import gg.ingot.iron.sql.expressions.ordering.Order
import gg.ingot.iron.sql.scopes.select.OrderBySelectScope
import gg.ingot.iron.sql.types.Column

/**
 * A partial containing the methods for the `ORDER BY` clause.
 * @author santio
 * @since 2.0
 */
interface OrderByPartial {

    fun orderBy(vararg order: Order): OrderBySelectScope
    fun orderBy(vararg columns: String): OrderBySelectScope
    fun orderBy(vararg columns: Column): OrderBySelectScope
    infix fun orderBy(order: List<Order>): OrderBySelectScope
    infix fun orderBy(column: Column): OrderBySelectScope
    infix fun orderBy(column: String): OrderBySelectScope
    infix fun orderBy(order: Order): OrderBySelectScope

}