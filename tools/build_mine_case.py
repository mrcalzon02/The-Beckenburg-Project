"""Fit indexed Mine furnishings to measured floors and attach physical case records."""
import json, math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, bounds, point
from build_lookout_native_arrival import table
from build_mine_distribution import samples
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh,match_geometry

BASE=ROOT/"development/levels/rebuild/mine-sump-pass"
ROUGH=ROOT/"development/levels/primitive-props-v1/models"
DOWN=Vector((0,0,-1))


class Case(Assembly):
    def __init__(self):
        super().__init__()
        self.support_surfaces=bvh([o for o in self.scene.objects if o.type=="MESH" and o.visible_get()])

    def fit(self,asset,name,zone,x,y,z,yaw=0,scale=1,support=None):
        directory=ROUGH if asset.startswith("level_blockout_") else ASSETS
        path=directory/(asset+".glb")
        before=set(self.scene.objects)
        bpy.ops.import_scene.gltf(filepath=str(path))
        pieces=sorted((o for o in set(self.scene.objects)-before if o.type=="MESH"),key=lambda o:o.name)
        assert pieces
        turn=Matrix.Rotation(yaw,4,"Z")@Matrix.Scale(scale,4)
        extents=[turn @ o.matrix_world @ v.co for o in pieces for v in o.data.vertices]
        cx=(min(p.x for p in extents)+max(p.x for p in extents))/2
        cz=(min(p.y for p in extents)+max(p.y for p in extents))/2
        low=min(p.z for p in extents)
        offset=point(x,y,z)-Vector((cx,cz,low))
        frame=Matrix.Translation(offset)@turn
        placed=[]
        for i,original in enumerate(pieces,1):
            obj=bpy.data.objects.new(f"Mine case {name} {i:02d}",original.data)
            self.scene.collection.objects.link(obj)
            obj.matrix_world=frame@original.matrix_world
            obj["source_asset"]=asset
            obj["zone"]=zone
            obj["role"]="supported Mine case furnishing"
            placed.append(obj)
        for ob in set(self.scene.objects)-before-set(placed):
            bpy.data.objects.remove(ob,do_unlink=True)
        bpy.context.view_layer.update()
        lo,hi=bounds(placed)
        assert abs(lo[1]-y)<.002,(name,lo)
        solver=bvh(support) if support else self.support_surfaces
        for u in (lo[0]+.018,hi[0]-.018):
            for v in (lo[2]+.018,hi[2]-.018):
                position=point(u,y+.032,v)
                hit,n,_,_=solver.ray_cast(position,DOWN,.078)
                assert hit is not None and abs(hit.z-y)<.004 and n.z>.98,(name,u,v,hit)
        self.built.extend(placed)
        return placed,dict(asset=asset,sha256=sha(path),bounds=[lo,hi],pivot=[offset.x,offset.z,-offset.y],
                           yaw=yaw,scale=scale,support=[o.name for o in support] if support else "measured existing slab")


