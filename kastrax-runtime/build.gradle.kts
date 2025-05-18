plugins {
    kotlin("jvm") apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    repositories {
        mavenCentral()
    }
    
    dependencies {
        "implementation"(kotlin("stdlib"))
        "testImplementation"(kotlin("test"))
    }
}
