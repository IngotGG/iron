# Iron Documentation

## Introduction

Iron is an opinionated database library for Kotlin that aims to make it easy to interact with databases.
It internally wraps over the JDBC API and Hikari and provides a fluent API for interacting with the database.

> [!NOTE]  
> Iron is developed by [Ingot](https://ingot.gg), and is designed to be used in a Kotlin environment. While
> a lot of Iron is designed to work with Java, it isn't a first class citizen and certain features (like
> controllers) are only available in Kotlin because of syntactical limitations.

### Iron's Philosophy
Iron employs a number of useful features to make interacting with one or multiple databases as easy
and straightforward as possible. By default, Iron is purely a mapper for tables, however it has features
on top of that, which allows it to function almost like a full ORM.

### Error Handling
While Iron is designed to keep database interactions as simple as possible, Iron does however throw
when it encounters an error, this allows you to catch errors and handle them appropriately. This is
especially useful if you pair Iron with a web framework like [Ktor](https://ktor.io/), and it's 
[status pages plugin](https://ktor.io/docs/server-status-pages.html).

## Table of Contents

- [Setting up Iron](connecting.md)
- [Making queries](querying.md)
- [Inserting data](inserting.md)
- [Controllers](../controller/README.md)