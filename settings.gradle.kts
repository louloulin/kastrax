pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google() // 添加Google仓库，用于Android库
    }

    // 定义插件版本
    plugins {
        kotlin("multiplatform") version "1.9.22"
        kotlin("jvm") version "1.9.22"
        kotlin("plugin.serialization") version "1.9.22"
        id("com.android.library") version "8.1.0"
        id("org.graalvm.buildtools.native") version "0.10.6"
    }
}

// 为kastrax-codex模块配置版本目录
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("kastrax-codex/gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kastrax"

include(":kastrax-core")
include(":kastrax-zod")
include(":kastrax-memory-api")
include(":kastrax-memory-impl")
include(":kastrax-rag")
//include(":kastrax-rag-backup")
include(":kastrax-rag-test")
include(":kastrax-evals")
include(":kastrax-cli")
include(":kastrax-deployer")
include(":kastrax-observability")
include(":kastrax-server")
include(":kastrax-server:common")
include(":kastrax-server:spring")
// 暂时禁用 Ktor 模块，因为它有依赖问题
include(":kastrax-server:ktor")
include(":kastrax-server:quarkus")
include(":kastrax-integrations:kastrax-openai")
include(":kastrax-integrations:kastrax-deepseek")
include(":kastrax-integrations:kastrax-anthropic")
include(":kastrax-integrations:kastrax-gemini")
// include(":kastrax-integrations:kastrax-mistral") // 暂时禁用，因为模块不存在
// 暂时禁用 MCP 模块，因为它仍在开发中
 include(":kastrax-mcp")
// Data Source modules
include(":kastrax-datasource-common")
include(":kastrax-datasource")
include(":kastrax-datasource:kastrax-database")
include(":kastrax-datasource:kastrax-api")
include(":kastrax-datasource:kastrax-filesystem")
include(":kastrax-datasource:kastrax-nosql")
include(":kastrax-datasource:kastrax-cloud-storage")

include(":fastembed-kotlin")
// 已修复 AdvancedWorkflowExample.kt 文件的编译错误
// 其他示例文件还有错误，但我们只需要编译这个文件
include(":examples")
include(":zod-test")
include(":kastrax-examples")

include(":kastrax-app")
include(":kastrax-agent-templates")
include(":kastrax-a2a")
include(":kastrax-a2x")
include(":kastrax-actor")
include(":kastrax-store")
include(":kactor")

// Include kactor subprojects
include(":kactor:proto-mailbox")
include(":kactor:proto-actor")
include(":kactor:proto-router")
include(":kactor:proto-remote")
include(":kactor:proto-cluster")
include(":kactor:proto-persistence")
include(":kactor:proto-stream")
include(":kactor:proto-benchmarks")
include(":kactor:proto-plugin")
include(":kactor:proto-plugin-examples")
include(":kactor:proto-cluster-libp2p")
include(":kactor:examples")
include(":kactor:native-example")
include(":kactor:simple-native")
include(":kactor:benchmark-native")
include(":kactor:native-minimal")
// include(":kactor:dataflare") // 目录不存在

// Native modules - 暂时注释掉，避免构建错误
// include(":kastrax-native")

// GraalVM Native modules
include(":graal-native")
// include(":graal-native:sdk-rust")
// include(":graal-native:sdk-go")
// include(":graal-native:sdk-js")

// Examples modules
include(":examples-modules")
include(":examples-modules:workflow")
include(":examples-modules:rag")
include(":examples-modules:memory")
include(":examples-modules:tools")
include(":examples-modules:agent")
include(":examples-modules:other")
include(":examples-modules:plugin")
include(":examples-modules:hello")
include(":examples-modules:hello-fixed")
include(":examples-modules:agent-hello")
include(":examples-modules:agent-simple")
include(":examples-modules:tools-hello")
include(":examples-modules:tools-simple")
include(":examples-modules:workflow-hello")
include(":examples-modules:rag-hello")
include(":examples-modules:workflow-simple")
include(":examples-modules:workflow-fixed")
include(":kastrax-store:memory")
include(":kastrax-store:chroma")
include(":kastrax-store:pinecone")
include(":kastrax-store:qdrant")
include(":kastrax-store:postgres")
include(":kastrax-store:lancedb")
