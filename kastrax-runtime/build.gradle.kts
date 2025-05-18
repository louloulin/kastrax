plugins {
    kotlin("jvm") apply false
    id("org.jetbrains.intellij.platform") version "2.5.0" apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        // IntelliJ Platform repositories
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }

    dependencies {
        "implementation"(kotlin("stdlib"))
        "testImplementation"(kotlin("test"))
    }
}
