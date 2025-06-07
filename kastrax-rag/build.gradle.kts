plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    application
}

// GraalPy configuration is handled manually in the code

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://repository.apache.org/content/repositories/snapshots/")
}



dependencies {
    // Project dependencies
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-store"))
    implementation(project(":fastembed-kotlin"))
    implementation(project(":kastrax-evals"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // HTTP Client
    val ktorVersion = "3.1.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")


    // HTML Parsing
    implementation("org.jsoup:jsoup:1.16.1")

    // Vector Similarity
    implementation("org.apache.commons:commons-math3:3.6.1")

    // PDF Processing
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    // CSV Processing
    implementation("org.apache.commons:commons-csv:1.10.0")

    // Excel Processing
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // XML Processing
    implementation("org.dom4j:dom4j:2.1.4")

    // JSON 处理
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    // GraalVM Polyglot API
    implementation("org.graalvm.sdk:graal-sdk:24.2.0")
    implementation("org.graalvm.truffle:truffle-api:24.2.0")

    // PDF processing
    implementation("org.apache.pdfbox:pdfbox:2.0.29")

    // CSV processing
    implementation("org.apache.commons:commons-csv:1.10.0")

    // HTML processing
    implementation("org.jsoup:jsoup:1.16.2")

    // Token counting
    implementation("com.knuddels:jtokkit:0.6.1")

    // FAISS (optional, for vector search)
    // Note: FAISS JNI bindings need to be installed separately
    // Uncomment the following line when FAISS JNI bindings are available
    // compileOnly(files("libs/faiss-jni.jar"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// Publishing configuration moved to global setup

application {
    mainClass.set("ai.kastrax.rag.examples.RagVerificationExample")
}

// 配置 vanniktech maven publish 插件
mavenPublishing {
    pom {
        name.set("Kastrax RAG")
        description.set("Retrieval-Augmented Generation components for Kastrax")
    }
}
