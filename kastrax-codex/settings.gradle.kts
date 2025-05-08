// Kastrax-Codex 设置文件 - 使用复合构建方式与 Kastrax 集成

rootProject.name = "kastrax-codex"

// 包含子项目
include(":codegpt-treesitter")
include(":codegpt-telemetry")

// 复合构建配置 - 包含 kastrax 项目
includeBuild("/Users/louloulin/Documents/linchong/agent/kastra/kastrax") {
    dependencySubstitution {
        // 替换 kastrax 核心模块的依赖
        substitute(module("ai.kastrax:kastrax-core")).using(project(":kastrax-core"))
        substitute(module("ai.kastrax:kastrax-memory-api")).using(project(":kastrax-memory-api"))
        substitute(module("ai.kastrax:kastrax-memory-impl")).using(project(":kastrax-memory-impl"))
        substitute(module("ai.kastrax:kastrax-zod")).using(project(":kastrax-zod"))
        substitute(module("ai.kastrax:kastrax-integrations:kastrax-deepseek")).using(project(":kastrax-integrations:kastrax-deepseek"))
    }
}