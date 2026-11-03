# Actual native OBJ export and cleanup fixture

The actual human editor exports through its Swing Save chooser, reopens the copy using the human importer, retains welded connectivity and a per-corner UV seam, and publishes the result through the game decoder. Every runtime position/UV/winding and native archive is compared. The cleanup fixture previews removal of one unused vertex after degenerate-face removal. It exercises Apply, Undo, Redo, native save/reopen and runtime publication. These captures are fixture UI, not production art or full G2 acceptance.

The independent editor-JAR test checks separate coincident identities, deterministic bytes, flat normal facing, current native owner/revision, conflicting export revisions, unchanged history/dirty-state and refusal to drop unused vertices silently. The shared document store validates staged readback before atomic replacement. Native BTOP remains authoritative. Numeric native IDs are not claimed to survive OBJ; connectivity does. Custom authored normals, MTL/texture interchange and advanced modeling remain open. No mesh operation is exposed to agents.

Local Java17 full compile/package and independent JAR export/maximum weld/face-modeling checks pass. Full native host suite passes, including the real human chooser and CLI. Source-specific remote acceptance is recorded in the recovery checkpoint after publication.
