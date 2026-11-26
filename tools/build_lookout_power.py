"""Build and commission the Lookout's roadhead service shelter on surveyed ground."""
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT/"tools"), str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import ASSETS, Assembly, bounds, point, world
from build_lookout_native_arrival import OWNERS, SOURCE, table
from build_prison_sally_gate import export
from build_roster_foundations import material
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT/"development/levels/rebuild/lookout-cover-pass"
GENERATOR = "level_trace_generator_refined"
BANK = "level_electrical_bank_worn"
ELECTRICAL_ROOMS = (
    ("hut", (16, 16, 0, -1), ((13, 17), (15, 23))),
    ("store", (47, 38.5, 0, 1), ((43, 34),)),
    ("cab", (27.5, 6.45, 0, 1), ((27, 5.5),)),
)
GROUND = (34, 66, 39.6, 69.6)
SLAB_Y = .018
ENTRY = dict(id="roadhead-shelter", owner=13, y=SLAB_Y, height=2.75)


class LookoutElectrical(Assembly):
    def effect(self, key, typ, owner, xyz, yaw, circuit):
        super().effect(key.replace("college-", "lookout-"), typ, owner, xyz, yaw, circuit.replace("EM-", "WP-"))

    def shed(self):
        x0, z0, x1, z1 = GROUND
        for x in (x0, (x0+x1)/2, x1):
            for z in (65.60, z0, (z0+z1)/2, z1):
                at, _, bearing = self.measured(point(x, .4, z), Vector((0,0,-1)), .6, "service shelter bearing")
                assert abs(at.z)<.003 and bearing.name=="WLD-04 saved ground"
        siding = material("Lookout weathered shelter planking", (.25,.19,.13))
        framing = material("Lookout dark timber frame", (.20,.15,.10))
        zinc = material("Lookout weathered zinc roofing", (.30,.34,.35))

        def shell(name, rect, low, high, mat, role):
            self.box(name, rect, low, high, mat, role, ENTRY["id"])
            return self.built[-1]

        def roof_grade(z):
            return (z-66)*.105

        def slope(obj, top_only=False):
            tallest=max((obj.matrix_world @ v.co).z for v in obj.data.vertices)
            inverse=obj.matrix_world.inverted()
            for vertex in obj.data.vertices:
                at=obj.matrix_world @ vertex.co
                if not top_only or abs(at.z-tallest)<.0001:
                    at.z-=roof_grade(-at.y)
                    vertex.co=inverse @ at
            obj.data.update()

        # One cast slab with a shallow formed entrance bevel and a level
        # equipment floor. The buried front edge avoids a raised vertical lip.
        corners=((x0,65.60),(x1,65.60),(x1,66.16),(x1,z1),(x0,z1),(x0,66.16))
        levels=(-.008,-.008,SLAB_Y,SLAB_Y,SLAB_Y,SLAB_Y)
        vertices=[point(x,-.23,z) for x,z in corners]+[point(x,y,z) for (x,z),y in zip(corners,levels)]
        faces=[(0,1,2,3,4,5),(6,7,8,11),(8,9,10,11)]
        faces.extend((i,(i+1)%6,(i+1)%6+6,i+6) for i in range(6))
        mesh=bpy.data.meshes.new("Lookout formed shelter slab")
        mesh.from_pydata(vertices,[],faces)
        mesh.update()
        bm=bmesh.new()
        bm.from_mesh(mesh)
        bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
        assert bm.calc_volume(signed=True)>0
        bm.to_mesh(mesh)
        bm.free()
        slab=bpy.data.objects.new(mesh.name,mesh)
        self.scene.collection.objects.link(slab)
        slab.data.materials.append(self.concrete)
        slab["role"]="founded equipment slab and formed entrance bevel"
        self.built.append(slab)
        for xa,xb in ((34,34.16),(39.44,39.6)):
            side=shell("roadhead shelter sidewall", (xa,66.12,xb,69.42), SLAB_Y, 2.80, siding, "supported shed sidewall")
            slope(side,True)
            shell("roadhead shelter front jamb", (xa,66,xb,66.18),SLAB_Y,2.80,framing,"roof-bearing entry post")
        back=shell("roadhead shelter rear wall", (34,69.42,39.6,69.6),SLAB_Y,2.80,siding,"supported shed rear wall")
        slope(back,True)
        beam=shell("roadhead shelter entry header", (34,66,39.6,66.18),2.57,2.80,framing,"header spanning clear shed entry")
        slope(beam,True)
        for z in (66.09,67.16,68.25,69.51):
            member=shell("roadhead shelter roof purlin", (34,z-.045,39.6,z+.045),
                  2.80,2.92,framing,"roof-bearing purlin")
            slope(member)
        roof=shell("roadhead weather roof", (33.83,65.82,39.77,69.82),2.92,3.04,zinc,"continuous sloped weather roof")
        slope(roof)
        shell("shelter lamp timber fixing block", (35.97,67.00,37.39,67.32),2.59,2.70,
              framing,"purlin-backed lamp receiver")
        self.originals.update(o.name for o in self.built)
        bpy.context.view_layer.update()
        self.deps=bpy.context.evaluated_depsgraph_get()

    def fit_plant(self):
        self.box("distribution plinth", (36.56,68.82,38.94,69.42),SLAB_Y,.06,self.concrete,
                 "founded distribution plinth",ENTRY["id"])
        for y in (.68,1.63):
            self.box("distribution anchored rear rail", (36.63,69.25,38.87,69.43),y,y+.09,
                     self.mount,"distribution wall rail",ENTRY["id"])
        placements = (
            (GENERATOR, (35.35,SLAB_Y,68.84), 0, "diesel generator"),
            (BANK, (37.75,.06,69.13), math.pi, "distribution bank"),
        )
        equipment={}
        assembly_parts={}
        for key, at, yaw, name in placements:
            pieces=self.place(key,Matrix.Translation(point(*at)) @ Matrix.Rotation(yaw,4,"Z"),
                              name,"founded remote supply equipment",ENTRY["id"])
            assembly_parts[key]=pieces
            envelope=bounds(pieces)
            assert abs(envelope[0][1]-at[1])<.003 and envelope[1][1]<2.4
            equipment[key]=dict(asset_sha256=sha(ASSETS/(key+".glb")),position=at,yaw=yaw, bounds=envelope)
        # Find the canonical generator's flat control face under the complete backing plate.
        casing=bvh(assembly_parts[GENERATOR])
        for x in (35.215,35.415):
            for y in (.444,.710):
                hit,normal,_,_=casing.ray_cast(point(x,y,68.40),point(0,0,1),.3)
                assert hit is not None and abs(-hit.y-68.535)<.004 and normal.y>.98,(x,y,hit)
        self.box("generator starter plate", (35.215,68.510,35.415,68.538),.442,.712,
                 self.mount,"casing-backed generator starter",ENTRY["id"])
        self.place("button_12",Matrix.Translation(point(35.315,.475,68.509)) @ Matrix.Rotation(math.pi,4,"Z") @ Matrix.Scale(.30,4),
                   "generator stop-start control","mounted generator starter",ENTRY["id"])
        self.effect("lookout-generator","generator",13,(35.315,.475,68.509),math.pi,"WP-MAIN")
        self.effect("lookout-breaker","breaker",13,(37.75,.90,68.895),math.pi,"WP-MAIN")
        self.raceway("weatherproof generator feed", [(35.1,.29,69.20),(35.1,.30,69.38),
            (35.1,2.292,69.38),(38.58,2.292,69.38)],.029)
        terminals=(36.8522,37.6122,38.3722)
        board=bvh(assembly_parts[BANK])
        for i,x in enumerate(terminals,1):
            count=len(self.built)
            self.raceway(f"bank lead {i}",[(x,2.168,69.17),(x,2.292,69.17),(x,2.292,69.38)],.018)
            bpy.context.view_layer.update()
            assert board.overlap(bvh(self.built[count:count+1])),x
        self.raceway("buried main from shelter wall service boot",
            [(38.58,2.292,69.38),(39.39,2.292,69.38),(39.39,-.37,69.38),(39.79,-.37,69.38)],.032)
        self.raceway("shelter lighting branch on wall and purlin",
            [(38.58,2.292,69.38),(39.42,2.292,69.38),(39.42,2.57,67.16),
             (36.68,2.57,67.16)],.022)
        return equipment


