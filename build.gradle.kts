plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kvision)
}

group = "at.yawk.kcd2dicesim"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "main.bundle.js"
                sourceMaps = false
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        binaries.executable()
    }
    sourceSets["jsMain"].dependencies {
        implementation(libs.kvision)
        implementation(libs.kvision.bootstrap)
        implementation(libs.kvision.state)
    }
    sourceSets["jsTest"].dependencies {
        implementation(libs.kotlin.test.js)
        implementation(libs.kvision.testutils)
    }
    sourceSets["jvmTest"].dependencies {
        implementation(libs.junit.jupiter.engine)
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}