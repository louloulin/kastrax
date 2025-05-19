plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    implementation(project(":kastrax-runtime:kastrax-runtime-api"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Android dependencies
    compileOnly("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    compileOnly("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    compileOnly("androidx.lifecycle:lifecycle-common:2.6.2")

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.20")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
