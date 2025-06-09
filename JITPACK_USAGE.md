# 📦 Kastrax JitPack 使用指南

## 🚀 快速开始

### 添加仓库
```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}
```

### 添加依赖
```kotlin
dependencies {
    // 核心模块
    implementation("ai.kastrax:kastrax-core:v0.1.0")

    // 内存管理
    implementation("ai.kastrax:kastrax-memory-api:v0.1.0")

    // RAG系统
    implementation("ai.kastrax:kastrax-rag:v0.1.0")

    // 数据验证
    implementation("ai.kastrax:kastrax-zod:v0.1.0")

    // 或者使用完整项目 (通过JitPack)
    implementation("com.github.louloulin:kastrax:v0.1.0")
}
```

## 🔗 链接

- **JitPack页面**: https://jitpack.io/#louloulin/kastrax
- **构建状态**: https://jitpack.io/#louloulin/kastrax/v0.1.0
- **GitHub仓库**: https://github.com/louloulin/kastrax

## 📋 可用模块

| 模块 | 依赖 | 描述 |
|------|------|------|
| kastrax-core | `ai.kastrax:kastrax-core:v0.1.0` | 核心框架 |
| kastrax-memory-api | `ai.kastrax:kastrax-memory-api:v0.1.0` | 内存管理API |
| kastrax-rag | `ai.kastrax:kastrax-rag:v0.1.0` | RAG系统 |
| kastrax-zod | `ai.kastrax:kastrax-zod:v0.1.0` | 数据验证 |

## 🎯 版本信息

- **当前版本**: v0.1.0
- **发布时间**: 2025-06-09 22:09:39
- **JDK要求**: 17+
- **Kotlin版本**: 1.9.25

## 🔄 更新版本

要使用新版本，只需更新版本号:
```kotlin
// 通过JitPack使用
implementation("com.github.louloulin:kastrax:NEW_VERSION")

// 或者直接使用发布的包
implementation("ai.kastrax:kastrax-core:NEW_VERSION")
```

JitPack会自动构建新版本。

## 🐛 问题排查

如果遇到构建问题:
1. 检查JitPack构建日志
2. 确认版本标签存在
3. 验证GitHub仓库可访问

## 📞 支持

- GitHub Issues: https://github.com/louloulin/kastrax/issues
- JitPack文档: https://jitpack.io/docs/
