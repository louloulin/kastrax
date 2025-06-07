plugins {
    id("com.vanniktech.maven.publish")
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("io.ktor.plugin") apply false

    application
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    // Project dependencies
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-evals"))
    implementation(project(":kastrax-store"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // CLI
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
    implementation("com.github.ajalt.mordant:mordant:2.2.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // File handling
    implementation("commons-io:commons-io:2.15.0")
    implementation("org.apache.commons:commons-text:1.10.0")

    // Template engine
    implementation("org.freemarker:freemarker:2.3.32")

    // Ktor for server
    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-netty:3.1.2")
    implementation("io.ktor:ktor-server-cors:3.1.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.2")
    implementation("io.ktor:ktor-serialization-jackson:3.1.2")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ai.kastrax.cli.KastraXCliKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "ai.kastrax.cli.KastraXCliKt"
    }

    // Include all dependencies in the jar
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Add explicit dependency on kastrax-store:jar
    dependsOn(":kastrax-store:jar")
}


// 配置 vanniktech maven publish 插件
mavenPublishing {
    pom {
        name.set("Kastrax CLI")
        description.set("Command-line interface for Kastrax")
    }
}
