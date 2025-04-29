plugins {
    application
    java
    id("org.graalvm.buildtools.native") apply false
}

application {
    mainClass.set("actor.proto.examples.inprocessbenchmark.InProcessBenchmarkKt")
}

dependencies {
    add("implementation", "org.jctools:jctools-core:4.0.1")
    add("implementation", "com.google.protobuf:protobuf-java:3.25.1")
    add("implementation", "org.slf4j:slf4j-simple:2.0.9")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    add("implementation", project(":kactor:proto-actor"))
    add("implementation", project(":kactor:proto-router"))
    add("implementation", project(":kactor:proto-remote"))
    add("implementation", project(":kactor:proto-mailbox"))
    add("implementation", project(":kactor:proto-persistence"))
    add("implementation", project(":kactor:proto-cluster"))
}

// Task to run ProtobufPersistenceExample
tasks.register<JavaExec>("runProtobufPersistenceExample") {
    group = "application"
    description = "Run the ProtobufPersistenceExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("actor.proto.examples.persistence.ProtobufPersistenceExampleKt")
}

// Task to run PubSubExtensionsExample
tasks.register<JavaExec>("runPubSubExtensionsExample") {
    group = "application"
    description = "Run the PubSubExtensionsExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("actor.proto.examples.pubsub.PubSubExtensionsExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
    // Print the classpath for debugging
    doFirst {
        println("Classpath: ${classpath.asPath}")
    }
}

// Task to run ConsensusExample
tasks.register<JavaExec>("runConsensusExample") {
    group = "application"
    description = "Run the ConsensusExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("actor.proto.examples.consensus.ConsensusExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
}

// Task to run MessageBatch example
tasks.register<JavaExec>("runMessageBatchExample") {
    group = "application"
    description = "Run the MessageBatch example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("actor.proto.examples.messagebatch.KotlinBatchDemo")
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
}

// GraalVM 配置已禁用，因为插件应用方式不兼容
// graalvmNative {
//     binaries {
//         named("main") {
//             imageName.set("protoactor-example")
//             mainClass.set("actor.proto.examples.helloworld.HelloWorldKt")
//             buildArgs.add("--no-fallback")
//             buildArgs.add("--report-unsupported-elements-at-runtime")
//             buildArgs.add("-H:+ReportExceptionStackTraces")
//             buildArgs.add("-H:+PrintClassInitialization")
//         }
//     }
//     metadataRepository {
//         enabled.set(true)
//     }
// }
