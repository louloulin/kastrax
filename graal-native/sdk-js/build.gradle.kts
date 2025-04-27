plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled.set(true)
                }
            }
        }
        nodejs {
            // Node.js specific configuration
        }
        binaries.executable()
    }
    
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

// Task to generate NPM package
tasks.register<Copy>("generateNpmPackage") {
    dependsOn("jsJar")
    
    from("${projectDir}/package.json")
    from("${projectDir}/README.md")
    from("${layout.buildDirectory.get()}/js/packages/${project.name}")
    
    into("${layout.buildDirectory.get()}/npm")
}

// Task to publish to NPM
tasks.register<Exec>("publishNpm") {
    dependsOn("generateNpmPackage")
    
    workingDir = file("${layout.buildDirectory.get()}/npm")
    commandLine = listOf("npm", "publish")
}