def connect_feeders(assembly):
    # The deep duct carries the services below the existing terrain. Vertical
    # ends are enclosed in foundation boots or clipped to intact wall faces.
    trunk=[(39.79,-.37,69.38),(40.13,-.37,69.38),(40.13,-2.2,69.38),
           (50.90,-2.2,69.38),(50.90,-2.2,12.8)]
    assembly.raceway("buried roadhead supply trunk",trunk,.03,False)
    assembly.raceway("buried hut branch",[(50.90,-2.2,12.8),(16,-2.2,12.8),
        (16,5.40,12.8),(16,5.40,13.83)],.03,False)
    assembly.raceway("hut wall-backed riser and sealed north sleeve",[(16,5.40,13.83),
        (16,8.77,13.83),(16,8.77,14.24),(16,8.822,14.24),(16,8.822,14.109)],.021)
    for y in (6.48,7.58,8.57):
        assembly.box(f"hut riser saddle {y}",(15.95,13.80,16.05,13.928),y-.035,y+.035,
                     assembly.mount,"measured north-wall riser saddle","hut")

    assembly.raceway("buried store branch",[(50.9,-2.2,40.25),(47,-2.2,40.25),(47,5.30,40.25)],.03,False)
    assembly.raceway("store wall-backed riser and sealed south sleeve",[(47,5.30,40.25),(47,5.80,40.25),
        (47,5.80,40.12),(47,8.77,40.12),(47,8.77,39.70),(47,8.822,39.70),(47,8.822,39.851)],.021)
    for y in (6.45,7.56,8.56):
        assembly.box(f"store riser saddle {y}",(46.95,40.073,47.05,40.15),y-.035,y+.035,
                     assembly.mount,"measured south-wall riser saddle","store")

    assembly.raceway("buried tower branch",[(50.9,-2.2,12.8),(50.9,-2.2,1.90),
        (29.8,-2.2,1.90),(29.8,5.54,1.90)],.03,False)
    assembly.raceway("tower column riser and roof sleeve",[(29.8,5.54,1.90),
        (29.8,5.90,2.015),(29.8,14.66,2.015),(29.8,14.80,1.93),
        (29.8,17.82,1.93),(27,17.82,1.93),(27,17.82,2.22),(27,17.982,2.22),(27,17.982,5.5)],.021)
    for y in (6.18,7.78,9.38,10.98,12.58,14.18):
        hit,_,receiver=assembly.measured(point(29.86,y,1.96),point(0,0,1),.22,f"tower riser bearing {y}")
        assert "frame column" in receiver.name and abs(-hit.y-2.06)<.006,receiver.name
        assembly.box(f"tower riser column clip {y}",(29.75,1.989,29.85,2.075),y-.033,y+.033,
                     assembly.mount,"clipped tower-frame riser","cab")
    for x in (27.1,28.45,29.7):
        assembly.box(f"cab high conduit clip {x}",(x-.04,1.905,x+.04,2.09),17.786,17.85,
                     assembly.mount,"clipped cabin north facade conduit","cab")


