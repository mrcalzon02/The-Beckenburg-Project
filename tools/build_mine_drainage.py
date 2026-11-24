"""Fit Mine roof runoff to fixed construction and saved terrain catchments."""
from dataclasses import dataclass
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_cemetery_grounds import replace_terrain_on_export,terrain_surface
from build_college_electrical import Assembly,point,world
from build_lookout_native_arrival import table
from build_mine_distribution import samples
from build_mine_yard import combined_plate
from build_prison_sally_gate import export
from build_roster_foundations import material
from export_roster_collision import face_keys,native_faces
from review_roster_macroterrain import records,sha
from verify_college_containment import triangles
from verify_college_electrical import native_records
from verify_college_services import match_geometry

BASE=ROOT/"development/levels/rebuild/mine-lower-slabs-pass"
DOWN=Vector((0,0,-1))
WEST_BOX=(8.5,64.8)
EAST_BOX=(44.25,73.08)
JUNCTION=(33.15,60.30)


@dataclass(frozen=True)
class Roof:
    key: str
    cap: str
    bounds: tuple
    datum: float
    reference: tuple
    gradients: tuple

    def height(self,x,z):
        x0,z0,y=self.reference
        dx,dz=self.gradients
        return y+(x-x0)*dx+(z-z0)*dz


ROOFS=(
    Roof("crew","Ceiling cap shack",(2,46,20,62),3,(2,62,3.105),(0,-.039)),
    Roof("stores","Ceiling cap stores",(2,30,14,46),3,(2,62,3.105),(0,-.039)),
    Roof("assay","Ceiling cap assay",(40,46,54,62),3,(40,62,3.105),(0,-.032)),
    Roof("wash","Ceiling cap wash",(40,30,54,46),3,(40,62,3.105),(0,-.032)),
    Roof("haul","Mine haul stair weather roof region0",(26.62,30.70,31.38,49),3.36,
         (31.38,49,3.455),(-.015,-.048)),
    Roof("headhouse","Mine headhouse roof region0",(26.38,48.88,31.38,54.4),3.26,
         (30.75,54.4,3.365),(0,-.045)),
    Roof("vent","Mine vent outside weather roof region0",(20,50.72,26.5,53.30),3.36,
         (24.6,53.30,3.467),(0,-.072)),
)
ROOF_BY_NAME={r.key:r for r in ROOFS}


@dataclass(frozen=True)
class Collector:
    key: str
    axis: str
    eave: float
    sign: int
    limits: tuple
    outlet: float
    pipe: tuple
    underground: float
    face: str


COLLECTORS=(
    Collector("crew","x",62,1,(2.14,19.86),3.55,(3.55,62.26),-.61,"Wall shack"),
    Collector("assay","x",62,1,(40.14,53.86),50.0,(50.0,62.26),-.68,"Wall assay"),
    Collector("haul","z",31.38,1,(35.65,48.84),48.65,(31.61,48.65),-.37,"haul east retaining"),
    Collector("headhouse","x",54.4,1,(26.55,31.22),30.76,(30.76,54.59),-.33,"headhouse public mouth"),
    Collector("vent","x",53.3,1,(20.14,26.35),24.6,(24.6,53.51),-.36,"vent south outdoor"),
    Collector("shed","x",67.17,-1,(33.89,39.57),39.1,(39.1,67.13),-.37,"rear steel enclosure"),
)


def mesh_object(name,vertices,faces,mat,role):
    mesh=bpy.data.meshes.new(name)
    mesh.from_pydata(vertices,[],faces)
    mesh.update()
    bm=bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    assert all(e.is_manifold for e in bm.edges),name
    if bm.calc_volume(signed=True)<0:
        bmesh.ops.reverse_faces(bm,faces=list(bm.faces))
    assert bm.calc_volume(signed=True)>0,name
    bm.to_mesh(mesh)
    bm.free()
    obj=bpy.data.objects.new(name,mesh)
    bpy.context.scene.collection.objects.link(obj)
    mesh.materials.append(mat)
    obj["role"]=role
    return obj


