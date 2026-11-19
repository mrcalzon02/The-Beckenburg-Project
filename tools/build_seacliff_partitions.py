"""Build the Battery's lower service partitions on their existing concrete slabs."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_coast_entrances import Structure
from build_college_electrical import point
from build_college_doors import validate_volume
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild/seacliff-power-pass"
STEM = "lower-service-partitions"
LEAF = ROOT / "assets/runtime/models/door_1_loose.glb"

# Full bearing runs at the shared walls. The north ends seat against the
# retained perimeter. The other ends meet the continuous lower gallery wall.
BAYS = (
    ("barracks east partition","x",24,(14,25.88),
     (("barracks-records",20,1.4,2.22,("barracks","records")),)),
    ("records east partition","x",34,(14,25.88),
     (("records-pump",20,1.4,2.22,("records","pump")),)),
    ("plant west partition","x",44,(14,25.88),
     (("pump-plant",20,1.4,2.22,("pump","plant")),)),
    ("lower gallery north partition","z",26,(24,44),
     (("records-gallery",29,1.4,2.22,("records","lowgallery")),
      ("pump-gallery",39,1.4,2.22,("pump","lowgallery")))),
)
REPLACED_FACES = (("barracks","E"),("records","W"),("records","E"),("pump","W"),
                  ("pump","E"),("plant","W"),("records","S"),("pump","S"),("lowgallery","N"))


def write(path, value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+"\n",encoding="utf-8",newline="\n")


def guide(layout, apertures):
    # Assembly outputs determine the threshold coordinates. Guide points
    # stay on the retained slabs and turn inside the room between doors.
    f = {door["id"]: door["frame_center"] for door in apertures}
    placed = {
        "route-13": ([[16,0,20], f["barracks-records"], [29,0,20], f["records-gallery"], [29,0,29], [34,0,30]],
            ["barracks","records","lowgallery"], ["barracks-records","records-gallery"]),
        "route-14": ([[34,0,30],[39,0,30],f["pump-gallery"],[39,0,20],f["pump-plant"],[52,0,20]],
            ["lowgallery","pump","plant"], ["pump-gallery","pump-plant"]),
        "route-15": ([[34,0,30],[29,0,30],f["records-gallery"],[29,0,20]],
            ["lowgallery","records"], ["records-gallery"]),
        "route-16": ([[29,0,20],f["records-pump"],[39,0,20]],
            ["records","pump"], ["records-pump"]),
        "route-17": ([[39,0,20],f["pump-plant"],[52,0,20]],
            ["pump","plant"], ["pump-plant"]),
    }
    for link in layout["links"]:
        if link["id"] in placed:
            link["points"],link["architectural_rooms"],link["fitted_thresholds"]=placed[link["id"]]
            link["construction"]="Room-to-room guide on the existing slabs through the fitted frames."
    return placed


def build(out):
    out.mkdir(parents=True,exist_ok=True)
    source={ext:BASE/"wld-17"/("powered-service-stack."+ext) for ext in ("blend","glb","hcs2")}
    meta=json.loads((BASE/"wld-17/construction.json").read_bytes())
    assert all(sha(source[k])==meta[h] for k,h in (("blend","blend_sha256"),("glb","glb_sha256"),("hcs2","physics_sha256")))
    layout=json.loads((BASE/"wld-17/authored-layout.json").read_bytes())
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    original={o.name:o for o in bpy.context.scene.objects if o.type=="MESH"}
    structure=Structure("Battery lower",original)
    structure.retired.clear()
    structure.retire_faces(*REPLACED_FACES)
    old=native_records(source["hcs2"])
    assert structure.retired <= {p[0].split(":",1)[-1] for p in old}
    for name in structure.retired:
        obj=original[name]
        verts=[obj.matrix_world@v.co for v in obj.data.vertices]
        assert min(v.z for v in verts)>=-.008 and max(v.z for v in verts)<=3.01,name
    material=original["Flat route shell Wall records E"].data.materials[0]
    for label,axis,boundary,ends,frames in BAYS:
        structure.face(label,axis,boundary,ends,0,3,material,frames,label)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    for name in sorted(structure.retired):
        bpy.data.objects.remove(original[name],do_unlink=True)
    for obj in structure.built:
        validate_volume(obj)
    floor=bvh([obj for name,obj in original.items() if name not in structure.retired
               and obj.get("role")=="floor" and obj.get("zone") in ("barracks","records","pump","plant","lowgallery")])
    bearings=0
    for slot,d in enumerate(structure.portals,421):
        x,z=((d["boundary"],d["along"]) if d["axis"]=="x" else (d["along"],d["boundary"]))
        d.update(frame_center=[x,0,z],frame_span=[d["boundary"]-.12,d["boundary"]+.12],
                 portal_id=slot,leaves=1,hinge_side=-1,swing_direction=1,leaf_sha256=sha(LEAF))
        for n in (-.8,0,.8):
            for across in (-.52,0,.52):
                bx,bz=(x+n,z+across) if d["axis"]=="x" else (x+across,z+n)
                hit,normal,_,_=floor.ray_cast(point(bx,.14,bz),Vector((0,0,-1)),.26)
                assert hit is not None and abs(hit.z)<.003 and normal.z>.98,(d["id"],bx,bz)
                bearings+=1
    guide(layout,structure.portals)
    layout["authored_doorways"]+=structure.portals

    targets={ext:out/(STEM+"."+ext) for ext in source}
    bpy.ops.object.select_all(action="DESELECT")
    for o in structure.built:
        o.select_set(True)
    temp=out/"_additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(temp),export_format="GLB",use_selection=True,export_extras=True)
    append(source["glb"],temp,targets["glb"],remove_mesh_names=structure.retired)
    native_write(source["hcs2"],targets["hcs2"],structure.retired,structure.built)
    retained=[p for p in old if p[0].split(":",1)[-1] not in structure.retired]
    native=native_records(targets["hcs2"])
    assert native[:len(retained)]==retained
    extra=native[len(retained):]
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in extra:
        visible=[t for t,_ in records(bpy.context.scene.objects[part[0]])]
        assert match_geometry(triangles(part),visible),part[0]

    hashes={ext:sha(path) for ext,path in targets.items()}
    prior=(BASE/"verification/runtime.tsv").read_text(encoding="utf-8").splitlines()
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows += [tuple(line.split("\t")) for line in prior[1:] if not line.startswith("LINK\t")]
    for d in structure.portals:
        rows.append(("BAY",d["portal_id"],d["id"],*d["rooms"],*d["frame_center"],d["clear_width"],
                     d["axis"],*d["frame_span"],d["clear_height"],d["hinge_side"],d["swing_direction"],d["leaf_sha256"]))
    for route in layout["links"]:
        rows.extend(("LINK",route["id"],*p) for p in route["points"])
    (out/"runtime.tsv").write_text("".join("\t".join(map(str,r))+"\n" for r in rows),encoding="utf-8",newline="\n")
    write(out/"authored-layout.json",layout)
    write(out/"partitions.json",dict(source={k:sha(p) for k,p in source.items()},export=hashes,
          footprints=[dict(label=name,axis=axis,boundary=plane,ends=ends) for name,axis,plane,ends,_ in BAYS],
          fitted_frames=structure.portals,retired_wall_components=sorted(structure.retired),
          receiving_floor_probes=bearings,retained_native_components=len(retained),
          actual_new_surface_triangles=sum(p[2] for p in extra)))
    print("PASS five doors",bearings,"slab bearings",len(retained),"retained source parts")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
