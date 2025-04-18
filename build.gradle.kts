plugins {
    kotlin("jvm") version "1.9.20" apply false
    kotlin("plugin.serialization") version "1.9.20" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
}

allprojects {
    group = "ai.kastrax"
    version = "0.1.0"
    
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
