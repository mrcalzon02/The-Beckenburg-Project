"""Install measured caretaker distribution in the Cemetery and Vale Mortuary."""
import json
from pathlib import Path
import sys

import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]

from append_static_glb import append
from build_circulation_construction import native_write
from build_college_electrical import Assembly, point, world
from build_mine_distribution import samples
from build_prison_sally_gate import export
from compose_room_terrain import split_native
from prepare_cemetery_doors import BASE, load
from review_roster_macroterrain import records, sha
from verify_college_services import bvh, match_geometry

POWER=BASE/"cemetery-power-pass"
OUTPUT=BASE/"cemetery-distribution-pass"
UP=Vector((0,0,1))
DEPTH=-.55
VAULT=-.365

SWITCHES={
    "chapel":(16.65,54.8,0,1), "cottage":(40.5,54.8,0,1), "tool":(47.25,48.4,-1,0),
    "west-stair":(9.4,19.13,0,1), "east-stair":(46.4,19.13,0,1), "ossuary":(28.9,13.25,0,-1),
    "chamber":(23.4,10.8,0,1), "mort-hall":(57,62.3,-1,0), "mort-viewing":(61.2,48.35,-1,0),
    "mort-transfer":(74.9,60.45,1,0), "mort-cold":(75.4,56.7,0,1),
}
LAMPS={
    "chapel":(15.9,45.8), "cottage":(39.1,47.7), "tool":(50,45.1),
    "west-stair":(8.5,18.5), "east-stair":(47.4,18.4), "ossuary":(28.7,16.6), "chamber":(22,6.35),
    "mort-hall":(58,57), "mort-reception":(66.3,59.7), "mort-viewing":(66,46),
    "mort-transfer":(74,61.3), "mort-bay":(64,70.5), "mort-office":(79.3,61.3),
    "mort-cold":(77.3,53.8), "mort-prep":(78.1,45.4),
}


def relative(path):
    return path.relative_to(ROOT).as_posix()


def stable(p):
    return tuple(round(v,5) for v in p)


