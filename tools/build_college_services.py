"""Build College service installations from the occupied rooms and their actual bearing faces."""
from dataclasses import dataclass
from pathlib import Path
from math import pi
import json
import sys

import bpy
import bmesh
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/"tools"),str(ROOT/"development/levels/blockouts")]
from append_static_glb import append
from build_architectural_doorways import MODEL,frame_template
from build_circulation_construction import native_write
from build_college_doors import aabb
from build_college_electrical import point,world
from build_deliberate_entrances import solid
from build_roster_foundations import material
from review_roster_macroterrain import sha
from verify_college_electrical import native_records

BASE=ROOT/"development/levels/rebuild/college-containment-pass"
RUNTIME=ROOT/"assets/runtime/models"
ROUGH=ROOT/"development/levels/primitive-props-v1/models"
FLOOR_PIPE="below structural walking slabs"
BOILER="level_boiler_modern_worn"
RADIATOR="level_radiator_worn"
BASIN="level_bath_basin"
TOILET="toilet_mp_1"
SHOWER="level_bath_shower_worn"
COUNTER="level_kitchen_counter"
WASHER="level_laundry_washer"
MANIFOLD="level_blockout_cellar_service_manifold"
HOOD="level_blockout_lab_fume_hood"
RANGE="level_blockout_domestic_kitchen_range"
LEAF="door_1_loose"


@dataclass(frozen=True)
class Fixture:
    key: str
    room: str
    asset: str
    x: float
    z: float
    yaw: float = 0


WET=(
    Fixture("maintenance-basin","maintenance",BASIN,12.56,29.85,pi/2),
    Fixture("lab-wash-basin","lab-wash",BASIN,18.45,16.50),
    Fixture("public-basin","public-wc",BASIN,26.70,53.54,pi),
    Fixture("public-wc","public-wc",TOILET,24.96,53.15,pi),
    Fixture("cleaner-basin","cleaner",BASIN,26.5,46.53),
    Fixture("laundry-left","laundry",WASHER,44.6,34.62),
    Fixture("laundry-right","laundry",WASHER,46.15,34.62),
    Fixture("staff-cleaner-basin","staff-cleaner",BASIN,56.4,30.50),
    Fixture("staff-basin","staff-wc",BASIN,60.95,33.54,pi),
    Fixture("staff-wc","staff-wc",TOILET,59.25,33.15,pi),
    Fixture("kitchen-sink","kitchen",COUNTER,57.45,50.67),
    Fixture("washup-sink","washup",COUNTER,57.45,65.35,pi),
    Fixture("residence-basin","res-wc",BASIN,57,22.52),
    Fixture("residence-wc","res-wc",TOILET,55.15,22.81),
    Fixture("residence-shower-west","res-shower",SHOWER,59.05,22.73),
    Fixture("residence-shower-east","res-shower",SHOWER,60.9,22.73),
)

HEAT=(
    Fixture("seminar-radiator","seminar",RADIATOR,9,24.35),
    Fixture("lecture-front-radiator","lecture",RADIATOR,6.44,40,pi/2),
    Fixture("lecture-rear-radiator","lecture",RADIATOR,6.44,49,pi/2),
    Fixture("lobby-radiator","teaching-lobby",RADIATOR,21.5,61.62,pi),
    Fixture("upper-seminar-radiator","upper-seminar",RADIATOR,10.1,24.42),
    Fixture("library-reading-radiator","reading",RADIATOR,45.1,19.6,pi),
    Fixture("library-stack-radiator","upper-stacks",RADIATOR,40.1,4.42),
    Fixture("cafe-radiator","cafe",RADIATOR,42.45,62.10,pi/2),
    Fixture("residence-hall-radiator","upper-res-hall",RADIATOR,53.59,47.8,-pi/2),
    Fixture("dormitory-radiator","res-west-1",RADIATOR,42.43,36.4,pi/2),
)

# Widths and paths belong to this utility installation. The entire horizontal
# carrier system runs under the retained cellar floor, separated from circulation.
CARRIERS={
    "flow":(31.35,-3.44,.022,3,73),
    "return":(31.63,-3.48,.022,3,73),
    "cold":(32.35,-3.46,.017,3,109),
    "hot":(32.60,-3.50,.017,3,73),
}
INLET_Z=109
DRAIN_X=33.08