def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    a=Case()
    details=[]
    story=[]
    for name,zone,room,x,z,doc,record in (
            ("assay bench","assay",1,50.55,48.65,"ASSAY_LEDGER","level_story_letters"),
            ("crew dispatch bench","shack",0,13.65,48.55,"RESCUE_TALLY","level_trace_journal")):
        wood,receipt=a.fit("level_blockout_workbench",name,zone,x,0,z)
        details.append(receipt)
        hit,n,_,_=bvh(wood).ray_cast(point(x,1.65,z),DOWN,1.6)
        assert hit is not None and n.z>.98,name
        top=hit.z+.0005
        if room==1:
            _,receipt=a.fit("level_blockout_assay_balance","assay balance",zone,x+.39,top,z,scale=.47,support=wood)
            details.append(receipt)
        px=x-.33
        paper,receipt=a.fit(record,doc.lower(),zone,px,top,z,scale=.52,support=wood)
        details.append(receipt)
        ox,oy,oz=receipt["pivot"]
        ax,az=px+.027,z+.024
        surface,_,_,_=bvh(paper).ray_cast(point(ax,top+.12,az),DOWN,.14)
        assert surface is not None,doc
        story.append(("STORY","mine-story-"+doc.lower(),doc,room,record,ox,oy,oz,0,.52,px,0,z+.94,ax,surface.z,az))
    for args in (
            ("level_blockout_mine_hoist_winch","idle hoist","lift",52.32,-6,10.73,0,.78),
            ("level_blockout_cave_rockfall_heap","rockfall behind hoist","lift",52.70,-6,9.51,0,.60),
            ("level_blockout_mine_sorting_bins","haul sorting bins","haul",24.12,-6,36.45,0,.80),
            ("level_blockout_mine_pick_tool_rack","stores tool rack","stores",4.30,0,39.9,0,.85)):
        _,receipt=a.fit(*args)
        details.append(receipt)
    metal=material("Mine closed hoist bay steel",(.34,.32,.25))
    for i,(x,z) in enumerate(((50.55,8.16),(50.55,10.65),(50.55,12.4),(52.15,12.4),(53.82,12.4)),1):
        _,n,_=a.measured(point(x,-5.9,z),DOWN,.16,"hoist fence footing")
        assert n.z>.98
        a.box(f"hoist exclusion bolted foot {i}",(x-.067,z-.064,x+.067,z+.064),-6.004,-5.957,metal,
              "fixed guard floor plate","lift")
        a.tube(f"hoist exclusion post {i}",(x,-5.962,z),(x,-4.83,z),.048,metal)
    for y in (-5.44,-4.83):
        a.tube("hoist exclusion west rail",(50.55,y,8.16),(50.55,y,12.4),.037,metal)
        a.tube("hoist exclusion front rail",(50.55,y,12.4),(53.82,y,12.4),.037,metal)
    # The extraction machinery stays within a permanently screened display bay.
    for x,z in ((50.55,9.75),(50.55,11.68),(51.30,12.4),(52.95,12.4)):
        a.box("hoist exclusion infill",(x-.039,z-.039,x+.039,z+.039),-5.95,-4.85,metal,"fixed rail infill","lift")
    paths={}
    for line in (BASE/"arrival.tsv").read_text().splitlines():
        row=line.split("\t")
        if row[0]=="POINT":paths.setdefault(row[1],[]).append(tuple(map(float,row[2:5])))
    for receipt in details:
        lo,hi=receipt["bounds"]
        gap=min((math.hypot(max(lo[0]-x,0,x-hi[0]),max(lo[2]-z,0,z-hi[2]))
                 for path in paths.values() for first,last in zip(path,path[1:]) for x,y,z in samples(first,last,.20)
                 if lo[1]-1.4<=y<=hi[1]+.06),default=999)
        assert gap>.8,(receipt,gap)
        receipt["route_clearance_m"]=round(gap,4)
    for obj in a.built:
        if obj.name.startswith("College "):obj.name="Mine case "+obj.name.removeprefix("College ")
    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-case.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_case.glb"
    export(addition,a.built)
    append(ROOT/source["glb"],addition,targets["glb"],reuse_embedded_images=True)
    addition.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],set(),a.built,
                 open_components={o.name for o in a.built if o.get("source_asset")})
    before=native_records(ROOT/source["native"])
    after=native_records(targets["hcs2"])
    assert after[:len(before)]==before
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in after[len(before):]:
        assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes={ext:sha(f) for ext,f in targets.items()}
    start=("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])
    rows=[start,*[line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:]],*story]
    rows.append(("SPACE","sealed-hoist-relic",8,50.57,8.075,53.925,12.38,-6,2.85,"false","false"))
    table(out/"arrival.tsv",rows)
    for file in ("door-survey.tsv","mission.tsv","rail-probes.tsv","switches.tsv","drainage-probes.tsv","slab-probes.tsv","mine-sump.water"):
        (out/file).write_bytes((BASE/file).read_bytes())
    info=dict(exports=hashes,retained_components=len(before),added_components=len(after)-len(before),items=details,
              records=[s[2] for s in story])
    (out/"furnishings.json").write_text(json.dumps(info,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Mine supported case assets and guarded hoist",len(details),len(after)-len(before))


def structure(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-case.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shade=scene.display.shading
    shade.color_type="MATERIAL"
    shade.show_cavity=True
    shade.show_shadows=True
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1280
    scene.render.resolution_y=870
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.type="ORTHO"
    destination=out/"captures"/"structure"
    destination.mkdir(parents=True,exist_ok=True)
    for name,eye,target,scale,groups in (
        ("mine-assay-station",point(47.2,4.1,53.3),point(50.55,.55,48.65),4.65,
             ("Mine case assay","Floor assay")),
        ("mine-closed-hoist",point(47,-1.05,17),point(52,-5.45,10.75),7.4,
             ("Mine case idle hoist","Mine case rockfall","Mine case hoist exclusion","Floor lift","Wall lift E","Wall lift N")),
    ):
        for obj in scene.objects:
            if obj.type=="MESH":obj.hide_render=not obj.name.startswith(groups)
        camera.location=eye
        camera.rotation_euler=(target-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=scale
        scene.render.filepath=str(destination/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    (structure if "--captures-only" in args else build)(output)
