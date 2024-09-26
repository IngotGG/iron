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

## Installation

Tags & Releases can be found on our [Jitpack](https://jitpack.io/#gg.ingot/iron).
You will also need to add the JDBC driver of the DBMS you're using. For more information, see 
the [Adding a JDBC Driver](docs/connecting.md#adding-a-jdbc-driver) documentation page.

### Gradle

```kts
repositories {
  maven("https://jitpack.io")
}

dependencies {
    implementation("gg.ingot.iron:iron:TAG")
    
    // Optional, if you want to use the controller module (Kotlin only)
    implementation("gg.ingot.iron:controller:TAG")
}
```
### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>gg.ingot.iron</groupId>
    <artifactId>iron</artifactId>
    <version>TAG</version>
</dependency>

<!-- Optional, if you want to use the controller module (Kotlin only) -->
<dependency>
    <groupId>gg.ingot.iron</groupId>
    <artifactId>controller</artifactId>
    <version>TAG</version>
</dependency>
```

## Documentation

Check out the [documentation](docs/README.md) for more information on how to use Iron.

## License

Iron is licensed under the [Apache License, Version 2.0](LICENCE).