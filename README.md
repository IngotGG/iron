<a href="https://ingot.gg/">
    <p align="center">
        <img width="225" height="225" src="https://raw.githubusercontent.com/IngotGG/branding/master/branding.svg" alt="iron"/>
    </p>
</a>

<p align="center">
    <strong>Iron, a simple JDBC Wrapper.</strong>
</p>

--- 

# Iron [![](https://jitpack.io/v/gg.ingot/iron.svg)](https://jitpack.io/#gg.ingot/iron) [![](https://jitci.com/gh/IngotGG/iron/svg)](https://jitci.com/gh/IngotGG/iron)
 
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
* [Simple Connection Pooling](#pooled-connection)
* [Query Model Mapping](#query-model-mapping)
* [Built-in JSON Field Deserialization](#json-deserialization-support)
* Kotlin Coroutines Support

## Basic Usage

### Connection
```kotlin
data class User(val id: Int, val firstName: String, val lastName: String)

suspend fun main() {
    val connection = Iron("jdbc:sqlite:memory:").connect()

    val user = connection.transaction {
        execute("CREATE TABLE users (id INTEGER PRIMARY KEY, firstName TEXT NOT NULL, lastName TEXT NOT NULL)")
        execute("INSERT INTO users (firstName, lastName) VALUES ('Ingot', 'Team')")
        
        query("SELECT * FROM users LIMIT 1;")
            .single<User>()
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

    // Pooled connections are identical to single connections
    // in terms of how you interact with them, but more connections
    // allow for more throughput in your application.
    val sum = connection.query("SELECT 1+1;")
        .columnSingle<Int>() // Gets the only value from the only column, throws if there's more than 1 value or column

    println(sum)
}
```

### Query Model Mapping
```kotlin
data class PartialUser(val firstName: String, val lastName: String)

suspend fun main() {
    val connection = Iron("jdbc:sqlite:memory:").connect()

    // we can easily map data from our queries to a model.
    val user = connection.query("SELECT firstName, lastName FROM users LIMIT 1;")
        .single<PartialUser>() // Enforces a row check, throws if more or less than 1 is returned
    
    // Or get all the users in the query
    val users = connection.prepare("SELECT firstName, lastName FROM users WHERE age > ?", 18)
        .all<PartialUser>()
    
    println(user)
    println(users.size)
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
