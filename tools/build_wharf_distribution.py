"""Fit supported distribution from the Wharf forecourt plant to its six interior circuits."""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly,point,world
from build_mine_distribution import samples
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from verify_college_services import bvh,match_geometry

BASE=ROOT/"development/levels/rebuild"
SOURCE=BASE/"wharf-power-pass"
OUT=BASE/"wharf-distribution-pass"
BOOT=(34.51,-.37,54.87)
DECK_Y=-.12
PLAN=(
    ("cottage-a",(13.30,52.90,0,1),(12,47.1)),
    ("cottage-b",(53.30,52.90,0,1),(52,47.1)),
    ("boathouse",(18.85,20.65,1,0),(12,18)),
    ("net",(45.12,18.48,-1,0),(50,16.5)),
    ("fish",(33.65,11.18,0,-1),(32,16.5)),
    ("undercroft",(31.2,34.62,0,1),(31.4,30.9)),
)
UP=Vector((0,0,1))
DECK_ROLES={"floor","authored dock deck","authored retained dock-side walk",
            "rectangular stair-cell roof or dock deck","piled perimeter boardwalk deck",
            "structural-stair-landing"}


def relative(path):
    return path.relative_to(ROOT).as_posix()


def stable(p):
    return tuple(round(v,5) for v in p)


class WharfDistribution(Assembly):
    def __init__(self):
        super().__init__()
        meshes=[o for o in self.scene.objects if o.type=="MESH"]
        self.deck=bvh([o for o in meshes if o.get("role") in DECK_ROLES])
        self.ground=bvh([o for o in meshes if o.name.startswith("Saved editor geographic earthworks")])
        self.support=bvh(meshes)
        self.graph={}
        self.surveys=[]
        self.anchors=[]

    def pipe(self,name,points,form,radius=.021):
        chain=[stable(p) for i,p in enumerate(points) if not i or stable(p)!=stable(points[i-1])]
        for a,b in zip(chain,chain[1:]):
            self.graph.setdefault(a,set()).add(b)
            self.graph.setdefault(b,set()).add(a)
        self.raceway(name,chain,radius,form not in {"buried","deck duct"})
        self.routes[-1]["installation"]=form

    def buried(self,name,positions):
        count=0
        for a,b in zip(positions,positions[1:]):
            for x,y,z in samples(a,b,.20):
                floor,_,_,_=self.ground.ray_cast(point(x,1,z),-UP,2)
                assert floor is not None and floor.z>y+.065,(name,x,z,floor)
                count+=1
        self.surveys.append((name,"buried land duct",count))
        self.pipe(name,positions,"buried",.029)

    def deck_duct(self,name,plan,shore_transition=False):
        count=0
        for a,b in zip(plan,plan[1:]):
            for x,z in samples(a,b,.15):
                top,_,_,_=self.deck.ray_cast(point(x,.12,z),-UP,.40)
                bottom,_,_,_=self.deck.ray_cast(point(x,-.76,z),UP,.70)
                covered=top is not None and bottom is not None and top.z>=-.006 and bottom.z<-.145
                if shore_transition and not covered:
                    ground,_,_,_=self.ground.ray_cast(point(x,1,z),-UP,2)
                    covered=ground is not None and ground.z>-.075
                assert covered,(name,x,z,top,bottom)
                count+=1
        self.surveys.append((name,"duct inside deck sections",count))
        self.pipe(name,[(x,DECK_Y,z) for x,z in plan],"deck duct",.018)

    def clip(self,name,at,direction,limit):
        source=point(*at)
        contact,normal,_,_=self.support.ray_cast(source,direction,limit)
        assert contact is not None and normal.dot(direction)<-.95,(name,at,contact)
        finish=contact+direction*.006
        self.tube(name,at,world(finish),.018,self.mount)
        self.anchors.append((name,at,world(contact)))

    def fit(self,zones):
        for name,switch,(x,z) in PLAN:
            zone=zones[name]
            self.switch(zone,*switch)
            self.mounted_lamp(zone,"wharf-load-"+name,point(x,zone["y"]+1.65,z),UP,1.8,point(1,0,0))
        for route in self.routes:
            for a,b in zip(route["points"],route["points"][1:]):
                a,b=stable(a),stable(b)
                self.graph.setdefault(a,set()).add(b)
                self.graph.setdefault(b,set()).add(a)

    def branch(self,i,feed):
        drop=next(r for r in self.routes if r["id"]==f"EM-{i:02d} switch drop")
        low,high=map(stable,drop["points"])
        x,_,z=low
        ctl=self.controllers[i]
        direction=point(ctl["at"][0]-x,0,ctl["at"][2]-z).normalized()
        for factor in (.3,.7):
            y=low[1]+factor*(high[1]-low[1])
            self.clip(f"WH-{i:02d} switch drop clip",(x,y,z),direction,.2)
        lx,ly,lz=self.loads[i]["mount"]
        top=round(ly-.065,5)
        upper=(x,top,z)
        if i==5:
            self.pipe("sealed dry-cell ceiling sleeve",[feed,upper],"sealed sleeve")
        else:
            foot=(x,.032,z)
            self.pipe(f"WH-{i:02d} sealed floor sleeve",[feed,foot],"sealed sleeve")
            self.pipe(f"WH-{i:02d} mounted wall riser",[foot,low],"wall riser")
            for y in (.26,.74,1.13):
                self.clip(f"WH-{i:02d} wall riser clip",(x,y,z),direction,.20)
        self.pipe(f"WH-{i:02d} service-box return",[high,upper],"mounted return")
        run=[upper,(lx,top,z),(lx,top,lz)]
        for a,b in zip(run,run[1:]):
            for at in samples(a,b,1.05)[1:-1]:
                self.clip(f"WH-{i:02d} overhead saddle",at,UP,.21)
        self.pipe(f"WH-{i:02d} switched lamp conduit",run+[(lx,ly-.012,lz)],"soffit conduit")
        return low,stable((lx,ly-.012,lz))

    def network(self,zones):
        self.fit(zones)
        tails=[stable(next(r for r in self.routes if r["id"]==f"EM-{i:02d} switch drop")["points"][0])
               for i in range(6)]
        x0,_,z0=tails[0]
        x1,_,z1=tails[1]
        hub=(32,-.37,54.87)
        self.buried("shelter service boot", [BOOT,hub])
        self.buried("west cottage street duct",[hub,(x0,-.37,54.87),(x0,-.37,54.19),(x0,DECK_Y,54.19)])
        self.buried("east cottage street duct",[BOOT,(x1,-.37,54.87),(x1,-.37,54.19),(x1,DECK_Y,54.19)])
        self.buried("quay duct transition",[hub,(32,DECK_Y,54.87)])
        self.deck_duct("west cottage threshold sleeve",[(x0,54.19),(x0,z0)],True)
        self.deck_duct("east cottage threshold sleeve",[(x1,54.19),(x1,z1)],True)
        x2,_,z2=tails[2]
        x3,_,z3=tails[3]
        x4,_,z4=tails[4]
        x5,_,z5=tails[5]
        self.deck_duct("quay and central roof duct",[(32,54.87),(32,39),(32,z5),(32,18),(x4,18),(x4,z4)],True)
        self.deck_duct("west boardwalk duct",[(32,39),(22,39),(14,39),(14,29),(16,29),
                      (16,27.6),(16,z2),(x2,z2)])
        self.deck_duct("east pier-head duct beside the stair well",[(32,39),(42,39),(42,37),
                      (48,37),(48,29),(42,29),(42,z3),(x3,z3)])
        self.deck_duct("dry-cell roof takeoff",[(32,z5),(x5,z5)])
        needed=[]
        for i,(x,_,z) in enumerate(tails):
            needed.extend(self.branch(i,(x,DECK_Y,z)))
        seen=set()
        visit=[stable(BOOT)]
        while visit:
            at=visit.pop()
            if at not in seen:
                seen.add(at)
                visit.extend(self.graph[at]-seen)
        assert all(p in seen for p in needed),[p for p in needed if p not in seen]


