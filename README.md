<a href="https://ingot.gg/">
    <p align="center">
        <img width="325" height="325" src="https://raw.githubusercontent.com/IngotGG/branding/master/branding.svg" alt="iron"/>
    </p>
</a>

<p align="center">
    <strong>Iron, a simple JDBC Wrapper.</strong>
</p>

--- 

# Iron [![](https://jitpack.io/v/gg.ingot/iron.svg)](https://jitpack.io/#gg.ingot/iron)

Iron is a simple yet powerful JDBC Wrapper used by [ingot.gg](https://ingot.gg) backend services to interact
with our SQL Databases.

## Importing

### Gradle

```kts
repositories {
  maven("https://jitpack.io")
}

dependencies {
    implementation("gg.ingot:iron:TAG")
}
```
### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>gg.ingot</groupId>
    <artifactId>iron</artifactId>
    <version>TAG</version>
</dependency>

```

## Features
* Simple Connection Pooling
* Automatic Model Deserialization
* Built-in JSON Field Deserialization
* Kotlin Coroutines Support

## Basic Usage

### Connection
```kotlin
data class User(val id: Int, val name: String)

suspend fun main() {
    val connection = Iron("jdbc:sqlite:memory:").connect()

    val user = connection.transaction {
        execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")

        query<User>("SELECT * FROM users LIMIT 1;")
            .single()
    }

    println(user)
}
```

### Pooled Connection
```kotlin
suspend fun main() {
    val connection = Iron(
        "jdbc:postgresql://localhost:5432/postgres",
        /** Increasing maximum connections automatically makes it pooled. */
        IronSettings(
            minimumActiveConnections = 2,
            maximumConnections = 8
        )
    ).connect()

    // Pooled connections are identical to single connections.
    val added = connection.query("SELECT 1+1;")
        .getInt(1)
    println(added)
}
```
### JSON Deserialization Support
```kotlin
data class Example(val field: String)
data class ExampleModel(
    @Column(json = true)
    val example: Example
)

suspend fun main() {
    val connection = Iron(
        "jdbc:sqlite:memory:",
        IronSettings(
            /** Built-in GSON & Kotlinx Serialization support. */
            serialization = SerializationAdapter.Gson(Gson())
        )
    ).connect()

    val model = connection.query<ExampleModel>("SELECT example FROM table LIMIT 1;")
        .singleNullable()
    println(model?.example)
}
```