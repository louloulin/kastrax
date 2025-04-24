plugins {
    kotlin("jvm")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("io.quarkus") apply false
    id("io.ktor.plugin") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.jetbrains.kotlin.plugin.allopen") apply false
}

allprojects {
    group = "ai.kastrax.server"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        // 所有子项目共享的依赖
        "implementation"(kotlin("stdlib-jdk8"))
        "implementation"(kotlin("reflect"))
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

// 定义Quarkus平台版本
extra["quarkusPlatformGroupId"] = "io.quarkus.platform"
extra["quarkusPlatformArtifactId"] = "quarkus-bom"
extra["quarkusPlatformVersion"] = "3.5.0"

// 定义Ktor版本
extra["ktorVersion"] = "2.3.5"
extra["logbackVersion"] = "1.4.11"
extra["koinVersion"] = "3.5.0"
