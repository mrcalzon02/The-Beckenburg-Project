"""Render the shared defect ledger without dropping historical identities or notes."""
import argparse
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
FOLDER = ROOT / "development/quality"


def render_ledger(data):
    issues = data["issues"]
    ids = [issue["id"] for issue in issues]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate canonical defect IDs; reconcile identities before rendering")

    reconciliation = data.get("id_reconciliation", {})
    aliases = {alias["canonical_id"]: alias for alias in reconciliation.get("aliases", [])}
    if not set(aliases).issubset(ids):
        raise ValueError("A historical alias has no canonical defect record")

    lines = [
        "# Development defect ledger", "",
        "Every discovered error, regression, placement problem or unresolved issue belongs here. "
        "`defects.json` is authoritative, including retained `ledger_notes`; regenerate this view "
        "with `python tools/build_defect_ledger.py` or verify it with `--check`. "
        "A scoped fix never certifies unaudited maps.", "",
        "[Required fitness checks](MAP_FITNESS_CHECKLIST.md) · "
        "[Screenshot gallery](../../beckenburg-development-gallery.html)", "",
        "## Required issue record", "",
        "Stable ID, symptom, cause or uncertainty, affected sites, reproduction/evidence, "
        "fix/status, verification and older-map audit obligations. Add discoveries immediately, "
        "including issues introduced by fixes; do not delete failed evidence.", "",
    ]
    if reconciliation:
        lines += [
            "## Identity and evidence scope", "",
            f"[{reconciliation['date']} identity reconciliation]({reconciliation['record']}) "
            "maps historical branch IDs and titles to unique canonical records. "
            "Historical references retain their original source meaning; a bare old ID may be ambiguous.", "",
            reconciliation["status_scope"], "",
        ]
    lines += ["## Recorded defects", ""]
    for issue in issues:
        lines += [f"### {issue['id']} — {issue['title']}", ""]
        alias = aliases.get(issue["id"])
        if alias:
            origins = dict.fromkeys((origin["branch"], origin["revision"]) for origin in alias["origins"])
            provenance = "; ".join(f"`{branch}` at `{revision}`" for branch, revision in origins)
            lines += [
                f"Historical identifier: **{alias['original_id']}** — {alias['title']}. "
                f"Source: {provenance}. "
                f"[Alias mapping]({reconciliation['record']}).", "",
            ]
        lines += [
            f"**Status:** {issue['status']} · **Affected:** {issue['affected']}", "",
            issue["symptom"], "",
            f"Cause / uncertainty: {issue['cause']}", "",
            f"Fix / next action: {issue['action']}", "",
            f"Evidence and verification: {issue['evidence']}", "",
            f"Older-map obligation: {issue['older_maps']}", "",
        ]
    lines += ["## Required audit categories", ""]
    lines += [f"- {category}" for category in data["audit_categories"]]
    lines += [
        "", "## Older-map review status", "",
        "Review each location independently against the complete fitness checklist. "
        "Consult its source-specific evidence and the "
        "[map-stage register](../levels/rebuild/stage-register.json) before changing its status. "
        "Existing individual passing tests remain valid within their recorded revision and scope; "
        "this ledger does not grant complete-map acceptance.", "",
        "## Continuity", "",
        "At every map/model checkpoint: update open issues; check for related defects in reused "
        "assets; record new checks and remaining limitations; refresh the shared gallery; commit "
        "and push actual evidence. Before a fix is marked complete, rerun the checks it can affect "
        "(especially stairs after changing floor depth).", "",
    ]
    rendered = "\n".join(lines)
    for note in data.get("ledger_notes", []):
        text = note["text"]
        if hashlib.sha256(text.encode("utf-8")).hexdigest() != note["sha256_utf8"]:
            raise ValueError(f"Retained note text changed without an updated digest: {note['heading']}")
        rendered += (
            f"\n## {note['heading']}\n\n"
            f"Preserved verbatim from `{note['source_path']}` on `{note['branch']}` at "
            f"`{note['revision']}`. Original IDs and verification statements retain that "
            "historical scope. The authoritative text is stored in `defects.json` under "
            "`ledger_notes`, so regeneration preserves these additions.\n"
        )
        rendered += text
    if data.get("ledger_notes"):
        rendered += "\n<!-- End of preserved historical ledger notes. -->\n"
    return rendered.encode("utf-8")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Verify the generated ledger without writing")
    args = parser.parse_args()
    data = json.loads((FOLDER / "defects.json").read_text(encoding="utf-8"))
    rendered = render_ledger(data)
    target = FOLDER / "DEFECT_LEDGER.md"
    if args.check:
        if not target.exists() or target.read_bytes() != rendered:
            parser.exit(1, "Defect ledger is stale; run python tools/build_defect_ledger.py\n")
        print(f"Verified shared ledger: {len(data['issues'])} unique issues and retained notes")
        return
    target.write_bytes(rendered)
    print(f"Updated shared ledger: {len(data['issues'])} unique issues and retained notes")


if __name__ == "__main__":
    main()
