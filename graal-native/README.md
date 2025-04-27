# KastraX GraalVM Native

KastraX的GraalVM Native实现，支持在多个平台上运行，并提供多语言SDK。

## 支持的平台

- macOS (Intel和Apple Silicon)
- Linux (x64和ARM64)
- Windows (x64)

## 构建说明

### 前提条件

- JDK 17或更高版本
- Gradle 8.13或更高版本
- GraalVM 22.3.0或更高版本
- Native Image工具 (`gu install native-image`)
- 对于Native构建，需要安装相应平台的工具链：
  - macOS: Xcode命令行工具
  - Linux: GCC和相关开发库
  - Windows: MSVC和Windows SDK

### 构建步骤

#### 使用脚本构建

在项目根目录下运行以下命令：

**Unix系统 (macOS/Linux)**
```bash
./gradlew :graal-native:nativeCompile
```

**Windows系统**
```batch
gradlew :graal-native:nativeCompile
```

#### 打包分发

构建完成后，可以创建分发包：

```bash
./gradlew :graal-native:packageNative
```

这将在`build/distributions`目录下创建一个包含可执行文件和必要资源的ZIP文件。

### 运行应用

构建成功后，可执行文件将位于以下位置：

**Unix系统 (macOS/Linux)**
```
graal-native/build/native/nativeCompile/kastrax
```

**Windows系统**
```
graal-native\build\native\nativeCompile\kastrax.exe
```

可以通过以下命令运行应用：

**Unix系统 (macOS/Linux)**
```bash
./graal-native/build/native/nativeCompile/kastrax [命令]
```

**Windows系统**
```batch
graal-native\build\native\nativeCompile\kastrax.exe [命令]
```

## 可用命令

- `server`: 启动服务器模式
- `cli`: 启动命令行界面模式
- `config`: 显示当前配置
- `help`: 显示帮助信息

## 配置

应用程序的配置文件位于`config/kastrax.json`，首次运行时会自动创建默认配置。

## SDK集成

GraalVM Native模块提供了多种语言的SDK，方便在不同语言中使用KastraX：

- **Rust SDK**: 通过JNI提供Rust语言接口
- **Go SDK**: 通过JNI提供Go语言接口
- **JavaScript SDK**: 通过Kotlin/JS提供JavaScript接口

详细的SDK使用说明请参考各SDK目录下的README文件：

- [Rust SDK](./sdk-rust/README.md)
- [Go SDK](./sdk-go/README.md)
- [JavaScript SDK](./sdk-js/README.md)

## 开发说明

### 项目结构

```
graal-native/
├── build.gradle.kts        # Gradle构建脚本
├── src/                    # 主要源代码
│   ├── main/java/          # Java源代码（JNI桥接）
│   ├── main/kotlin/        # Kotlin源代码
│   ├── main/resources/     # 资源文件
│   └── test/               # 测试代码
├── sdk-rust/               # Rust SDK
├── sdk-go/                 # Go SDK
├── sdk-js/                 # JavaScript SDK
└── README.md               # 本文档
```

### 添加新功能

1. 在`src/main/kotlin`中添加新的Kotlin实现
2. 在`src/main/java`中添加必要的JNI桥接代码
3. 在相应的SDK目录中添加语言特定的实现
4. 添加测试和文档

### 注意事项

- GraalVM Native Image对反射有特殊要求，需要在`reflection-config.json`中配置
- 某些JVM功能在Native Image中可能不可用，需要特别处理
- 启动时间更快，但峰值性能可能与JVM有所不同
