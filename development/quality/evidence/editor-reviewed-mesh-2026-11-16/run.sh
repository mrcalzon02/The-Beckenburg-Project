#!/bin/sh
# Compare the two published revisions with Java 17 on PATH.
set -eu
diagnostic=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

compile_run() {
    variant=$1
    revision=$2
    shift 2
    source="$work/$variant/source"
    classes="$work/$variant/classes"
    mkdir -p "$source" "$classes"
    git archive "$revision:src/main/java/org/beckenburg/editor" | tar -x -C "$source"
    javac --release 17 -encoding UTF-8 -d "$classes" "$source"/*.java "$source"/ai/*.java \
        "$diagnostic/UvPreviewAllocation.java" "$diagnostic/$variant/ProfilePath.java"
    echo "$variant $revision"
    java -Xms256m -Xmx256m -XX:-UsePerfData -Djava.awt.headless=true -cp "$classes" \
        org.beckenburg.editor.UvPreviewAllocation "$@"
}

compile_run before f3374fe4c4ded54cc87e52d79fbdf102ab09997d "$@"
compile_run after fb260c38177489f9ff075dd5013de3d6253a5e17 "$@"
