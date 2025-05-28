import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.serialization") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.1.10" apply false
    kotlin("plugin.allopen") version "2.1.10" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.5" apply false
    id("org.springframework.boot") version "3.2.5" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("io.quarkus") version "3.9.0" apply false
    id("io.ktor.plugin") version "3.1.2" apply false
}

allprojects {
    group = "ai.kastrax"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    plugins.withId("io.gitlab.arturbosch.detekt") {
        configure<DetektExtension> {
            config = files("${rootProject.projectDir}/detekt.yml")
            buildUponDefaultConfig = true
            autoCorrect = true
            ignoreFailures = true
        }
    }

    // 为非测试模块配置 maven-publish
    if (!project.name.contains("test") && project != rootProject) {
        apply(plugin = "maven-publish")
        
        plugins.withId("java") {
            plugins.withId("maven-publish") {
                configure<PublishingExtension> {
                    publications {
                        // 只有当没有现有的maven publication时才创建
                        if (publications.findByName("maven") == null) {
                            create<MavenPublication>("maven") {
                                from(components["java"])
                                
                                // 配置POM信息以满足Maven Central要求
                                pom {
                                    name.set(project.name)
                                    description.set(project.description ?: "Kastrax AI Framework - ${project.name}")
                                    url.set("https://github.com/kastrax/kastrax")
                                    
                                    licenses {
                                        license {
                                            name.set("Apache License 2.0")
                                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                                        }
                                    }
                                    
                                    developers {
                                        developer {
                                            id.set("lumos-team")
                                            name.set("louloulin")
                                            email.set("729883852@qq.com")
                                        }
                                    }
                                    
                                    scm {
                                        connection.set("scm:git:https://github.com/louloulin/kastrax.git")
                                         developerConnection.set("scm:git:ssh://github.com/louloulin/kastrax.git")
                                        url.set("https://github.com/louloulin/kastrax.git/tree/main")
                                    }
                                }
                            }
                        }
                    }
                    
                    repositories {
                        maven {
                            name = "OSSRH"
                            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                            
                            credentials {
                                username = findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
                                password = findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
                            }
                        }
                    }
                }
                
                // 为使用enforced platform的模块抑制验证错误
                tasks.withType<GenerateModuleMetadata>().configureEach {
                    suppressedValidationErrors.add("enforced-platform")
                }
            }
        }
        
        // 配置签名
        plugins.withId("signing") {
            configure<SigningExtension> {
                val signingKey = findProperty("signing.keyId") as String? ?: System.getenv("SIGNING_KEY_ID")
                val signingPassword = findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")
                val signingSecretKey = findProperty("signing.secretKeyRingFile") as String? ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")
                
                if (signingKey != null && signingPassword != null && signingSecretKey != null) {
                    useInMemoryPgpKeys(signingKey, signingPassword)
                    sign(extensions.getByType<PublishingExtension>().publications)
                }
            }
        }
    }
}
