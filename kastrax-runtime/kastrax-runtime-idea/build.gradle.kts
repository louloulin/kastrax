plugins {
    id("org.jetbrains.intellij.platform")
}

intellij {
    version.set("2024.1.2")
    type.set("IC")
    plugins.set(listOf("java", "org.jetbrains.kotlin"))
}

dependencies {
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // IntelliJ Platform dependencies are provided by the intellij plugin

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
