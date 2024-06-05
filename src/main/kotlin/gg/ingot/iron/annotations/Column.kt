package gg.ingot.iron.annotations

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Column(
    val name: String = "",
    val ignore: Boolean = false,
    val json: Boolean = false
)
