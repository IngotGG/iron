package gg.ingot.iron.test.models

data class Metric(
    val kind: Kind,
    val value: Double,
) {
    enum class Kind {
        TEMPERATURE,
        HUMIDITY,
        PRESSURE
    }
}
