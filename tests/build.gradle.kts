plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.test.driver.sqlite)
    implementation(libs.test.logback)
    implementation(rootProject)
    ksp(rootProject)
}

sourceSets {
    main {
        java {
            srcDir("build/generated")
        }
    }
}