plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0-RC3"

    `maven-publish`
}

group = "gg.ingot"
version = "1.3.3"

repositories {
    mavenCentral()
}

dependencies {
    // kotlin
    implementation(kotlin("reflect"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

    // serialization
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0-RC")
    compileOnly("com.google.code.gson:gson:2.11.0")

    // logging
    implementation("org.slf4j:slf4j-api:2.0.13")

    // unit tests
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")

    // serialization
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0-RC")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}