class CemeteryDistribution(Assembly):
    def __init__(self):
        super().__init__()
        meshes=[obj for obj in self.scene.objects if obj.type=="MESH"]
        self.cover=bvh([obj for obj in meshes if obj.get("role","").startswith("saved editor terrain")
                        or obj.get("role")=="floor"
                        or obj.name.startswith("Caretaker founded slab")])
        self.bearings=bvh([obj for obj in meshes
                          if not any(word in obj.get("role","").lower()
                                     for word in ("conduit","raceway","fitting"))])
        self.graph={}
        self.clips=[]
        self.ducts=[]
        self.tails={}
        self.ends={}
        self.targets={}

    def join(self,points):
        chain=[stable(p) for p in points]
        for a,b in zip(chain,chain[1:]):
            self.graph.setdefault(a,set()).add(b)
            self.graph.setdefault(b,set()).add(a)

    def pipe(self,name,points,kind,radius=.022):
        chain=[stable(p) for i,p in enumerate(points) if not i or stable(p)!=stable(points[i-1])]
        if len(chain)<2:
            return
        self.join(chain)
        self.raceway(name,chain,radius,kind!="buried duct")
        self.routes[-1]["installation"]=kind

    def buried(self,label,plan):
        path=[(x,DEPTH,z) for x,z in plan]
        checked=0
        for a,b in zip(path,path[1:]):
            for x,y,z in samples(a,b,.20):
                hit,normal,_,_=self.cover.ray_cast(point(x,1.1,z),-UP,1.8)
                assert hit is not None and hit.z-y>.15,(label,(x,y,z),hit)
                checked+=1
        self.ducts.append(dict(label=label,samples=checked,depth=DEPTH))
        self.pipe(label,path,"buried duct",.028)
        return path[-1]

    def clip(self,label,at,direction,span=.20):
        source=point(*at)
        hit,normal,_,_=self.bearings.ray_cast(source,direction,span)
        assert hit is not None and normal.dot(direction)<-.95,(label,at,hit,normal)
        finish=hit+direction*.006
        self.tube(label,at,world(finish),.017,self.mount)
        self.clips.append(dict(label=label,raceway=at,bearing=stable(world(hit))))

    def fit(self,zones):
        for name,stance in SWITCHES.items():
            if name in zones:
                self.switch(zones[name],*stance)
        for name,(x,z) in LAMPS.items():
            if name not in zones:
                continue
            zone=zones[name]
            start=point(x,zone["y"]+1.72,z)
            self.mounted_lamp(zone,"cemetery-load-"+name,start,UP,4.2,Vector((1,0,0)))
        for control in self.controllers:
            name=control["space"]
            drop=next(r for r in self.routes if r["id"]==control["circuit"]+" switch drop")
            low,high=map(stable,drop["points"])
            self.join(drop["points"])
            self.targets[name]=[low,high]
            self.tails[name]=low
            self.ends[name]=high
            dx,dz=SWITCHES[name][2:]
            for fraction in (.30,.72):
                y=low[1]+(high[1]-low[1])*fraction
                self.clip(name+" switch-drop clip",(low[0],y,low[2]),point(dx,0,dz))

    def floor_riser(self,name,feed):
        x,y,z=self.tails[name]
        foot=(x,.035,z)
        assert stable((x,DEPTH,z))==stable(feed),(name,feed)
        self.pipe(name+" sealed floor sleeve",[feed,foot],"sealed floor sleeve",.023)
        self.pipe(name+" protected wall riser",[foot,(x,y,z)],"supported wall riser")
        dx,dz=SWITCHES[name][2:]
        for height in (.25,.73,1.13):
            self.clip(name+" low riser clip",(x,height,z),point(dx,0,dz))

    def pavilion_entry(self,name,head):
        # The pavilion floor is suspended over the ossuary. Terminate the
        # buried leg outside the wall and enter at the switch backplate.
        x,y,z=self.tails[name]
        outside=20.19
        boot=self.buried(name+" courtyard duct",[head,(x,head[1]),(x,outside)])
        toe=(x,.055,outside)
        entry=(x,y,outside)
        self.pipe(name+" sealed grade boot",[boot,toe],"sealed grade sleeve",.023)
        self.pipe(name+" exterior wall riser",[toe,entry],"supported exterior riser")
        for height in (.23,.64,1.10):
            self.clip(name+" weatherproof wall saddle",(x,height,outside),point(0,0,-1))
        self.clip(name+" interior sleeve collar",(x,y,z),point(0,0,1))
        self.clip(name+" exterior sleeve collar",entry,point(0,0,-1))
        self.pipe(name+" packed pavilion wall sleeve",[entry,(x,y,z)],"sealed masonry sleeve",.016)

    def overhead(self,label,points):
        chain=[stable(p) for i,p in enumerate(points) if not i or stable(p)!=stable(points[i-1])]
        for a,b in zip(chain,chain[1:]):
            if abs(a[1]-b[1])>.005:
                continue
            for at in samples(a,b,.93)[1:-1]:
                self.clip(label+" soffit saddle",at,UP,.28)
        self.pipe(label,chain,"supported overhead raceway")
        return chain[-1]

    def lamp(self,space,from_at,plan=()):
        load=next(r for r in self.loads if r["space"]==space)
        x,y,z=load["mount"]
        level=round(y-.065,5)
        ax,_,az=from_at
        start=(ax,level,az)
        end=(x,level,z)
        self.pipe(space+" junction return",[from_at,start],"supported service return")
        self.overhead(space+" switched raceway",[start,*[(u,level,v) for u,v in plan],end])
        contact=stable((x,y-.012,z))
        self.pipe(space+" fixture termination",[end,contact],"mounting-box connection")
        switch=next(c["space"] for c in self.controllers if c["room"]==load["room"])
        self.targets[switch].append(contact)

    def external_network(self,source):
        tx,_,tz=source
        boot=(tx,DEPTH,tz)
        self.pipe("caretaker cabinet outgoing riser",[source,boot],"mounted plant riser")
        for y in (.48,1.25,2.05):
            self.clip("caretaker cabinet riser bracket",(tx,y,tz),point(0,0,-1))
        chapel=self.tails["chapel"]
        cottage=self.tails["cottage"]
        tool=self.tails["tool"]
        hub=(tx,57.8)
        yard=(28.9,57.8)
        head=(28.9,22.7)
        self.buried("caretaker buried yard feeder",[(tx,tz),hub])
        self.buried("south forecourt distribution",[hub,(cottage[0],57.8),yard,(chapel[0],57.8)])
        self.floor_riser("cottage",self.buried("cottage south footing",[ (cottage[0],57.8),(cottage[0],cottage[2])]))
        self.floor_riser("chapel",self.buried("chapel south footing",[(chapel[0],57.8),(chapel[0],chapel[2])]))
        self.floor_riser("tool",self.buried("tool-room footing feed",
                         [hub,(47.62,52.45),(tool[0],52.45),(tool[0],tool[2])]))
        self.buried("burial-avenue service duct",[yard,head])
        self.pavilion_entry("west-stair",head)
        self.pavilion_entry("east-stair",head)
        return hub,head

    def vault_network(self,head):
        ingress=self.buried("vault wall buried approach",[head,(28.9,20.24)])
        root=(28.9,VAULT,19.78)
        self.pipe("packed sleeve through vault south wall",[ingress,(28.9,VAULT,20.24),root],"sealed masonry sleeve")
        junction=(28.9,VAULT,14.38)
        self.overhead("vault upper service backbone",[root,junction])
        self.clip("vault ingress saddle",root,UP,.20)
        ox,_,oz=self.ends["ossuary"]
        end=(ox,VAULT,oz)
        self.overhead("vault switch supply",[junction,end])
        self.pipe("vault switch junction",[end,self.ends["ossuary"]],"surface junction")
        ax,_,az=self.ends["chamber"]
        near=(ax,VAULT,12.23)
        far=(ax,VAULT,11.77)
        self.overhead("archive wall approach",[junction,(ax,VAULT,14.38),near])
        self.pipe("packed archive upper-wall sleeve",[near,far],"sealed masonry sleeve")
        self.clip("archive sleeve receiving saddle",far,UP,.20)
        self.pipe("archive switch junction",[far,(ax,VAULT,az),self.ends["chamber"]],"surface junction")

    def annex_network(self,hub):
        hall=self.tails["mort-hall"]
        viewing=self.tails["mort-viewing"]
        transfer=self.tails["mort-transfer"]
        cold=self.tails["mort-cold"]
        # The public feed turns toward the visitor hall while the staff
        # feeder continues beneath the mortuary's actual founded floor.
        court=(55.4,62.9)
        self.buried("annex forecourt service duct",[hub,(hub[0],62.9),court])
        self.floor_riser("mort-hall",self.buried("visitor hall footing",[court,(55.4,hall[2]),(hall[0],hall[2])]))
        self.floor_riser("mort-viewing",self.buried("viewing partition footing",[court,(55.4,50.9),(viewing[0],50.9),
                                                                             (viewing[0],viewing[2])]))
        self.buried("staff-side sub-slab trunk",[court,(transfer[0],62.9)])
        self.floor_riser("mort-transfer",self.buried("staff vestibule wall riser feed",
                                                    [(transfer[0],62.9),(transfer[0],transfer[2])]))
        self.floor_riser("mort-cold",self.buried("cold room under-slab feed",[(transfer[0],62.9),
                                            (cold[0],62.9),(cold[0],cold[2])]))

    def annex_lamps(self):
        top=2.775
        hx,_,hz=self.ends["mort-hall"]
        hall_split=(58,top,hz)
        self.pipe("hall ceiling junction",[self.ends["mort-hall"],(hx,top,hz)],"supported service return")
        self.overhead("hall branching tray",[(hx,top,hz),hall_split])
        self.lamp("mort-hall",hall_split)
        self.lamp("mort-reception",hall_split,[(58,59.7),(59.77,59.7),(60.23,59.7)])
        self.lamp("mort-viewing",self.ends["mort-viewing"],[(61.3,48.35),(61.3,46)])

        tx,_,tz=self.ends["mort-transfer"]
        staff=(74,top,tz)
        self.pipe("transfer switch return",[self.ends["mort-transfer"],(tx,top,tz)],"supported service return")
        self.overhead("transfer branching tray",[(tx,top,tz),staff])
        self.lamp("mort-transfer",staff)
        self.lamp("mort-bay",staff,[(74,65),(72.23,65),(71.77,65),(64,65)])
        self.lamp("mort-office",staff,[(74,61.3),(75.77,61.3),(76.23,61.3)])

        cx,_,cz=self.ends["mort-cold"]
        cold=(77.3,top,cz)
        self.pipe("cold switch junction",[self.ends["mort-cold"],(cx,top,cz)],"supported service return")
        self.overhead("cold room branching tray",[(cx,top,cz),cold])
        self.lamp("mort-cold",cold)
        self.lamp("mort-prep",cold,[(78.1,cz),(78.1,50.23),(78.1,49.77)])

    def network(self,zones,source):
        self.fit(zones)
        hub,head=self.external_network(source)
        self.vault_network(head)
        for name in list(SWITCHES)[:7]:
            x,z=LAMPS[name]
            self.lamp(name,self.ends[name],[(x,self.ends[name][2])])
        if "mort-hall" in zones:
            self.annex_network(hub)
            self.annex_lamps()
        seen=set()
        queue=[stable(source)]
        while queue:
            at=queue.pop()
            if at not in seen:
                seen.add(at)
                queue.extend(self.graph[at]-seen)
        missing={name:[p for p in points if p not in seen] for name,points in self.targets.items()
                 if not all(p in seen for p in points)}
        assert not missing,missing
        return len(seen)


