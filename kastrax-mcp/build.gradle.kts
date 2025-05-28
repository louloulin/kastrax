plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

group = "ai.kastrax"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // KastraX 核心
    implementation(project(":kastrax-core"))

    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Kotlin 序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Ktor 客户端
    val ktorVersion = "3.1.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor 服务器
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // 日志
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // 测试
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

// Add run tasks for examples
tasks.register<JavaExec>("runSimpleMCPExample") {
    group = "examples"
    description = "Run the SimpleMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.SimpleMCPExampleKt")
}

tasks.register<JavaExec>("runComprehensiveMCPExample") {
    group = "examples"
    description = "Run the ComprehensiveMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.ComprehensiveMCPExampleKt")
}

tasks.register<JavaExec>("runAdvancedMCPAgentExample") {
    group = "examples"
    description = "Run the AdvancedMCPAgentExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.AdvancedMCPAgentExampleKt")
}

tasks.register<JavaExec>("runCustomTransportMCPExample") {
    group = "examples"
    description = "Run the CustomTransportMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.CustomTransportMCPExampleKt")
}

tasks.register<JavaExec>("runWeatherApiMCPExample") {
    group = "examples"
    description = "Run the WeatherApiMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.WeatherApiMCPExampleKt")
}

tasks.register<JavaExec>("runPublicApiMCPExample") {
    group = "examples"
    description = "Run the PublicApiMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.PublicApiMCPExampleKt")
}

tasks.register<JavaExec>("runRemoteMCPExample") {
    group = "examples"
    description = "Run the RemoteMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.RemoteMCPExampleKt")
}

tasks.register<JavaExec>("runMastraFetchMCPExample") {
    group = "examples"
    description = "Run the MastraFetchMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.MastraFetchMCPExampleKt")
}

tasks.register<JavaExec>("runRealMastraMCPExample") {
    group = "examples"
    description = "Run the RealMastraMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.RealMastraMCPExampleKt")
}

tasks.register<JavaExec>("runDockerFetchMCPExample") {
    group = "examples"
    description = "Run the DockerFetchMCPExample"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("ai.kastrax.mcp.examples.DockerFetchMCPExampleKt")
}

tasks.dokkaHtml {
    outputDirectory.set(file("$buildDir/dokka"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

// Publishing configuration moved to global setup
