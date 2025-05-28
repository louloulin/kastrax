dependencies {
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // IntelliJ Platform dependencies
    compileOnly("com.jetbrains.intellij.platform:core-api:223.8836.41")
    compileOnly("com.jetbrains.intellij.platform:util-coroutines:223.8836.41")
    compileOnly("com.jetbrains.intellij.platform:util:223.8836.41")
    compileOnly("com.jetbrains.intellij.platform:concurrency:223.8836.41")
    compileOnly("com.jetbrains.intellij.platform:extensions:223.8836.41")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3")
}
