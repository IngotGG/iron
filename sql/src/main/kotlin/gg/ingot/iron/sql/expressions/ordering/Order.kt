package gg.ingot.iron.sql.expressions.ordering

data class Order(
    val name: String,
    val variant: Variant,
) {

    constructor(name: String): this(name, Variant.ASCENDING)

    override fun toString(): String {
        return "$name ${variant.sql}"
    }

    enum class Variant(internal val sql: String) {
        ASCENDING("ASC"),
        DESCENDING("DESC"),
        ;
    }
}

fun asc(name: String): Order {
    return Order(name)
}

fun desc(name: String): Order {
    return Order(name, Order.Variant.DESCENDING)
}