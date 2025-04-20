import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

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

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<DetektExtension> {
            config = files("${rootProject.projectDir}/detekt.yml")
            buildUponDefaultConfig = true
            autoCorrect = true
            ignoreFailures = true
        }
    }
}
