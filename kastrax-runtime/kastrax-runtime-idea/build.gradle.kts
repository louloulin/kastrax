dependencies {
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("com.jetbrains.intellij.platform:core-api:233.13135.103")
    compileOnly("com.jetbrains.intellij.platform:util-coroutines:233.13135.103")
    
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
