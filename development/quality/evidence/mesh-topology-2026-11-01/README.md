# Actual human import UI captures

Both PNGs are real Swing component captures from AssetImportTopologyGuiTest, showing a four-face test OBJ in the existing human importer. `import-review.png` shows the corrected report and derived topology review; `import-review-before-newline-fix.png` preserves the observed DEV-189 formatting error. They are UI/fixture evidence, not authored map/model art or installed gameplay acceptance.

The test presses the real Publish button, resolves the resulting GLB through the game decoder and verifies unchanged source/archive bytes. The topology remains read-only at this checkpoint; no editing, remeshing, material fidelity or completed G2 gate is asserted.
