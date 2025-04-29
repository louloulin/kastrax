apply(plugin = "java-library")

// Set the artifact name
// extensions.configure<ArtifactExtension> {
//    name = "Proto.Actor Remote"
// }

plugins.withId("com.google.protobuf") {
    configure<com.google.protobuf.gradle.ProtobufExtension> {
        protoc {
            artifact = "com.google.protobuf:protoc:3.17.3"
        }

        plugins {
            create("grpc") {
                artifact = "io.grpc:protoc-gen-grpc-java:1.59.0"
            }
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
    add("api", "io.grpc:grpc-netty:1.59.0")
    add("api", "io.grpc:grpc-protobuf:1.59.0")
    add("api", "io.grpc:grpc-stub:1.59.0")
    add("api", "org.jctools:jctools-core:4.0.1")
    add("implementation", "com.google.protobuf:protobuf-java-util:3.25.1")
    add("implementation", "io.github.microutils:kotlin-logging:3.0.5")
    add("implementation", "javax.annotation:javax.annotation-api:1.3.2")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    add("api", project(":kactor:proto-actor"))
    add("api", project(":kactor:proto-mailbox"))

    add("testImplementation", "org.slf4j:slf4j-simple:2.0.9")
    add("testImplementation", "org.mockito:mockito-core:4.11.0")
    add("testImplementation", "org.mockito.kotlin:mockito-kotlin:4.1.0")
}
