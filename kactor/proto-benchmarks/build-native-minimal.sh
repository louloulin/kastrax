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

# Create a simple Java class with a main method
mkdir -p proto-benchmarks/build/native-image/src
cat > proto-benchmarks/build/native-image/src/HelloWorld.java << EOF
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello, Native World!");
    }
}
EOF

# Compile the Java class using GraalVM's javac
$GRAALVM_HOME/bin/javac -d proto-benchmarks/build/native-image proto-benchmarks/build/native-image/src/HelloWorld.java

# Build the native image using GraalVM directly
echo "Building native image..."
$GRAALVM_HOME/bin/native-image \
  --no-fallback \
  -cp "proto-benchmarks/build/native-image" \
  HelloWorld \
  proto-benchmarks/build/native-image/hello-native

if [ $? -eq 0 ]; then
  echo "Native image built successfully at proto-benchmarks/build/native-image/hello-native"
  echo "You can run it with: ./proto-benchmarks/build/native-image/hello-native"
  
  # Run the native image
  echo "Running the native image:"
  proto-benchmarks/build/native-image/hello-native
else
  echo "Native image build failed"
fi
