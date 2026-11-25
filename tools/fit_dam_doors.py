"""Fit canonical operating leaves to Blackwater Dam's measured openings."""
import json
from pathlib import Path
import sys

import bpy

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_architectural_doorways import MODEL, frame_template
from build_circulation_construction import native_write
from build_college_grounds import import_scene
from build_dam_spillway_gallery import rel, validate_solid
from build_deliberate_entrances import solid, write
from build_prison_sally_gate import export
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from fit_northwatch_doors import measure
from prepare_cemetery_doors import SOURCE_LEAF, write_tsv
from review_roster_macroterrain import records, sha
from verify_college_services import bvh

BASE=ROOT/"development/levels/rebuild"
PRIOR=BASE/"dam-valley-pass/roster.json"
TAILRACE=BASE/"dam-tailrace-pass/wld-09"
STAGE=BASE/"dam-door-pass"
OUT=STAGE/"wld-09"
GLB,NATIVE,BLEND=(OUT/("door-fitted-dam"+ext) for ext in (".glb",".hcs2",".blend"))
STATUS="DAM_FITTED_DOORS_AND_OWNED_CIRCULATION_STAGED"

# These six masonry openings existed in the legacy shell at the listed
# wall datums. Only their unframed edges receive new jambs and headers.
LEGACY=(
    ("crest-control","z",48,17,0,2.0),
    ("control-office","z",34,17,0,1.5),
    ("turbine-plant","z",22,45,-6,1.5),
    ("gallery-valve","z",32,43,-10,1.5),
    ("gallery-return","z",32,57,-10,1.5),
    ("valve-return","x",50,40,-10,1.5),
)
# Each ID is a real threshold. Upper and lower stair landings use different
# architectural volumes while keeping their stable gameplay owner.
DOORS=(
    (870,"crest-control","control","crest",2,1,-1),
    (871,"control-office","office","control",1,1,-1),
    (872,"DAM-HALL-x-26-28-0","office","west-stair",1,1,-1),
    (873,"DAM-HALL-z-48-31-0","west-stair","crest",2,1,1),
    (874,"DAM-HALL-x-36-35--6","west-lower-stair","turbine",2,1,1),
    (875,"WLD-09-restart-shared turbine workshop north","turbine","workshop",2,1,1),
    (876,"DAM-HALL-x-64-35--6","turbine","east-lower-stair",2,1,-1),
    (877,"DAM-HALL-z-48-68-0","east-stair","crest",2,1,1),
    (878,"DAM-HALL-x-70.5-47-0","east-stair","east-service-catwalk",1,1,1),
    (879,"DAM-SPILLWAY-LOWER-68","east-lower-stair","spillway",2,1,1),
    (880,"turbine-plant","plant","turbine",1,1,-1),
    (881,"gallery-valve","gallery","valve",1,-1,1),
    (882,"gallery-return","gallery","return",1,1,1),
    (883,"valve-return","valve","return",1,1,1),
)


def landing_guidance(layout):
    # A body crossing an exact tread edge may still be on the preceding
    # riser. Place these guide knots within the supported landing slabs.
    landings={
        (31,0,45):[31,0,45.23],(67,0,45):[67,0,45.23],
        (31,-6,25):[31,-6,24.78],(67,-6,25):[67,-6,24.78],
        (56,-10,23.5):[56,-10,23.26],(56,-6,30.5):[56,-6,30.74],
        (60.3,-10,36.4):[60.3,-10,36.19],(62.5,-6,36.4):[62.5,-6,36.19],
        (31,-3,35.9):[31,-3,35.70],(31,-3,34.1):[31,-3,34.30],
        (67,-3,35.9):[67,-3,35.70],(67,-3,34.1):[67,-3,34.30],
        (60.3,-8,40):[60.3,-8,40.22],(62.5,-8,40):[62.5,-8,40.22],
    }
    normal_to_door={
        "L13":[[50,-6,35],[45,-6,23.20],[45,-6,20.80],[45,-6,15]],
        "L15":[[50,-10,27],[43,-10,30.80],[43,-10,33.20],[43,-10,40]],
    }
    for link in layout["links"]:
        link["points"]=normal_to_door.get(link["id"],[landings.get(tuple(p),p) for p in link["points"]])


def old_frames(objects):
    template=frame_template()
    built=[]
    frames=[]
    for label,axis,wall,along,y,width in LEGACY:
        # Existing masonry is 160 mm. The steel casing covers the reveal
        # without reducing the original clear floor width.
        normal_half=.106
        height=2.37
        obj=bpy.data.objects.new("Dam indexed casing "+label,template.data.copy())
        bpy.context.scene.collection.objects.link(obj)
        for vert,orig in zip(obj.data.vertices,template.data.vertices,strict=True):
            p=template.matrix_world @ orig.co
            u=p.x*width
            v=p.y*normal_half/.10
            h=y+p.z*height/2.125
            vert.co=(along+u,-wall+v,h) if axis=="z" else (wall-v,-along+u,h)
        obj["role"]="Indexed steel jamb and head fitted over the existing masonry reveal"
        obj["source_asset"]=MODEL.stem
        obj.data.update()
        built.append(obj)
        frames.append(dict(id=label,axis=axis,boundary=wall,along=along,sill_y=y,
                           clear_width=width,clear_height=height,frame_span=[wall-normal_half,wall+normal_half]))
    bpy.data.objects.remove(template,do_unlink=True)
    # A low, continuous aperture continued east of the valve-room jamb
    # under its old header. Seat a solid return in the original wall plane.
    surface=objects["Flat route shell Wall gallery S.001"].data.materials[0]
    return_pier=solid("Dam lower gallery restored valve jamb return",(43.75,31.921,44.225,32.079),
                      -10,-7.779,surface,"Masonry closure beside the existing valve doorway","gallery")
    built.append(return_pier)
    bpy.context.view_layer.update()
    for obj in built:
        validate_solid(obj)
    return frames,built


