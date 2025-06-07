# 🚀 Kastrax 发布快速开始指南

## 📋 前置条件检查

✅ **已完成的配置**:
- vanniktech/gradle-maven-publish-plugin 已配置
- 22个核心模块已配置发布
- 发布脚本已创建
- GitHub Actions 已更新

## 🔧 第一次发布设置

### 1. 注册 Sonatype Central Portal

1. 访问 [https://central.sonatype.com/](https://central.sonatype.com/)
2. 使用GitHub账户登录
3. 验证命名空间 `ai.kastrax`
4. 生成发布令牌

### 2. 生成 GPG 密钥

```bash
# 生成GPG密钥对
gpg --gen-key

# 查看密钥
gpg --list-keys

# 导出公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# 导出私钥（ASCII格式）
gpg --armor --export-secret-keys YOUR_KEY_ID
```

### 3. 配置凭据

在 `~/.gradle/gradle.properties` 中添加：

```properties
# Maven Central Portal 凭据
mavenCentralUsername=your-central-portal-username
mavenCentralPassword=your-central-portal-token

# GPG 签名配置
signing.keyId=your-gpg-key-id
signing.password=your-gpg-key-password
signing.secretKey=-----BEGIN PGP PRIVATE KEY BLOCK-----\
your-ascii-armored-private-key-here\
-----END PGP PRIVATE KEY BLOCK-----
```

## 🧪 测试发布配置

```bash
# 测试配置是否正确
./scripts/test-publish-config.sh

# 测试本地发布
./gradlew publishToMavenLocal

# 检查生成的artifacts
ls ~/.m2/repository/ai/kastrax/
```

## 📦 发布流程

### 快照版本发布

```bash
# 发布快照版本（用于测试）
./scripts/publish.sh snapshot
```

### 正式版本发布

```bash
# 更新版本号（在 gradle.properties 中）
# VERSION_NAME=0.1.0

# 发布正式版本
./scripts/publish.sh release
```

## 🔍 验证发布

### 1. 检查 Central Portal

1. 登录 [https://central.sonatype.com/](https://central.sonatype.com/)
2. 查看 "Deployments" 页面
3. 检查发布状态

### 2. 检查 Maven Central

```bash
# 等待同步（通常需要10-30分钟）
# 然后检查是否可以搜索到
curl "https://search.maven.org/solrsearch/select?q=g:ai.kastrax"
```

### 3. 测试依赖

在新项目中测试：

```kotlin
// build.gradle.kts
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-memory-api:0.1.0")
    // ... 其他模块
}
```

## 🤖 自动化发布

### GitHub Actions

项目已配置自动发布：

1. **Release触发**: 创建GitHub Release时自动发布
2. **手动触发**: 在Actions页面手动触发发布

### 设置GitHub Secrets

在GitHub仓库设置中添加：

```
MAVEN_CENTRAL_USERNAME=your-username
MAVEN_CENTRAL_PASSWORD=your-token
SIGNING_KEY_ID=your-key-id
SIGNING_PASSWORD=your-key-password
SIGNING_SECRET_KEY=your-ascii-private-key
```

## 📊 发布的模块列表

### 核心模块
- `ai.kastrax:kastrax-core`
- `ai.kastrax:kastrax-memory-api`
- `ai.kastrax:kastrax-memory-impl`
- `ai.kastrax:kastrax-rag`
- `ai.kastrax:kastrax-zod`
- `ai.kastrax:kastrax-evals`
- `ai.kastrax:kastrax-cli`
- `ai.kastrax:kastrax-deployer`
- `ai.kastrax:kastrax-observability`
- `ai.kastrax:kastrax-mcp`
- `ai.kastrax:kastrax-codebase`
- `ai.kastrax:kastrax-datasource-common`

### 集成模块
- `ai.kastrax:kastrax-openai`
- `ai.kastrax:kastrax-deepseek`
- `ai.kastrax:kastrax-anthropic`
- `ai.kastrax:kastrax-gemini`
- `ai.kastrax:kastrax-qwen`

### 服务器模块
- `ai.kastrax:kastrax-server-common`
- `ai.kastrax:kastrax-server-spring`
- `ai.kastrax:kastrax-server-ktor`
- `ai.kastrax:kastrax-server-quarkus`

## 🆘 故障排除

### 常见问题

1. **签名失败**
   ```bash
   # 检查GPG密钥格式
   gpg --list-secret-keys
   ```

2. **认证失败**
   ```bash
   # 测试Central Portal连接
   curl -u "username:token" https://central.sonatype.com/api/v1/publisher/status
   ```

3. **模块未发布**
   ```bash
   # 检查模块配置
   ./gradlew :kastrax-core:tasks --group="publishing"
   ```

### 获取帮助

- 查看 [PUBLISHING.md](PUBLISHING.md) 详细指南
- 查看 [PUBLISHING_SETUP.md](PUBLISHING_SETUP.md) 配置说明
- 查看 [vanniktech插件文档](https://vanniktech.github.io/gradle-maven-publish-plugin/)

## 🎉 完成！

配置完成后，您就可以轻松地将Kastrax模块发布到Maven Central，让全世界的开发者都能使用您的AI框架！
