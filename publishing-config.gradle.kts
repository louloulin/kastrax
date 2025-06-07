/**
 * 共享的发布配置文件
 * 可以在子模块中通过 apply from: "$rootDir/publishing-config.gradle.kts" 使用
 */

// 为特定模块自定义发布配置的示例
if (project.name == "kastrax-core") {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax Core")
            description.set("Core components and APIs for the Kastrax AI Framework")
        }
    }
}

if (project.name == "kastrax-rag") {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax RAG")
            description.set("Retrieval-Augmented Generation components for Kastrax")
        }
    }
}

if (project.name == "kastrax-memory-api") {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax Memory API")
            description.set("Memory management API for Kastrax AI Framework")
        }
    }
}

if (project.name == "kastrax-memory-impl") {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax Memory Implementation")
            description.set("Memory management implementation for Kastrax AI Framework")
        }
    }
}

// 集成模块的配置
if (project.path.startsWith(":kastrax-integrations")) {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax ${project.name.removePrefix("kastrax-").capitalize()} Integration")
            description.set("${project.name.removePrefix("kastrax-").capitalize()} integration for Kastrax AI Framework")
        }
    }
}

// 服务器模块的配置
if (project.path.startsWith(":kastrax-server")) {
    configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
        pom {
            name.set("Kastrax Server ${project.name.removePrefix("kastrax-server-").capitalize()}")
            description.set("Server implementation using ${project.name.removePrefix("kastrax-server-")} for Kastrax")
        }
    }
}
