#!/bin/bash
cd "$(dirname "$0")"
./gradlew :kastrax-examples:run -PmainClass=ai.kastrax.examples.memory.MemoryManagerExampleKt
