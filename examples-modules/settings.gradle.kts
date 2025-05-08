rootProject.name = "examples-modules"

// 包含所有子模块
include(
    "workflow",
    "rag",
    "memory",
    "tools",
    "agent",
    "other",
    "plugin"
)

// 包含主项目的模块
includeBuild("..") {
    dependencySubstitution {
        // 替换主项目的模块依赖
        substitute(module("ai.kastrax:kastrax-core")).using(project(":kastrax-core"))
        substitute(module("ai.kastrax:kastrax-memory-api")).using(project(":kastrax-memory-api"))
        substitute(module("ai.kastrax:kastrax-memory-impl")).using(project(":kastrax-memory-impl"))
        substitute(module("ai.kastrax:kastrax-zod")).using(project(":kastrax-zod"))
        substitute(module("ai.kastrax:kastrax-rag")).using(project(":kastrax-rag"))
        substitute(module("ai.kastrax:kastrax-deepseek")).using(project(":kastrax-integrations:kastrax-deepseek"))
        substitute(module("ai.kastrax:kastrax-openai")).using(project(":kastrax-integrations:kastrax-openai"))
        substitute(module("ai.kastrax:fastembed-kotlin")).using(project(":fastembed-kotlin"))
        substitute(module("ai.kastrax:kastrax-actor")).using(project(":kastrax-actor"))

        // Kactor模块
        substitute(module("ai.kastrax:proto-actor")).using(project(":kactor:proto-actor"))
        substitute(module("ai.kastrax:proto-router")).using(project(":kactor:proto-router"))
        substitute(module("ai.kastrax:proto-remote")).using(project(":kactor:proto-remote"))
        substitute(module("ai.kastrax:proto-mailbox")).using(project(":kactor:proto-mailbox"))
        substitute(module("ai.kastrax:proto-persistence")).using(project(":kactor:proto-persistence"))
        substitute(module("ai.kastrax:proto-cluster")).using(project(":kactor:proto-cluster"))
    }
}
