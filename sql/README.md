# Iron SQL

Iron SQL is a type-safe SQL builder for Kotlin. The reason for its existence is that Iron requires
the ability to generate SQL queries dynamically and in a database-agnostic way. This library aims to
provide a simple and easy-to-use API for generating SQL queries. 

> [!NOTE]
> This library is designed for Iron's internal use and while you can use it in your own projects, it
> is not recommended because of it's lack of full coverage. SQL generation can only generate a
> fraction of the SQL queries that a database supports. Because of this module's experimental nature,
> it is not recommended to use this library standalone in production, however please report any bugs 
> you may find.

## Usage

### Creating a SQL query

To create a SQL query, you can use the `Sql` class.

```kotlin
val query = Sql.of("SELECT * FROM users")
```

This will create a SQL query that can be used to generate a string representation of the query.

```kotlin
println(query.toString())
```

This will print the following SQL query:

```sql
SELECT * FROM users;
```

### Create a type-safe SQL query

You can also create a type-safe SQL query by using the `Sql` function.

```kotlin
val query = Sql(DBMS.MYSQL)
    .select()
    .from("users")
```

This will create a SQL query that can be used to generate a string representation of the query.

```kotlin
println(query.toString())
```

This will print the following SQL query:

```sql
SELECT * FROM users;
```

Here is a more complex example showcasing more of the library's features:

```kotlin
val query = Sql(DBMS.SQLITE)
    .select(column("id"), column("name"))
    .from("users")
    .where(column("id") eq 1)
    .limit(10)
    .offset(5)
```

Alternatively, you can use the builder as a DSL as kotlin allows for infix functions.

```kotlin
val select = Sql(DBMS.SQLITE).select()
val query = select from "users" where { column("id") eq 1 } limit 10 offset 5
```