def main():
    source=SOURCE/"wharf-power"
    authority=json.loads((SOURCE/"checkpoint.json").read_bytes())
    assert all(sha(ROOT/p)==h for p,h in authority["exact_fit"]["exports"].items())
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    prior=(SOURCE/"room-layout.tsv").read_bytes()
    zones={}
    for row in prior.decode().splitlines():
        r=row.split("\t")
        if r[0]=="SPACE":
            zones[r[1]]=dict(id=r[1],owner=int(r[2]),y=float(r[7]),height=float(r[8]))
    assembly=WharfDistribution()
    assembly.network(zones)
    for obj in assembly.built:
        obj.name="Wharf "+obj.name.removeprefix("College ")
    for socket in assembly.sockets:
        socket["id"]=socket["id"].replace("college-","wharf-")
        socket["style"]=socket["style"].replace("EM-","WH-")
    OUT.mkdir(exist_ok=True)
    dest=OUT/"wharf-distribution"
    part=ROOT/"build/wharf-distribution-fragment.glb"
    export(part,assembly.built)
    append(source.with_suffix(".glb"),part,dest.with_suffix(".glb"),reuse_embedded_images=True)
    changed=native_write(source.with_suffix(".hcs2"),dest.with_suffix(".hcs2"),set(),assembly.built,
                         open_components={o.name for o in assembly.built if o.get("source_asset")})
    before=split_native(source.with_suffix(".hcs2"))
    after=split_native(dest.with_suffix(".hcs2"))
    assert after[:len(before)]==before and len(changed)==len(after)-len(before)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(dest.with_suffix(".blend")),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(dest.with_suffix(".glb")))
    for name,tri in changed:
        assert match_geometry([xyz for xyz,_ in tri],[xyz for xyz,_ in records(bpy.context.scene.objects[name])]),name
    rows=[("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets]
    rows.extend(("SWITCH_FEET",r["room"],*r["feet"]) for r in assembly.controllers)
    (OUT/"room-layout.tsv").write_bytes(prior+"".join("\t".join(map(str,r))+"\n" for r in rows).encode())
    (OUT/"door-schedule.tsv").write_bytes((SOURCE/"door-schedule.tsv").read_bytes())
    info=dict(plant_boot=BOOT,surveys=assembly.surveys,actual_supports=assembly.anchors,
              routes=assembly.routes,switches=assembly.controllers,loads=assembly.loads,
              retained_parts=len(before),new_parts=len(changed),
              exports={relative(d):sha(d) for d in (dest.with_suffix(e) for e in (".blend",".glb",".hcs2"))})
    (OUT/"construction.json").write_bytes((json.dumps(info,indent=2)+"\n").encode())
    print("PASS Wharf six circuits",sum(n for _,_,n in assembly.surveys),"duct samples",
          len(assembly.anchors),"physical support checks",flush=True)


if __name__=="__main__":
    main()
