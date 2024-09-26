# Making queries

## Table of Contents
- [Querying a single result](#querying-a-single-result)
- [Querying a multiple results](#querying-multiple-results)
- [Querying with a filter (passing in arguments)](#querying-with-a-filter-passing-in-arguments)
- [Grabbing a connection](#grabbing-a-connection)

> [!WARNING]  
> Before showing how to properly make queries, we need to talk about firstly the different ways 
> of making queries. Iron allows you to use `query`, `execute`, and `prepare` to make statements. 
> However, we highly recommend the usage of `prepare` as it prevents SQL injections and is more 
> secure because of that. All examples will be using `prepare` unless otherwise stated.

## Querying a Single Result

Iron at its core is a mapper for JDBC, meaning that you will need a class or data class to map the
results to. For our examples, we'll be using a simple `User` class. In java, you can either use a
record or a class to map the results to. The `@Model` annotation is required! For our examples we'll be using the following class.

```kotlin
data class User(
    val id: Int,
    val name: String,
    val age: Int
)
```

Here is how you would query a single result using `prepare`.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
val user: User? = iron.prepare("SELECT * FROM users").single<User>()
```

### Java
```java
final var iron = Iron.create("jdbc:sqlite::memory:").connect().blocking();
final User user = iron.prepare("SELECT * FROM users").single(User.class);
```

> [!IMPORTANT]  
> The `single` method will throw an exception if the result set is empty or more than one result.
> You can avoid this by using `singleNullable` instead, which will return `null` if the result set
> is empty or more than one result.
> If you want to get more than one, see [Querying Multiple Results](#querying-multiple-results).

## Querying Multiple Results

Querying multiple results is very similar to querying a single result, the only difference is that
you'll need to use `all` instead of `single`, and no exception will be thrown if the result set is
empty or more than one result. Limiting rows is recommended inside the `prepare` statement.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
val users: List<User> = iron.prepare("SELECT * FROM users LIMIT 10").all<User>()
```

### Java
```java
final var iron = Iron.create("jdbc:sqlite::memory:").connect().blocking();
final List<User> users = iron.prepare("SELECT * FROM users LIMIT 10").all(User.class);
```

## Querying with a Filter (passing in arguments)

In order to query with a filter, you'll need to learn how to pass in arguments to the `prepare` statement.
Luckily, Iron makes this very easy, you can pass in arguments to the `prepare` statement a few different ways
but the easiest is just passing it in since the method takes in a vararg of the arguments.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
val users: List<User> = iron.prepare("SELECT * FROM users WHERE id = ?", 1).all<User>()
```

### Java
```java
final var iron = Iron.create("jdbc:sqlite::memory:").connect().blocking();
final List<User> users = iron.prepare("SELECT * FROM users WHERE id = ?", 1).all(User.class);
```

Additionally, Iron also has the concept of named parameters which allows you to pass in arguments
using the `:` syntax. This is useful if you have a lot of arguments to pass in, or if you want to
pass in a whole model (more on this later). You typically won't need to use sqlParams, however this
won't be the last time we'll use named parameters.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
val users: List<User> = iron.prepare(
    "SELECT * FROM users WHERE id = :id", 
    sqlParams(
        "id" to 1
    )
).all<User>()
```

### Java
Not currently supported in Java.

## Grabbing a Connection

While Iron is designed to be a complete wrapper around JDBC, it does allow you to grab a connection
from the pool and use it directly in any rare situations where Iron isn't able to provide a solution.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()

iron.use { connection ->
    // This is a suspended lambda (coroutine)
}

iron.useBlocking { connection ->
    // This is a blocking lambda
}
```

### Java
Not currently supported in Java.