plugins {
    application
    java
    id("org.graalvm.buildtools.native") apply false
}

application {
    // 设置主类为HelloWorld
    mainClass.set("ai.kastrax.examples.HelloWorld")
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
    add("implementation", project(":kastrax-integrations:kastrax-openai"))
    add("implementation", project(":fastembed-kotlin"))

    // Kactor dependencies
    add("implementation", project(":kactor:proto-actor"))
    add("implementation", project(":kactor:proto-router"))
    add("implementation", project(":kactor:proto-remote"))
    add("implementation", project(":kactor:proto-mailbox"))
    add("implementation", project(":kactor:proto-persistence"))
    add("implementation", project(":kactor:proto-cluster"))

    // Actor integration
    add("implementation", project(":kastrax-actor"))

    // 添加其他可能需要的依赖
    add("implementation", "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    add("implementation", "io.ktor:ktor-client-core:3.1.2")
    add("implementation", "io.ktor:ktor-client-okhttp:3.1.2")
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

// 通用任务，用于运行任何示例
tasks.register<JavaExec>("runExample") {
    group = "examples"
    description = "Run any example by providing the example name as a parameter"
    classpath = sourceSets["main"].runtimeClasspath

    // 默认使用DynamicWorkflowExample作为示例
    val exampleName = project.findProperty("example")?.toString() ?: "workflow.DynamicWorkflowExample"
    mainClass.set("ai.kastrax.examples.${exampleName}Kt")

    jvmArgs = listOf("-Xms512m", "-Xmx1g")

    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    // 设置OpenAI API密钥（如果需要）
    environment("OPENAI_API_KEY", project.findProperty("openaiApiKey")?.toString() ?: "")

    doFirst {
        println("Running example: $exampleName")
        println("Using classpath: ${classpath.asPath}")
    }
}

sourceSets {
    main {
        kotlin {
            // 不排除任何文件，允许所有示例编译
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

// 注意：此任务已在第 76-83 行定义，此处注释掉以避免重复
// tasks.register<JavaExec>("runAdaptiveAgentExample") {
//     group = "application"
//     description = "Run the AdaptiveAgentExample example"
//     classpath = sourceSets["main"].runtimeClasspath
//     mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExampleKt")
//     jvmArgs = listOf("-Xms512m", "-Xmx1g")
// }

// Task to run AdaptiveAgentExample
tasks.register<JavaExec>("runAdaptiveAgentExampleNew") {
    group = "application"
    description = "Run the AdaptiveAgentExample example"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.agent.AdaptiveAgentExampleKt")
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

// Task to run WorkingMemoryExample
tasks.register<JavaExec>("runWorkingMemoryExample") {
    group = "examples"
    description = "Run the WorkingMemoryExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.memory.WorkingMemoryExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running WorkingMemoryExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run RAGExample
tasks.register<JavaExec>("runRAGExample") {
    group = "examples"
    description = "Run the RAGExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RAGExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running RAGExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run RAGWorkflowExample
tasks.register<JavaExec>("runRAGWorkflowExample") {
    group = "examples"
    description = "Run the RAGWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.RAGWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running RAGWorkflowExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run FastEmbedRAGExample
tasks.register<JavaExec>("runFastEmbedRAGExample") {
    group = "examples"
    description = "Run the FastEmbedRAGExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.FastEmbedRAGExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running FastEmbedRAGExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run DynamicWorkflowExample
tasks.register<JavaExec>("runDynamicWorkflowExample") {
    group = "examples"
    description = "Run the DynamicWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.DynamicWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running DynamicWorkflowExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run WorkflowExample
tasks.register<JavaExec>("runWorkflowExample") {
    group = "examples"
    description = "Run the WorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.WorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running WorkflowExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}

// Task to run AdvancedWorkflowExample
tasks.register<JavaExec>("runAdvancedWorkflowExample") {
    group = "examples"
    description = "Run the AdvancedWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.AdvancedWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    // 设置DeepSeek API密钥
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
    doFirst {
        println("Running AdvancedWorkflowExample...")
        println("Using classpath: ${classpath.asPath}")
    }
}
