# 📦 阿里云Maven仓库配置指南

## 🚀 快速开始

### 1. 注册阿里云账户
1. 访问 https://www.aliyun.com/
2. 注册阿里云账户
3. 实名认证

### 2. 创建Maven仓库
1. 登录阿里云控制台
2. 搜索"云效"或"Packages"
3. 创建Maven仓库实例
4. 获取仓库URL和凭据

### 3. 配置发布凭据
编辑 `~/.gradle/gradle.properties`:
```properties
# 阿里云Maven仓库配置
aliyunMavenUrl=https://packages.aliyun.com/maven/repository/YOUR-REPO-ID/
aliyunMavenUsername=YOUR_USERNAME
aliyunMavenPassword=YOUR_PASSWORD
```

### 4. 发布到阿里云
```bash
# 发布核心模块
./scripts/publish-to-aliyun.sh core

# 发布单个模块
./scripts/publish-to-aliyun.sh single kastrax-core

# 发布所有模块
./scripts/publish-to-aliyun.sh all
```

## 📋 使用发布的包

### 添加阿里云仓库
```kotlin
// build.gradle.kts
repositories {
    maven("https://packages.aliyun.com/maven/repository/YOUR-REPO-ID/")
    maven("https://maven.aliyun.com/repository/public") // 阿里云公共镜像
}
```

### 添加依赖
```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-memory-api:0.1.0")
    implementation("ai.kastrax:kastrax-rag:0.1.0")
    implementation("ai.kastrax:kastrax-zod:0.1.0")
}
```

## 🔧 高级配置

### 私有仓库配置
```properties
# 快照版本仓库
aliyunPrivateUrl=https://packages.aliyun.com/maven/repository/YOUR-SNAPSHOT-REPO/
aliyunPrivateUsername=YOUR_USERNAME
aliyunPrivatePassword=YOUR_PASSWORD
```

### 发布快照版本
```bash
./scripts/publish-to-aliyun.sh core snapshot
```

## 🌟 优势

- ✅ **国内访问速度快**: 阿里云CDN加速
- ✅ **稳定可靠**: 阿里云基础设施
- ✅ **免费使用**: 基础功能免费
- ✅ **企业级支持**: 付费版本提供企业级功能

## 📞 支持

- 阿里云文档: https://help.aliyun.com/
- 云效Packages: https://packages.aliyun.com/
- 技术支持: 阿里云工单系统
