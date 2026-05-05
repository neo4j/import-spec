#!/bin/bash

# Ensure we are in the root of the repo
REPO_ROOT=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT"

echo "Starting Kotlin/Native lib generation..."

./gradlew linkReleaseStaticMacosArm64 linkReleaseStaticLinuxX64 linkReleaseStaticLinuxArm64 && \
cp build/bin/macosArm64/releaseStatic/* go/internal/bridge/lib/macos-arm64/ && \
cp build/bin/linuxX64/releaseStatic/* go/internal/bridge/lib/linux-amd64/ && \
cp build/bin/linuxArm64/releaseStatic/* go/internal/bridge/lib/linux-arm64/

echo "✓ Updated Kotlin/Native libs for Go library"
