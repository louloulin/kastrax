apply(plugin = "java-library")

// Set the artifact name
// extensions.configure<ArtifactExtension> {
//    name = "Proto.Actor Core"
// }

plugins.withId("com.google.protobuf") {
    configure<com.google.protobuf.gradle.ProtobufExtension> {
        protoc {
            artifact = "com.google.protobuf:protoc:3.25.1"
        }
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/proto") {
        include("**/*.proto")
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

dependencies {
    add("api", "com.google.protobuf:protobuf-java:3.25.1")
    add("api", project(":kactor:proto-mailbox"))
    add("implementation", "io.github.microutils:kotlin-logging:3.0.5")
    add("implementation", "ch.qos.logback:logback-classic:1.4.7")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    // Prometheus 依赖
    add("implementation", "io.prometheus:simpleclient:0.16.0")
    add("implementation", "io.prometheus:simpleclient_hotspot:0.16.0")
    add("implementation", "io.prometheus:simpleclient_httpserver:0.16.0")

    add("testImplementation", "org.slf4j:slf4j-simple:2.0.9")
    add("testImplementation", "org.awaitility:awaitility:4.2.0")
    add("testImplementation", "org.mockito:mockito-core:4.11.0")
    add("testImplementation", "org.mockito.kotlin:mockito-kotlin:4.1.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
