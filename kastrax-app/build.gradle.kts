plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "ai.kastrax.app"
version = "0.1.0"

repositories {
    mavenCentral()
    // 如果需要，添加本地仓库
    // mavenLocal()
}

dependencies {
    // KastraX 核心依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-zod"))

    // KastraX 集成
    implementation(project(":kastrax-integrations:kastrax-openai"))
    implementation(project(":kastrax-integrations:kastrax-anthropic"))

    // 日志
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 工具库
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    // 测试
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("ai.kastrax.app.MainKt")
}
