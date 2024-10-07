plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)

    `maven-publish`
}

group = "gg.ingot"
version = "2.0.0"

allprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    repositories {
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        implementation(rootProject.libs.slf4j)
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

    tasks.test {
        useJUnitPlatform()
    }
}

dependencies {
    // kotlin
    implementation(kotlin("reflect"))
    compileOnly(libs.kotlinx.coroutines)

    // core modules
    api(project(":processor"))

    // serialization
    compileOnly(libs.kotlinx.serialization)
    compileOnly(libs.gson)

    // inflector
    implementation(libs.evo)

    // unit tests
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.testing)
    testImplementation(project(":controller"))
    kspTest(rootProject)

    // serialization
    testImplementation(libs.kotlinx.serialization)
    testImplementation(libs.gson)
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = libs.versions.kotlin.get()))
    }
}