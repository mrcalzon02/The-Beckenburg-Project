#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/editor-kit
rm -rf build/editor-kit/classes
mkdir -p build/editor-kit/classes
javac --release 17 -encoding UTF-8 -d build/editor-kit/classes src/main/java/org/beckenburg/editor/*.java
jar --create --file build/editor-kit/LevelEditorKit-Java17.jar -C build/editor-kit/classes org/beckenburg/editor
echo "Built standalone build/editor-kit/LevelEditorKit-Java17.jar"