@dataclass
class Asset:
    path: Path
    meshes: list
    tree: BVHTree
    corners: list


class Installation:
    def __init__(self):
        self.scene=bpy.context.scene
        self.assets={}
        self.rejected_degenerate_faces={}
        self.pieces=[]
        self.fixtures=[]
        self.terminals=[]
        self.runs=[]
        self.vents=[]
        self.approaches=[]
        self.street_controls=[]
        self.finished={
            "cast iron":material("College period service cast iron",(.115,.126,.122)),
            "copper":material("College oxidised domestic supply",(.39,.22,.12)),
            "painted":material("College identified primary return",(.20,.28,.32)),
            "soil":material("College sanitary iron",(.18,.16,.15)),
            "concrete":material("College service cast concrete",(.39,.39,.35)),
            "galvanised":material("College galvanised air duct",(.40,.45,.44)),
        }

    def load(self,key):
        if key in self.assets:
            return self.assets[key]
        path=RUNTIME/(key+".glb")
        if not path.exists():
            path=ROUGH/(key+".glb")
        before=set(self.scene.objects)
        bpy.ops.import_scene.gltf(filepath=str(path))
        created=set(self.scene.objects)-before
        meshes=[]
        vertices,faces=[],[]
        for obj in sorted(created,key=lambda o:o.name):
            if obj.type!="MESH":
                continue
            transform=obj.matrix_world.copy()
            mesh=obj.data
            dead=[p.index for p in mesh.polygons if p.area<1e-12]
            if dead:
                # Zero-area source faces carry no visible or contact surface.
                # Clean the placed instance while retaining the indexed master.
                mesh=mesh.copy()
                edit=bmesh.new()
                edit.from_mesh(mesh)
                edit.faces.ensure_lookup_table()
                bmesh.ops.delete(edit,geom=[edit.faces[i] for i in dead],context="FACES")
                edit.to_mesh(mesh)
                edit.free()
                self.rejected_degenerate_faces[key]=self.rejected_degenerate_faces.get(key,0)+len(dead)
            meshes.append((mesh,transform))
            start=len(vertices)
            vertices.extend(transform@v.co for v in mesh.vertices)
            faces.extend(tuple(start+j for j in p.vertices) for p in mesh.polygons)
        assert vertices and meshes,key
        item=Asset(path,meshes,BVHTree.FromPolygons(vertices,faces),vertices)
        for obj in created:
            bpy.data.objects.remove(obj,do_unlink=True)
        self.assets[key]=item
        return item

    def add(self,obj,role,room):
        obj["role"]=role
        obj["zone"]=room
        self.pieces.append(obj)
        return obj

    def place(self,key,frame,label,room,role):
        asset=self.load(key)
        parts=[]
        for i,(mesh,original) in enumerate(asset.meshes,1):
            o=bpy.data.objects.new(f"College {label} {i:02d}",mesh)
            self.scene.collection.objects.link(o)
            o.matrix_world=frame@original
            o["source_asset"]=key
            parts.append(self.add(o,role,room))
        bpy.context.view_layer.update()
        return parts

    def box(self,name,rect,low,high,finish,room,role):
        o=solid("College "+name,rect,low,high,self.finished[finish],role,room)
        return self.add(o,role,room)

    def probe(self,x,y,z,direction,reach):
        bpy.context.view_layer.update()
        return self.scene.ray_cast(bpy.context.evaluated_depsgraph_get(),point(x,y,z),direction,distance=reach)

    def require_floor(self,x,y,z,room):
        hit,p,n,_,obj,_=self.probe(x,y+.17,z,Vector((0,0,-1)),.28)
        assert hit and abs(p.z-y)<.008 and n.z>.98,(room,x,y,z,obj.name if hit else None,p[:])
        return obj.name

    def floor_item(self,fixture,rooms,lift=0):
        spec=rooms[fixture.room]
        item=self.load(fixture.asset)
        yaw=Matrix.Rotation(-fixture.yaw,4,'Z')
        locations=[yaw@v for v in item.corners]
        min_x=min(v.x for v in locations)
        max_x=max(v.x for v in locations)
        min_z=min(-v.y for v in locations)
        max_z=max(-v.y for v in locations)
        bottom=min(v.z for v in locations)
        y=spec["y"]
        bounds=(fixture.x+min_x,fixture.z+min_z,fixture.x+max_x,fixture.z+max_z)
        a,b,c,d=spec["bounds"]
        assert a+.082<bounds[0]<bounds[2]<c-.082 and b+.082<bounds[1]<bounds[3]<d-.082,(fixture.key,bounds)
        supports=[]
        for x in (bounds[0]+.035,bounds[2]-.035):
            for z in (bounds[1]+.035,bounds[3]-.035):
                supports.append(self.require_floor(x,y,z,fixture.room))
        if lift:
            pad=tuple(v+(-.045 if i<2 else .045) for i,v in enumerate(bounds))
            self.box(fixture.key+" housekeeping plinth",pad,y,y+lift,"concrete",fixture.room,"Floor-seated equipment plinth")
        frame=Matrix.Translation(point(fixture.x,y+lift-bottom,fixture.z))@yaw
        parts=self.place(fixture.asset,frame,fixture.key,fixture.room,"Native supported service fixture")
        aabbs=[aabb(o) for o in parts]
        self.fixtures.append(dict(
            key=fixture.key,room=fixture.room,asset=fixture.asset,floor=y,
            source=item.path.relative_to(ROOT).as_posix(),sha256=sha(item.path),
            size=[[min(b[i] for b in aabbs) for i in range(3)],[max(b[i] for b in aabbs) for i in range(3,6)]],
            original_floor_supports=supports,parts=[o.name for o in parts],plinth_m=lift))
        return frame

    def surface(self,fixture,frame,key,x,height,axis="rear",z=0):
        item=self.load(fixture.asset)
        if axis=="rear":
            arg,direction=Vector((x,1.5,height)),Vector((0,-1,0))
        elif axis in ("bottom","top"):
            up=axis=="bottom"
            arg,direction=Vector((x,-z,-1.5 if up else 3.5)),Vector((0,0,1 if up else -1))
        else:
            side=1 if axis=="right" else -1
            arg,direction=Vector((side*1.5,-z,height)),Vector((-side,0,0))
        hit,_,_,_=item.tree.ray_cast(arg,direction,4)
        assert hit is not None,(fixture.asset,key,x,height,axis,z)
        local=world(hit)
        position=world(frame@hit)
        offset=frame.to_3x3() @ -direction
        self.terminals.append(dict(fixture=fixture.key,asset=fixture.asset,circuit=key,source=local,position=position,
                                   face=axis))
        return position, (offset.x,offset.z,-offset.y)

    def pipe(self,name,a,b,radius,finish,room):
        start,stop=point(*a),point(*b)
        direction=stop-start
        assert direction.length>.0005,(name,a,b)
        bpy.ops.mesh.primitive_cylinder_add(vertices=8,radius=radius,depth=direction.length+.016,location=(start+stop)/2)
        obj=bpy.context.object
        obj.name="College "+name
        obj.rotation_mode="QUATERNION"
        obj.rotation_quaternion=direction.to_track_quat("Z","Y")
        obj.data.materials.append(self.finished[finish])
        return self.add(obj,"Closed tube installed from service schedule",room)

    def run(self,key,room,circuit,radius,finish,nodes,installation):
        compact=[]
        for row in nodes:
            if not compact or tuple(row)!=compact[-1]:
                compact.append(tuple(row))
        parts=[self.pipe(f"{key} {i:02d}",a,b,radius,finish,room).name
               for i,(a,b) in enumerate(zip(compact,compact[1:]),1)]
        self.runs.append(dict(id=key,room=room,circuit=circuit,radius=radius,nodes=compact,
                              installation=installation,parts=parts))

    @staticmethod
    def offset(position,normal,amount=.075):
        return tuple(round(a+b*amount,6) for a,b in zip(position,normal))

    def boiler_isolation(self,boiler,boiler_frame,manifold,manifold_frame):
        connections=(
            ("flow",-.20,.80,-.60, .75, -3.29),
            ("return",.20,.80,.60, .75, -3.34),
            ("cold",-.35,1,-.32,.75, -3.39),
            ("hot",.35,1,.32,.75, -3.44),
        )
        for circuit,x,h,mx,mh,bury in connections:
            origin,direction=self.surface(boiler,boiler_frame,circuit,x,h)
            dest,normal=self.surface(manifold,manifold_frame,circuit,mx,mh)
            a,b=self.offset(origin,direction),self.offset(dest,normal)
            finish="cast iron" if circuit in ("flow","return") else "copper"
            self.run("plant isolation "+circuit,"plant",circuit,.021 if circuit in ("flow","return") else .015,
                     finish,[origin,a,(a[0],bury,a[2]),(b[0],bury,a[2]),(b[0],bury,b[2]),b,dest],
                     "Boiler connection to the separate isolation manifold through the plant's service floor")
            self.branch(manifold,manifold_frame,circuit,mx,.95)

    def branch(self,fixture,frame,circuit,x,h,source_face="rear",z=0):
        position,normal=self.surface(fixture,frame,circuit,x,h,source_face,z)
        bx,by,bz=self.offset(position,normal)
        if circuit=="waste":
            # Gravity falls from the fixture to a below-grade transverse branch,
            # then along the south-falling sanitary collector beneath the gate.
            last=drain_datum(bz)
            y=last+abs(bx-DRAIN_X)*.015
            radius=.051 if fixture.asset==TOILET else .021
            self.run(fixture.key+" waste",fixture.room,"waste",radius,"soil",
                [position,(bx,by,bz),(bx,y,bz),(DRAIN_X,last,bz)],FLOOR_PIPE)
            return
        trunk_x,y,r,z0,z1=CARRIERS[circuit]
        assert z0<=bz<=z1,(fixture.key,circuit,bz)
        finish={"flow":"cast iron","return":"painted","cold":"copper","hot":"copper"}[circuit]
        self.run(fixture.key+" "+circuit,fixture.room,circuit,min(r,.015),finish,
                [position,(bx,by,bz),(bx,y,bz),(trunk_x,y,bz)],FLOOR_PIPE)

    def street_connections(self):
        # Both utility heads are outside the secured campus. Their buried
        # services cross the existing road at separate elevations.
        x,y,r,_,z=CARRIERS["cold"]
        water_x=x+5.20
        hit,p,n,_,terrain,_=self.probe(water_x,1,z,Vector((0,0,-1)),3)
        assert hit and terrain.name=="College district apron" and n.z>.85
        water_ground=p.z
        self.run("street cold service lateral","outside service reserve","cold",r,"copper",
                 [(water_x,water_ground-.10,z),(water_x,y,z),(x,y,z)],FLOOR_PIPE)
        self.box("covered water stop chamber",(water_x-.21,z-.21,water_x+.21,z+.21),
                 water_ground-.17,water_ground+.035,"concrete","outside service reserve",
                 "Grade-seated street water chamber")
        self.pipe("covered water cap",(water_x,water_ground+.028,z),(water_x,water_ground+.052,z),.165,
                  "cast iron","outside service reserve")
        hit,p,n,_,terrain,_=self.probe(DRAIN_X,1,z,Vector((0,0,-1)),3)
        assert hit and terrain.name=="College district apron" and n.z>.85
        grade=p.z
        self.run("foul sewer inspection riser","outside service reserve","cleanout",.10,"soil",
                 [(DRAIN_X,grade-.06,z),(DRAIN_X,drain_datum(z),z)],"Service manhole beyond the campus gate")
        self.box("foul sewer manhole seat",(DRAIN_X-.31,z-.31,DRAIN_X+.31,z+.31),grade-.18,grade+.025,
                 "concrete","outside service reserve","Grade-seated foul service chamber")
        self.pipe("foul sewer inspection lid",(DRAIN_X,grade+.017,z),(DRAIN_X,grade+.043,z),.235,
                  "cast iron","outside service reserve")
        self.street_controls.extend((
            dict(id="water stop",x=water_x,z=z,grade=round(water_ground,6),support="College district apron"),
            dict(id="sanitary cleanout",x=DRAIN_X,z=z,grade=round(grade,6),support="College district apron"),
        ))

    def basin_wet(self,fixture,frame):
        if fixture.asset in (BASIN,COUNTER):
            p=-.60 if fixture.asset==COUNTER else 0
            h=.50 if fixture.asset==COUNTER else .75
            for circuit,x in (("cold",p-.08),("hot",p+.08)):
                self.branch(fixture,frame,circuit,x,h)
            self.branch(fixture,frame,"waste",p,0,"bottom")
        elif fixture.asset==TOILET:
            self.branch(fixture,frame,"cold",0,.50)
            self.branch(fixture,frame,"waste",0,0,"bottom")
        elif fixture.asset==WASHER:
            self.branch(fixture,frame,"cold",0,.45)
            self.branch(fixture,frame,"waste",.12,.30)
        elif fixture.asset==SHOWER:
            for circuit,x,h in (("cold",-.06,1.05),("hot",.06,1.22)):
                self.branch(fixture,frame,circuit,x,h)
            self.branch(fixture,frame,"waste",0,0,"bottom",.02)

    def plant_wall(self):
        existing=self.scene.objects["Wall plant S"]
        assert aabb(existing)==(12.0,-3.0,23.92,20.0,0.0,24.08)
        mat=existing.data.materials[0]
        bpy.data.objects.remove(existing,do_unlink=True)
        x,width,z,y=16.38,1.68,24,-3
        clear=2.14
        trim=.105
        outer=width/2+trim
        frame=frame_template()
        for v in frame.data.vertices:
            p=frame.matrix_world@v.co
            edge=p.x*width if abs(p.x)<=.5 else (1 if p.x>0 else -1)*(width/2+(abs(p.x)-.5)*trim/.12)
            v.co=(x+edge,-z+p.y*.16/.18,y+p.z*clear/2.125)
        frame.matrix_world.identity()
        frame.name="College plant sealed staff frame"
        frame["source_asset"]="level_blockout_doorway"
        self.add(frame,"Fitted staff frame at the existing maintenance partition","plant")
        for name,a,b,low in (("west pier",12,x-outer,y),("east pier",x+outer,20,y),
                             ("lintel",x-outer,x+outer,y+clear*2.275/2.125)):
            piece=solid("College plant shared wall "+name,(a,23.92,b,24.08),low,-.16,mat,
                        "Complete plant masonry bearing under the slab","plant")
            self.add(piece,"Complete load-bearing plant partition","plant")
        for side in (-1,1):
            # Match the indexed closed native pose. The protected plant space
            # is context; the fitted static leaf has the full physical panel.
            matrix=Matrix.Translation(point(x+side*width/2,y,z-side*.03))
            matrix=matrix@Matrix.Rotation(0 if side>0 else pi,4,'Z')@Matrix.Diagonal(
                (width/2,1,clear/2.1,1))
            self.place(LEAF,matrix,f"plant captive leaf {side}","plant","Fitted closed staff door leaf")
        self.approaches.append(dict(id="plant-staff-door",rooms=["plant","maintenance"],
            center=[x,y,z],width=width,height=clear,prescribed_state="closed staff context",
            floor_approach=[x,y,25.05],asset_source=sha(MODEL)))
        return {"Wall plant S"}

    def duct(self,key,room,a,b,side=.19):
        x,y,z=a
        u,v,w=b
        assert sum(abs(m-n)>.0005 for m,n in zip(a,b))==1,(key,a,b)
        rect=(min(x,u)-side,min(z,w)-side,max(x,u)+side,max(z,w)+side)
        return self.box(key,rect,min(y,v)-side,max(y,v)+side,"galvanised",room,
                        "Formed rectangular ventilation duct")

    def air(self,key,room,pickup,shaft,soffit,roof_lookup,intake):
        x,y,z=pickup
        a,b=shaft
        assert x==a or z==b
        near=(a,y,b)
        parts=[self.duct(key+" branch",room,pickup,near).name]
        throat=(x,intake,z)
        if intake<y-.19:
            parts.append(self.duct(key+" intake boot",room,throat,pickup,side=.125).name)
        else:
            assert abs(y-.19-intake)<.04,(key,pickup,intake)
        roof,heights=roof_lookup(a,b,.20)
        top=max(heights)+.32
        parts.append(self.duct(key+" weathered riser",room,near,(a,top-.25,b)).name)
        collar=(a-.28,b-.28,a+.28,b+.28)
        parts.append(self.box(key+" curb and flashing",collar,min(heights)-.06,max(heights)+.14,
                    "galvanised",room,"Mounted duct penetration curb with roof contact").name)
        parts.append(self.box(key+" hood cap",(a-.30,b-.30,a+.30,b+.30),top,top+.09,
                    "cast iron",room,"Drip hood supported on riser").name)
        for i,(dx,dz) in enumerate(((-.15,-.15),(-.15,.15),(.15,-.15),(.15,.15)),1):
            x0,z0=a+dx,b+dz
            parts.append(self.box(f"{key} hood post {i}",(x0-.026,z0-.026,x0+.026,z0+.026),top-.08,top,
                        "galvanised",room,"Hood post seated on the riser lip").name)
        for fraction in (.23,.71):
            cx=x+(a-x)*fraction
            cz=z+(b-z)*fraction
            hit,p,n,_,obj,_=self.probe(cx,y+.20,cz,Vector((0,0,1)),.75)
            assert hit and n.z<-.96 and abs(p.z-soffit)<.04,(key,cx,cz,p[:],obj.name if hit else None)
            parts.append(self.pipe(key+" hanger "+str(fraction),(cx,y+.17,cz),(cx,p.z,cz),.016,
                        "cast iron",room).name)
        self.vents.append(dict(id=key,room=room,pickup=pickup,measured_ceiling=soffit,shaft=shaft,
                               roof=roof,mount_heights=heights,room_intake_y=intake,parts=parts))


