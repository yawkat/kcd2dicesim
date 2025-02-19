plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
}

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
        binaries.library()
    }
    wasmJs {
        nodejs()
        binaries.library()
    }
    wasmWasi {
        nodejs()
        binaries.library()
    }
    sourceSets["commonMain"].dependencies {
        api(libs.kotlinx.serialization.json)
    }
    sourceSets["jvmTest"].dependencies {
        implementation(libs.junit.jupiter.engine)
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}