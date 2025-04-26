val ktorVersion: String by project
val logbackVersion: String by project
val koinVersion: String by project

plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

group = "ai.kastrax.server"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("ai.kastrax.server.ktor.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    mavenLocal()
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("io.ktor:ktor-server-sse:$ktorVersion"))
            .using(module("io.ktor:ktor-server-core:$ktorVersion"))
    }

    // 强制使用一致的Ktor版本
    resolutionStrategy.force("io.ktor:ktor-server-core:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-netty:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-content-negotiation:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-serialization-kotlinx-json:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-serialization-jackson:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-cors:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-swagger:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-status-pages:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-call-logging:3.1.2")
    // 路由功能已经包含在ktor-server-core中，不需要单独的依赖
    // resolutionStrategy.force("io.ktor:ktor-server-routing:3.1.2")
    resolutionStrategy.force("io.ktor:ktor-server-test-host:3.1.2")
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    // 路由功能已经包含在ktor-server-core中，不需要单独的依赖
    // implementation("io.ktor:ktor-server-routing:$ktorVersion")

    // KastraX Core
    implementation(project(":kastrax-core"))

    // Common
    implementation(project(":kastrax-server:common"))

    // Koin
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    implementation("io.insert-koin:koin-logger-slf4j:$koinVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    // JUnit Jupiter
    val junitVersion = "5.10.0"
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")

    // 强制使用一致的kotlin-test版本
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-test:1.9.0")
        resolutionStrategy.force("org.jetbrains.kotlin:kotlin-test-junit:1.9.0")
    }

    // Mockito
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    // Koin Test
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // 禁用测试
    enabled = false
}

// 禁用shadowJar
tasks.named("shadowJar") {
    enabled = false
}
