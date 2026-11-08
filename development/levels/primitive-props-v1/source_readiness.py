#!/usr/bin/env python3
"""Deterministic source-mesh readiness inventory; explicitly NOT production approval.

Records every model's GLB integrity, source dimensions, preview anchor and
remaining downstream responsibilities. Warnings are diagnostics, not release
failures or authorization to add source geometry to live scenes.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import fit
from audit import read_glb

ROOT=Path(__file__).resolve().parent
OUTPUT=ROOT/"readiness-report.json"

def digest(value: bytes)->str:
    return hashlib.sha256(value).hexdigest()

def render()->bytes:
    raw=(ROOT/"catalog.json").read_bytes()
    catalog=json.loads(raw)
    manifest=json.loads((ROOT/"verification.json").read_text(encoding="utf-8"))
    if len(catalog["models"])!=len(manifest["models"]):
        raise ValueError("Source catalog and generated GLB manifest counts differ")
    manifest_by_id={r["id"]:r for r in manifest["models"]}
    if len(manifest_by_id)!=len(manifest["models"]):
        raise ValueError("Duplicate generated model IDs")
    site_counts={site["id"]:len(site["prototypes"]) for site in catalog["sites"]}
    items=[]
    warnings=[]
    for model in catalog["models"]:
        name=model["id"]
        if model["runtime_approved"] is not False:
            raise ValueError("Unexpected approved production asset "+name)
        if name not in manifest_by_id:
            raise ValueError("Missing generated GLB "+name)
        record=manifest_by_id[name]
        path=ROOT/"models"/(name+".glb")
        doc,binary=read_glb(path)
        if digest(binary)!=record["sha256"] or len(binary)!=record["bytes"]:
            raise ValueError("Stale generated binary "+name)
        bbox,lo,hi=fit.footprint(model,0,0,0)
        width=round(bbox[2]-bbox[0],4)
        depth=round(bbox[3]-bbox[1],4)
        low=round(lo,4)
        high=round(hi,4)
        hints=model.get("authoring_hints",{})
        mount=hints.get("mount","unclassified")
        tags=[]
        if mount=="floor" and abs(lo)>.08:
            tags.append("review_floor_contact_offset")
        if mount in ("wall","ceiling","roof","opening"):
            tags.append("review_attachment_anchor")
        if mount in ("structure","terrain","water","display_surface"):
            tags.append("review_mount_constraint")
        # The kit builder sizes stands up to 6.6m on 7m centers.
        plate_width=max(5.65,2*max(abs(bbox[0]),abs(bbox[2]))+.32)
        plate_depth=max(5.60,2*max(abs(bbox[1]),abs(bbox[3]))+.32)
        if plate_width>6.6 or plate_depth>6.6:
            tags.append("review_gallery_stand_overflow")
        if high-low>4.2:
            tags.append("review_vertical_scale")
        if not hints or hints.get("mount_confidence")!="provisional_unreviewed":
            tags.append("review_mount_contract_missing")
        if hints.get("source_pivot_reviewed") is not False or hints.get("renderer_orientation_reviewed") is not False:
            raise ValueError("Unreviewed source pivot falsely accepted: "+name)
        if hints.get("runtime_placement") is not False:
            raise ValueError("Source-only model prematurely registered: "+name)
        if tags:
            warnings.append({"model":name,"diagnostics":tags})
        assigned=[s["id"] for s in catalog["sites"] if name in s["prototypes"]]
        if set(assigned)!=(set(site_counts) if "*" in model["maps"] else set(model["maps"])):
            raise ValueError("Mismatch between per-model map assignments and kits: "+name)
        if doc["extras"]["runtimeApproved"] is not False:
            raise ValueError("Generated GLB approved itself "+name)
        items.append({
            "id":name,
            "label":model["label"],
            "maps":assigned,
            "source_parts":len(model["parts"]),
            "tapered_parts":sum(part["type"]=="frustum" for part in model["parts"]),
            "source_materials":sorted(set(part["material"] for part in model["parts"])),
            "triangles":record["triangles"],
            "glb_sha256":record["sha256"],
            "model_bounds_xz": [round(x,4) for x in bbox],
            "width_m":width,
            "depth_m":depth,
            "min_y_m":low,
            "max_y_m":high,
            "mount_hint":mount,
            "mount_confidence":hints.get("mount_confidence","missing"),
            "source_pivot_reviewed":False,
            "renderer_orientation_reviewed":False,
            "diagnostics":tags,
            "source_glb_verified":True,
            "native_renderer_accepted":False,
            "collision_and_navmesh_accepted":False,
            "runtime_editor_registered":False,
            "production_approved":False
        })
    output={
        "schema":"beckenburg.source-mesh-readiness/1",
        "status":"SOURCE_ONLY_REVIEW_NOT_PRODUCTION_APPROVAL",
        "catalog_sha256":digest(raw),
        "model_count":len(items),
        "map_kit_count":len(site_counts),
        "validated_source_glbs":len(items),
        "runtime_approved_models":0,
        "native_rendered_models_accepted_here":0,
        "models_with_diagnostic_flags":len(warnings),
        "purpose":"Recorded geometry and next-stage review; never game asset approval",
        "required_before_production":[
            "Native four-view in-engine screenshots and texture/material inspection",
            "Editor transform/pivot and surface attachment validation",
            "Collision, navmesh and player interaction inspection where appropriate",
            "Actual level geometry and lighting/occlusion fit",
            "Explicit approved runtime model registration and tested scene placement"
        ],
        "map_kit_assignments":site_counts,
        "diagnostics":warnings,
        "models":items
    }
    return (json.dumps(output,indent=2,ensure_ascii=False)+"\n").encode("utf-8")

def main():
    a=argparse.ArgumentParser()
    a.add_argument("--check",action="store_true")
    args=a.parse_args()
    payload=render()
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_bytes()!=payload:
            raise SystemExit("Missing or stale source readiness report")
    else:
        OUTPUT.write_bytes(payload)
    report=json.loads(payload)
    print("PASS readiness source inventory:",report["model_count"],"model GLBs,",
          report["map_kit_count"],"map kits,",
          report["models_with_diagnostic_flags"],"authoring diagnostics,",
          report["runtime_approved_models"],"runtime approvals,",
          "mode",("check" if args.check else "generate"))

if __name__=="__main__":
    main()
