# Shared development gallery

Published on the project's existing main/root GitHub Pages site:
https://mrcalzon02.github.io/The-Beckenburg-Project/beckenburg-development-gallery.html

The concept-art gallery remains on the project landing page. This linked gallery
indexes original development PNGs from previews, captures, screenshots and rough
blockouts without moving, repainting or replacing their bytes. It includes image
type labels, search, filtering, pagination, full-resolution links, GitHub history,
dimensions and SHA-256 metadata.

After every new map/model screenshot:

1. Save the actual PNG in a development `previews`, `captures` or `screenshots`
   folder. Give controlled fixtures and studio images honest names/captions.
2. Run `python tools/build_development_gallery.py` and `--check`.
3. Commit the PNGs, root gallery HTML and `development/gallery/index.json` with
   the implementation; reconcile and push main without force.
4. Wait for the normal Pages build, then run
   `python tools/verify_development_gallery.py` and commit its report.

CI rejects a stale index so future captures cannot silently be omitted. It does
not upload uncommitted files or claim a failed deployment succeeded. The live
verifier reads the page, manifest, every PNG URL/header/dimensions and full hashes
for the current worklight/defect/Hollow Creek batches. Add new batch paths to its
full-hash list as work advances. No separate scheduled automation is required.
