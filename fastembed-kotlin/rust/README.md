# fastembed-jni

JNI bindings for the [fastembed-rs](https://github.com/Anush008/fastembed-rs) library, allowing it to be used from Java and Kotlin.

## Building

To build the library, you need to have Rust and Cargo installed. Then, run:

```bash
cargo build --release
```

This will produce a shared library in the `target/release` directory:
- `libfastembed_jni.so` on Linux
- `libfastembed_jni.dylib` on macOS
- `fastembed_jni.dll` on Windows

## Features

The library provides JNI bindings for the following fastembed-rs features:

- Creating text embedding models
- Generating embeddings for text
- Calculating cosine similarity between embeddings
- Managing model resources

## Implementation Details

The library uses a global cache to store model instances, which are referenced by a unique ID. This allows the Java/Kotlin side to hold a reference to a model without keeping the actual model in memory on the Java side.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
