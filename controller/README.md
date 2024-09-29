# Iron Controllers

Alongside the base of Iron which works for flexible usage, Iron Controllers provides an easy way to create controllers for your application.
By default, controllers come with a set of methods that provide basic CRUD operations for your entities, however their
functionality can be extended with extension functions.

> [!WARNING]  
> It's important to note that Iron Controllers is relatively new, support for different DBMS' is limited and may not work as expected.
> As of this moment, only Sqlite, MySQL and PostgreSQL are supported. Please [open a pull request](https://github.com/ingotgg/iron/pull) to help add support for
> other DBMS'. You can additionally register your own [`DBMSEngine` implementation](#registering-an-engine) to add support for a DBMS not supported by default.

# Installation

Installing Iron Controllers is as simple as adding the dependency to your project alongside the base Iron dependency:
```kts
dependencies {
    // Base Iron
    implementation("gg.ingot.iron:iron:VERSION")

    // Iron Controllers
    implementation("gg.ingot.iron:controller:VERSION")
}
```

# Usage

To create a controller, you must already have a model you can use to represent your entity. For this example, we will use a simple `User` model:
```kotlin
@Model
data class User(
    val id: UUID,
    var name: String,
    var age: Int
)
```

This is nothing different, if you have used Iron before, there is nothing new here. Now to mark this model as an entity 
(something that can be managed by a controller), you must mark it as a controller:
```kotlin
@Model
@Controller
data class User(
    val id: UUID,
    var name: String,
    var age: Int
)
```

This will create a controller for the `User` entity with the default CRUD operations. You can now use this controller to manage your `User` entities:
```kotlin
val users = iron.controller<User>()
```

## Basic CRUD Operations

The controller provides the following basic CRUD operations:
- `insert(entity: T, fetch: Boolean = false): T`
- `first(filter: SqlFilter<T>? = null): T?`
- `update(entity: T, fetch: Boolean = false): T`
- `delete(entity: T)`
- A few more...
*(Note: `T` is the entity type)*

These operations can be used to manage your entities. For example, to insert a new user:
```kotlin
val users = iron.controller<User>()
val user = User(UUID.randomUUID(), "John Doe", 25)

users.insert(user)
```

In the case where the database might have a default value for the `id` field, you can fetch the entity after inserting it:
```kotlin
val users = iron.controller<User>()
val user = User(UUID.randomUUID(), "John Doe", 25)

val insertedUser = users.insert(user, fetch = true) // If fetch isn't true, the inserted entity will be returned
```

## Creating interceptors

Interceptors are a nice way to register functions that will be called before an entity is inserted or updated in the
table. This can be useful for things like validation or logging. Here's an example of how you can create an interceptor
to keep an updated timestamp on the entity:

```kotlin
@Model
@Controller
data class User(
    val id: UUID,
    var name: String,
    var age: Int,
    var updatedAt: Long = System.currentTimeMillis()
)

iron.controller<User>().interceptor {
    it.apply { updatedAt = System.currentTimeMillis() }
}
```

It should be noted that interceptors are still experimental and while they will work, their usage may change in the future.
At the moment, it is not possible to remove an interceptor once it has been added.

## Filtering

Some methods in controllers (like `first`, `delete`, and `all`) accept a filter parameter. This parameter can be used to
filter the results returned by the controller. Here's an example of how you can use a filter to get the first user with
the name "John Doe":

```kotlin
val user = iron.controller<User>().first { User::name eq "John Doe" }
```

Filters can be created using the `eq`, `neq`, `lt`, `lte`, `gt`, `gte`, `like`, and `ilike` functions. These functions
can be used to create complex filters. Here's an example of how you can use multiple filters to get all users with the
name "John Doe" and age 25:

```kotlin
val users = iron.controller<User>().all {
    (User::name eq "John Doe") and (User::age eq 25)
}
```

Due to some limitations in Kotlin, you need to wrap the filters in parentheses when using multiple filters. This is
because the `and` and `or` functions are infix functions and have the same precedence as the comparison functions.

Complex filters past the ones shown off are better off being created as extension functions to keep your code clean and
easy to read, alongside that it allows for easy reuse of filters.

## Extending Controllers

By default, controllers come with basic methods for CRUD operations. However, these can be extended with extension
functions to suit your needs. Here's an example of how you can extend the `User` controller to get all users that
are above a certain age:

```kotlin
suspend fun TableController<User>.getUsersAboveAge(age: Int): List<User> {
    return iron.prepare("SELECT * FROM $tableName WHERE age > ?", age)
        .all<User>()
}
```

As you can see, a bit of work was also done for us, the controller provides us access to the `iron` instance used
to create the controller, which we can use to prepare queries. Alongside that the controller also has context
of what table it is managing, which can be accessed via the `tableName` property, using this is optional but can
help in keeping your queries easier to refactor.

> [!NOTE]  
> While the default controller methods are designed to work in multiple DBMS', the extension functions you write
> do not by default, you must ensure that the queries you write are compatible with the DBMS you are using.

## Registering an Engine

Unlike Iron, Iron Controllers does not come with support for all DBMS' out of the box because of the inherent complexity,
however you can register your own `DBMSEngine` implementation to add support for a DBMS not supported by default.

To create an engine, simply implement the `DBMSEngine` interface:
```kotlin
class MyCustomEngine : DBMSEngine {
    ...
}
```

You can then register this engine with Iron Controllers:
```kotlin
DBMSEngine.register(DBMS.SQLSERVER, MyCustomEngine())
```

We recommend first checking out the built-in engines to get an idea of how to implement your own engine. Here are two examples:
- [SQLite Engine](https://github.com/IngotGG/iron/blob/controller/src/main/kotlin/gg/ingot/iron/controller/engine/impl/SqliteEngine.kt)
- [Postgres Engine](https://github.com/IngotGG/iron/blob/controller/src/main/kotlin/gg/ingot/iron/controller/engine/impl/PostgresEngine.kt)

> [!CAUTION]
> Engines deal directly with dynamically creating SQL queries, it is important to ensure that the queries you generate
> are safe and do not allow for SQL injection. Always use prepared statements and never concatenate values. Iron has
> security measures in place to prevent SQL injection on table names and column names, but any other values must be
> considered unsafe.