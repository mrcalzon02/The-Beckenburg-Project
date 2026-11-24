"""Mount the first twelve Ravenshade circuits on retained School structure."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, ASSETS, bounds, point, world
from review_roster_macroterrain import records, sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import bvh, match_geometry

BASE = ROOT / "development/levels/rebuild/school-mission-graph-pass"
ROOMS = (
    ("foyer", (24,35.1,0,1), ((22.5,33),)),
    ("corridor", (22.1,22.5,1,0), ((21.5,16),(21.5,24),(21.5,28))),
    ("class-a", (11.1,30, -1,0), ((15,27),(15,31))),
    ("science", (11.1,18.5,-1,0), ((15,15),(15,18.5))),
    ("dining", (30.85,30,-1,0), ((33,27),(35,32))),
    ("health-link", (31,18.8,-1,0), ((31,16),(31,22))),
    ("gym", (49.15,12,1,0), ()),
    ("upper-corridor", (21.05,25.1,-1,0), ((21.5,15),(21.5,23),(21.5,32))),
    ("library", (18.9,27,1,0), ((15,27),(15,33))),
    ("class-b", (18.9,15.5,1,0), ((15,15),(15,19))),
    ("assembly", (31,31,-1,0), ((34,27),(38,32))),
    ("plant", (24.1,18.8,-1,0), ((25.1,16),)),
)

CUPBOARDS = (
    ("foyer",20.92,34.1,-math.pi/2),
    ("staff",26.29,18.55,math.pi/2),
    ("library",12.3,25.1,0),
)

class SchoolElectrical(Assembly):
    def effect(self, key, typ, owner, xyz, yaw, style):
        super().effect(key.replace("college-", "school-"), typ, owner, xyz, yaw,
                       style.replace("EM-", "RV-"))

    def switch(self, zone, x, z, dx, dz):
        super().switch(zone, x, z, dx, dz)
        self.controllers[-1]["id"] = self.controllers[-1]["id"].replace("college-", "school-")
        self.controllers[-1]["circuit"] = f"RV-{zone['owner']:02d}"
        direction = point(dx,0,dz)
        mount,_,wall = self.measured(point(x+dz*.32, zone["y"]+2, z-dx*.32),
                                      direction,1.95,"School alarm backing")
        at = mount - direction * .026
        alarm = f"school-alarm-{zone['owner']:02d}"
        self.effect(alarm,"device",zone["owner"],world(at),math.atan2(dx,dz),"FIRE_ALARM")
        self.controllers[-1]["alarm"]={"id":alarm,"position":world(at),"support":wall.name}

    def main_board(self, zone):
        west = point(-1,0,0)
        down = Vector((0,0,-1))
        floor,_,_ = self.measured(point(23.5,-2.6,16.45),down,.7,"plant board floor")
        wall,_,support = self.measured(point(24,-1.5,16.45),west,1.2,"plant board wall")
        assert abs(floor.z + 3) < .001
        for z in (15.38,16.45,17.52):
            face,_,_ = self.measured(point(24,-1.5,z),west,1.2,"board back bearing")
            assert abs(face.x-wall.x)<.004
            bear,_,_ = self.measured(point(23.46,-2.65,z),down,.55,"board base bearing")
            assert abs(bear.z-floor.z)<.004
        self.box("School plant floor plinth", [wall.x,15.24,wall.x+.63,17.66], -3,-2.94,
                 self.concrete,"founded switchgear plinth",zone["id"])
        for y in (-2.53,-1.51):
            self.box("School plant back rail", [wall.x,15.29,wall.x+.17,17.61], y,y+.08,
                     self.mount,"wall anchored switchgear rail",zone["id"])
        board = "level_electrical_bank_worn"
        origin = (wall.x+.18, -2.94, 16.45)
        parts = self.place(board, Matrix.Translation(point(*origin)) @ Matrix.Rotation(math.pi/2,4,"Z"),
                           "School switchgear", "founded building distribution",zone["id"])
        lower,upper=bounds(parts)
        front=upper[0]+.02
        self.effect("school-main-breaker","breaker",11,(front,-2.10,16.45),-math.pi/2,"RV-MAIN")
        # Terminal centers measured at the ends of the canonical angled lugs.
        duct_x=origin[0]-.04
        terminal_z=[origin[2]+offset for offset in (-.91,-.15,.61)]
        manifold_y=-.48
        cabinet=bvh(parts)
        drops=[]
        for i,z in enumerate(terminal_z,1):
            start=len(self.built)
            self.raceway(f"School switchgear terminal {i}",[(duct_x,-.856,z),(duct_x,manifold_y,z)],.017)
            bpy.context.view_layer.update()
            drop=bvh(self.built[start:])
            assert cabinet.overlap(drop), ("Detached cabinet lug",i)
            drops.append(drop)
            self.box(f"School incoming duct clip {i}",[wall.x,z-.036,duct_x+.034,z+.036],
                     manifold_y-.035,manifold_y+.035,self.mount,"wall backed switchgear duct clip",zone["id"])
        start=len(self.built)
        self.raceway("School switchgear manifold",
                     [(duct_x,manifold_y,terminal_z[0]-.03),(duct_x,manifold_y,terminal_z[-1]+.03)],.025)
        bpy.context.view_layer.update()
        manifold=bvh(self.built[start:])
        assert all(manifold.overlap(drop) for drop in drops)
        feed_y=-.23
        feed_z=terminal_z[1]
        backing,_,receiver=self.measured(point(duct_x,feed_y,feed_z),west,.45,
                                          "School surface feed box backing")
        assert abs(backing.x-wall.x)<.004
        self.box("School plant feed box",[backing.x,feed_z-.065,backing.x+.075,feed_z+.065],
                 feed_y-.06,feed_y+.06,self.mount,"wall backed utility feed box",zone["id"])
        start=len(self.built)
        self.raceway("School plant incoming riser",[(duct_x,manifold_y-.012,feed_z),
                     (duct_x,feed_y,feed_z),(backing.x+.03,feed_y,feed_z)],.019)
        bpy.context.view_layer.update()
        assert manifold.overlap(bvh(self.built[start:start+1]))
        assert bvh(self.built[start:start+1]).overlap(bvh(self.built[start+1:]))
        assert bvh(self.built[start-1:start]).overlap(bvh(self.built[start+1:]))
        return {"position":origin,"source":board,"source_sha256":sha(ASSETS/(board+".glb")),
                "wall":support.name,"bounds":[lower,upper],"floor":floor.z,"breaker_x":front,
                "connected_terminals":[(duct_x,-.856,z) for z in terminal_z],
                "incoming_receiver":{"component":receiver.name,"at":world(backing)}}


def cupboards(assembly,zones,positions=CUPBOARDS,site="school"):
    key="level_hiding_cupboard_worn"
    originals=bvh([obj for obj in assembly.scene.objects if obj.type=="MESH" and obj.name in assembly.originals
                   and obj.get("role")!="floor"])
    bearings=bvh([obj for obj in assembly.scene.objects if obj.type=="MESH" and obj.name in assembly.originals])
    installed=[]
    for room,x,z,yaw in positions:
        zone=zones[room]
        floor=zone["y"]
        name=f"{site}-{room}-cupboard"
        transform=Matrix.Translation(point(x,floor,z)) @ Matrix.Rotation(-yaw,4,"Z")
        pieces=assembly.place(key,transform,name,"founded accessible cupboard",room)
        lo,hi=bounds(pieces)
        for obj in pieces:
            obj.location.z+=floor-lo[1]
        bpy.context.view_layer.update()
        lo,hi=bounds(pieces)
        assert abs(lo[1]-floor)<.002 and hi[1]<floor+zone["height"]-.08,(room,lo,hi)
        if originals.overlap(bvh(pieces)):
            candidates=[]
            for obj in assembly.scene.objects:
                if obj.type=="MESH" and obj.name in assembly.originals and obj.get("role")!="floor":
                    if bvh([obj]).overlap(bvh(pieces)):
                        candidates.append((obj.name,obj.get("role"),bounds([obj])))
            raise AssertionError((name,candidates))
        contacts=set()
        for obj in pieces:
            for vertex in obj.data.vertices:
                at=obj.matrix_world @ vertex.co
                if abs(at.z-floor)<.008:
                    contacts.add((round(at.x,5),round(-at.y,5)))
        assert len(contacts)>=3,room
        for px,pz in contacts:
            hit,normal,_,_=bearings.ray_cast(point(px,floor+.20,pz),Vector((0,0,-1)),.36)
            assert hit is not None and normal.z>.98 and abs(hit.z-floor)<.006,(room,px,pz,hit)
        assembly.effect(name,"hide",zone["owner"],(x,floor,z),yaw,"walk-in-cupboard")
        installed.append({"id":name,"space":room,"owner":zone["owner"],"position":[x,floor,z],"yaw":yaw,
                          "bounds":[lo,hi],"base_contacts":len(contacts),"source_sha256":sha(ASSETS/(key+".glb"))})
    return installed

def build(out):
    out.mkdir(parents=True,exist_ok=True)
    source={ext:BASE/f"assembly-partition.{ext}" for ext in ("blend","glb","hcs2")}
    partition=json.loads((BASE/"partition.json").read_bytes())
    assert all(sha(path)==partition["exports"][ext] for ext,path in source.items())
    layout=json.loads((BASE/"authored-layout.json").read_bytes())
    zones={item["id"]:item for item in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    assembly=SchoolElectrical()
    hiding=cupboards(assembly,zones)
    for owner,(room,control,stations) in enumerate(ROOMS):
        zone=zones[room]
        assert zone["owner"]==owner
        assembly.switch(zone,*control)
        for index,(x,z) in enumerate(stations,1):
            tangent=Vector((0,1,0)) if "corridor" in room or room=="health-link" else Vector((1,0,0))
            assembly.mounted_lamp(zone,f"school-{room}-load-{index}",point(x,zone["y"]+.95,z),
                                 Vector((0,0,1)),5.5,tangent)
        if room=="gym":
            for index,z in enumerate((11,15,19,22),1):
                assembly.mounted_lamp(zone,f"school-gym-load-{index}",point(49,2.85,z),point(1,0,0),
                                     1.4,point(0,0,1))
    board=assembly.main_board(zones["plant"])
    for load in assembly.loads:
        load["circuit"]=load["circuit"].replace("EM-","RV-")
    for obj in assembly.built:
        if obj.name.startswith("College "):
            obj.name="Ravenshade "+obj.name[len("College "):]

    targets={ext:out/f"school-electrical.{ext}" for ext in source}
    bpy.ops.object.select_all(action="DESELECT")
    for obj in assembly.built:
        obj.select_set(True)
    additions=out/"_additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(additions),export_format="GLB",use_selection=True,export_extras=True)
    append(source["glb"],additions,targets["glb"],reuse_embedded_images=True)
    additions.unlink()
    retained=native_records(source["hcs2"])
    native_write(source["hcs2"],targets["hcs2"],set(),assembly.built,
                 open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    result=native_records(targets["hcs2"])
    assert result[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    for part in result[len(retained):]:
        assert match_geometry(triangles(part),[tri for tri,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    rows=(BASE/"circulation.tsv").read_text(encoding="utf-8").splitlines()
    rows[0]="\t".join(("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),sha(targets["glb"]),
                                targets["hcs2"].relative_to(ROOT).as_posix(),sha(targets["hcs2"])))
    (out/"circulation.tsv").write_text("\n".join(rows)+"\n",encoding="utf-8",newline="\n")
    for name in ("door-schedule.tsv","mission.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    schedule=(BASE/"door-schedule.tsv").read_text(encoding="utf-8")
    for s in assembly.sockets:
        schedule+="\t".join(map(str,("SOCKET",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"])))+"\n"
    (out/"door-schedule.tsv").write_text(schedule,encoding="utf-8",newline="\n")
    proof={"board":board,"hiding_cupboards":hiding,"circuit_count":len(assembly.controllers),"controllers":assembly.controllers,
        "loads":assembly.loads,"local_drops":assembly.routes,"retained_native_components":len(retained),
        "new_native_components":len(result)-len(retained),"exports":{ext:sha(path) for ext,path in targets.items()}}
    (out/"electrical.json").write_text(json.dumps(proof,indent=2)+"\n",encoding="utf-8",newline="\n")
    print(f"PASS {len(assembly.controllers)} mounted switches, {len(assembly.loads)} fitted loads, {len(retained)} retained physical components")

if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
