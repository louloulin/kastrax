#!/bin/bash

# Find all build.gradle.kts files in chapi subdirectories
find chapi -name "build.gradle.kts" -type f | while read -r file; do
  echo "Processing $file"
  
  # Update Kotlin serialization plugin version
  sed -i '' 's/kotlin("plugin.serialization") version "1.6.10"/kotlin("plugin.serialization")/g' "$file"
  
  # Update kotlinx-serialization-json version
  sed -i '' 's/implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")/implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")/g' "$file"
  
  # Update kotlinx-coroutines-core version if it's 1.6.0
  sed -i '' 's/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")/implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")/g' "$file"
done

echo "All files updated!"
