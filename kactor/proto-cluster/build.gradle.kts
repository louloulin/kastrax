plugins {
    kotlin("jvm")
    id("com.google.protobuf")
}

dependencies {
    api(project(":kactor:proto-actor"))
    api(project(":kactor:proto-remote"))

    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
}

plugins.withId("com.google.protobuf") {
    configure<com.google.protobuf.gradle.ProtobufExtension> {
        protoc {
            artifact = "com.google.protobuf:protoc:3.17.3"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    enabled = false // Temporarily disable tests
}