def slab(name,bounds,bottom,top,mat,role):
    a,b,c,d=bounds
    corners=((a,b),(c,b),(c,d),(a,d))
    verts=[point(x,datum(x,z),z) for datum in (bottom,top) for x,z in corners]
    faces=[(0,1,2,3),(4,7,6,5),*((i,(i+1)%4,(i+1)%4+4,i+4) for i in range(4))]
    return mesh_object(name,verts,faces,mat,role)


def grade(obj,x,z):
    matrix=obj.matrix_world
    inverse=matrix.inverted()
    found,at,n,_=obj.ray_cast(inverse@point(x,15,z),inverse.to_3x3()@DOWN,distance=35)
    return (matrix@at).z if found else None


class Runoff(Assembly):
    def __init__(self,terrain):
        super().__init__()
        self.terrain=terrain
        self.weather=material("Mine sheet zinc roof",(.34,.39,.40))
        self.framing=material("Mine stained timber roof sleepers",(.23,.19,.145))
        self.drains=material("Mine zinc gutter and downpipe",(.39,.43,.42))
        self.stone=material("Mine coarse soakaway stone",(.30,.30,.263))
        self.roof_audit=[]
        self.drain_audit=[]
        self.pipes=[]

    def roof(self,r):
        cap=self.scene.objects[r.cap]
        x0,z0,x1,z1=r.bounds
        area=(x1-x0)*(z1-z0)
        backings=0
        for i in range(1,5):
            for j in range(1,5):
                x=x0+(x1-x0)*i/5
                z=z0+(z1-z0)*j/5
                assert abs(grade(cap,x,z)-r.datum)<.002,(r.key,x,z)
                backings+=1
        underside=lambda x,z:r.height(x,z)-.067
        assert min(underside(x,z) for x in (x0,x1) for z in (z0,z1))>r.datum+.024
        skin=slab(f"Mine runoff {r.key} pitched skin",r.bounds,underside,r.height,self.weather,
                  "continuous pitched roof weather layer")
        self.built.append(skin)
        # The intact cap is a continuous deck. Deep battens follow the roof
        # underside and the fascia closes each exposed perimeter of the buildup.
        n=max(2,math.ceil((z1-z0)/2.3))
        rails=[(x0+.055,z0+.12,x0+.14,z1-.12),(x1-.14,z0+.12,x1-.055,z1-.12)]
        for i in range(n):
            z=z0+.10+(z1-z0-.29)*i/(n-1)
            rails.append((x0+.12,z,x1-.12,z+.09))
        rims=[(x0,z0,x1,z0+.056),(x0,z1-.056,x1,z1),
              (x0,z0+.056,x0+.055,z1-.056),(x1-.055,z0+.056,x1,z1-.056)]
        for i,bounds in enumerate(rails+rims,1):
            role="cap-backed roof sleeper" if i<=len(rails) else "closed weather fascia"
            self.built.append(slab(f"Mine runoff {r.key} backing {i:02d}",bounds,lambda x,z:r.datum-.007,
                                   lambda x,z:underside(x,z)+.008,self.framing,role))
        corners=[(x,r.height(x,z),z) for x in (x0,x1) for z in (z0,z1)]
        self.roof_audit.append(dict(name=r.key,receiver=r.cap,area=round(area,3),roof_grade=r.gradients,
                                    corners=corners,deck_bearing_probes=backings))
        return skin

    def hollow_pipe(self,name,points,radius):
        path=[point(*xyz) for xyz in points]
        edges=[(b-a).normalized() for a,b in zip(path,path[1:])]
        tangents=[edges[0],*((a+b).normalized() for a,b in zip(edges,edges[1:])),edges[-1]]
        vertices=[]
        n=12
        basis=tuple(Vector(unit) for unit in ((1,0,0),(0,1,0),(0,0,1)))
        reference=min(basis,key=lambda v:max(abs(v.dot(axis)) for axis in tangents))
        for center,axis in zip(path,tangents):
            horizontal=axis.cross(reference).normalized()
            vertical=axis.cross(horizontal)
            for r in (radius,radius-.008):
                for j in range(n):
                    angle=j*math.tau/n
                    vertices.append(center+r*(horizontal*math.cos(angle)+vertical*math.sin(angle)))
        faces=[]
        for station in range(len(path)-1):
            for shell in (0,1):
                a=station*2*n+shell*n
                b=a+2*n
                faces.extend((a+j,a+(j+1)%n,b+(j+1)%n,b+j) for j in range(n))
        for base in (0,2*n*(len(path)-1)):
            faces.extend((base+j,base+(j+1)%n,base+(j+1)%n+n,base+j+n) for j in range(n))
        obj=mesh_object("Mine runoff "+name,vertices,faces,self.drains,"continuous hollow storm pipe")
        self.built.append(obj)
        return obj

    def gutter(self,c):
        # The gutter invert falls to the outlet from wherever the eave drains.
        roof=ROOF_BY_NAME.get(c.key)
        def coords(s,offset=0):
            boundary=c.eave+c.sign*offset
            return (s,boundary) if c.axis=="x" else (boundary,s)
        def lip(s):
            x,z=coords(s)
            if roof is not None:
                return roof.height(x,z)
            # Exact original service shed low rear sheet; retain its pitch.
            obj=self.scene.objects["Mine service yard continuous shed roof"]
            edge=grade(obj,x,z+.012)
            assert edge is not None
            return edge
        pan_floor=lip(c.outlet)-.258
        def invert(s):
            return pan_floor+abs(s-c.outlet)*(.046 if c.key=="haul" else .009)
        stations=(c.limits[0],c.outlet,c.limits[1])
        def metal_section(label,profile,ends):
            start,end=ends
            lengths=[start,c.outlet,end] if start<c.outlet<end else [start,end]
            verts=[]
            for s in lengths:
                for off,y in profile:
                    x,z=coords(s,.083+off)
                    verts.append(point(x,invert(s)+y,z))
            n=len(profile)
            offset=(len(lengths)-1)*n
            faces=[tuple(range(n)),tuple(range(offset,offset+n))]
            for layer in range(len(lengths)-1):
                k=layer*n
                faces.extend((k+i,k+(i+1)%n,k+(i+1)%n+n,k+i+n) for i in range(n))
            obj=mesh_object(f"Mine runoff {c.key} gutter {label}",verts,faces,self.drains,
                            "formed metal rain collector with open discharge throat")
            self.built.append(obj)

        metal_section("inner fold",((-0.114,0),(-.068,0),(-.068,.023),(-.093,.023),
                                     (-.110,.112),(-.129,.112)),c.limits)
        metal_section("outer fold",((.068,0),(.125,0),(.149,.114),(.13,.114),
                                     (.099,.023),(.068,.023)),c.limits)
        center=((-0.068,0),(.068,0),(.068,.023),(-.068,.023))
        throat=.068
        for label,span in (("into outlet", (stations[0],c.outlet-throat)),
                           ("beyond outlet", (c.outlet+throat,stations[-1]))):
            assert span[1]-span[0]>.06,(c.key,span)
            metal_section(label,center,span)
        end_profile=((-0.115,.01),(.124,.01),(.138,.111),(-.118,.111))
        metal_section("closed first end",end_profile,(stations[0],stations[0]+.023))
        metal_section("closed far end",end_profile,(stations[-1]-.023,stations[-1]))

        hanger_proof=[]
        length=c.limits[1]-c.limits[0]
        for i,(s,) in enumerate(samples((c.limits[0]+.10,),(c.limits[1]-.10,),1.55),1):
            x,z=coords(s,-.16 if c.key!="shed" else -.20)
            cap=roof.cap if roof is not None else "Mine service yard continuous shed roof"
            roof_bearing=grade(self.scene.objects[cap],x,z)
            assert roof_bearing is not None,(c.key,s,cap)
            x1,z1=coords(s,.12)
            self.tube(f"runoff {c.key} gutter hanger {i}",(x1,invert(s)-.012,z1),
                      (x,roof_bearing-.015,z),.027,self.drains)
            hanger_proof.append([s,roof_bearing])
        outlet_x,outlet_z=coords(c.outlet,.083)
        x,z=c.pipe
        shaft_top=(x,pan_floor-.26,z)
        soil=grade(self.terrain,x,z)
        assert soil is not None and soil-c.underground>.20,(c.key,soil,c.underground)
        self.downpipe(c,[(outlet_x,pan_floor+.028,outlet_z),(outlet_x,pan_floor-.086,outlet_z),
                         shaft_top,(x,c.underground,z)],soil)
        flow=[(s,lip(s),invert(s)) for (s,) in samples((stations[0],),(stations[-1],),.32)]
        assert all(level-bottom>.10 and level-bottom<.44 for s,level,bottom in flow),(c.key,flow)
        self.drain_audit.append(dict(name=c.key,channel_length=round(length,3),runoff_outlet=shaft_top,
                                     surveyed_ground_y=soil,outlet_invert=c.underground,gutter_hangers=hanger_proof,
                                     throat=[outlet_x,pan_floor+.024,outlet_z],
                                     pan=[*coords(c.outlet-.12,.083),invert(c.outlet-.12)+.023],
                                     gutter_lip_and_invert_samples=flow))

    def downpipe(self,c,points,soil):
        x,z=c.pipe
        direction=point(0,0,-c.sign) if c.axis=="x" else point(-c.sign,0,0)
        low=max(.25,soil+.34)
        high=min(2.73,points[1][1]-.19)
        assert high-low>1.2
        for i,fraction in enumerate((.05,.42,.75,1.0),1):
            y=low+(high-low)*fraction
            backing,end,ob=self.measured(point(x,y,z),direction,.32,f"{c.key} downpipe bracket")
            assert c.face in ob.name,(c.key,y,ob.name)
            bearing=world(backing+direction*.009)
            self.tube(f"runoff {c.key} downpipe collar {i}",(x,y,z),bearing,.034,self.drains)
        self.hollow_pipe(c.key+" open downpipe",points,.061)

    def connection(self,name,points,initial,terminal):
        lengths=[math.dist(a,b) for a,b in zip(points,points[1:])]
        total=sum(lengths)
        assert (initial-terminal)/total>=.009,(name,total,initial,terminal)
        run=0.0
        buried=[(points[0][0],initial,points[0][1])]
        cover=[]
        for (x,z),length in zip(points[1:],lengths):
            run+=length
            buried.append((x,initial+(terminal-initial)*run/total,z))
        for a,b in zip(buried,buried[1:]):
            for x,y,z in samples(a,b,.31):
                soil=grade(self.terrain,x,z)
                assert soil is not None and soil-y>.153,(name,(x,y,z),soil)
                cover.append(soil-y)
        self.hollow_pipe(name+" buried storm conductor",buried,.061)
        self.pipes.append(dict(name=name,route=buried,total_slope=round((initial-terminal)/total,4),
                               cover_samples=len(cover),minimum_cover=round(min(cover),4)))

    def networks(self):
        self.connection("crew west filter bed",[(3.55,62.26),(3.61,63.12),(5.8,64.38),WEST_BOX],-.61,-.79)
        self.connection("assay east filter bed",[(50,62.26),(49.6,63.57),(47,66),(45.1,69.4),EAST_BOX],-.68,-.90)
        self.connection("haul to inspection chamber",[(31.61,48.65),(32.07,49.52),(32.15,55.3),JUNCTION],-.37,-.60)
        self.connection("headhouse to inspection chamber",[(30.76,54.59),(31.46,55.13),(32.06,56.2),JUNCTION],-.33,-.60)
        self.connection("vent to inspection chamber",[(24.6,53.51),(24.6,54.8),(27.95,55.55),
                                                       (31.32,56.85),JUNCTION],-.36,-.60)
        self.connection("yard shed east filter bed",[(39.1,67.13),(39.85,67.13),(42.0,68.95),EAST_BOX],-.37,-.82)
        self.connection("inspection chamber to east filter bed",[JUNCTION,(36.1,62.1),(40.75,65.6),
                                                                  (43.2,67.0),EAST_BOX],-.60,-.90)
        # Underground chambers are founded in the same terrain volume. The
        # inspection cover is flush and stays away from the door approaches.
        x,z=JUNCTION
        ground=grade(self.terrain,x,z)
        self.built.append(combined_plate("Mine runoff inspection chamber base",
                                        [(x-.22,z-.22,x+.22,z+.22)],-.74,.06,self.stone))
        side_depth=ground+.56
        strips=((x-.22,z-.22,x+.22,z-.17),(x-.22,z+.17,x+.22,z+.22),
                (x-.22,z-.17,x-.17,z+.17),(x+.17,z-.17,x+.22,z+.17))
        self.built.append(combined_plate("Mine runoff open storm manhole",strips,ground-.035,
                                        side_depth-.035,self.stone))
        self.box("runoff flush inspection lid",(x-.17,z-.17,x+.17,z+.17),ground-.035,ground+.005,self.drains,
                 "flush inspection lid","yard")
        for side,(x,z) in (("west",WEST_BOX),("east",EAST_BOX)):
            y=grade(self.terrain,x,z)
            assert -.57<y<-.18,(side,y)
            self.box(f"runoff {side} buried stone filter gallery",(x-.66,z-.42,x+.66,z+.42),
                     -1.04,y-.16,self.stone,"subgrade soakaway under sculpted filter bed","yard")


