plugins {
    id("java-library")
    id("maven-publish")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "ai.kastrax.store"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":kastrax-store"))
    
    // LanceDB Java Client
    implementation("com.lancedb:lance-core:0.18.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Publishing configuration moved to global setup
