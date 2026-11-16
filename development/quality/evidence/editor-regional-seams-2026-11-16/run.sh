#!/bin/sh
# Run with a Java 17 JDK on PATH. The gltf mode also requires Node.js.
set -eu
mode=${1:-}
case "$mode" in allocation|rows|window|gltf) ;; *) echo "Usage: sh run.sh allocation|rows|window|gltf [validator.tgz]" >&2; exit 2 ;; esac
diagnostic=$(CDPATH='' cd -- "$(dirname -- "$0")" && pwd)
root=$(git -C "$diagnostic" rev-parse --show-toplevel)
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
baseline=f17b806c0ed43d30d9ed250582543e7bbaeec0c6
candidate=8173f2edb367da3b602eb045080c90a855fc3441
if [ "$mode" = rows ] || [ "$mode" = window ]; then
    baseline=$candidate
    candidate=494a257c91c9f42afef40510581705b9d772c3fb
fi
if [ "$mode" = window ]; then
    printf 'variant\tsample\toperation\tcanvasWidth\tcanvasHeight\tvisibleRows\tobserveValidateMs\tfullPaintMs\tobserveAndFullPaintMs\tuvClipRepaintMs\tlistClipRepaintMs\n'
elif [ "$mode" = rows ]; then
    printf 'variant\tsample\toperation\tadded\tremoved\tchanged\tcovered\tselectionEvents\tbytes\tms\n'
fi
editor=src/main/java/org/beckenburg/editor
tests=src/test/java/org/beckenburg/editor

for variant in before after; do
    output="$work/$variant"
    source="$output/source"
    mkdir -p "$output/classes" "$source"
    revision=$baseline
    if [ "$variant" = after ]; then revision=$candidate; fi
    git -C "$root" archive "$revision" "$editor" "$tests/EditorGlbAuthoringTest.java" "$tests/EditorMeshMaterialTest.java" > "$output/source.tar"
    tar -xf "$output/source.tar" -C "$source"
    if [ "$mode" != gltf ]; then
        javac -J-XX:-UsePerfData --release 17 -encoding UTF-8 -d "$output/classes" "$source/$editor"/*.java "$source/$editor"/ai/*.java \
            "$diagnostic/UvPreviewAllocation.java" "$diagnostic/MaterialRowsProfile.java" "$diagnostic/MaterialWindowProfile.java" \
            "$diagnostic/../editor-reviewed-mesh-2026-11-16/after/ProfilePath.java"
        if [ "$mode" = window ]; then
            java -Xms256m -Xmx256m -XX:-UsePerfData -Djava.awt.headless=false -cp "$output/classes" \
                org.beckenburg.editor.MaterialWindowProfile "$variant"
            continue
        fi
        if [ "$mode" = rows ]; then
            java -Xms256m -Xmx256m -XX:-UsePerfData -Djava.awt.headless=true -cp "$output/classes" \
                org.beckenburg.editor.MaterialRowsProfile "$variant"
            continue
        fi
        for ids in dense sparse; do
            echo "$variant-$ids"
            if [ "$ids" = sparse ]; then set -- --sparse; else set --; fi
            java -Xms256m -Xmx256m -XX:-UsePerfData -Djava.awt.headless=true -cp "$output/classes" \
                org.beckenburg.editor.UvPreviewAllocation "$@"
        done
    else
        javac -J-XX:-UsePerfData --release 17 -encoding UTF-8 -d "$output/classes" "$source/$editor"/*.java "$source/$editor"/ai/*.java \
            "$source/$tests/EditorGlbAuthoringTest.java" "$source/$tests/EditorMeshMaterialTest.java" "$diagnostic/ValidatorFixtures.java"
        java -XX:-UsePerfData -Djava.awt.headless=true -cp "$output/classes" org.beckenburg.editor.ValidatorFixtures "$output/models" "$diagnostic/alpha-source.glb"
    fi
done

if [ "$mode" = gltf ]; then
    archive=${2:-"$work/validator.tgz"}
    if [ "$#" -lt 2 ]; then
        curl -fsSL 'https://registry.npmjs.org/gltf-validator/-/gltf-validator-2.0.0-dev.3.10.tgz' -o "$archive"
    fi
    expected=4e03dbdc3bc0d1342afd5d2d7ae341a7e3474502ccb872b02dbbaddaee0fdec6
    printf '%s  %s\n' "$expected" "$archive" | sha256sum -c - >&2
    tar -xzf "$archive" -C "$work"
    node "$diagnostic/validate.cjs" "$work" > "$work/validator-results.tsv"
    cat "$work/validator-results.tsv"
    diff -u "$diagnostic/validator-results.tsv" "$work/validator-results.tsv"
fi
