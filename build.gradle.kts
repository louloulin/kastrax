import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.serialization") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.allopen") version "2.1.10" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.5" apply false
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("io.quarkus") version "3.9.0" apply false
    id("io.ktor.plugin") version "3.1.2" apply false
    id("com.vanniktech.maven.publish") version "0.32.0" apply false
}

allprojects {
    // 只为根项目设置group和version，子项目会继承或自己设置
    if (project == rootProject) {
        group = "ai.kastrax"
        version = "0.1.1"
    }

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

// 配置发布插件的通用设置
// 注意：实际的插件应用需要在各个子模块中单独进行
