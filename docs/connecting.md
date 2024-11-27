# Setting up Iron

## Table of Contents
- [Adding a JDBC Driver](#adding-a-jdbc-driver)
- [Connecting to a database](#connecting-to-a-database)
- [Configuring Iron](#configuring-iron)
- [Picking an executor](#picking-an-executor)

## Adding a JDBC Driver

Before we can connect to a database, we need to add a JDBC driver to the classpath. This is required
because Iron by default attempts to support every database management system (DBMS) that has a JDBC
driver available, however it doesn't include any drivers by default. You can find a list of some 
drivers at [soapui.org](https://www.soapui.org/docs/jdbc/reference/jdbc-drivers/).

> [!NOTE]  
> Iron by default attempts to support every database management system (DBMS) that has a JDBC driver 
> available. While we unit test against SQLite, Postgres, and H2, it is possible that some issues may
> arise, please open a GitHub issue if you encounter any problems.

## Connecting to a Database

To connect to a database, you'll need to create an instance of the `Iron` class. The way to do this
is different based on the language you're using, examples will be provided for both Java and Kotlin
wherever applicable.

### Kotlin
```kotlin
// We recommend the usage of var for the iron instance because of the varying
// return types (blocking, completable, etc.)
val iron = Iron("jdbc:sqlite::memory:").connect() 
// Not specifying an executor will default to coroutines
```

### Java
```java
// We recommend the usage of var for the iron instance because of the varying
// return types (blocking, completable, etc.)
final var iron = Iron.create("jdbc:sqlite::memory:")
        .connect()
        .blocking(); // Uses the blocking executor
```

## Configuring Iron

Changing settings for Iron is done in the constructor of the `Iron` class. Kotlin uses the DSL pattern
for this, and Java uses the builder pattern. See the KDoc for information on each setting.

### Kotlin
```kotlin
val iron = Iron("jdbc:sqlite::memory:") {
    namingStrategy = NamingStrategy.SNAKE_CASE
    strictBooleans = true
}.connect()
```

### Java
```java
final var iron = Iron.create(
    "jdbc:sqlite::memory:",
    new IronSettings.Builder()
        .namingStrategy(NamingStrategy.SNAKE_CASE)
        .strictBooleans(true)
        .build()
).connect().blocking();
```

## Picking an executor

Iron supports multiple executors by default (and you can even make your own), we recommend using
coroutines as it is the most efficient and idiomatic way to Iron in Kotlin. However multiple executors
are supported for your convenience.

| Executor        |Description|Kotlin Support|Java Support|
|-----------------|---|---|---|
| `coroutines()`  |Coroutines are a lightweight alternative to threads, and are the preferred executor for Kotlin.|✅|❌|
| `deferred()`    |Deferred value is a non-blocking cancellable future — it is a Job with a result.|✅|❌|
| `completable()` |CompletableFuture is a lightweight alternative to threads, and is the preferred executor for Java.|✅|✅|
| `blocking()`    |Blocks the current thread until the operation is complete.|✅|✅|
