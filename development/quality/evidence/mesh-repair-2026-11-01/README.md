# Actual native weld/repair fixture UI

The actual human importer selects all six source vertices using an X-ray rectangle, previews exact coincident welding, then applies four shared native vertices while retaining the per-corner UV seam and every rendered position. Undo/redo, native save/fresh open and actual runtime publication are exercised. The second capture previews removal of a real zero-area source triangle, retaining the valid face and all vertex IDs.

These are real Swing fixture UI captures, not production model art or complete G2 acceptance. Both operations reuse the same native candidate/apply/history/save/runtime path as face modeling; no agent/MCP operation is added. The max test welds360000 source vertices across120000 triangles into60501 grid vertices under256MiB. Welding is exact and per part; incomplete seams, point-only nonmanifold joins and invalid resulting topology are refused. Arbitrary distance merging and general nonmanifold/orientation repair remain open.
