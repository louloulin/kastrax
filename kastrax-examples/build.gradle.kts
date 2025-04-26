plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-datasource:kastrax-filesystem"))
    implementation(project(":kastrax-datasource:kastrax-database"))
    implementation(project(":kastrax-agent-templates"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))

    // 添加SQLite依赖
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}

application {
    mainClass.set(System.getProperty("mainClass") ?: "ai.kastrax.examples.dataflow.DataFlowVisualizerExample")
}

tasks.withType<JavaExec> {
    if (project.hasProperty("args")) {
        args = project.property("args").toString().split("\\s+".toRegex())
    }
}
