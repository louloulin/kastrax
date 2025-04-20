plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    application
    `maven-publish`
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
    jvmToolchain(21)
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
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ai.kastrax"
            artifactId = "kastrax-cli"
            version = "0.1.0"

            from(components["java"])
        }
    }
}
