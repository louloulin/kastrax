#!/bin/bash

# Change to the project root directory
cd "$(dirname "$0")"

# Run the AdaptiveAgentExample using kotlin command
./gradlew :examples:compileKotlin && kotlin -cp examples/build/classes/kotlin/main:$(./gradlew -q :examples:printClasspath) ai.kastrax.examples.agent.AdaptiveAgentExampleKt
