package gg.ingot.iron.transformer.adapter

import gg.ingot.iron.Iron

internal object BooleanValueAdapter: ValueAdapter<Boolean>() {
    override fun fromDatabaseValue(value: Any, iron: Iron): Boolean {
        if (value is Boolean) {
            return value
        } else if (value is Int) {
            return value > 0
        } else if (value is String && !iron.settings.strictBooleans) {
            return value.equals("true", true) || value == "1" || value.equals("yes", true)
        }

        error("Expected a boolean, but the database gave back ${value::class.java.name}")
    }

    override fun toDatabaseValue(value: Boolean, iron: Iron): Any {
        return value
    }
}