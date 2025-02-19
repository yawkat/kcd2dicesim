plugins {
    alias(libs.plugins.kotlin.plugin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kvision)
}

repositories {
    mavenCentral()
}

val commonWasm = project.parent!!.project("common-wasm")
val webWorker = project.parent!!.project("web-worker")

val wasmDebug = false

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
    sourceSets["jsMain"].resources.srcDir(commonWasm.layout.buildDirectory.dir("compileSync/wasmJs/main/${if (wasmDebug) "development" else "production"}Executable/optimized").get().asFile)
    sourceSets["jsMain"].resources.srcDir(webWorker.layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile)
}

tasks.named("jsProcessResources").configure {
    dependsOn(
        commonWasm.tasks.named("compile${if (wasmDebug) "Development" else "Production"}ExecutableKotlinWasmJsOptimize"),
        webWorker.tasks.named("jsBrowserDistribution"),
    )
}