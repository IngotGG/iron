plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(rootProject)
    ksp(rootProject)
}