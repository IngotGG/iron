# Iron Controllers

Alongside the base of Iron which works for flexible usage, Iron Controllers provides an easy way to create controllers for your application.
By default, controllers come with a set of methods that provide basic CRUD operations for your entities, however their
functionality can be extended with extension functions.

> [!WARNING]  
> Iron uses JOOQ for dynamically generating queries, this however means that the available DBMS's are limited to those
> that JOOQ supports. If you wish to see what DBMS's are supported, check out the [JOOQ Support Matrix](https://www.jooq.org/download/support-matrix).

# Installation

Installing Iron Controllers is as simple as adding the dependency to your project alongside the base Iron dependency:
```kts
dependencies {
    // Base Iron
    implementation("gg.ingot.iron:iron:VERSION")
    ksp("gg.ingot.iron:processor:VERSION")

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

We heavily recommend using proper annotations for controller entities, or you might run into exceptions.
See the example below for examples of how a fully annotated model would look like.

```kotlin
@Model(table = "users")
data class User(
    @Column(primaryKey = true)
    val id: UUID,
    val name: String,
    val age: Int
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

> [!WARNING]
> Interceptors are still being worked on, please do not use them yet until proper documentation is written.

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
> do not by default, you must ensure that the queries you write are compatible with the DBMS you are using. Better
> support for this will be added in the future using JOOQ.