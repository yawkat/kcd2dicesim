plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kvision)
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
                sourceMaps = true
            }
            testTask {
                useMocha()
            }
        }
        binaries.executable()
    }
    sourceSets["jsMain"].dependencies {
        implementation(libs.kvision)
        implementation(libs.kvision.bootstrap)
        implementation(libs.kvision.state)
        implementation(project(":common"))
        implementation(project(":common-wasm"))
    }
    sourceSets["jsTest"].dependencies {
        implementation(libs.kotlin.test.js)
        implementation(libs.kvision.testutils)
    }
}