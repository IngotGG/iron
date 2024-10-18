# Making queries

## Table of Contents
- [Inserting data](#inserting-data)
- [Using a Transaction](#using-a-transaction)

> [!TIP]  
> We recommend looking at the [querying](querying.md) page first as some extra information can
> be found there.

## Inserting Data

Iron at its core is a mapper for JDBC, meaning that you will need a class or data class to map the
results to. For our examples, we'll be using a simple `User` class. In java, you can either use a
record or a class to map the results to. The `@Model` annotation is required! For our examples we'll be using the following class.

```kotlin
@Model
data class User(
    val id: Int,
    val name: String,
    val age: Int
)
```

> [!TIP]
> Iron will dynamically generate bindings for models in prepare statements even if they are missing
> the `Bindings` interface. This is useful for when you want to use the model as a parameter to a
> query. You will however no longer be able to call `bindings()` on the model.

Inserting data is very simple, and we'll use the `prepare` method to insert data in this example. 
Alongside that we'll create a table into the in-memory database to make this a working example. We
recommend using [Flyway](https://github.com/flyway/flyway) for production environments.

Notice the usage of Named Parameters, you're required to use named parameters when inserting data
into a table, because the order of the parameters is important and might not follow the order of the
parameters inside the class, nor will all data need to be passed in.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
iron.prepare("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, age INTEGER)")

val user = User(1, "John Doe", 30)
iron.prepare("INSERT INTO users (name, age) VALUES (:name, :age)", user)
```

### Java
```java
final var iron = Iron.create("jdbc:sqlite::memory:").connect().blocking();
iron.prepare("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT, age INTEGER)")

final User user = new User(1, "John Doe", 30);
iron.prepare("INSERT INTO users (name, age) VALUES (:name, :age)", user);
```

## Using a Transaction

Like all other libraries, Iron allows you to use transactions to make sure your data is inserted and to
push multiple operations into a single transaction.

> [!WARNING]
> When using transactions, do not call the methods inside the iron instance, instead call the
> methods on the transaction instance. (see examples below)

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:").connect()
val userOne = User(1, "John Doe", 30)
val userTwo = User(2, "Jane Doe", 25)

iron.transaction {
    // Run any operations you normally world
    prepare("INSERT INTO users (name, age) VALUES (:name, :age)", user)
    prepare("INSERT INTO users (name, age) VALUES (:name, :age)", userTwo)
}
```

### Java
```java
final var iron = Iron.create("jdbc:sqlite::memory:").connect().blocking();
final User userOne = new User(1, "John Doe", 30);
final User userTwo = new User(2, "Jane Doe", 25);

iron.transaction((transaction) -> {
    // Run any operations you normally world
    transaction.prepare("INSERT INTO users (name, age) VALUES (:name, :age)", user);
    transaction.prepare("INSERT INTO users (name, age) VALUES (:name, :age)", userTwo);
});
```