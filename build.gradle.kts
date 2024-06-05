plugins {
    kotlin("jvm") version "2.0.0"

    `maven-publish`
}

group = "gg.ingot.iron"
version = "1.0.2"

repositories {
    mavenCentral()
}

dependencies {
    // kotlin
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")

    // logging
    implementation("org.slf4j:slf4j-api:2.0.13")

    // unit tests
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0-RC")
    testImplementation("org.xerial:sqlite-jdbc:3.46.0.0")
    testImplementation("ch.qos.logback:logback-classic:1.5.6")
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