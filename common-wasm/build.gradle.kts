plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    wasmJs {
        nodejs()
        binaries.executable()
    }
    sourceSets["wasmJsMain"].dependencies {
        implementation(project(":common"))
    }
}
