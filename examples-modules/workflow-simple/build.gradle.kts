plugins {
    kotlin("jvm")
    application
}

dependencies {
    // 核心依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    
    // 其他依赖
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
}

application {
    mainClass.set("ai.kastrax.examples.workflow.simple.SimpleWorkflowExampleKt")
}

tasks.register<JavaExec>("runSimpleWorkflow") {
    group = "examples"
    description = "Run the SimpleWorkflowExample"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("ai.kastrax.examples.workflow.simple.SimpleWorkflowExampleKt")
    jvmArgs = listOf("-Xms512m", "-Xmx1g")
    environment("DEEPSEEK_API_KEY", "sk-85e83081df28490b9ae63188f0cb4f79")
}
