plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kvision)
}

repositories {
    mavenCentral()
}

val commonWasm = project.parent!!.project("common-wasm")

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
    }
    sourceSets["jsTest"].dependencies {
        implementation(libs.kotlin.test.js)
        implementation(libs.kvision.testutils)
    }
    sourceSets["jsMain"].resources.srcDir(commonWasm.layout.buildDirectory.dir("compileSync/wasmJs/main/productionExecutable/optimized").get().asFile)
}

tasks.named("jsProcessResources").configure {
    dependsOn(commonWasm.tasks.named("compileProductionExecutableKotlinWasmJsOptimize"))
}