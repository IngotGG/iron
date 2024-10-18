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

Iron uses annotation processors to automatically generate SQL representations of your models.
If you are using Kotlin, you will need to use KSP, for Java you can use the gradle annotation processor.
Please take a look below to see details, please keep in mind you only need to use one of the two.

### Gradle (Kotlin DSL)

```kts
plugins {
    // Make sure you use the correct version, the version below is the one Iron is using.
    // Required only if opting for Kotlin and not the java annotation processor
    id("com.google.devtools.ksp") version "2.0.10-1.0.24"
}

repositories {
  maven("https://jitpack.io")
}

dependencies {
    implementation("gg.ingot.iron:iron:TAG")
    ksp("gg.ingot.iron:processor:TAG") // Use for kotlin (java records are not supported)
    annotationProcessor("gg.ingot.iron:processor:TAG") // Use for java only (records supported, kotlin models are not)
    
    // Optional, if you want to use the controller module (Kotlin only)
    implementation("gg.ingot.iron:controller:TAG")
}
```

## Documentation

Check out the [documentation](docs/README.md) for more information on how to use Iron.

## License

Iron is licensed under the [Apache License, Version 2.0](LICENCE).