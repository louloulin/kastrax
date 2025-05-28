plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

allprojects {
    group = "ai.kastrax.examples"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "application")
    apply(plugin = "java")

    dependencies {
        // 公共依赖
        implementation("org.jetbrains.kotlin:kotlin-stdlib")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        implementation("org.slf4j:slf4j-simple:2.0.9")

        // 引用主项目的核心模块
        implementation(project(":kastrax-core"))
        implementation(project(":kastrax-memory-api"))
        implementation(project(":kastrax-memory-impl"))
        implementation(project(":kastrax-zod"))
        implementation(project(":kastrax-rag"))
        implementation(project(":kastrax-integrations:kastrax-deepseek"))
        implementation(project(":kastrax-integrations:kastrax-openai"))
        implementation(project(":fastembed-kotlin"))

        // Kactor依赖
        implementation(project(":kactor:proto-actor"))
        implementation(project(":kactor:proto-router"))
        implementation(project(":kactor:proto-remote"))
        implementation(project(":kactor:proto-mailbox"))
        implementation(project(":kactor:proto-persistence"))
        implementation(project(":kactor:proto-cluster"))

        // Actor集成
        implementation(project(":kastrax-actor"))
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
}
