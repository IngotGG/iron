package gg.ingot.iron.sql

import gg.ingot.iron.DBMS
import gg.ingot.iron.sql.expressions.filter.gt
import gg.ingot.iron.sql.expressions.filter.inList
import gg.ingot.iron.sql.types.coalesce
import gg.ingot.iron.sql.types.column
import gg.ingot.iron.sql.types.count
import gg.ingot.iron.sql.types.max

object Temp {

    @JvmStatic
    fun main(args: Array<String>) {
        val test1 = Sql.of("SELECT * FROM users")
        println(test1.toString())

        val test2 = Sql(DBMS.MYSQL)
            .select(column("nerd") alias "abc", column("age"))
            .from("users")
        println(test2.toString())

        val test3 = Sql(DBMS.MYSQL)
            .select(max(count(column("id"))) alias "count")
            .from("users")
        println(test3.toString())

        val test4 = Sql(DBMS.MYSQL)
            .select(coalesce(column("nerd"), "abc"))
            .from("users")
            .where((column("age") gt 18) and (column("id") inList listOf(1, 2, 3)))
        println(test4.toString())
    }

}