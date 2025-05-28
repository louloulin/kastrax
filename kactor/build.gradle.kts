import org.gradle.jvm.tasks.Jar
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

buildscript {
    // val kotlinVersion by extra("1.9.22")

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.netflix.nebula:nebula-release-plugin:20.2.0")
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
    }
}

plugins {
    id("com.github.ben-manes.versions") version "0.50.0"
    id("org.jetbrains.kotlin.jvm") apply false
    id("com.google.protobuf") version "0.9.4" apply false
    id("org.graalvm.buildtools.native") apply false
}

allprojects {
    group = "actor.proto"

    apply(plugin = "idea")
    // apply(plugin = "nebula.release")

    // Create the artifact extension for all projects
    // extensions.create<ArtifactExtension>("artifact")
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "com.google.protobuf")

    extra["kotlinVersion"] = "1.9.22"
    extra["coroutinesVersion"] = "1.8.0"
    extra["protobufVersion"] = "3.25.3"
    extra["grpcVersion"] = "1.62.2"
    extra["slf4jVersion"] = "2.0.12"
    extra["awaitilityVersion"] = "4.2.1"
    extra["junitPlatformVersion"] = "5.10.2"
    extra["kotlinLoggingVersion"] = "3.0.5"
    extra["jctoolsVersion"] = "4.0.5"
    extra["javaxAnnotationsVersion"] = "1.3.2"

    repositories {
        mavenCentral()
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${project.extra["kotlinVersion"]}")

        add("testImplementation", "org.junit.jupiter:junit-jupiter-api:${project.extra["junitPlatformVersion"]}")
        add("testRuntimeOnly", "org.junit.jupiter:junit-jupiter-engine:${project.extra["junitPlatformVersion"]}")
    }

    tasks.withType<JavaCompile> {
        targetCompatibility = JavaVersion.VERSION_17.toString()
        sourceCompatibility = JavaVersion.VERSION_17.toString()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            allWarningsAsErrors = false
        }
    }

    configure<com.google.protobuf.gradle.ProtobufExtension> {
        protoc {
            artifact = "com.google.protobuf:protoc:${project.extra["protobufVersion"]}"
        }
    }

    apply(plugin = "java")

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:${project.extra["junitPlatformVersion"]}")
        "testRuntimeOnly"("org.junit.jupiter:junit-jupiter-engine:${project.extra["junitPlatformVersion"]}")
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        // Configure JVM arguments for tests
        jvmArgs = listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED"
        )
    }
}

configure(subprojects.filter { it.name != "examples" }) {
    // apply(plugin = "maven-publish") // 使用全局配置
    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        // Update to latest version that supports Java 23
        toolVersion = "0.8.12"
    }

    // Configure Java toolchain for all subprojects
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(project.the<SourceSetContainer>()["main"].allSource)
        dependsOn(tasks["classes"])
    }

    // PublishingExtension配置已移至全局配置

    tasks.named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        dependsOn(tasks.named("test"))
    }

    tasks.named("check") {
        dependsOn("jacocoTestReport")
    }
}

// tasks.wrapper {
//     gradleVersion = "8.7"
// }
