# 🌍 Kastrax 多仓库发布配置指南

## 📋 支持的仓库列表

### 🌐 国际仓库
1. **Maven Central** ✅ (已配置)
   - 全球最大的Maven仓库
   - 网址: https://central.sonatype.com/
   - 状态: 已完成配置

### 🇨🇳 国内仓库

#### 1. 阿里云Maven仓库 (推荐)
- **网址**: https://maven.aliyun.com/
- **优势**: 国内访问速度快，稳定可靠
- **支持**: 公开仓库 + 私有仓库
- **费用**: 免费

#### 2. 华为云Maven仓库
- **网址**: https://repo.huaweicloud.com/
- **优势**: 华为云生态，企业级支持
- **支持**: 公开仓库 + 私有仓库
- **费用**: 免费

#### 3. 腾讯云Maven仓库
- **网址**: https://mirrors.cloud.tencent.com/
- **优势**: 腾讯云生态集成
- **支持**: 镜像仓库 + 私有仓库
- **费用**: 免费

#### 4. 百度云Maven仓库
- **网址**: https://cloud.baidu.com/
- **优势**: 百度云生态
- **支持**: 私有仓库
- **费用**: 按使用量计费

#### 5. JitPack (GitHub集成)
- **网址**: https://jitpack.io/
- **优势**: 直接从GitHub发布，无需额外配置
- **支持**: 公开仓库
- **费用**: 免费

### 🏢 企业级仓库

#### 1. Nexus Repository (自建)
- **类型**: 私有仓库管理器
- **优势**: 完全控制，支持多种格式
- **部署**: 本地或云端

#### 2. JFrog Artifactory
- **类型**: 企业级仓库管理
- **优势**: 功能强大，CI/CD集成
- **费用**: 有免费版和付费版

## 🛠️ 多仓库发布配置

### 配置文件结构
```
kastrax/
├── gradle.properties (全局配置)
├── build.gradle.kts (根项目配置)
├── scripts/
│   ├── publish-to-aliyun.sh
│   ├── publish-to-huawei.sh
│   ├── publish-to-jitpack.sh
│   └── publish-multi-repo.sh
└── kastrax-*/build.gradle.kts (模块配置)
```

### Gradle多仓库配置示例

```kotlin
// build.gradle.kts (根项目)
allprojects {
    repositories {
        // 国内镜像 (优先)
        maven("https://maven.aliyun.com/repository/public")
        maven("https://repo.huaweicloud.com/repository/maven/")
        
        // 国际仓库
        mavenCentral()
        gradlePluginPortal()
    }
}

// 发布配置
publishing {
    repositories {
        // Maven Central
        maven {
            name = "MavenCentral"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")
            credentials {
                username = findProperty("mavenCentralUsername") as String?
                password = findProperty("mavenCentralPassword") as String?
            }
        }
        
        // 阿里云仓库
        maven {
            name = "AliyunMaven"
            url = uri("https://packages.aliyun.com/maven/repository/2421751-release-XXX/")
            credentials {
                username = findProperty("aliyunUsername") as String?
                password = findProperty("aliyunPassword") as String?
            }
        }
        
        // 华为云仓库
        maven {
            name = "HuaweiMaven"
            url = uri("https://repo.huaweicloud.com/repository/maven/")
            credentials {
                username = findProperty("huaweiUsername") as String?
                password = findProperty("huaweiPassword") as String?
            }
        }
        
        // GitHub Packages
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/louloulin/kastrax")
            credentials {
                username = findProperty("githubUsername") as String?
                password = findProperty("githubToken") as String?
            }
        }
    }
}
```

## 🚀 快速配置方案

### 方案1: JitPack (最简单)
**优势**: 无需额外配置，直接从GitHub发布

1. **推送代码到GitHub** ✅ (已完成)
2. **创建Release标签**
3. **访问JitPack**: https://jitpack.io/#louloulin/kastrax
4. **自动构建发布**

**使用方式**:
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.louloulin:kastrax:v0.1.0")
}
```

### 方案2: 阿里云Maven仓库
**优势**: 国内访问速度快

1. **注册阿里云账户**
2. **创建Maven仓库**
3. **配置发布凭据**
4. **执行发布**

### 方案3: GitHub Packages
**优势**: 与GitHub集成，版本管理方便

1. **生成GitHub Token**
2. **配置发布设置**
3. **自动发布**

## 📝 配置步骤详解

### 1. JitPack配置 (推荐首选)

#### 步骤1: 创建Release
```bash
# 创建并推送标签
git tag v0.1.0
git push origin v0.1.0

# 或在GitHub上创建Release
```

#### 步骤2: 验证JitPack构建
- 访问: https://jitpack.io/#louloulin/kastrax
- 点击 "Get it" 触发构建
- 等待构建完成

#### 步骤3: 使用发布的包
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.louloulin:kastrax:v0.1.0")
    implementation("com.github.louloulin.kastrax:kastrax-core:v0.1.0")
}
```

### 2. 阿里云Maven配置

#### 步骤1: 注册并创建仓库
1. 访问 https://maven.aliyun.com/
2. 注册阿里云账户
3. 创建Maven仓库实例

#### 步骤2: 配置凭据
```properties
# ~/.gradle/gradle.properties
aliyunUsername=YOUR_ALIYUN_USERNAME
aliyunPassword=YOUR_ALIYUN_PASSWORD
aliyunRepositoryUrl=YOUR_REPOSITORY_URL
```

#### 步骤3: 发布
```bash
./gradlew publishToAliyunMaven
```

### 3. GitHub Packages配置

#### 步骤1: 生成Token
1. GitHub Settings → Developer settings → Personal access tokens
2. 生成token，权限选择 `write:packages`

#### 步骤2: 配置凭据
```properties
# ~/.gradle/gradle.properties
githubUsername=louloulin
githubToken=YOUR_GITHUB_TOKEN
```

## 🎯 推荐发布策略

### 阶段1: 快速发布 (立即可用)
1. **JitPack** - 最简单，立即可用
2. **GitHub Packages** - 与项目集成

### 阶段2: 国内优化
1. **阿里云Maven** - 国内用户友好
2. **华为云Maven** - 企业用户

### 阶段3: 全球分发
1. **Maven Central** - 全球标准 ✅ (已配置)
2. **多仓库同步**

## 📊 仓库对比

| 仓库 | 配置难度 | 国内速度 | 全球可用 | 费用 | 推荐度 |
|------|----------|----------|----------|------|--------|
| JitPack | ⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 免费 | ⭐⭐⭐⭐⭐ |
| 阿里云Maven | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | 免费 | ⭐⭐⭐⭐ |
| GitHub Packages | ⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | 免费 | ⭐⭐⭐⭐ |
| Maven Central | ⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ | 免费 | ⭐⭐⭐⭐⭐ |
| 华为云Maven | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ | 免费 | ⭐⭐⭐ |

## 🎉 总结

**推荐的发布顺序**:
1. **JitPack** (立即) - 最快最简单
2. **GitHub Packages** (本周) - 项目集成
3. **阿里云Maven** (本月) - 国内优化
4. **Maven Central** (已准备) - 全球标准

这样可以确保：
- ✅ 立即可用 (JitPack)
- ✅ 国内友好 (阿里云)
- ✅ 全球标准 (Maven Central)
- ✅ 多重备份 (多仓库)
