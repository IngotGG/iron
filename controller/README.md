# Iron Processor

Iron also has an optional module for including a compiler plugin, this plugin provides useful utilities used to
speed up development workflows.

# Installation

The processor uses `KSP` which requires an additional dependency alongside Iron if you do not have already:
```kts
plugins {
    id("com.google.devtools.ksp") version "2.0.10-1.0.24"
}

dependencies {
    implementation("gg.ingot:iron-processor")
    ksp("com.google.dagger:dagger-compiler:2.51.1")
}
```