def build(site,authority):
    source=POWER/site/"caretaker-power"
    assert all(sha(ROOT/name)==digest for name,digest in authority["exports"].items())
    assert all(sha(ROOT/name)==digest for name,digest in authority["evidence"].items())
    bpy.ops.wm.open_mainfile(filepath=str(source.with_suffix(".blend")))
    bpy.context.preferences.filepaths.save_version=0
    folder=POWER/site
    prior=(folder/"room-layout.tsv").read_bytes()
    zones={}
    for line in prior.decode().splitlines():
        r=line.split("\t")
        if r[0]=="SPACE":
            zones[r[1]]=dict(id=r[1],owner=int(r[2]),y=float(r[7]),height=float(r[8]))
    confirmed=load(folder/"construction.json")
    source_terminal=next(route["points"][-1] for route in confirmed["plant_routes"]
                         if route["id"]=="weatherproof generator feed")
    assembly=CemeteryDistribution()
    connected=assembly.network(zones,source_terminal)
    for obj in assembly.built:
        obj.name="Cemetery "+obj.name.removeprefix("College ")
    for socket in assembly.sockets:
        socket["id"]=socket["id"].replace("college-","cemetery-")
        socket["style"]=socket["style"].replace("EM-","CT-")
    out=OUTPUT/site
    out.mkdir(parents=True,exist_ok=True)
    stem=out/"distributed-grounds"
    fragment=ROOT/"build/cemetery-distribution-fragment.glb"
    export(fragment,assembly.built)
    append(source.with_suffix(".glb"),fragment,stem.with_suffix(".glb"),reuse_embedded_images=True)
    fragment.unlink()
    changed=native_write(source.with_suffix(".hcs2"),stem.with_suffix(".hcs2"),set(),assembly.built,
                         open_components={obj.name for obj in assembly.built if obj.get("source_asset")})
    before=split_native(source.with_suffix(".hcs2"))
    after=split_native(stem.with_suffix(".hcs2"))
    assert after[:len(before)]==before and len(after)-len(before)==len(changed)
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(stem.with_suffix(".blend")),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(stem.with_suffix(".glb")))
    for name,tri in changed:
        actual=records(bpy.context.scene.objects[name])
        assert match_geometry([face for face,_ in tri],[face for face,_ in actual]),name

    rows=[("SERVICE",s["id"],s["type"],s["room"],*s["position"],s["yaw"],s["style"]) for s in assembly.sockets]
    rows.extend(("SWITCH_FEET",c["room"],*c["feet"]) for c in assembly.controllers)
    (out/"room-layout.tsv").write_bytes(prior+"".join("\t".join(map(str,r))+"\n" for r in rows).encode())
    (out/"door-schedule.tsv").write_bytes((folder/"door-schedule.tsv").read_bytes())
    paths=[stem.with_suffix(ext) for ext in (".blend",".glb",".hcs2")]
    receipt=dict(source_terminal=source_terminal,connected_junctions=connected,duct_checks=assembly.ducts,
             mount_contacts=assembly.clips,conduits=assembly.routes,switches=assembly.controllers,
             loads=assembly.loads,retained_collision_parts=len(before),new_parts=len(changed),
             exports={relative(p):sha(p) for p in paths})
    (out/"construction.json").write_text(json.dumps(receipt,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",site,len(assembly.controllers),"circuits",len(assembly.loads),"loads",
          sum(row["samples"] for row in assembly.ducts),"buried samples",len(assembly.clips),"mounted clips")


def capture_pavilion(site):
    packet=OUTPUT/site
    source=packet/"distributed-grounds.glb"
    assert sha(source)==load(packet/"construction.json")["exports"][relative(source)]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.display.shading.cavity_type="BOTH"
    scene.render.resolution_x=1440
    scene.render.resolution_y=900
    scene.render.resolution_percentage=100
    bpy.ops.object.camera_add(location=point(13.8,3.32,27.2))
    camera=bpy.context.object
    camera.rotation_euler=(point(9.4,1.04,20.0)-camera.location).to_track_quat("-Z","Y").to_euler()
    camera.data.type="ORTHO"
    camera.data.ortho_scale=6.2
    scene.camera=camera
    folder=packet/"captures"/"structure"
    folder.mkdir(parents=True,exist_ok=True)
    scene.render.filepath=str(folder/"west-pavilion-exterior-feed.png")
    bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    sites=("wld-07","wld-07-mortuary")
    if "--captures-only" not in sys.argv:
        checkpoint=load(POWER/"checkpoint.json")
        for site in sites:
            build(site,checkpoint["sites"][site.upper()])
    for site in sites:
        capture_pavilion(site)