def build(out):
    mine=json.loads((BASE/"construction.json").read_bytes())
    for kind,key in (("blend","blend_sha256"),("glb","glb_sha256"),("native","physics_sha256")):
        assert sha(ROOT/mine[kind])==mine[key]
    saved=out/"worlds/wld-05/terrain-world"
    manifest=json.loads((saved/"verification.json").read_bytes())
    assert manifest["physics_sha256"]==sha(saved/"terrain-physics.hcs2")
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/mine["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    previous=bpy.context.scene.objects["WLD-05 saved ground"]
    original=previous.name
    terrain=terrain_surface("WLD-05 runoff",previous,saved,out/"program/wld-05")
    terrain["role"]="saved editor terrain / Mine runoff catchments"
    terrain_name=terrain.name
    terrain.data.materials[-1].name="Compacted Mine runoff gravel"
    assert face_keys(records(terrain))==face_keys(native_faces(saved/"terrain-physics.hcs2"))
    bpy.data.objects.remove(previous,do_unlink=True)
    a=Runoff(terrain)
    for roof in ROOFS:
        a.roof(roof)
    # Retain the joined elevations of the paired roof runs and the actual
    # detached caps. The haul's buried high shoulder gets a raised weather curb.
    for pair,x in (("crew stores",8),("assay wash",47)):
        one,two=(ROOF_BY_NAME[k] for k in pair.split())
        assert abs(one.height(x,46)-two.height(x,46))<.0001
    haul=ROOF_BY_NAME["haul"]
    headhouse=ROOF_BY_NAME["headhouse"]
    def joint_top(x,z):
        f=(z-48.77)/.40
        return haul.height(x,48.77)*(1-f)+headhouse.height(x,49.17)*f+.014
    a.built.append(slab("Mine runoff haul and headhouse lap flashing",(26.62,48.77,31.37,49.17),
                        lambda x,z:joint_top(x,z)-.032,joint_top,a.weather,
                        "stepped flashing protects adjoining roofs and turns runoff to the gutter"))
    a.built.append(slab("Mine runoff haul uphill retaining seam",(31.27,30.70,31.38,35.65),
                        lambda x,z:haul.height(x,z)-.016,lambda x,z:haul.height(x,z)+.135,a.weather,
                        "raised edge directs high-shoulder water toward exposed collector"))
    for collector in COLLECTORS:
        a.gutter(collector)
    a.networks()
    for obj in a.built:
        obj.name=obj.name.replace("College ","Mine ",1) if obj.name.startswith("College ") else obj.name
    out.mkdir(parents=True,exist_ok=True)
    targets={ext:out/f"mine-drainage.{ext}" for ext in ("blend","glb","hcs2")}
    addition=out/"_runoff.glb"
    export(addition,[terrain,*a.built])
    append(ROOT/mine["glb"],addition,targets["glb"],remove_mesh_names=[original],reuse_embedded_images=True)
    addition.unlink()
    replace_terrain_on_export(ROOT/mine["native"],targets["hcs2"],{original},a.built,saved,False)
    before=native_records(ROOT/mine["native"])
    after=native_records(targets["hcs2"])
    retained=[p for p in before if not p[0].startswith("terrain-")]
    assert after[:len(retained)]==retained
    assert [p for p in after if p[0].startswith("terrain-")]==native_records(saved/"terrain-physics.hcs2")
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(targets["glb"]))
    assert face_keys(records(bpy.context.scene.objects[terrain_name]))==face_keys(native_faces(saved/"terrain-physics.hcs2"))
    for part in after[len(retained):]:
        if not part[0].startswith("terrain-"):
            assert match_geometry(triangles(part),[t for t,_ in records(bpy.context.scene.objects[part[0]])]),part[0]
    hashes={ext:sha(path) for ext,path in targets.items()}
    rows=[("SOURCE",targets["glb"].relative_to(ROOT).as_posix(),hashes["glb"],
           targets["hcs2"].relative_to(ROOT).as_posix(),hashes["hcs2"])]
    rows.extend(line.split("\t") for line in (BASE/"arrival.tsv").read_text().splitlines()[1:])
    table(out/"arrival.tsv",rows)
    climate_rows=[]
    for roof in ROOFS:
        x0,z0,x1,z1=roof.bounds
        for fx,fz in ((.25,.25),(.75,.25),(.25,.75),(.75,.75)):
            x,z=x0+(x1-x0)*fx,z0+(z1-z0)*fz
            climate_rows.append(("ROOF",roof.key,x,roof.height(x,z),z))
    for drain in a.drain_audit:
        climate_rows.append(("OPEN",drain["name"],*drain["throat"]))
        x,z,y=drain["pan"]
        climate_rows.append(("PAN",drain["name"],x,y,z))
    for side,stations in (("west",[(4.5,64.8),(6.4,65.5),(8.0,66.35)]),
                           ("east",[(42.25,66.2),(43.0,69.45),(44.25,73.08)])):
        imported=bpy.context.scene.objects[terrain_name]
        for x,z in stations:
            y=grade(imported,x,z)
            assert y is not None,(side,x,z)
            climate_rows.append(("BED",side,x,y,z))
    table(out/"drainage-probes.tsv",climate_rows)
    for name in ("door-survey.tsv","mission.tsv","rail-probes.tsv","switches.tsv","slab-probes.tsv"):
        (out/name).write_bytes((BASE/name).read_bytes())
    verdict=dict(exports=hashes,retained_components=len(retained),added_non_terrain_components=len(a.built),
                 terrain_manifest=manifest,roof_surveys=a.roof_audit,drain_audits=a.drain_audit,pipes=a.pipes)
    (out/"drainage.json").write_text(json.dumps(verdict,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS Mine runoff",len(a.roof_audit),"pitched roof members,",len(a.drain_audit),
          "anchored gutters,",len(a.pipes),"gravity pipes and exact saved catchments")


def capture_structure(out):
    bpy.ops.wm.open_mainfile(filepath=str(out/"mine-drainage.blend"))
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    shading=scene.display.shading
    shading.color_type="MATERIAL"
    shading.show_cavity=True
    shading.show_shadows=True
    shading.cavity_type="BOTH"
    scene.view_settings.view_transform="Standard"
    scene.render.resolution_x=1450
    scene.render.resolution_y=940
    scene.render.resolution_percentage=100
    scene.render.image_settings.file_format="PNG"
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    camera.data.type="ORTHO"
    scene.camera=camera
    output=out/"captures"/"structure"
    output.mkdir(parents=True,exist_ok=True)
    for name,eye,target,scale in (
        ("mine-weather-roof-overview",point(75,41,78),point(28,1,54),67),
        ("mine-shaft-roof-and-downpipe",point(41,13,54),point(29,1.8,43),25),
        ("mine-crew-gutter-throat",point(1.25,5.3,64.55),point(4.6,2.31,62.1),7),
        ("mine-yard-eastern-filter-bed",point(51,10,80),point(42,-.16,71),18.5),
    ):
        camera.location=eye
        camera.rotation_euler=(target-eye).to_track_quat("-Z","Y").to_euler()
        camera.data.ortho_scale=scale
        scene.render.filepath=str(output/(name+".png"))
        bpy.ops.render.render(write_still=True)


if __name__=="__main__":
    args=sys.argv[sys.argv.index("--")+1:]
    output=Path(args[0]).resolve()
    if "--captures-only" in args:
        capture_structure(output)
    else:
        build(output)
