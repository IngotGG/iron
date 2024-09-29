package gg.ingot.iron.test.models

import gg.ingot.iron.annotations.Column
import gg.ingot.iron.annotations.Model

@Model
data class User(
    @Column(primaryKey = true)
    val id: Int,
    val name: String,
    val age: Int,
    val email: String = "test@example.com",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    @Column(ignore = true)
    val isOnline: Boolean = false,
    @Column(json = true)
    val metadata: Map<String, Any?> = mapOf(),
) {
    companion object {
        val tableDefinition = """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY,
                name TEXT NOT NULL,
                age INTEGER NOT NULL,
                email TEXT NOT NULL DEFAULT 'test@example.com',
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                metadata TEXT NOT NULL DEFAULT '{}'
            );
        """.trimIndent()
    }
}