def schedule(layout,frames):
    original=(TAILRACE/"runtime-zones.tsv").read_text(encoding="utf8").splitlines()
    rows=[row.split("\t") for row in original]
    owners={row[1]:int(row[2]) for row in rows if row[0] in ("ROOM","SPACE")}
    stage=next(row for row in rows if row[0]=="ROOM" and row[1]=="stage")
    stage[11:]=["39.8","0","80.3"]
    rows.extend((
        ("SPACE","crest-forecourt-transition",12,35.8,56,44,62,0,3,"true","Service forecourt approach",40,0,59),
        ("SPACE","haul-flight",6,54.9,22.55,57.1,31.2,-10,4.05,"false","Gallery haul stair",56,-10,23.5),
        ("SPACE","return-flight",8,59.25,34.6,63.55,42,-10,4.05,"false","Lower return stair bay",62.5,-8,40.88),
        ("OPEN",900,13,14,36,0,78.372,2.15,"z","trail"),
        ("OPEN",901,12,13,36,0,76,3.2,"z","trail"),
        ("OPEN",902,11,12,40,0,56,3.2,"z","trail"),
        ("OPEN",903,3,6,56,-8,27,1.8,"z","stair"),
        ("OPEN",904,3,8,62.5,-8,40,1.8,"z","stair"),
    ))
    for link in layout["links"]:
        rows.extend(("POINT",link["id"],*p) for p in link["points"])
    write_tsv(OUT/"runtime-input.tsv",rows)
    table=[]
    leaf=sha(SOURCE_LEAF)
    for ident,key,a,b,leaves,hinge,swing in DOORS:
        frame=frames[key]
        mount=frame["frame_span"][int(swing>0)]+swing*.004
        x,z=(frame["along"],mount) if frame["axis"]=="z" else (mount,frame["along"])
        table.append((ident,key,a,b,owners[a],owners[b],x,frame["sill_y"],z,frame["clear_width"],frame["axis"],
                      mount-.026,mount+.026,frame["clear_height"],leaves,hinge,swing,leaf))
    write_tsv(OUT/"door-schedule.tsv",table)
    return table


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    roster=json.loads(PRIOR.read_bytes())
    source=next(row for row in roster["active_sources"] if row["id"]=="WLD-09")
    for kind in ("glb","native"):
        assert sha(ROOT/source[kind])==source[kind+"_sha256"],kind
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    objects=import_scene(ROOT/source["glb"])
    legacy,additions=old_frames(objects)
    landing_guidance(layout)
    layout["authored_doorways"].extend(legacy)
    frames={row["id"]:row for row in layout["authored_doorways"]}
    assert frames.keys()=={entry[1] for entry in DOORS}
    tables=schedule(layout,frames)
    layout["operating_door_schedule"]=rel(OUT/"door-schedule.tsv")
    layout["status"]=STATUS
    tree=bvh([obj for obj in bpy.context.scene.objects if obj.type=="MESH"])
    measurements={key:measure(tree,val) for key,val in frames.items()}

    laid=OUT/"authored-layout.json"
    write(laid,layout)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND),compress=True)
    fragment=ROOT/"build/dam-indexed-door-addition.glb"
    export(fragment,additions)
    append(ROOT/source["glb"],fragment,GLB)
    native_write(ROOT/source["native"],NATIVE,[],additions)
    old,old_data=read(ROOT/source["glb"])
    new,new_data=read(GLB)
    assert new_data.startswith(old_data)
    for key in ("nodes","meshes","materials","accessors","bufferViews","textures","images"):
        assert new.get(key,[])[:len(old.get(key,[]))]==old.get(key,[]),key
    retained=split_native(ROOT/source["native"])
    assert split_native(NATIVE)[:len(retained)]==retained
    names=[o.name for o in additions]
    model=import_scene(GLB)
    collision=read_parts(NATIVE)
    for name in names:
        validate_solid(model[name])
        assert face_keys(records(model[name]))==face_keys(collision[name][1]),name
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
        source_physics=source["native"],source_physics_sha256=source["native_sha256"],
        before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
        before_segment_map=meta["source_segment_map"],before_stair_components=meta["stair_components"],
        layout=rel(laid),layout_sha256=sha(laid),glb_sha256=sha(GLB),physics_sha256=sha(NATIVE),blend_sha256=sha(BLEND),
        removed_source_meshes=[],fitted_doors=[dict(id=r[0],frame=r[1],a=r[2],b=r[3],floor_measurements=measurements[r[1]]) for r in tables],
        indexed_casings=len(legacy),retained_native_records=len(retained),new_interface_parts=names,
        leaf_source=rel(SOURCE_LEAF),leaf_sha256=sha(SOURCE_LEAF),frame_source=rel(MODEL),frame_sha256=sha(MODEL),
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)))
    write(OUT/"construction.json",meta)
    source.update(construction=rel(OUT/"construction.json"),layout=rel(laid),glb=rel(GLB),
        native=rel(NATIVE),blend=rel(BLEND),glb_sha256=sha(GLB),native_sha256=sha(NATIVE))
    next(row for row in roster["sites"] if row["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR)
    write(STAGE/"roster.json",roster)
    print("PASS",len(tables),"measured frames,",len(legacy),"legacy casings and",len(retained),"exact retained native records")


if __name__=="__main__":
    build()
