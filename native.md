# KastraX Native Integration Guide

This document outlines the native packaging approach for KastraX and provides guidance on using the SDKs for Rust, Go, and JavaScript.

## Table of Contents

1. [Overview](#overview)
2. [GraalVM Native Image](#graalvm-native-image)
3. [SDK Architecture](#sdk-architecture)
4. [Rust SDK](#rust-sdk)
5. [Go SDK](#go-sdk)
6. [JavaScript SDK](#javascript-sdk)
7. [Building from Source](#building-from-source)
8. [Integration Examples](#integration-examples)
9. [Troubleshooting](#troubleshooting)

## Overview

KastraX provides native integration capabilities through two main approaches:

1. **GraalVM Native Image**: Compile KastraX directly to native executables
2. **Language-specific SDKs**: Use KastraX from Rust, Go, and JavaScript

This dual approach allows for both high-performance standalone applications and seamless integration with existing codebases in various languages.

## GraalVM Native Image

### Prerequisites

- GraalVM 22.3.0 or later
- Native Image tool (`gu install native-image`)
- Appropriate build tools for your platform (Visual Studio, XCode, GCC, etc.)

### Building a Native Image

```bash
# Build the project
./gradlew :kastrax-native:nativeCompile

# Find the executable in build/native/nativeCompile/
```

### Configuration

KastraX provides the following GraalVM configuration files:

- `reflection-config.json`: Configures classes for reflection
- `resource-config.json`: Specifies resources to include
- `native-image.properties`: Default native-image arguments

These files are automatically included when building with Gradle.

### Limitations

When using GraalVM Native Image, be aware of the following limitations:

- Dynamic class loading is limited
- Reflection requires explicit configuration
- Some JVM features are not available
- Startup is faster, but peak performance may differ from JVM

## SDK Architecture

All SDKs follow a common architecture:

1. **Core Bridge**: Low-level FFI/JNI interface to KastraX
2. **High-level API**: Idiomatic API for each language
3. **Serialization Layer**: Handles data conversion between languages
4. **Resource Management**: Manages memory and lifecycle of KastraX objects

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Language API   │────▶│ Serialization   │────▶│   Core Bridge   │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                         │
                                                         ▼
                                               ┌─────────────────┐
                                               │  KastraX Core   │
                                               └─────────────────┘
```

### Implementation Details

#### Core Bridge

The Core Bridge layer is implemented using different technologies for each language:

- **Rust**: Uses JNI (Java Native Interface) to communicate with the KastraX JVM
- **Go**: Uses CGO and JNI to bridge between Go and the KastraX JVM
- **JavaScript**: Uses Kotlin/JS to compile KastraX code directly to JavaScript

#### Serialization Layer

All data passing between the SDKs and KastraX core is serialized using JSON. This provides a language-neutral format that can be easily processed by all supported languages. The serialization layer handles:

- Converting language-specific objects to JSON
- Converting JSON to language-specific objects
- Handling type conversions and validation

#### Resource Management

Each SDK implements proper resource management to ensure that native resources are properly cleaned up:

- **Rust**: Uses RAII (Resource Acquisition Is Initialization) pattern with Drop trait
- **Go**: Uses defer statements and finalizers
- **JavaScript**: Uses JavaScript's garbage collection with explicit cleanup methods

### SDK Development

To add support for a new language:

1. Create a new module in the KastraX project
2. Implement the Core Bridge layer for the language
3. Create idiomatic API wrappers for the language
4. Implement serialization and resource management
5. Create examples and documentation

## Rust SDK

### Installation

```bash
# Add to Cargo.toml
[dependencies]
kastrax = "0.1.0"
```

### Basic Usage

```rust
use kastrax::{Agent, Tool};

fn main() {
    // Create an agent
    let mut agent = Agent::builder()
        .name("my-agent")
        .model("deepseek-coder")
        .build();

    // Add a tool
    agent.add_tool(Tool::new("calculator", |input| {
        // Tool implementation
    }));

    // Run the agent
    let response = agent.run("Calculate 2+2");
    println!("{}", response);
}
```

### Advanced Features

The Rust SDK provides full access to KastraX features:

- Agent creation and management
- Tool registration and execution
- Memory and state management
- RAG capabilities
- Model integration

## Go SDK

### Installation

```bash
go get github.com/kastrax/kastrax-go
```

### Basic Usage

```go
package main

import (
    "fmt"
    "github.com/kastrax/kastrax-go"
)

func main() {
    // Create an agent
    agent := kastrax.NewAgent(kastrax.AgentConfig{
        Name: "my-agent",
        Model: "deepseek-coder",
    })

    // Add a tool
    agent.AddTool(kastrax.Tool{
        Name: "calculator",
        Function: func(input string) (string, error) {
            // Tool implementation
            return "4", nil
        },
    })

    // Run the agent
    response, err := agent.Run("Calculate 2+2")
    if err != nil {
        panic(err)
    }

    fmt.Println(response)
}
```

### Advanced Features

The Go SDK provides access to all KastraX features through an idiomatic Go API.

## JavaScript SDK

### Installation

```bash
npm install kastrax
```

### Basic Usage

```javascript
import { Agent, Tool } from 'kastrax';

// Create an agent
const agent = new Agent({
  name: 'my-agent',
  model: 'deepseek-coder'
});

// Add a tool
agent.addTool(new Tool({
  name: 'calculator',
  function: (input) => {
    // Tool implementation
    return '4';
  }
}));

// Run the agent
agent.run('Calculate 2+2')
  .then(response => console.log(response))
  .catch(error => console.error(error));
```

### Browser Support

The JavaScript SDK supports both Node.js and browser environments. For browsers, use:

```html
<script src="https://unpkg.com/kastrax@0.1.0/dist/kastrax.min.js"></script>
<script>
  const agent = new kastrax.Agent({
    name: 'my-agent',
    model: 'deepseek-coder'
  });

  // Use the agent...
</script>
```

## Building from Source

### Prerequisites

- JDK 17 or later
- Gradle 7.3 or later
- Rust (for Rust SDK)
- Go 1.18 or later (for Go SDK)
- Node.js 16 or later (for JavaScript SDK)
- GraalVM 22.3.0 or later (for native image)

### Build Commands

```bash
# Build everything
./gradlew build

# Build specific components
./gradlew :kastrax-native:build
./gradlew :kastrax-sdk-rust:build
./gradlew :kastrax-sdk-go:build
./gradlew :kastrax-sdk-js:build

# Build native image
./gradlew :kastrax-native:nativeCompile
```

## Integration Examples

See the `examples` directory for complete integration examples:

- `examples/rust/`: Rust SDK examples
- `examples/go/`: Go SDK examples
- `examples/js/`: JavaScript SDK examples
- `examples/native/`: Native image examples

## Troubleshooting

### Common Issues

1. **JNI/FFI Errors**: Ensure the native libraries are in the correct location and accessible
2. **Memory Management**: Watch for memory leaks when using the SDKs
3. **GraalVM Compatibility**: Some libraries may not be compatible with GraalVM native-image

### Getting Help

- File issues on GitHub
- Join the KastraX community Discord
- Check the detailed documentation at https://kastrax.ai/docs/native
