# Castle saved-scene rebuild

The three property files were exported by their actual historical Java sources;
`source-manifest.json` binds each file to its source commit and SHA-256.
`export_baselines.py` reproduces the exports in isolated build directories using
Java 17 and the existing packaged runtime resources. Original baseline bytes are
also shipped as immutable, hash-checked native resources.

An unchanged known built-in scene upgrades automatically. A changed scene opens
unchanged. In the Castle editor, use **VERIFY SCENE → REBUILD CASTLE / RETAIN EDITS**
and select the built-in layout on which the scene was authored. The operation
uses the same native Editor Agent Service and undo history as ordinary edits.
It preserves changed, added and deleted records through a three-way merge.
It does not save implicitly. Use UNDO to revert or SAVE to persist the result.

Actual retained props are checked against rebuilt masonry, new fittings and
support surfaces. The merged native scene must retain all 39 accessible spaces,
three permanent seals and nine station approaches from its saved spawn.
A conflict rejects the complete transaction and identifies the retained object;
resolve it in the original scene before retrying. This is bounded Castle
validation, not acceptance of arbitrary custom geometry or other locations.

The first saved generation transition archives the original document bytes as
`castle.properties.before-first-principles.properties`, in addition to the
normal rotating previous-save journal. Subsequent loads and repeated rebuilds
preserve current-generation deletions. Authored structure-v1 scenes retain their
payload; partial or unsupported documents are refused without overwriting them.

The four `castle-native-upgrade` captures show the actual saved custom scene
after rebuilding: retained moved chair/crate, museum display, annex and cellar.
The dedicated regression checks native transactions, undo/redo, persistence,
backups, collision/containment/duplicate/support rejection and old-format safety.
