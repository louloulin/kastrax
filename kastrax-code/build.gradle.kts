import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.process.CommandLineArgumentProvider

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm") version "2.1.10" // Kotlin support
    id("org.jetbrains.intellij.platform") version "2.5.0" // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.changelog") version "2.2.1" // Gradle Changelog Plugin
    id("org.jetbrains.qodana") version "2024.3.4" // Gradle Qodana Plugin
    id("org.jetbrains.kotlinx.kover") version "0.9.1" // Gradle Kover Plugin
}

group = "ai.kastrax.code"
version = "0.1.0"

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    // Kastrax 依赖
    implementation(project(":kastrax-core"))
    implementation(project(":kastrax-codebase"))
    implementation(project(":kastrax-rag"))
    implementation(project(":kastrax-memory-api"))
    implementation(project(":kastrax-memory-impl"))
    implementation(project(":kastrax-integrations:kastrax-deepseek"))
    // implementation(project(":kastrax-tools")) // 暂时移除不存在的依赖

    // IntelliJ平台UI依赖
    // 使用IntelliJ Platform Gradle Plugin提供的依赖

    // Kotlin 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // 日志
    // 使用IntelliJ平台的日志系统而不是Kotlin日志
    // 已经通过 intellijPlatform 依赖提供

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")
    testImplementation("io.mockk:mockk:1.13.8")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create("IC", "2024.2.5")

        // Plugin Dependencies - bundled plugins
        bundledPlugins(listOf("com.intellij.java", "org.jetbrains.kotlin"))

        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = "KastraX Code"
        version = "0.1.0"

        // Plugin description
        description = "AI-powered coding assistant based on KastraX framework"

        // Change notes
        changeNotes = "Initial release of KastraX Code"

        ideaVersion {
            sinceBuild = "232"
            untilBuild = "242.*"
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // Release channel
        channels = listOf("default")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = "https://github.com/kastrax/kastrax"
}

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    withType<Wrapper> {
        gradleVersion = "8.13"
    }

    named("publishPlugin") {
        dependsOn("patchChangelog")
    }

    // 直接依赖 patchChangelog 任务，不需要引用它
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
