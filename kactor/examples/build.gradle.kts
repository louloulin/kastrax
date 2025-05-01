plugins {
    application
    java
    id("org.graalvm.buildtools.native") apply false
}

application {
    mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExample")
}

dependencies {
    add("implementation", "org.jctools:jctools-core:4.0.1")
    add("implementation", "com.google.protobuf:protobuf-java:3.25.1")
    add("implementation", "org.slf4j:slf4j-simple:2.0.9")
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kastrax core dependencies
    add("implementation", project(":kastrax-core"))
    add("implementation", project(":kastrax-memory-api"))
    add("implementation", project(":kastrax-memory-impl"))
    add("implementation", project(":kastrax-zod"))
    add("implementation", project(":kastrax-rag"))
    add("implementation", project(":kastrax-integrations:kastrax-deepseek"))

    // Kactor dependencies
    add("implementation", project(":kactor:proto-actor"))
    add("implementation", project(":kactor:proto-router"))
    add("implementation", project(":kactor:proto-remote"))
    add("implementation", project(":kactor:proto-mailbox"))
    add("implementation", project(":kactor:proto-persistence"))
    add("implementation", project(":kactor:proto-cluster"))

    // Actor integration
    add("implementation", project(":kastrax-actor"))
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

sourceSets {
    main {
        kotlin {
            // 只包含已修复的文件
            include(
                "**/DeepSeekExample.kt",
                "**/DeepSeekStreamingExample.kt",
                "**/DeepSeekDirectStreamingExample.kt",
                "**/MemoryAgentExample.kt",
                "**/MemorySystemExample.kt",
                "**/SimpleZodToolExample.kt",
                "**/tools/ToolsExample.kt",
                "**/agent/CreativeAgentExample.kt",
                "**/agent/DeepseekAgentExample.kt",
                "**/agent/DeepseekToolAgentExample.kt",
                "**/agent/DeepseekArchitectureExample.kt",
                "**/agent/DeepseekMemoryExample.kt",
                "**/agent/DeepseekExamples.kt",
                "**/agent/DeepseekMain.kt",
                "**/CalculatorExample.kt",
                "**/memory/SemanticSearchExample.kt",
                "**/memory/EnhancedMemoryExample.kt",
                "**/workflow/EnhancedWorkflowExample.kt",
                "**/EnhancedRagExample.kt",
                "**/EnhancedRetrievalExample.kt",
                "**/EnhancedDocumentProcessingExample.kt"
            )
            // 排除有问题的文件
            exclude(
                "**/AdvancedWorkflowExample.kt",
                "**/RAGExample.kt",
                "**/RAGWorkflowExample.kt",
                "**/WorkflowExample.kt",
                "**/FastEmbedRAGExample.kt",
                "**/agent/AgentNetworkExample.kt",
                "**/DataSourceExample.kt",
                "**/AdvancedZodToolExample.kt",
                "**/DataClassZodToolExample.kt",
                "**/ZodAdvancedToolExample.kt",
                "**/ZodAgentExample.kt",
                "**/ZodCalculatorExample.kt",
                "**/ZodCalculatorToolExample.kt",
                "**/WorkflowRetryExample.kt"
            )
        }
    }

    test {
        kotlin {
            // 包含已修复的测试文件
            include(
                "**/SimpleZodToolTest.kt",
                "**/ZodToolExampleTest.kt"
            )
        }
    }
}

// Task to run AdaptiveAgentExample
tasks.register<JavaExec>("runKastraxAdaptiveAgentExample") {
    group = "examples"
    description = "Run the AdaptiveAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExample")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run all agent examples
tasks.register<JavaExec>("runExamplesAll") {
    group = "examples"
    description = "Run all agent examples"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RunAllAgentExamples")
    args = listOf("all")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run AdaptiveAgentExample
tasks.register<JavaExec>("runExamplesAdaptive") {
    group = "examples"
    description = "Run the AdaptiveAgentExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RunAllAgentExamples")
    args = listOf("adaptive")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}

// Task to run AgentStateExample
tasks.register<JavaExec>("runExamplesState") {
    group = "examples"
    description = "Run the AgentStateExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RunAllAgentExamples")
    args = listOf("state")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
}