def build(out):
    source=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/source[kind])==source[key]
    layout=json.loads((SOURCE/"authored-layout.json").read_bytes())
    zones={z["id"]:dict(z,owner=OWNERS[z["id"]][0]) for z in layout["zones"] if z["id"] in {"hut","store","cab"}}
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=LookoutElectrical()
    assembly.shed()
    equipment=assembly.fit_plant()
    # The shelter lamp bears on a front purlin, with the feed following the purlin and sidewall.
    assembly.mounted_lamp(ENTRY,"lookout-service-light",point(36.68,2,67.16),
                          Vector((0,0,1)),1.6,point(1,0,0))
    for name,switch,positions in ELECTRICAL_ROOMS:
        zone=zones[name]
        assembly.switch(zone,*switch)
        for i,(x,z) in enumerate(positions,1):
            assembly.mounted_lamp(zone,f"lookout-{name}-light-{i}",point(x,zone["y"]+1, z),
                                  Vector((0,0,1)),3.4,point(1,0,0))
        # Interior branch connects the actual plate drop and installed ceiling emitters.
        loads=[lamp for lamp in assembly.loads if lamp["room"]==zone["owner"]]
        drop=next(r for r in reversed(assembly.routes) if "switch drop" in r["id"])["points"][-1]
        x,y,z=drop
        reach=z+(.13 if switch[3]<0 else -.13)
        top=loads[0]["mount"][1]-.018
        points=[drop,(x,top,z),(x,top,reach)]
        for lamp in loads:
            lx,_,lz=lamp["mount"]
            points.extend(((lx,top,points[-1][2]),(lx,top,lz)))
        assembly.raceway(f"WP-{zone['owner']:02d} ceiling branch",points,.016)
    connect_feeders(assembly)
    for row in assembly.loads+assembly.controllers+assembly.routes:
        if "circuit" in row:
            row["circuit"]=row["circuit"].replace("EM-","WP-")
        row["id"]=row["id"].replace("college-","lookout-").replace("EM-","WP-")
    for obj in assembly.built:
        obj.name="Lookout "+obj.name.removeprefix("College ").replace("EM-","WP-")

    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"lookout-power.{ext}" for ext in ("blend","glb","hcs2")}
    additions=out/"_additions.glb"
    export(additions,assembly.built)
    append(ROOT/source["glb"],additions,targets["glb"],reuse_embedded_images=True)
    additions.unlink()
    native_write(ROOT/source["native"],targets["hcs2"],set(),assembly.built,
                 open_components={o.name for o in assembly.built if o.get("source_asset")})
    before=native_records(ROOT/source["native"])
    after=native_records(targets["hcs2"])
    assert after[:len(before)]==before

    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in after[len(before):]:
        meshes=bpy.context.scene.objects[part[0]]
        assert match_geometry(triangles(part),[tri for tri,_ in records(meshes)]),part[0]
    hashes={ext:sha(f) for ext,f in targets.items()}
    source_lines=(BASE/"arrival.tsv").read_text().splitlines()
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in source_lines[1:])
    rows.extend(("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets)
    rows.extend((("POINT","roadhead-service-shelter",28,0,66),
                 ("POINT","roadhead-service-shelter",33,0,64.95),
                 ("POINT","roadhead-service-shelter",36.55,0,65.62),
                 ("POINT","roadhead-service-shelter",36.55,SLAB_Y,67.25)))
    table(out/"arrival.tsv",rows)
    for name in ("door-survey.tsv","mission.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    details=dict(exports=hashes,retained_components=len(before),added_components=len(after)-len(before),
                 shelter=GROUND,plant=equipment,loads=assembly.loads,controllers=assembly.controllers,
                 routes=assembly.routes, service_feeders={
                   "WP-03":"Buried feeder to hut service boot, with the interior branch along the north head datums.",
                   "WP-05":"Buried feeder to store isolation point and ceiling branch.",
                   "WP-10":"Buried supply to tower foot and riser on fixed column behind the tower, independent of the stair.",
                   "WP-YARD":"Shelter branch ahead of local switches, obeys supply and breaker."})
    (out/"electrical.json").write_text(json.dumps(details,indent=2)+"\n",newline="\n")
    print("PASS founded Lookout supply shelter, mounted services and",len(before),"retained native components")


def review_shelter(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"lookout-power.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1200
    scene.render.resolution_y=800
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    camera.data.type="ORTHO"
    dst=out/"captures/structure"
    dst.mkdir(parents=True,exist_ok=True)
    target=point(36.8,1.15,67.8)
    for name,eye,scale in (("shelter-approach",point(32,4.8,60),10.7),
                           ("shelter-roof-cutaway",point(32.6,11,59.8),9.3)):
        if name.endswith("cutaway"):
            for obj in scene.objects:
                if obj.get("role")=="continuous sloped weather roof":
                    obj.hide_render=True
        camera.location=eye
        camera.rotation_euler=(target-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=scale
        scene.render.filepath=str(dst/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        review_shelter(output)
    else:
        build(output)
