dependencies {
    implementation(kotlin("reflect"))
    compileOnly(rootProject)

    testImplementation(kotlin("test"))
    testImplementation(rootProject)
    testImplementation(libs.bundles.testing)
}