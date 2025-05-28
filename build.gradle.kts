import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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

    // 为非测试模块配置 maven-publish
    if (!project.name.contains("test") && project != rootProject) {
        apply(plugin = "maven-publish")
        
        plugins.withId("java") {
            plugins.withId("maven-publish") {
                configure<PublishingExtension> {
                    publications {
                        // 只有当没有现有的maven publication时才创建
                        if (publications.findByName("maven") == null) {
                            create<MavenPublication>("maven") {
                                from(components["java"])
                            }
                        }
                    }
                }
            }
        }
    }
}
