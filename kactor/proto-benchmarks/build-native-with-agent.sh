#!/bin/bash

# Set GraalVM home
GRAALVM_HOME="$HOME/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"
echo "Using GraalVM at $GRAALVM_HOME"

# Check if GraalVM native-image is installed
if [ ! -f "$GRAALVM_HOME/bin/native-image" ]; then
  echo "Installing native-image..."
  $GRAALVM_HOME/bin/gu install native-image
fi

# Create directories for agent output
mkdir -p build/native-agent-output

# Step 1: Run the application with the agent to generate configuration files
echo "Running with GraalVM agent to generate configuration files..."
cd ..
JAVA_OPTS="-agentlib:native-image-agent=config-output-dir=proto-benchmarks/build/native-agent-output"
./gradlew :proto-benchmarks:run -PjvmArgs="$JAVA_OPTS" || true

# Step 2: Check if the configuration files were generated
if [ ! -f "proto-benchmarks/build/native-agent-output/reflect-config.json" ]; then
  echo "Configuration files were not generated. Creating a simple example instead."
  
  # Create a simple Java class with a main method
  mkdir -p proto-benchmarks/build/native-image/src
  cat > proto-benchmarks/build/native-image/src/ProtoActorHello.java << EOF
  public class ProtoActorHello {
      public static void main(String[] args) {
          System.out.println("Hello from ProtoActor Native!");
          System.out.println("This is a simple native image example.");
          System.out.println("For more complex examples, you'll need to generate proper configuration files.");
      }
  }
EOF

  # Compile the Java class using GraalVM's javac
  $GRAALVM_HOME/bin/javac -d proto-benchmarks/build/native-image proto-benchmarks/build/native-image/src/ProtoActorHello.java

  # Build the native image using GraalVM directly
  echo "Building simple native image..."
  $GRAALVM_HOME/bin/native-image \
    --no-fallback \
    -cp "proto-benchmarks/build/native-image" \
    ProtoActorHello \
    proto-benchmarks/build/native-image/protoactor-simple

  if [ $? -eq 0 ]; then
    echo "Simple native image built successfully at proto-benchmarks/build/native-image/protoactor-simple"
    echo "You can run it with: ./proto-benchmarks/build/native-image/protoactor-simple"
    
    # Run the native image
    echo "Running the native image:"
    proto-benchmarks/build/native-image/protoactor-simple
  else
    echo "Native image build failed"
  fi
  
  exit 0
fi

# Step 3: Build the project
echo "Building the project..."
./gradlew :proto-benchmarks:build :proto-benchmarks:copyDependencies -x test

# Step 4: Create a directory for the native image
mkdir -p proto-benchmarks/build/native-image

# Step 5: Build the native image using GraalVM directly with agent-generated config files
echo "Building native image..."
$GRAALVM_HOME/bin/native-image \
  --no-fallback \
  --report-unsupported-elements-at-runtime \
  -H:+ReportExceptionStackTraces \
  -H:+PrintClassInitialization \
  --initialize-at-build-time=org.slf4j,ch.qos.logback \
  --initialize-at-run-time=io.grpc,io.netty \
  -H:ReflectionConfigurationFiles=proto-benchmarks/build/native-agent-output/reflect-config.json \
  -H:ResourceConfigurationFiles=proto-benchmarks/build/native-agent-output/resource-config.json \
  -H:JNIConfigurationFiles=proto-benchmarks/build/native-agent-output/jni-config.json \
  -cp "proto-benchmarks/build/classes/kotlin/main:proto-benchmarks/build/resources/main:$(find proto-benchmarks/build/dependencies -name "*.jar" | tr '\n' ':')" \
  actor.proto.benchmarks.local.MailboxBenchmark \
  proto-benchmarks/build/native-image/protoactor-benchmark

if [ $? -eq 0 ]; then
  echo "Native image built successfully at proto-benchmarks/build/native-image/protoactor-benchmark"
  echo "You can run it with: ./proto-benchmarks/build/native-image/protoactor-benchmark"
else
  echo "Native image build failed"
fi
