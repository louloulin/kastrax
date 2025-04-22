plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    // Project dependencies
    implementation(project(":kastrax-core"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:2.20.56"))
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:apigateway")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:cloudformation")

    // Docker
    implementation("com.github.docker-java:docker-java-core:3.3.0")
    implementation("com.github.docker-java:docker-java-transport-httpclient5:3.3.0")

    // Kubernetes
    implementation("io.kubernetes:client-java:18.0.0")

    // Configuration
    implementation("com.typesafe:config:1.4.2")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Redis
    implementation("io.lettuce:lettuce-core:6.2.4.RELEASE")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.testcontainers:testcontainers:1.18.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "ai.kastrax"
            artifactId = "kastrax-deployer"
            version = "0.1.0"

            from(components["java"])
        }
    }
}
