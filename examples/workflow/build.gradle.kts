plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    // 项目依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-store"))
    implementation(project(":kastrax-store:memory"))
    implementation(project(":fastembed-kotlin"))

    // Kotlin 依赖
    implementation(kotlin("stdlib"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // HTTP 客户端依赖
    implementation("io.ktor:ktor-client-core:3.1.2")
    implementation("io.ktor:ktor-client-okhttp:3.1.2")

    // 日志依赖
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set("ai.kastrax.examples.workflow.SimpleWorkflowExampleKt")
}
