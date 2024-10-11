plugins {
    alias(libs.plugins.ksp)
}

dependencies {
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