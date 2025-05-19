dependencies {
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // IntelliJ Platform dependencies
    compileOnly("com.jetbrains.intellij.platform:core-api:233.13135.103")
    compileOnly("com.jetbrains.intellij.platform:util-coroutines:233.13135.103")
    compileOnly("com.jetbrains.intellij.platform:util:233.13135.103")
    compileOnly("com.jetbrains.intellij.platform:concurrency:233.13135.103")
    compileOnly("com.jetbrains.intellij.platform:extensions:233.13135.103")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.3")
}
