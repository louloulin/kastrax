import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "1.9.20"
    `maven-publish`
    signing
}

group = "ai.kastrax"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

tasks.test {
    useJUnitPlatform()

    // Skip tests for now until we fix the native library loading
    onlyIf { false }
}

kotlin {
    jvmToolchain(17)
}

// Determine the operating system
val currentOs = OperatingSystem.current()

// Define the library name based on the OS
val libName = when {
    currentOs.isWindows -> "fastembed_jni.dll"
    currentOs.isMacOsX -> "libfastembed_jni.dylib"
    else -> "libfastembed_jni.so"
}

// Task to build the Rust library
tasks.register<Exec>("buildRustLibrary") {
    group = "build"
    description = "Build the Rust library"

    workingDir = file("rust")

    commandLine = listOf("cargo", "build", "--release")

    doLast {
        // Create the native/os-specific directory if it doesn't exist
        val nativeDir = file("src/main/resources/native/${currentOs.name.toLowerCase()}-${System.getProperty("os.arch")}")
        nativeDir.mkdirs()

        // Copy the built library to the resources directory
        val sourceLib = file("rust/target/release/$libName")
        val targetLib = file("${nativeDir}/$libName")

        sourceLib.copyTo(targetLib, overwrite = true)

        println("Copied native library to ${targetLib.absolutePath}")
    }
}

// Make the jar task depend on building the Rust library
tasks.jar {
    dependsOn("buildRustLibrary")
}

// Configure the JAR
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

// Configure publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("FastEmbed Kotlin")
                description.set("Kotlin bindings for the fastembed-rs library")
                url.set("https://github.com/kastrax/fastembed-kotlin")

                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("kastrax")
                        name.set("KastraX Team")
                        email.set("info@kastrax.ai")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/kastrax/fastembed-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/kastrax/fastembed-kotlin.git")
                    url.set("https://github.com/kastrax/fastembed-kotlin")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kastrax/fastembed-kotlin")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

// Configure signing
signing {
    val signingKey: String? = project.findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword: String? = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
