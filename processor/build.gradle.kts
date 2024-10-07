dependencies {
    implementation(kotlin("reflect"))
    implementation(libs.ksp.api)
    implementation(libs.bundles.kotlinpoet)

    compileOnly(libs.kotlinx.serialization)
    compileOnly(libs.gson)
}