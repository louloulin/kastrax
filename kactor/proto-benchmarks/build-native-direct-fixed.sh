#!/bin/bash

# Set GraalVM home
GRAALVM_HOME="$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
echo "Using GraalVM at $GRAALVM_HOME"

# Check if GraalVM native-image is installed
if [ ! -f "$GRAALVM_HOME/bin/native-image" ]; then
  echo "Installing native-image..."
  $GRAALVM_HOME/bin/gu install native-image
fi

# Build the project
cd ..
./gradlew :proto-benchmarks:build :proto-benchmarks:copyDependencies -x test

# Create a directory for the native image
mkdir -p proto-benchmarks/build/native-image

# Build the native image using GraalVM directly
echo "Building native image..."
$GRAALVM_HOME/bin/native-image \
  --no-fallback \
  --report-unsupported-elements-at-runtime \
  -H:+ReportExceptionStackTraces \
  -H:+PrintClassInitialization \
  --initialize-at-build-time=org.slf4j,ch.qos.logback \
  --initialize-at-run-time=io.grpc,io.netty \
  -cp "proto-benchmarks/build/classes/kotlin/main:proto-benchmarks/build/resources/main:$(find proto-benchmarks/build/dependencies -name "*.jar" | tr '\n' ':')" \
  actor.proto.benchmarks.local.MailboxBenchmark \
  proto-benchmarks/build/native-image/protoactor-benchmark

if [ $? -eq 0 ]; then
  echo "Native image built successfully at proto-benchmarks/build/native-image/protoactor-benchmark"
  echo "You can run it with: ./proto-benchmarks/build/native-image/protoactor-benchmark"
else
  echo "Native image build failed"
fi
