// 这是一个占位符 build.gradle.kts 文件
// 将 kastrax-codex 作为 kastrax 的子项目，但暂时不构建它
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.RunIdeTask
import java.io.FileInputStream
import java.util.*

plugins {
    id("codegpt.java-conventions")
    id("org.jetbrains.changelog") version "2.2.1"
    id("com.google.protobuf") version "0.9.4"
}

group = "ai.kastrax"
version = "0.1.0-SNAPSHOT"





val localPropertiesFile = file("local.properties")
val env = environment("env").getOrNull()

fun loadProperties(filename: String): Properties = Properties().apply {
    load(FileInputStream(filename))
}

val localProperties: Properties? = if (localPropertiesFile.exists()) {
    loadProperties("local.properties")
} else {
    null
}

val customIdePath: String? = localProperties?.getProperty("customIdePath")

fun properties(key: String): Provider<String> {
    if ("win-arm64" == env) {
        val property = loadProperties("gradle-win-arm64.properties").getProperty(key)
            ?: return providers.gradleProperty(key)
        return providers.provider { property }
    }
    return providers.gradleProperty(key)
}

fun environment(key: String) = providers.environmentVariable(key)



group = properties("pluginGroup").get()
version = properties("pluginVersion").get() + "-" + properties("pluginSinceBuild").get()

checkstyle {
    toolVersion = "10.15.0"
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))
    plugins.set(listOf("java", "PythonCore:241.14494.240", "Git4Idea", "org.jetbrains.kotlin"))
}

changelog {
    groups.empty()
    repositoryUrl.set(properties("pluginRepositoryUrl"))
}

dependencies {
    implementation(project(":codegpt-telemetry"))
    implementation(project(":codegpt-treesitter"))

    // Kastrax 依赖
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-memory-api:0.1.0")
    implementation("ai.kastrax:kastrax-memory-impl:0.1.0")
    implementation("ai.kastrax:kastrax-zod:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-deepseek:0.1.0")

    // 原有依赖
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.18.3"))
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8") {
        // vulnerable transitive dependency
        exclude(group = "org.jsoup", module = "jsoup")
    }
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("com.knuddels:jtokkit:1.1.0")
    implementation("io.grpc:grpc-protobuf:1.71.0")
    implementation("io.grpc:grpc-stub:1.71.0")
    implementation("io.grpc:grpc-netty-shaded:1.71.0")

    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // 测试依赖
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}


/**
 * Task to run a custom IntelliJ IDEA sandbox.
 *
 * This task launches a custom IntelliJ IDEA installation using the path specified in the
 * 'customIdePath' property from local.properties.
 *
 * IMPORTANT:
 * - On macOS, the path must include the 'Contents' directory (e.g., /Applications/IntelliJ IDEA.app/Contents).
 * - For Windows or Linux, specify the appropriate path to the IntelliJ IDEA installation.
 *
 * Usage:
 *   ./gradlew runCustomIde
 */
if (customIdePath != null) {
    tasks.register<RunIdeTask>("runCustomIde") {
        group = "intellij"
        description = "Start custom idea sandbox"
        ideDir.set(file(customIdePath))
        environment("ENVIRONMENT", "LOCAL")
        autoReloadPlugins.set(false)
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    verifyPlugin {
        enabled = true
    }

    runPluginVerifier {
        enabled = true
    }

    patchPluginXml {
        enabled = true
        version.set(properties("pluginVersion").get() + "-" + properties("pluginSinceBuild").get())
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        pluginDescription.set(providers.fileContents(layout.projectDirectory.file("DESCRIPTION.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in DESCRIPTION.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        })

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes.set(properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        })
    }


    signPlugin {
        enabled = true
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    buildPlugin {
        enabled = true
    }

    publishPlugin {
        enabled = true
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        channels.set(listOf("stable"))
    }

    runIde {
        enabled = true
        environment("ENVIRONMENT", "LOCAL")
        autoReloadPlugins.set(false) // is triggered when building llama server
    }

    test {
        exclude("**/testsupport/*")
        useJUnitPlatform()
        testLogging {
            events("started", "passed", "skipped", "failed")
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.71.0"
        }
    }
    generateProtoTasks {
        all()
            .forEach {
                it.plugins {
                    create("grpc")
                }
            }
    }
}
