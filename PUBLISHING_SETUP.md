# Kastrax Maven Central 发布配置

本文档说明了Kastrax项目的Maven Central发布配置，使用了`vanniktech/gradle-maven-publish-plugin`来简化发布流程。

## 🚀 快速开始

### 1. 测试配置

```bash
# 测试发布配置是否正确
./scripts/test-publish-config.sh
```

### 2. 配置凭据

在 `~/.gradle/gradle.properties` 中添加：

```properties
# Maven Central Portal 凭据
mavenCentralUsername=your-username
mavenCentralPassword=your-token

# GPG 签名配置
signing.keyId=your-key-id
signing.password=your-key-password
signing.secretKey=your-ascii-armored-private-key
```

### 3. 发布

```bash
# 发布快照版本
./scripts/publish.sh snapshot

# 发布正式版本
./scripts/publish.sh release
```

## 📋 配置详情

### 插件配置

项目使用 `vanniktech/gradle-maven-publish-plugin` v0.32.0，具有以下优势：

- ✅ **自动配置**: 自动处理大部分发布设置
- ✅ **统一API**: 支持所有项目类型（Kotlin、Java、Android等）
- ✅ **Central Portal**: 直接支持新的Sonatype Central Portal
- ✅ **内存签名**: 支持内存GPG签名，CI友好
- ✅ **简化配置**: 最少的配置即可工作

### 已配置的发布模块

✅ **核心模块** (已配置):
- `kastrax-core` - 核心API和组件
- `kastrax-memory-api` - 内存管理API
- `kastrax-memory-impl` - 内存管理实现
- `kastrax-rag` - RAG系统组件
- `kastrax-zod` - Zod验证组件
- `kastrax-evals` - 评估框架
- `kastrax-cli` - 命令行工具
- `kastrax-deployer` - 部署工具
- `kastrax-observability` - 监控工具
- `kastrax-mcp` - MCP协议实现
- `kastrax-codebase` - 代码理解工具
- `kastrax-datasource-common` - 数据源通用接口

✅ **集成模块** (已配置):
- `kastrax-integrations:kastrax-openai` - OpenAI集成
- `kastrax-integrations:kastrax-deepseek` - DeepSeek集成
- `kastrax-integrations:kastrax-anthropic` - Anthropic集成
- `kastrax-integrations:kastrax-gemini` - Gemini集成
- `kastrax-integrations:kastrax-qwen` - Qwen集成

✅ **服务器模块** (已配置):
- `kastrax-server:common` - 服务器通用组件
- `kastrax-server:spring` - Spring Boot实现
- `kastrax-server:ktor` - Ktor实现
- `kastrax-server:quarkus` - Quarkus实现

❌ **排除的模块**:
- 测试模块 (`*test*`)
- 示例模块 (`examples:*`)
- 基准测试模块 (`*benchmark*`)
- 特殊模块 (`fastembed-kotlin`, `graal-native`, `kactor:*`)

### 版本管理

版本在 `gradle.properties` 中配置：

```properties
VERSION_NAME=0.1.0          # 正式版本
VERSION_NAME=0.1.0-SNAPSHOT # 快照版本
```

## 🔧 高级配置

### 自定义模块配置

如需为特定模块自定义发布配置，可以在模块的 `build.gradle.kts` 中添加：

```kotlin
configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {
    pom {
        name.set("Custom Module Name")
        description.set("Custom description")
    }
}
```

### 环境变量

支持以下环境变量：

```bash
MAVEN_CENTRAL_USERNAME    # Central Portal用户名
MAVEN_CENTRAL_PASSWORD    # Central Portal令牌
SIGNING_KEY_ID           # GPG密钥ID
SIGNING_PASSWORD         # GPG密钥密码
SIGNING_SECRET_KEY       # GPG私钥（ASCII格式）
```

### GitHub Actions

项目包含自动化发布的GitHub Actions工作流：

- **自动触发**: 创建GitHub Release时自动发布
- **手动触发**: 可以手动指定版本和是否为快照版本
- **安全**: 使用GitHub Secrets存储敏感信息

## 🛠️ 故障排除

### 常见问题

1. **签名失败**
   ```bash
   # 检查GPG密钥格式
   gpg --armor --export-secret-keys YOUR_KEY_ID
   ```

2. **认证失败**
   ```bash
   # 检查Central Portal凭据
   curl -u "username:token" https://central.sonatype.com/api/v1/publisher/status
   ```

3. **模块未发布**
   ```bash
   # 检查模块是否被排除
   ./gradlew tasks --all | grep publishToMavenCentral
   ```

### 调试命令

```bash
# 测试本地发布
./gradlew publishToMavenLocal

# 检查生成的POM
./gradlew generatePomFileForMavenPublication

# 验证签名
./gradlew signMavenPublication

# 干运行发布
./gradlew publishToMavenCentral --dry-run
```

## 📚 相关文档

- [完整发布指南](PUBLISHING.md)
- [vanniktech插件文档](https://vanniktech.github.io/gradle-maven-publish-plugin/)
- [Sonatype Central Portal](https://central.sonatype.com/)

## 🎯 最佳实践

1. **版本控制**: 使用语义化版本
2. **测试**: 发布前运行完整测试
3. **文档**: 保持README和API文档最新
4. **安全**: 妥善保管GPG密钥和访问令牌
5. **自动化**: 使用CI/CD进行发布
