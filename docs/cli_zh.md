# KastraX CLI 工具

## 1. 概述

KastraX CLI 是一个命令行工具，用于创建和管理 KastraX 项目。它提供了以下功能：

- 创建新的 KastraX 项目
- 启动交互式游乐场
- 部署 KastraX 项目

CLI 工具使用 Kotlin 编写，基于 [Clikt](https://ajalt.github.io/clikt/) 库构建。

## 2. 安装

### 2.1 使用预编译的二进制文件

1. 从 [GitHub Releases](https://github.com/kastrax/kastrax/releases) 下载最新版本的 KastraX CLI
2. 解压下载的文件
3. 将解压后的目录添加到系统的 PATH 环境变量中

### 2.2 从源代码构建

1. 克隆 KastraX 仓库：
   ```bash
   git clone https://github.com/kastrax/kastrax.git
   ```

2. 进入项目目录：
   ```bash
   cd kastrax
   ```

3. 构建 CLI 工具：
   ```bash
   ./gradlew :kastrax-cli:build
   ```

4. 创建可执行 JAR 文件：
   ```bash
   ./gradlew :kastrax-cli:jar
   ```

5. 运行 CLI 工具：
   ```bash
   java -jar kastrax-cli/build/libs/kastrax-cli-0.1.0.jar
   ```

## 3. 使用方法

### 3.1 创建新项目

使用 `new` 命令创建新的 KastraX 项目：

```bash
kastrax new my-project
```

#### 选项

- `-t, --template`: 项目模板类型，可选值为 `simple`（默认）、`rag`、`agent`、`workflow`
- `-o, --output-dir`: 输出目录，默认为当前目录
- `-f, --force`: 强制覆盖现有目录

#### 示例

创建一个使用 RAG 模板的项目：

```bash
kastrax new my-rag-project --template rag
```

创建一个项目到指定目录：

```bash
kastrax new my-project --output-dir /path/to/projects
```

### 3.2 启动交互式游乐场

使用 `playground` 命令启动交互式游乐场：

```bash
kastrax playground
```

#### 选项

- `-p, --port`: 游乐场服务器端口，默认为 8080
- `-c, --config`: 配置文件路径，默认为 `kastrax.json`
- `-v, --verbose`: 启用详细日志

#### 示例

在指定端口启动游乐场：

```bash
kastrax playground --port 9000
```

使用自定义配置文件启动游乐场：

```bash
kastrax playground --config my-config.json
```

### 3.3 部署项目

使用 `deploy` 命令部署 KastraX 项目：

```bash
kastrax deploy
```

#### 选项

- `-t, --target`: 部署目标，可选值为 `local`（默认）、`docker`、`aws`、`gcp`、`azure`
- `-c, --config`: 部署配置文件路径，默认为 `kastrax-deploy.json`
- `--dry-run`: 执行部署的模拟运行，不进行实际部署
- `-v, --verbose`: 启用详细日志

#### 示例

部署到 Docker：

```bash
kastrax deploy --target docker
```

执行部署的模拟运行：

```bash
kastrax deploy --target aws --dry-run
```

## 4. 项目模板

KastraX CLI 提供了以下项目模板：

### 4.1 Simple 模板

简单的 KastraX 项目，包含基本的 Agent 实现。适合初学者或简单的应用场景。

```bash
kastrax new my-project --template simple
```

### 4.2 RAG 模板

包含检索增强生成 (RAG) 功能的项目。适合需要从文档中检索信息的应用场景。

```bash
kastrax new my-rag-project --template rag
```

### 4.3 Agent 模板

包含自定义 Agent 和工具的项目。适合需要实现复杂 Agent 行为的应用场景。

```bash
kastrax new my-agent-project --template agent
```

### 4.4 Workflow 模板

包含工作流功能的项目。适合需要实现多步骤、多 Agent 协作的应用场景。

```bash
kastrax new my-workflow-project --template workflow
```

## 5. 配置文件

### 5.1 CLI 配置文件

CLI 工具使用 JSON 格式的配置文件。默认配置文件为 `kastrax.json`，位于当前目录。

示例配置文件：

```json
{
  "llm": {
    "provider": "openai",
    "model": "gpt-4",
    "apiKey": "${OPENAI_API_KEY}"
  },
  "logging": {
    "level": "INFO",
    "file": "kastrax.log"
  },
  "playground": {
    "port": 8080,
    "theme": "dark"
  }
}
```

### 5.2 部署配置文件

部署命令使用 JSON 格式的配置文件。默认配置文件为 `kastrax-deploy.json`，位于当前目录。

示例配置文件：

```json
{
  "app": {
    "name": "my-kastrax-app",
    "version": "1.0.0"
  },
  "targets": {
    "docker": {
      "image": "my-kastrax-app",
      "tag": "latest",
      "ports": [8080]
    },
    "aws": {
      "region": "us-west-2",
      "service": "lambda",
      "memory": 512,
      "timeout": 30
    }
  },
  "env": {
    "OPENAI_API_KEY": "${OPENAI_API_KEY}"
  }
}
```

## 6. 最佳实践

### 6.1 项目结构

推荐的项目结构：

```
my-project/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── kastrax.json
├── kastrax-deploy.json
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   ├── Main.kt
│   │   │   ├── agents/
│   │   │   ├── tools/
│   │   │   └── workflows/
│   │   └── resources/
│   │       └── logback.xml
│   └── test/
│       └── kotlin/
└── README.md
```

### 6.2 版本控制

建议将以下文件添加到 `.gitignore`：

```
.gradle/
build/
*.log
.idea/
*.iml
*.ipr
*.iws
out/
.DS_Store
```

### 6.3 环境变量

敏感信息（如 API 密钥）应通过环境变量提供，而不是硬编码在配置文件中。

在配置文件中，使用 `${ENV_VAR}` 语法引用环境变量：

```json
{
  "llm": {
    "apiKey": "${OPENAI_API_KEY}"
  }
}
```

## 7. 故障排除

### 7.1 常见问题

#### 问题：CLI 工具无法启动

可能的原因：
- Java 未安装或版本不兼容
- JAR 文件损坏

解决方案：
- 确保安装了 Java 21 或更高版本
- 重新下载或构建 CLI 工具

#### 问题：项目创建失败

可能的原因：
- 输出目录不存在或没有写入权限
- 目标目录已存在且未使用 `--force` 选项

解决方案：
- 确保输出目录存在且有写入权限
- 使用 `--force` 选项覆盖现有目录，或选择一个不存在的目录名

#### 问题：游乐场无法启动

可能的原因：
- 端口已被占用
- 配置文件不存在或格式错误

解决方案：
- 使用 `--port` 选项指定一个未被占用的端口
- 检查配置文件是否存在且格式正确

### 7.2 日志

CLI 工具使用 Logback 进行日志记录。默认情况下，日志输出到控制台。

要启用详细日志，请使用 `--verbose` 选项：

```bash
kastrax playground --verbose
```

## 8. 总结

KastraX CLI 工具提供了一种简单的方式来创建和管理 KastraX 项目。通过使用不同的项目模板，你可以快速开始构建各种类型的 AI 应用程序。

CLI 工具的主要命令包括：
- `new`：创建新项目
- `playground`：启动交互式游乐场
- `deploy`：部署项目

每个命令都有多个选项，可以根据需要进行自定义。
