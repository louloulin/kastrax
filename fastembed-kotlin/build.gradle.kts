import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    application
}

group = "ai.kastrax"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Coroutines for async API
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.test {
    useJUnitPlatform()

    // Make sure the native library is built and copied before running tests
    dependsOn("copyNativeLibrary")

    // Set system property for test logging
    systemProperty("java.util.logging.config.file", "${projectDir}/src/test/resources/logging.properties")

    // Set system property for test mode
    systemProperty("ai.kastrax.fastembed.test.mode", "true")

    // Increase memory for tests
    maxHeapSize = "1g"

    // Skip tests that require the native library for now
    filter {
        excludeTestsMatching("ai.kastrax.fastembed.TextEmbeddingTest")
        excludeTestsMatching("ai.kastrax.fastembed.AsyncTextEmbeddingTest")
        excludeTestsMatching("ai.kastrax.fastembed.LibraryLoadTest")
    }
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

// Define the OS-specific directory name
val osName = currentOs.name.lowercase().replace(" ", "-")
val osArch = System.getProperty("os.arch")
val osDir = "$osName-$osArch"

// Define source and target paths as strings to avoid script object references
val rustSrcDir = "rust/src"
val cargoToml = "rust/Cargo.toml"
val rustTargetDir = "rust/target/release"
val nativeDirPath = "src/main/resources/native/$osDir"

// Task to build the Rust library
tasks.register<Exec>("buildRustLibrary") {
    group = "build"
    description = "Build the Rust library"

    workingDir = file("rust")
    commandLine = listOf("cargo", "build", "--release")

    // Define inputs and outputs for proper task caching
    inputs.dir(rustSrcDir)
    inputs.file(cargoToml)
    outputs.file(rustTargetDir + "/" + libName)
}

// Disable configuration cache for this module
tasks.withType<Copy>().configureEach {
    notCompatibleWithConfigurationCache("Copy tasks in this module have configuration cache issues")
}

// Use Copy task to copy the native library
tasks.register<Copy>("copyNativeLibrary") {
    group = "build"
    description = "Copy the built Rust library to resources"

    dependsOn("buildRustLibrary")

    // Configure the copy task
    from("$rustTargetDir/$libName")
    into(nativeDirPath)
    rename { libName }

    // Make sure the directory exists
    doFirst {
        mkdir(nativeDirPath)
    }

    // Log the copy operation
    doLast {
        logger.lifecycle("Copied native library to $nativeDirPath/$libName")
    }
}

// Make processResources depend on copyNativeLibrary
tasks.named("processResources") {
    dependsOn("copyNativeLibrary")
}

// Make the jar task depend on copying the native library
tasks.jar {
    dependsOn("copyNativeLibrary")
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



// Configure signing
signing {
    val signingKey: String? = project.findProperty("signing.key") as String? ?: System.getenv("SIGNING_KEY")
    val signingPassword: String? = project.findProperty("signing.password") as String? ?: System.getenv("SIGNING_PASSWORD")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
