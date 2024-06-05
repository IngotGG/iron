<a href="https://ingot.gg/">
    <p align="center">
        <img width="225" height="225" src="https://raw.githubusercontent.com/IngotGG/branding/master/branding.svg" alt="iron"/>
    </p>
</a>

<p align="center">
    <strong>Iron, a simple JDBC Wrapper.</strong>
</p>

--- 

# Iron [![](https://jitpack.io/v/gg.ingot/iron.svg)](https://jitpack.io/#gg.ingot/iron)

Iron is a simple yet powerful JDBC Wrapper used by [ingot.gg](https://ingot.gg) backend services to interact
with our SQL Databases.

Feel free to read the [Contribution Guide](https://github.com/IngotGG/iron/blob/master/CONTRIBUTING.md) to learn how to contribute to Iron or report issues.

## Importing

Tags & Releases can be found on our [Jitpack](https://jitpack.io/#gg.ingot/iron).

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

### [Connection](#single-connection)
```kotlin
data class User(val id: Int, val name: String)

suspend fun main() {
    val connection = Iron("jdbc:sqlite:memory:").connect()

    val user = connection.transaction {
        execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT NOT NULL)")
        execute("INSERT INTO users (name) VALUES ('Ingot')")
        
        query<User>("SELECT * FROM users LIMIT 1;")
            .single()
    }

    println(user)
}
```

### [Pooled Connection](#pooled-connection)
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
### [JSON Deserialization Support](#json-deserde)
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