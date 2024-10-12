plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(libs.test.driver.sqlite)
    implementation(libs.test.logback)
    implementation(rootProject)
    ksp(rootProject)

    //gson
    implementation("com.google.code.gson:gson:2.9.0")
}

sourceSets {
    main {
        java {
            srcDir("build/generated")
        }
    }
}