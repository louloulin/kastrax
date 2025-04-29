#!/bin/bash

# Build the native image
cd ..
./gradlew :proto-benchmarks:nativeCompile

# Run the native image
./proto-benchmarks/build/native/nativeCompile/protoactor-benchmark -h
