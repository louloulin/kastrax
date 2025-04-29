#!/bin/bash

# Set GraalVM home
GRAALVM_HOME="$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
echo "Using GraalVM at $GRAALVM_HOME"

# Build the project
cd ..
./gradlew :proto-benchmarks:build -x test

# Create a directory for the native image
mkdir -p proto-benchmarks/build/native-image

# Create a simple reflection config file
cat > proto-benchmarks/build/native-image/reflect-config.json << EOF
[
  {
    "name": "actor.proto.benchmarks.local.MailboxBenchmark",
    "allDeclaredFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "methods": [
      { "name": "main", "parameterTypes": ["java.lang.String[]"] }
    ]
  },
  {
    "name": "actor.proto.benchmarks.local.MailboxBenchmark\$CounterActor",
    "allDeclaredFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true
  },
  {
    "name": "org.openjdk.jmh.Main",
    "allDeclaredFields": true,
    "allDeclaredMethods": true,
    "allPublicMethods": true,
    "methods": [
      { "name": "main", "parameterTypes": ["java.lang.String[]"] }
    ]
  }
]
EOF

# Build the native image using GraalVM directly
$GRAALVM_HOME/bin/native-image \
  --no-fallback \
  --report-unsupported-elements-at-runtime \
  -H:+ReportExceptionStackTraces \
  -H:+PrintClassInitialization \
  --initialize-at-build-time=org.slf4j,ch.qos.logback \
  --initialize-at-run-time=io.grpc,io.netty \
  -H:ReflectionConfigurationFiles=proto-benchmarks/build/native-image/reflect-config.json \
  -cp "proto-benchmarks/build/classes/kotlin/main:proto-benchmarks/build/resources/main:$(find proto-benchmarks/build/dependencies -name "*.jar" | tr '\n' ':')" \
  actor.proto.benchmarks.local.MailboxBenchmark \
  proto-benchmarks/build/native-image/protoactor-benchmark

echo "Native image built at proto-benchmarks/build/native-image/protoactor-benchmark"
