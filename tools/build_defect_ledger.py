"""Render the shared defect ledger from its authoritative structured records."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'development/quality'
d=json.loads((folder/'defects.json').read_text(encoding='utf-8'))
lines=['# Development defect ledger','','Every discovered error, regression, placement problem or unresolved issue belongs here. `defects.json` is authoritative; regenerate this view with `python tools/build_defect_ledger.py`. A scoped fix never certifies unaudited maps.','','[Required fitness checks](MAP_FITNESS_CHECKLIST.md) · [Screenshot gallery](../../beckenburg-development-gallery.html)','','## Required issue record','','Stable ID, symptom, cause or uncertainty, affected sites, reproduction/evidence, fix/status, verification and older-map audit obligations. Add discoveries immediately, including issues introduced by fixes; do not delete failed evidence.','','## Recorded defects','']
for issue in d['issues']:
    lines += [f"### {issue['id']} — {issue['title']}",'',f"**Status:** {issue['status']} · **Affected:** {issue['affected']}",'',issue['symptom'],'',f"Cause / uncertainty: {issue['cause']}",'',f"Fix / next action: {issue['action']}",'',f"Evidence and verification: {issue['evidence']}",'',f"Older-map obligation: {issue['older_maps']}",'']
lines+=['## Required audit categories','']+[f"- {a}" for a in d['audit_categories']]
lines+=['','## Older-map review status','','House, Farmhouse, School, Church, Castle and all other previously authored areas remain **not audited against the complete new checklist**. Existing individual passing tests remain valid within their scope. Review each location independently and attach evidence before changing its audit status. The 17 rough blockouts remain rough, not failed or completed playable maps.','','## Continuity','','At every map/model checkpoint: update open issues; check for related defects in reused assets; record new checks and remaining limitations; refresh the shared gallery; commit and push actual evidence. Before a fix is marked complete, rerun the checks it can affect (especially stairs after changing floor depth).','']
(folder/'DEFECT_LEDGER.md').write_bytes('\n'.join(lines).encode('utf-8'))
print('Updated shared ledger:',len(d['issues']),'issues')
