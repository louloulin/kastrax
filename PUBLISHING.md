# 发布到 Maven Central 指南

本指南详细说明如何将 Kastrax 项目发布到 Maven Central 仓库。

## 前置条件

### 1. 注册 Sonatype OSSRH 账户

1. 访问 [Sonatype JIRA](https://issues.sonatype.org/)
2. 创建账户并登录
3. 创建一个新的项目票据来申请 `ai.kastrax` 组ID
4. 等待 Sonatype 团队批准你的请求

### 2. 生成 GPG 密钥对

```bash
# 生成新的GPG密钥对
gpg --gen-key

# 列出密钥
gpg --list-keys

# 导出公钥到密钥服务器
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID

# 导出私钥（用于签名）
gpg --export-secret-keys YOUR_KEY_ID > secring.gpg
```

## 配置

### 1. 环境变量配置

在你的环境中设置以下变量：

```bash
export OSSRH_USERNAME="your-sonatype-username"
export OSSRH_PASSWORD="your-sonatype-password"
export SIGNING_KEY_ID="your-gpg-key-id"
export SIGNING_PASSWORD="your-gpg-key-password"
export SIGNING_SECRET_KEY_RING_FILE="/path/to/secring.gpg"
```

### 2. Gradle 属性配置（可选）

在 `~/.gradle/gradle.properties` 中添加：

```properties
ossrhUsername=your-sonatype-username
ossrhPassword=your-sonatype-password
signing.keyId=your-gpg-key-id
signing.password=your-gpg-key-password
signing.secretKeyRingFile=/path/to/secring.gpg
```

## 发布流程

### 1. 准备发布版本

确保版本号不包含 `SNAPSHOT` 后缀：

```kotlin
// 在 build.gradle.kts 中
version = "1.0.0"  // 而不是 "1.0.0-SNAPSHOT"
```

### 2. 应用签名插件到需要发布的模块

在需要发布的子模块的 `build.gradle.kts` 中添加：

```kotlin
plugins {
    // ... 其他插件
    signing
}
```

### 3. 执行发布

```bash
# 发布到 staging 仓库
./gradlew publishToSonatype

# 或者发布所有模块
./gradlew publish
```

### 4. 在 Sonatype 中管理发布

1. 登录 [Nexus Repository Manager](https://s01.oss.sonatype.org/)
2. 进入 "Staging Repositories"
3. 找到你的发布（通常以 `aikastrax-` 开头）
4. 选择并点击 "Close" 来验证发布
5. 验证通过后，点击 "Release" 来发布到 Maven Central

## 自动化发布（推荐）

### GitHub Actions 配置

创建 `.github/workflows/publish.yml`：

```yaml
name: Publish to Maven Central

on:
  release:
    types: [published]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v2
    
    - name: Publish to Maven Central
      env:
        OSSRH_USERNAME: ${{ secrets.OSSRH_USERNAME }}
        OSSRH_PASSWORD: ${{ secrets.OSSRH_PASSWORD }}
        SIGNING_KEY_ID: ${{ secrets.SIGNING_KEY_ID }}
        SIGNING_PASSWORD: ${{ secrets.SIGNING_PASSWORD }}
        SIGNING_SECRET_KEY_RING_FILE: ${{ secrets.SIGNING_SECRET_KEY_RING_FILE }}
      run: |
        echo "${{ secrets.SIGNING_SECRET_KEY }}" | base64 --decode > secring.gpg
        export SIGNING_SECRET_KEY_RING_FILE="$(pwd)/secring.gpg"
        ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

### 设置 GitHub Secrets

在 GitHub 仓库设置中添加以下 secrets：

- `OSSRH_USERNAME`: Sonatype 用户名
- `OSSRH_PASSWORD`: Sonatype 密码
- `SIGNING_KEY_ID`: GPG 密钥 ID
- `SIGNING_PASSWORD`: GPG 密钥密码
- `SIGNING_SECRET_KEY`: GPG 私钥的 base64 编码

## 验证发布

发布成功后，你可以在以下位置找到你的包：

1. [Maven Central Search](https://search.maven.org/)
2. [MVN Repository](https://mvnrepository.com/)

通常需要等待 10-30 分钟才能在 Maven Central 上搜索到新发布的包。

## 故障排除

### 常见问题

1. **签名失败**: 确保 GPG 密钥配置正确
2. **认证失败**: 检查 OSSRH 凭据
3. **POM 验证失败**: 确保所有必需的 POM 元素都已配置
4. **版本冲突**: 确保版本号是唯一的

### 调试命令

```bash
# 检查发布配置
./gradlew publishToMavenLocal --info

# 验证签名
./gradlew signMavenPublication

# 检查生成的 POM
find . -name "*.pom" -exec cat {} \;
```

## 最佳实践

1. **版本管理**: 使用语义化版本控制
2. **文档**: 确保 README 和 API 文档是最新的
3. **测试**: 在发布前运行完整的测试套件
4. **备份**: 保存 GPG 密钥的安全备份
5. **自动化**: 使用 CI/CD 进行自动发布

## 相关链接

- [Sonatype OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Gradle Signing Plugin](https://docs.gradle.org/current/userguide/signing_plugin.html)
- [Maven Central Requirements](https://central.sonatype.org/publish/requirements/)