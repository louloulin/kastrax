# KastraX Native

KastraX的Native应用实现，支持在多个平台上运行。

## 支持的平台

- macOS (Intel和Apple Silicon)
- Linux (x64和ARM64)
- Windows (x64)

## 构建说明

### 前提条件

- JDK 17或更高版本
- Gradle 8.13或更高版本
- Kotlin 1.9.22或更高版本
- 对于Native构建，需要安装相应平台的工具链：
  - macOS: Xcode命令行工具
  - Linux: GCC和相关开发库
  - Windows: MSVC和Windows SDK

### 构建步骤

#### 使用脚本构建

在项目根目录下运行以下命令：

**Unix系统 (macOS/Linux)**
```bash
./build-native.sh
```

**Windows系统**
```batch
build-native.bat
```

#### 手动构建

也可以使用Gradle命令手动构建：

```bash
# 清理之前的构建
./gradlew :kastrax-native:clean

# 构建所有平台的可执行文件
./gradlew buildNative
```

### 运行应用

构建成功后，可执行文件将位于以下位置：

**Unix系统 (macOS/Linux)**
```
build/native/kastrax-native
```

**Windows系统**
```
build\native\kastrax-native.exe
```

可以通过以下命令运行应用：

**Unix系统 (macOS/Linux)**
```bash
./build/native/kastrax-native [命令]
```

**Windows系统**
```batch
build\native\kastrax-native.exe [命令]
```

## 可用命令

- `server`: 启动服务器模式
- `cli`: 启动命令行界面模式
- `config`: 显示当前配置
- `help`: 显示帮助信息

## 配置

应用程序的配置文件位于`config/kastrax-native.json`，首次运行时会自动创建默认配置。

## 开发说明

### 项目结构

```
kastrax-native/
├── build.gradle.kts        # Gradle构建脚本
├── src/
│   ├── commonMain/         # 跨平台共享代码
│   ├── commonTest/         # 跨平台共享测试
│   ├── jvmMain/            # JVM平台特定代码
│   ├── jvmTest/            # JVM平台特定测试
│   ├── nativeMain/         # Native平台特定代码
│   └── nativeTest/         # Native平台特定测试
└── README.md               # 本文档
```

### 添加新功能

1. 首先在`commonMain`中添加跨平台接口和实现
2. 在`jvmMain`和`nativeMain`中添加平台特定实现
3. 在`commonTest`、`jvmTest`和`nativeTest`中添加相应的测试

### 注意事项

- Native平台的内存管理与JVM不同，需要注意内存泄漏问题
- 不是所有JVM库都能在Native平台上使用，可能需要寻找替代方案
- 某些功能可能需要针对不同平台单独实现
