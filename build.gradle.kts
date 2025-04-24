import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    kotlin("jvm") version "1.9.20" apply false
    kotlin("plugin.serialization") version "1.9.20" apply false
    kotlin("plugin.spring") version "1.9.20" apply false
    kotlin("plugin.allopen") version "1.9.20" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.0" apply false
    id("org.springframework.boot") version "3.1.5" apply false
    id("io.spring.dependency-management") version "1.1.3" apply false
    id("io.quarkus") version "3.5.0" apply false
    id("io.ktor.plugin") version "2.3.5" apply false
}

// 定义Quarkus平台版本
extra["quarkusPlatformGroupId"] = "io.quarkus.platform"
extra["quarkusPlatformArtifactId"] = "quarkus-bom"
extra["quarkusPlatformVersion"] = "3.5.0"

// 定义Ktor版本
extra["ktorVersion"] = "2.3.5"
extra["logbackVersion"] = "1.4.11"
extra["koinVersion"] = "3.5.0"

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
