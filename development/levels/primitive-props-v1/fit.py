#!/usr/bin/env python3
"""Fit graybox props to measured planning zones without mutating game maps.

Creates route-aware, ritual-aware placement records and standalone GLB overlays.
These are AUTHORING STUDIES, never playable scenes or runtime approvals.
No Blender dependency is needed; the canonical individual GLBs are unchanged.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
from pathlib import Path
import build as primitive

HERE=Path(__file__).resolve().parent
LAYOUTS=HERE.parent/"layouts"
GRID=(.12,.13,.15,.18,.21,.24,.28,.33,.40,.50,.60,.67,.72,.76,.79,.82,.85,.87,.88)
ANCHORS={"NW":(.12,.12),"NE":(.88,.12),"SW":(.12,.88),"SE":(.88,.88),
         "N":(.5,.12),"S":(.5,.88),"W":(.12,.5),"E":(.88,.5),"CENTER":(.5,.5)}

def sha(data:bytes)->str:
    return hashlib.sha256(data).hexdigest()

def transform_horizontal(x,z,yaw):
    c,s=math.cos(yaw),math.sin(yaw)
    return x*c-z*s,x*s+z*c

def local_extent(part):
    kind=part["type"]
    if kind in ("box","tri_prism"):
        w,h,d=part["size"]
        if kind=="box":
            roll=part.get("roll",0.0)
            c,s=abs(math.cos(roll)),abs(math.sin(roll))
            return (c*w/2+s*h/2,s*w/2+c*h/2,d/2)
        return w/2,h/2,d/2
    if kind=="frustum":
        r=max(part["bottom_radius"],part["top_radius"])
        half=part["height"]/2
        axis=part.get("axis","y")
        return ((r,half,r) if axis=="y" else
                (half,r,r) if axis=="x" else
                (r,r,half))
    if kind=="cylinder":
        r,half=part["radius"],part["height"]/2
        axis=part.get("axis","y")
        return ((r,half,r) if axis=="y" else
                (half,r,r) if axis=="x" else
                (r,r,half))
    raise ValueError("Unsupported part "+kind)

def footprint(model,yaw,x,z):
    lowx=lowz=float("inf");highx=highz=float("-inf")
    lowy=float("inf");highy=float("-inf")
    for p in model["parts"]:
        sx,sy,sz=local_extent(p)
        px,py,pz=p["p"];yaw_part=p.get("yaw",0)+yaw
        center_x,center_z=transform_horizontal(px,pz,yaw)
        for xx in (-sx,sx):
            for zz in (-sz,sz):
                dx,dz=transform_horizontal(xx,zz,yaw_part)
                wx,wz=x+center_x+dx,z+center_z+dz
                lowx=min(lowx,wx);highx=max(highx,wx)
                lowz=min(lowz,wz);highz=max(highz,wz)
        lowy=min(lowy,py-sy);highy=max(highy,py+sy)
    return (lowx,lowz,highx,highz),lowy,highy

def inflate(rect,pad):
    a,b,c,d=rect
    return a-pad,b-pad,c+pad,d+pad

def overlap(a,b):
    return a[0]<b[2] and a[2]>b[0] and a[1]<b[3] and a[3]>b[1]

def line_hits_box(p,q,rect):
    """Exact segment/axis-aligned-rectangle intersection in plan (not sampled)."""
    ax,az=p;bx,bz=q
    dx,dz=bx-ax,bz-az
    lo,hi=0.0,1.0
    for value,delta,left,right in ((ax,dx,rect[0],rect[2]),
                                   (az,dz,rect[1],rect[3])):
        if abs(delta)<1e-10:
            if value<left or value>right:return False
        else:
            t1=(left-value)/delta;t2=(right-value)/delta
            lo=max(lo,min(t1,t2));hi=min(hi,max(t1,t2))
            if lo>hi:return False
    return True

def zone_contains(rect,bounds,margin):
    x0,z0,x1,z1=bounds
    return (rect[0]>=x0+margin-1e-7 and rect[2]<=x1-margin+1e-7
            and rect[1]>=z0+margin-1e-7 and rect[3]<=z1-margin+1e-7)

def route_clear(rect,zone,layout,recipe):
    y=zone["y"]
    for link in layout["links"]:
        buffer=link["width"]/2+recipe["route_clearance_extra_m"]
        check=inflate(rect,buffer)
        for a,b in zip(link["points"],link["points"][1:]):
            # Clip an inclined connector to the candidate floor's vertical
            # band before checking its plan footprint. Its distant, lower
            # staircase must not block furnishings at the upper landing.
            lo,hi=y-1.0,y+2.2
            dy=b[1]-a[1]
            if abs(dy)<1e-10:
                if not lo<=a[1]<=hi:continue
                t0,t1=0.0,1.0
            else:
                ta=(lo-a[1])/dy;tb=(hi-a[1])/dy
                t0=max(0.0,min(ta,tb));t1=min(1.0,max(ta,tb))
                if t0>t1:continue
            p=(a[0]+(b[0]-a[0])*t0,a[2]+(b[2]-a[2])*t0)
            q=(a[0]+(b[0]-a[0])*t1,a[2]+(b[2]-a[2])*t1)
            if line_hits_box(p,q,check):
                return False
    return True

def ritual_clear(rect,zone,layout,recipe):
    pad=2.2+recipe["ritual_clearance_extra_m"]
    for rite in layout["rituals"]:
        x,y,z=rite["point"]
        if abs(y-zone["y"])>1.5:continue
        if overlap(rect,(x-pad,z-pad,x+pad,z+pad)):return False
    return True

def candidate_positions(zone,preferred):
    pref=ANCHORS[preferred]
    points=[(u,v) for u in GRID for v in GRID]
    points.sort(key=lambda uv: ((uv[0]-pref[0])**2+(uv[1]-pref[1])**2,
                                 abs(uv[0]-.5)+abs(uv[1]-.5),
                                 uv[0],uv[1]))
    a,b,c,d=zone["bounds"]
    for u,v in points:
        yield u,v,a+(c-a)*u,b+(d-b)*v

def fit_target(target,zone,model,layout,recipe,placed):
    if zone["id"] in ("stage","entry") or zone["kind"]=="staging":
        raise ValueError("Do not obstruct van staging or arrival: "+zone["id"])
    yaw=target["yaw"]
    if not math.isfinite(yaw):raise ValueError("Invalid yaw")
    problems=set()
    for u,v,x,z in candidate_positions(zone,target["preferred_anchor"]):
        rect,miny,maxy=footprint(model,yaw,x,z)
        if not zone_contains(rect,zone["bounds"],recipe["wall_clearance_m"]):
            problems.add("wall");continue
        if zone["kind"]=="inside" and maxy-miny>zone["height"]-.18:
            problems.add("ceiling");continue
        if not ritual_clear(rect,zone,layout,recipe):
            problems.add("ritual");continue
        if not route_clear(rect,zone,layout,recipe):
            problems.add("route");continue
        if any(other["zone"]==zone["id"] and
               overlap(inflate(rect,recipe["other_prop_clearance_m"]),other["world_bounds_xz"])
               for other in placed):
            problems.add("other_prop");continue
        result={"id":target["instance"],"zone":zone["id"],"model":model["id"],
                "position":[round(x,4),round(zone["y"]-miny,4),round(z,4)],
                "yaw_rad":yaw,"anchor":[u,v],
                "world_bounds_xz":[round(q,4) for q in rect],
                "floor_elevation":zone["y"],"clearance_checked":True,
                "runtime_approved":False}
        return result
    raise ValueError("NO FIT for "+target["instance"]+" zone="+zone["id"]
                     +" model="+model["id"]+" blockers="+",".join(sorted(problems)))

def world_part(part,instance):
    p=dict(part)
    px,py,pz=p["p"]
    dx,dz=transform_horizontal(px,pz,instance["yaw_rad"])
    x,y,z=instance["position"]
    p["p"]=[round(x+dx,7),round(y+py,7),round(z+dz,7)]
    if part["type"] in ("box","cylinder","frustum"):
        p["yaw"]=round(part.get("yaw",0)+instance["yaw_rad"],7)
    return p

def review_glb(placements,models,zones):
    """Standalone overlay for a 3D viewer, NOT a new playable level shell."""
    parts=[]
    visited=set()
    for instance in placements:
        zone=zones[instance["zone"]]
        if zone["id"] not in visited:
            x0,z0,x1,z1=zone["bounds"]
            parts.append({"type":"box","p":[(x0+x1)/2,zone["y"]-.065,(z0+z1)/2],
                          "size":[x1-x0,.08,z1-z0],"material":"stone"})
            visited.add(zone["id"])
        parts.extend(world_part(p,instance) for p in models[instance["model"]]["parts"])
    model={"id":"review_fit_"+placements[0]["id"].split("-")[0],
           "label":"Measured zone placement review ONLY","parts":parts}
    blob,tri=primitive.glb(model)
    return blob,tri

def render():
    recipe=json.loads((HERE/"fit-recipe.json").read_text(encoding="utf-8"))
    catalog=json.loads((HERE/"catalog.json").read_text(encoding="utf-8"))
    assert recipe["schema"]=="beckenburg.primitive-prop-fit/1"
    models={m["id"]:m for m in catalog["models"]}
    bysite={s["id"]:s for s in catalog["sites"]}
    assert len(recipe["maps"])==5 and sum(len(m["targets"]) for m in recipe["maps"])>=61
    results={};previews={};entries=[]
    for proposed in recipe["maps"]:
        site=proposed["id"]
        path=LAYOUTS/proposed["layout_folder"]/"layout.json"
        raw=path.read_bytes();layout=json.loads(raw)
        if layout["id"]!=site:raise ValueError("Map layout/recipe mismatch "+site)
        if site not in bysite:raise ValueError("Unknown site "+site)
        zones={z["id"]:z for z in layout["zones"]}
        used=set();placed=[]
        for target in proposed["targets"]:
            if target["instance"] in used:raise ValueError("Duplicate instance "+target["instance"])
            used.add(target["instance"])
            zone=zones[target["zone"]];model=models[target["model"]]
            if model["id"] not in bysite[site]["prototypes"]:
                raise ValueError("Prop not assigned to site "+target["model"]+" "+site)
            if model.get("runtime_approved") is not False:
                raise ValueError("Do not use approved production asset as new graybox")
            placed.append(fit_target(target,zone,model,layout,recipe,placed))
        scene,tri=review_glb(placed,models,zones)
        summary={"schema":"beckenburg.primitive-prop-placement/1",
                 "site":site,"title":layout["title"],
                 "layout_source":path.relative_to(HERE.parent).as_posix(),
                 "layout_source_sha256":sha(raw),"status":"review_only",
                 "world_frame":"X east/right; Y elevation up; Z source plan depth",
                 "placements":placed,
                 "runtime_scene_changed":False,"collision_verified":False}
        key=proposed["layout_folder"]
        result=(json.dumps(summary,indent=2,ensure_ascii=False)+"\n").encode("utf-8")
        results[key]=result;previews[key]=scene
        entries.append({"site":site,"placements":len(placed),
                        "review_triangles":tri,"source_layout_sha256":sha(raw),
                        "placement_sha256":sha(result),"preview_sha256":sha(scene)})
    report={"status":"geometric-layout-review-only","runtime_approved":False,
            "sites":len(entries),"placements":sum(e["placements"] for e in entries),
            "checks":["zone membership","floor pivot","wall margin","plan route sweep",
                      "ritual reservation","within-zone prop separation","vertical clearance"],
            "limitations":["Not actual collision/navmesh or playable map acceptance",
                           "Door, stair, ladder and dynamic machinery logic excluded",
                           "Existing production geometry and scene records untouched"],
            "maps":entries}
    return results,previews,(json.dumps(report,indent=2)+"\n").encode("utf-8")

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--check",action="store_true")
    args=ap.parse_args()
    placements,scenes,report=render()
    outputs={HERE/"fit-verification.json":report}
    for site,data in placements.items():
        outputs[HERE/"placements"/(site+".json")]=data
    for site,data in scenes.items():
        outputs[HERE/"previews"/(site+".glb")]=data
    for path,content in outputs.items():
        if args.check:
            if not path.is_file() or path.read_bytes()!=content:
                raise SystemExit("Missing or stale placement review: "+str(path))
        else:
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(content)
    report=json.loads(report)
    print("FITTED",report["sites"],"MAPS",report["placements"],"STATIC PROPS",
          "MODE",("check" if args.check else "build"))
if __name__=="__main__":main()