def roof_probe(scene):
    meshes=[o for o in scene.objects if o.type=="MESH" and "roof" in o.get("role","")]
    trees=[]
    for obj in meshes:
        vertices=[obj.matrix_world@v.co for v in obj.data.vertices]
        faces=[tuple(p.vertices) for p in obj.data.polygons]
        trees.append((obj.name,BVHTree.FromPolygons(vertices,faces)))

    def pickup(x,z,r):
        matches=[]
        for dx,dz in ((-r,-r),(-r,r),(r,-r),(r,r)):
            at=point(x+dx,13,z+dz)
            hits=[]
            for name,tree in trees:
                hit,_,_,_=tree.ray_cast(at,Vector((0,0,-1)),11)
                if hit is not None:
                    hits.append((hit.z,name))
            assert hits,("No permanent weather cover at proposed exhaust",x,z,dx,dz)
            matches.append(max(hits))
        assert min(h[0] for h in matches)>3.5
        return sorted({h[1] for h in matches}),[round(h[0],6) for h in matches]

    return pickup


def drain_datum(z):
    return round(-3.80-(z-10)*.018,6)


def build(out):
    before=BASE/"college"
    source={ext:before/("college-containment."+ext) for ext in ("blend","glb","hcs2")}
    prior=json.loads((before/"construction.json").read_bytes())
    assert sha(source["blend"])==prior["blend_sha256"] and sha(source["glb"])==prior["glb_sha256"]
    assert sha(source["hcs2"])==prior["physics_sha256"]
    layout=json.loads((before/"authored-layout.json").read_bytes())
    rooms={row["id"]:row for row in layout["zones"]}
    bpy.ops.wm.open_mainfile(filepath=str(source["blend"]))
    bpy.context.preferences.filepaths.save_version=0
    services=Installation()
    cover=roof_probe(services.scene)
    obsolete=services.plant_wall()
    boiler=Fixture("west-plant-boiler","plant",BOILER,16.85,13.45)
    boiler_frame=services.floor_item(boiler,rooms,.08)
    manifold=Fixture("west-plant-manifold","plant",MANIFOLD,13.22,20.50,pi/2)
    manifold_frame=services.floor_item(manifold,rooms,.065)
    services.boiler_isolation(boiler,boiler_frame,manifold,manifold_frame)
    for fixture in WET:
        frame=services.floor_item(fixture,rooms)
        services.basin_wet(fixture,frame)
    for fixture in HEAT:
        frame=services.floor_item(fixture,rooms)
        services.branch(fixture,frame,"flow",0,.72,"right",.02)
        services.branch(fixture,frame,"return",0,.15,"left",-.03)
    hood=Fixture("lab-extract-cabinet","lab",HOOD,12.8,10.65,pi)
    hood_frame=services.floor_item(hood,rooms)
    hood_terminal=services.surface(hood,hood_frame,"extract",0,0,"top")[0]
    stove=Fixture("union-kitchen-range","kitchen",RANGE,60.15,50.75,pi)
    services.floor_item(stove,rooms)
    services.box("union kitchen canopy",(59.50,50.18,60.80,51.32),2.13,2.38,
                 "galvanised","kitchen","Capture canopy seated at the actual grease duct")
    for key,(x,y,r,z0,z1) in CARRIERS.items():
        finish={"flow":"cast iron","return":"painted","cold":"copper","hot":"copper"}[key]
        services.run(key+" campus carrier","campus",key,r,finish,[(x,y,z0),(x,y,z1)],FLOOR_PIPE)
    services.run("south gravity collector","campus","waste",.075,"soil",
                 [(DRAIN_X,drain_datum(10),10),(DRAIN_X,drain_datum(INLET_Z),INLET_Z)],FLOOR_PIPE)
    services.street_connections()
    for key,room,pickup,shaft,soffit,intake in (
        ("lab dedicated extract","lab",(12.80,3.06,10.65),(9.25,10.65),3.44,hood_terminal[1]),
        ("kitchen independent extract","kitchen",(60.15,3.06,50.75),(61.30,50.75),3.44,2.38),
        ("east shower extract","res-shower",(59.10,6.62,23.4),(61.22,23.4),7.04,6.43),
        ("plant room air shaft","plant",(17.65,-.55,11.35),(19.10,11.35),-.16,-.74),
    ):
        services.air(key,room,pickup,shaft,soffit,cover,intake)
    out.mkdir(parents=True,exist_ok=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in services.pieces:
        obj.select_set(True)
    additions=out/"services-additions.glb"
    targets={ext:out/("college-services."+ext) for ext in source}
    bpy.ops.export_scene.gltf(filepath=str(additions),export_format="GLB",use_selection=True,export_extras=True)
    append(source["glb"],additions,targets["glb"],remove_mesh_names=obsolete)
    parts=native_write(source["hcs2"],targets["hcs2"],obsolete,services.pieces)
    original=native_records(source["hcs2"])
    retained=[p for p in original if p[0].split(":",1)[-1] not in obsolete]
    assert len(original)-len(retained)==1
    assert native_records(targets["hcs2"])[:len(retained)]==retained
    bpy.ops.file.pack_all()
    bpy.ops.wm.save_as_mainfile(filepath=str(targets["blend"]),compress=True)
    packet=dict(assets={key:dict(path=v.path.relative_to(ROOT).as_posix(),sha256=sha(v.path))
                        for key,v in sorted(services.assets.items())},
                fixtures=services.fixtures,terminals=services.terminals,service_runs=services.runs,
                rejected_degenerate_source_faces=services.rejected_degenerate_faces,
                ventilation=services.vents,staff_doorways=services.approaches,street_controls=services.street_controls,
                removed=sorted(obsolete),retained_collision_parts=len(retained),
                native_parts=len(parts),native_triangles=sum(len(r) for _,r in parts),
                source={key:sha(p) for key,p in source.items()},exports={p.name:sha(p) for p in targets.values()})
    (out/"services.json").write_text(json.dumps(packet,indent=2)+"\n",encoding="utf-8",newline="\n")
    lines=(BASE/"verification/runtime.tsv").read_bytes().splitlines(keepends=True)
    lines[0]=f"SOURCE\t{sha(targets['glb'])}\t{sha(targets['hcs2'])}\n".encode()
    (out/"runtime.tsv").write_bytes(b"".join(lines))
    rows=[]
    for item in services.fixtures:
        a,b=item["size"]
        rows.append(("FIXTURE",item["key"],item["floor"],a[0],a[2],b[0],b[2]))
    for item in services.vents:
        rows.append(("VENT",item["id"],*item["shaft"],*item["mount_heights"]))
    text="".join("\t".join(map(str,row))+"\n" for row in rows)
    (out/"services.tsv").write_text(text,encoding="utf-8",newline="\n")
    print("COLLEGE SERVICES",len(services.fixtures),"mounted fixtures,",len(services.terminals),"measured terminations,",len(services.runs),"runs")


if __name__=="__main__":
    build(Path(sys.argv[sys.argv.index("--")+1]).resolve())
