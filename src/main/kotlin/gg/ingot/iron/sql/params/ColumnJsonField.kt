package gg.ingot.iron.sql.params

data class ColumnJsonField internal constructor(val value: Any?)

fun jsonField(value: Any?) = ColumnJsonField(value)