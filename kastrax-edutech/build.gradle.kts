plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("io.gitlab.arturbosch.detekt")
    id("com.vanniktech.maven.publish")
}

dependencies {
    // Kastrax Core Dependencies
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-zod"))
    implementation(project(":kastrax-actor"))

    // Kotlin Standard Libraries
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Coroutines for Actor Model
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Serialization for Data Models
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // HTTP Client for LMS Integration
    val ktorVersion = "3.1.2"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Database Support
    implementation("org.jetbrains.exposed:exposed-core:0.44.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.44.1")
    implementation("org.postgresql:postgresql:42.6.0")

    // Redis for Caching
    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")

    // Vector Database Support
    implementation("dev.langchain4j:langchain4j-chroma:0.25.0")

    // Machine Learning Libraries
    implementation("org.jetbrains.kotlinx:multik-core:0.2.2")
    implementation("org.jetbrains.kotlinx:multik-default:0.2.2")

    // Testing Dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("org.testcontainers:junit-jupiter:1.19.1")

    // Integration Testing
    testImplementation(project(":kastrax-integrations:kastrax-deepseek"))
    testImplementation(project(":kastrax-integrations:kastrax-openai"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
    
    // Configure test execution
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // Memory settings for tests
    minHeapSize = "512m"
    maxHeapSize = "2g"
    
    // Test reporting
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xjsr305=strict",
            "-Xcontext-receivers",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}

detekt {
    config = files("${rootProject.projectDir}/detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = true
    ignoreFailures = true
}

// Maven Publishing Configuration
mavenPublishing {
    pom {
        name.set("Kastrax EduTech")
        description.set("Educational Technology AI components for Kastrax Framework")
        url.set("https://github.com/louloulin/kastrax")
        
        licenses {
            license {
                name.set("Apache License 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        
        developers {
            developer {
                id.set("louloulin")
                name.set("louloulin")
                email.set("729883852@qq.com")
            }
        }
        
        scm {
            connection.set("scm:git:https://github.com/louloulin/kastrax.git")
            developerConnection.set("scm:git:ssh://github.com/louloulin/kastrax.git")
            url.set("https://github.com/louloulin/kastrax")
        }
    }
}
