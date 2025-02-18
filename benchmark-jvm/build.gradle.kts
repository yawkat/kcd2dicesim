plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jmh)
}

repositories {
    mavenCentral()
}

dependencies {
    jmh(project(":common"))
}

jmh {
    profilers.add("async:libPath=/opt/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so;output=flamegraph")
}