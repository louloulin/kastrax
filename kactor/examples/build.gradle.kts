plugins {
    application
    java
    id("org.graalvm.buildtools.native") apply false
}

application {
    mainClass.set("ai.kastrax.examples.RunAdaptiveAgent")
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

// Task to run AdaptiveAgentExample
tasks.register<JavaExec>("runAdaptiveAgentExample") {
    group = "examples"
    description = "Run the AdaptiveAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run GoalOrientedAgentExample
tasks.register<JavaExec>("runGoalOrientedAgentExample") {
    group = "examples"
    description = "Run the GoalOrientedAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.GoalOrientedAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run HierarchicalAgentExample
tasks.register<JavaExec>("runHierarchicalAgentExample") {
    group = "examples"
    description = "Run the HierarchicalAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.HierarchicalAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run ReflectiveAgentExample
tasks.register<JavaExec>("runReflectiveAgentExample") {
    group = "examples"
    description = "Run the ReflectiveAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.ReflectiveAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run DeepseekToolAgentExample
tasks.register<JavaExec>("runDeepseekToolAgentExample") {
    group = "examples"
    description = "Run the DeepseekToolAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.DeepseekToolAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run DeepseekMemoryExample
tasks.register<JavaExec>("runDeepseekMemoryExample") {
    group = "examples"
    description = "Run the DeepseekMemoryExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.DeepseekMemoryExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run AgentNetworkExample
tasks.register<JavaExec>("runAgentNetworkExample") {
    group = "examples"
    description = "Run the AgentNetworkExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AgentNetworkExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run AdvancedAgentExample
tasks.register<JavaExec>("runAdvancedAgentExample") {
    group = "examples"
    description = "Run the AdvancedAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdvancedAgentExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
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

// Task to print classpath
tasks.register("printClasspath") {
    doLast {
        println(sourceSets["main"].runtimeClasspath.asPath)
    }
}

// Task to run AdaptiveAgentExample directly
tasks.register<JavaExec>("runAdaptiveAgent") {
    group = "examples"
    description = "Run the AdaptiveAgentExample directly"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RunAdaptiveAgent")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
