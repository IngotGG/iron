plugins {
    kotlin("jvm") version "1.9.23"
    `maven-publish`
}

val githubActor: String = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
val githubToken: String = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")

group = "gg.ingot.iron"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // jasync-sql
    implementation("com.github.jasync-sql:jasync-postgresql:2.2.4")

    // unit tests
    testImplementation(kotlin("test"))
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


// Allow for publishing to Maven local.
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/IngotGG/iron")
            credentials {
                username = githubActor
                password = githubToken
            }
        }
    }

    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
        }
    }
}