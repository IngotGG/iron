plugins {
    kotlin("jvm") version "1.9.23"
}

group = "gg.ingot.iron"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    // jasync-sql
    implementation("com.github.jasync-sql:jasync-postgresql:2.2.4")

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