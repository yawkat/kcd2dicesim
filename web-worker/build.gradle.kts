plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            useEsModules()
            commonWebpackConfig {
                outputFileName = "worker.bundle.js"
                sourceMaps = true
            }
            testTask {
                useMocha()
            }
        }
        binaries.executable()
    }
    sourceSets["jsMain"].dependencies {
        implementation(project(":common"))
        implementation(project(":common-wasm"))
    }
}