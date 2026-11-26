"""Build founded canyon stairs and a continuous piled marsh settlement deck."""
import copy
import json
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append,read
from build_cemetery_grounds import rel,replace_terrain_on_export,terrain_surface
from build_coast_entrances import Structure,rectangle
from build_college_grounds import import_scene,place_review_van,terrain_tree
from build_deliberate_entrances import write
from build_mine_yard import punched_plate
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys,native_faces
from prepare_canyon_marsh import (BASE,BASELINE,CANYON,MARSH,PASS,SITES,SOURCE_WORLDS,VAN,
                                 WEST_CUT,RIDGE_CUT,CAVE_SILL,program,saved_world)
from review_roster_macroterrain import records,sha

STEM="canyon-marsh"
CAPTURES=BASE/"captures/canyon-marsh-batch"
WEST_RUNS=((44.6,40.88,0,2),(39.06,35.34,2,4))
RIDGE_RUNS=((29.77,26.05,4,6),(24.23,20.51,6,8))
MARSH_RUN=(65.4,62,0,1.2)
BRIDGE=(47.85,11.95,66,15.25)
DECKS=((28,14,32,56),(48,14,52,56),(28,56,52,62),(28,8,52,14),(28,-6,40,8))
GALLERY_WELL=(56.10,26.5,57.90,33.50)


def profile_wall(s,label,a,b,profile,base,mat,zone):
    points=[(a,-z,base) for z,_ in profile]+[(b,-z,base) for z,_ in profile]
    points += [(a,-z,y) for z,y in profile]+[(b,-z,y) for z,y in profile]
    n=len(profile)
    faces=[(0,n,n*3,n*2),(n-1,n*2-1,n*4-1,n*3-1)]
    for k in range(n-1):
        l=k+1
        faces.extend(((k,l,l+n,k+n),(k+2*n,l+2*n,l+3*n,k+3*n),
                      (k,l,l+2*n,k+2*n),(k+n,l+n,l+3*n,k+3*n)))
    mesh=bpy.data.meshes.new(label)
    mesh.from_pydata(points,[],faces)
    bm=bmesh.new()
    bm.from_mesh(mesh)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,label
    bm.to_mesh(mesh)
    bm.free()
    mesh.materials.append(mat)
    obj=bpy.data.objects.new(s.prefix+" "+label,mesh)
    bpy.context.collection.objects.link(obj)
    obj["role"],obj["zone"]="closed stone retaining side and stair parapet",zone
    s.built.append(obj)


def stair(s,label,left,right,run,foundation,n,mat,zone):
    start,end,low,high=run
    for k in range(n):
        a=start+(end-start)*k/n
        b=start+(end-start)*(k+1)/n
        y=low+(high-low)*(k+1)/n
        obj=s.box(f"{label} riser {k+1:02}",(left,min(a,b),right,max(a,b)),foundation,y,mat,
                  "founded full-depth stair tread",zone)
        s.stair_additions.append(obj.name)


def rebuilt_stair_bay(s,label,footprint,runs,base,stone,zone):
    a,b,c,d=footprint
    p,q=runs
    lower_landing=(a+.38,p[0],c-.38,d)
    middle_landing=(a+.38,q[0],c-.38,p[1])
    upper_landing=(a+.38,b,c-.38,q[1])
    for name,rectangle_,y in (("lower",lower_landing,p[2]),("middle",middle_landing,p[3]),
                               ("upper",upper_landing,q[3])):
        obj=s.box(f"{label} {name} landing",rectangle_,base,y,stone,"supported complete stair landing",zone)
        s.stair_additions.append(obj.name)
    for i,run in enumerate(runs,1):
        stair(s,f"{label} flight {i}",a+.38,c-.38,run,base,12,stone,zone)
    profile=((b,q[3]+.95),(q[1],q[3]+1.08),(q[0],q[2]+1.08),(p[1],p[3]+1.08),
             (p[0],p[2]+1.08),(d,p[2]+.93))
    for side,l,r in (("west",a,a+.38),("east",c-.38,c)):
        profile_wall(s,label+" "+side+" retaining side",l,r,profile,base,stone,zone)


def stair_guard(s,x,run,mat):
    front,rear,low,high=run
    for label,base,thickness in (("handrail",.96,.12),("midrail",.43,.09)):
        vertices=[(xx,-z,y+base+h) for z,y in ((front,low),(rear,high))
                  for xx,h in ((x-.055,0),(x+.055,0),(x+.055,thickness),(x-.055,thickness))]
        faces=((0,1,2,3),(7,6,5,4),(0,4,5,1),(1,5,6,2),(2,6,7,3),(3,7,4,0))
        mesh=bpy.data.meshes.new(f"Marsh stair {label} {x:g}")
        mesh.from_pydata(vertices,[],faces)
        bm=bmesh.new()
        bm.from_mesh(mesh)
        bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
        bm.to_mesh(mesh)
        bm.free()
        mesh.materials.append(mat)
        obj=bpy.data.objects.new(mesh.name,mesh)
        bpy.context.collection.objects.link(obj)
        obj["role"],obj["zone"]="closed sloped deck stair guard","south-deck"
        s.built.append(obj)
    for k,t in enumerate((.04,.43,.79,.98),1):
        at=front+(rear-front)*t
        y=low+(high-low)*t
        s.box(f"stair post {x:g} {k}",(x-.056,at-.057,x+.056,at+.057),y-.06,y+.96,
              mat,"stair edge guard fixed to tread","south-deck")


def canyon(s):
    ashlar=material("Red Mesa dry-set cut sandstone",(.57,.39,.26))
    core=material("Red Mesa sandy structural masonry",(.59,.46,.32))
    weather=material("Expedition shelter weather roof",(.29,.29,.26))
    guard=material("Red Mesa expedition dark timber rails",(.35,.28,.20))
    assert len(s.strips)==13
    s.retired.update(name for name,obj in s.old.items() if name.startswith(
        ("L04 flight","L07 flight","Authored stair end WLD-03 L04","Authored stair end WLD-03 L07",
         "Opened stair shell Wall shelter N"))
        or obj.get("role")=="structural-arrival-end-bearing"
        or (obj.get("role"),obj.get("zone")) in {("ceiling-cap","shelter"),("floor","lower-cave"),("floor","gallery")})
    s.retire_faces(("shelter","N"),("lower-cave","S"),("gallery","W"),("chamber","W"))
    s.face("expedition shelter north wall","z",46,(4,20),0,3,ashlar,
           [("shelter stair door",12,1.84,2.27,["shelter","lower-path"])],"shelter")
    s.box("complete expedition shelter roof",(3.91,45.92,20.09,58.09),2.82,3.12,weather,
          "complete weather roof above the expedition shelter","shelter")
    s.face("lower cave south approach wall","z",40,(48,66),4,4,ashlar,
           [("lower ledge cave door",52,1.88,2.34,["lower-path","lower-cave"])],"lower-cave")
    s.part("full lower cave floor and landing",[rectangle((48,22,66,40)),rectangle(CAVE_SILL)],
           3.76,4,core,"continuous cave floor and founded ledge threshold","lower-cave")
    s.box("retaining foundation beneath cave court",CAVE_SILL,1.35,3.76,ashlar,
          "founded stone site threshold","lower-cave")
    for x,z in ((48.44,40.73),(48.44,43.67),(54.96,41.08),(54.96,43.58)):
        s.box(f"records alcove pier at {x:g} {z:g}",(x-.24,z-.24,x+.24,z+.24),
              3.76,7.84,ashlar,"alcove footing and sheltered lower cave court support","lower-cave")
    s.face("upper gallery terrace wall","x",48,(22,40),8,3,ashlar,
           [("dwelling terrace gallery door",31,1.93,2.34,["dwellings","gallery"])],"gallery")
    s.face("chamber bridge wall","x",66,(4,22),8,3,ashlar,
           [("chamber bridge door",13.6,1.97,2.36,["upper-path","chamber"])],"chamber")
    s.part("gallery floor around the enclosed stair bay",
           [rectangle(b) for b in punched_plate((48,22,66,40),GALLERY_WELL)],
           7.84,8,core,"complete gallery floor with deliberate stair opening","gallery")
    a,b,c,d=GALLERY_WELL
    for label,x in (("west",a-.057),("east",c+.057)):
        s.rail("gallery stair "+label+" guard",(x,b-.06),(x,d-.055),8,guard,"gallery")
    s.rail("gallery well lower end guard",(a-.05,b-.06),(c+.05,b-.06),8,guard,"gallery")

    rebuilt_stair_bay(s,"shelter hillside stair",WEST_CUT,WEST_RUNS,-.57,ashlar,"lower-path")
    rebuilt_stair_bay(s,"upper terrace hillside stair",RIDGE_CUT,RIDGE_RUNS,3.32,ashlar,"upper-path")

    s.box("chamber crossing deck",BRIDGE,7.69,8,core,
          "complete bridge deck across lower return apron","upper-path")
    for x in (48.38,51.62,60.55,65.48):
        # Foundations remain away from the north cave door and its full apron.
        s.box(f"crossing masonry pier at {x:g}",(x-.40,12.18,x+.40,15.02),2.72,7.35,ashlar,
              "founded bridge pier on the measured canyon ledge","upper-path")
    for z in (12.31,14.89):
        s.box(f"continuous bridge beam at {z:g}",(47.89,z-.22,66,z+.22),7.34,7.69,guard,
              "bridge beam on masonry piers","upper-path")
    for label,z in (("north",12.08),("south",15.12)):
        s.rail("chamber bridge "+label+" guard",(48.12,z),(65.88,z),8,guard,"upper-path")

    s.areas.update(west_stair=WEST_CUT,west_runs=WEST_RUNS,upper_stair=RIDGE_CUT,ridge_runs=RIDGE_RUNS,
                   lower_cave_landing=CAVE_SILL,upper_chamber_bridge=BRIDGE,gallery_well=GALLERY_WELL)
    s.controls.extend([(34,0,82.372),(34,0,53),(12,0,45.1),(12,2,40),(12,4,34.6),
                       (23.625,4,30.6),(23.625,6,25),(23.625,8,19.8),
                       (34,8,31),(38,4,42.5),(58,8,13.6),(57,4,14)])


def marsh(s,ground,layout):
    timber=material("Marsh aged plank settlement boardwalk",(.43,.33,.23))
    piles=material("Marsh treated structural timber",(.29,.24,.19))
    skin=material("Marsh painted timber facades",(.50,.46,.35))
    assert len(s.strips)==20
    landings={"main","service","south-deck","north-deck","ferry"}
    rooms=[z["bounds"] for z in layout["zones"] if z["kind"]=="inside"]
    s.retired.update(name for name,obj in s.old.items() if name.startswith(("L02 flight","Authored stair end WLD-11 L02"))
                     or obj.get("role")=="structural-arrival-end-bearing"
                     or (obj.get("role")=="floor" and obj.get("zone") in landings))
    for name,obj in s.old.items():
        if obj.get("role")!="terrain-bearing foundation construction":
            continue
        points=[obj.matrix_world@Vector(v) for v in obj.bound_box]
        x=sum(p.x for p in points)/len(points)
        z=-sum(p.y for p in points)/len(points)
        if not any(a<=x<=c and b<=z<=d for a,b,c,d in rooms):
            s.retired.add(name)
    s.part("continuous settlement boardwalk",[rectangle(box) for box in DECKS],.95,1.2,timber,
           "closed continuous piled settlement and ferry deck","boardwalk")
    # The rock shelf is dry at the rear of the low landing. Piles carry the
    # front of the landing and the main stair over the former bank.
    s.box("dry levee landing",(37.7,65.4,42.3,70),-.25,0,timber,
          "complete grade to stair landing","entry")
    stair(s,"wide approach stair",38.68,41.32,MARSH_RUN,-.24,7,timber,"south-deck")
    tree=terrain_tree(ground)

    def bent(label,points,top):
        for x,z in points:
            hit=tree.ray_cast(Vector((x,-z,4)),Vector((0,0,-1)))[0]
            assert hit is not None,(label,x,z)
            if hit.z<top-.34:
                s.box(f"{label} pile at {x:g} {z:g}",(x-.18,z-.18,x+.18,z+.18),hit.z-.52,top-.23,piles,
                      "paired pile bearing into flooded bed","boardwalk")
        x0,z0=points[0]
        x1,z1=points[1]
        s.box(label+" cross tie",(min(x0,x1)-.21,min(z0,z1)-.20,max(x0,x1)+.21,max(z0,z1)+.20),
              top-.46,top-.22,piles,"deck cross beam seated on timber piles","boardwalk")

    for k,z in enumerate((15.1,20.2,25.9,31.6,37.3,43,48.7,54.4),1):
        for name,x in (("main",28.43),("service",48.43)):
            bent(f"{name} boardwalk bent {k}",[(x,z),(x+3.14,z)],1.2)
    for label,z in (("north",8.6),("south",56.63)):
        for i,x in enumerate((28.58,34.23,40.0,45.77,51.42),1):
            bent(f"{label} cross-deck bent {i}",[(x,z),(x,z+4.76)],1.2)
    for i,z in enumerate((-5.35,-1.5,2.6,6.58),1):
        points=[(28.58,z),(39.42,z)]
        # Where the old map ends at the water, the real deck cantilevers
        # back to the last two rows fixed in the bed.
        if all(tree.ray_cast(Vector((x,-t,4)),Vector((0,0,-1)))[0] is not None for x,t in points):
            bent(f"ferry wharf bent {i}",points,1.2)
    for name,z,y in (("foreshore",69.20,0),("low landing",65.74,0),("mid stair",63.68,.56)):
        bent(name+" transfer bent",[(38.88,z),(41.12,z)],y)
    for x in (38.74,41.26):
        stair_guard(s,x,MARSH_RUN,piles)
    for x in (38.92,41.08):
        profile_wall(s,f"founded approach stair beam {x:g}",x-.11,x+.11,[(62,1.16),(65.4,-.04)],
                     -.42,piles,"south-deck")

    for label,axis,x,lo,hi,z,rooms_ in (
        ("west house","x",28,38,54,46,["main","house-a"]),
        ("east house","x",52,38,54,46,["service","house-b"]),
        ("chapel","x",28,16,34,25,["main","chapel"]),
        ("warehouse public","x",32,14,36,25,["main","warehouse"]),
        ("warehouse goods","x",48,14,36,25,["warehouse","service"]),
        ("east work shack","x",52,18,32,25,["service","shack"]),
        ("pump room","x",52,54,64,58,["service","pump"]),
    ):
        room=next(room for room in rooms_ if room not in ("main","service"))
        side="E" if x in (28,48) else "W"
        s.retire_faces((room,side))
        s.face(label+" boardwalk elevation",axis,x,(lo,hi),1.2,3,skin,
               [(label+" door",z,1.94,2.26,rooms_)],room)

    edges=(
        ("west dry edge",(28.08,54.04),(28.08,61.91)),
        ("west between houses",(28.08,34.10),(28.08,37.92)),
        ("north west edge",(28.08,-5.94),(28.08,15.95)),
        ("north east edge",(51.92,8.08),(51.92,17.94)),
        ("east between houses",(51.92,32.08),(51.92,37.94)),
        ("western wet edge",(31.92,36.08),(31.92,55.91)),
        ("eastern wet edge",(48.08,36.08),(48.08,55.91)),
        ("south wet edge",(31.92,56.08),(48.08,56.08)),
        ("south landing west guard",(28.08,61.91),(38.54,61.91)),
        ("south landing east guard",(41.46,61.91),(51.92,61.91)),
        ("ferry far end",(28.08,-5.91),(39.92,-5.91)),
        ("ferry east edge",(39.92,-5.91),(39.92,8.08)),
        ("ferry cross-deck edge",(39.92,8.08),(51.92,8.08)),
    )
    for label,a,b in edges:
        s.rail(label,a,b,1.2,piles,"boardwalk")
    s.rail("roadhead landing west edge",(37.79,65.48),(37.79,69.85),0,piles,"entry")
    s.rail("roadhead landing east edge",(42.21,65.48),(42.21,69.85),0,piles,"entry")

    # Visible standing water is a separate native review surface. The actual
    # editor bed and supported decks supply all terrain and body authority.
    mesh=bpy.data.meshes.new("Mourning Marsh flood basin")
    outline=((4,-13),(70,-13),(74,48),(62,72),(46,76),(42,70),(21,74),(13,78),(4,67))
    mesh.from_pydata([(x,-z,-.90) for x,z in outline],[],[tuple(reversed(range(len(outline))))])
    mesh.update()
    mesh.materials.append(material("Still floodwater over marsh bed",(.19,.32,.30)))
    water=bpy.data.objects.new("Mourning Marsh floodwater review surface",mesh)
    bpy.context.collection.objects.link(water)
    water["role"]="opaque review water surface above the saved wetland bed"
    s.visual_additions.append(water)
    s.areas.update(deck_footprints=DECKS,approach_stair=MARSH_RUN,lower_landing=(37.7,65.4,42.3,70),
                   flood_review_level=-.90,pile_bents=(4,4.76))
    s.controls.extend([(32,0,78.372),(40,0,68.3),(40,1.2,59),(30,1.2,32),
                       (50,1.2,36),(40,1.2,11),(34,1.2,1)])


def guidance(site,s,layout):
    links={row["id"]:row for row in layout["links"]}
    original={}
    changed_flights={}
    added=[]

    def guide(number,points,width=None):
        key=f"L{number:02}"
        entry=links[key]
        original[key]=copy.deepcopy(entry)
        entry["points"]=[list(p) for p in points]
        if width is not None:
            entry["width"]=width
        entry["note"]="Nonphysical guidance through developed doors, terrain, decks and real stair landings."

    def level(number,points,y=0,width=None):
        guide(number,[(x,y,z) for x,z in points],width)

    if site==CANYON:
        level(1,[(34,82.372),(34,80),(34,73)])
        level(3,[(34,53),(22,52),(20,52),(12,52)])
        guide(4,[(12,0,52),(12,0,46),(12,0,44.6),(12,2,40.88),(12,2,39.06),
                 (12,4,35.34),(12,4,34),(12,4,33.5)],1.85)
        level(5,[(34,53),(46,52),(48,52),(54,52)])
        level(6,[(54,52),(48,52),(46,52),(34,53)])
        x=23.625
        guide(7,[(12,4,33.5),(14.9,4,32.0),(20.3,4,32.65),(x,4,32.46),(x,4,29.77),
                 (x,6,26.05),(x,6,24.23),(x,8,20.51),(x,8,18.45),(28,8,16),(34,8,16)],1.85)
        level(8,[(34,16),(34,22),(34,31)],8)
        level(9,[(34,31),(47,31),(48,31),(52.3,31),(55,31)],8)
        level(10,[(55,31),(55,34.7),(55.6,35.0),(64.5,35.0),(64.7,31),(66,31),(71,31)],8,1.40)
        level(12,[(74,13),(68,13.6),(66,13.6),(49,13.6),(47.85,13.6),(43.8,14.3),(34,16)],8,1.65)
        level(15,[(57,14),(57,22),(57,23.6),(52,24.8),(52,40),(52,42.35),(46.2,42.35),(44.0,42.25),
                  (32,42.05),(24.6,41.38),(21.82,40.88),(18.7,38.58),(15.05,34.6),(15.05,32.90),(12,32.90),(12,33.5)],4,1.58)
        for key,first,second,new in (("L04",2,4,24),("L07",4,6,25)):
            changed_flights[key]={first:0,second:new}
            added.append(dict(link=key,segment=new,layout_segment=second,points=links[key]["points"][second:second+2]))
    else:
        level(1,[(32,78.372),(32,76),(32,72)])
        guide(2,[(32,0,72),(39.98,0,72),(39.98,0,70.17),(39.98,0,65.46),
                 (39.98,1.2,62),(39.98,1.2,59),(40,1.2,59)],1.85)
        level(3,[(40,59),(30,59),(30,38)],1.2)
        level(4,[(40,59),(50,59),(50,38)],1.2)
        level(5,[(30,38),(30,11),(40,11)],1.2)
        level(6,[(40,11),(50,11),(50,38)],1.2)
        changed_flights["L02"]={3:0}
    layout["authored_doorways"]=s.portals
    layout["architectural_interfaces"]=dict(s.areas,nonphysical_guidance=True)
    return original,changed_flights,added


def review(site,out,meta,layout,controls):
    glb,native=(out/(STEM+extension) for extension in (".glb",".hcs2"))
    prior,prior_bytes=read(ROOT/meta["source_glb"])
    current,current_bytes=read(glb)
    assert current_bytes[:len(prior_bytes)]==prior_bytes
    for field in ("meshes","materials","accessors","bufferViews","images","textures"):
        original=prior.get(field,[])
        assert current.get(field,[])[:len(original)]==original,field
    removed=set(meta["removed_source_meshes"])
    for index,node in enumerate(prior["nodes"]):
        expected=dict(node)
        if node.get("name") in removed:
            expected.pop("mesh",None)
        assert current["nodes"][index]==expected,node.get("name")
    expected_parts=[part for part in split_native(ROOT/meta["source_physics"])
                    if not part[0].startswith("terrain-") and part[0].split(":",1)[-1] not in removed]
    assert split_native(native)[:len(expected_parts)]==expected_parts

    objects=import_scene(glb)
    native_parts=read_parts(native)
    ground=objects[meta["terrain_mesh"]]
    assert face_keys(records(ground))==face_keys(native_faces(saved_world(site)/"terrain-physics.hcs2"))
    assert not any(obj.get("role","").startswith("rough route support") for obj in objects.values())
    vertices,faces=[],[]
    for name,obj in objects.items():
        if name in meta["native_visual_only"]:
            continue
        start=len(vertices)
        vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
        if name not in meta["new_interface_parts"]:
            continue
        bm=bmesh.new()
        bm.from_mesh(obj.data)
        bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001)
        assert all(edge.is_manifold for edge in bm.edges) and bm.calc_volume(signed=True)>0,name
        bm.free()
        assert face_keys(records(obj))==face_keys(native_parts[name][1]),name
    assert all(name not in native_parts for name in meta["native_visual_only"])
    tree=BVHTree.FromPolygons(vertices,faces)

    def ray(a,b):
        a,b=Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def stand(x,y,z):
        hit=ray((x,-z,y+.08),(x,-z,y-.15))
        assert hit is not None and abs(hit.z-y)<.052,(site,(x,y,z),hit)
        assert ray((x,-z,y+.081),(x,-z,y+1.91)) is None,(site,(x,y,z),"headroom")

    apertures,sills=0,0
    for door in meta["new_portals"]:
        fixed,along,y=(door[k] for k in ("boundary","along","sill_y"))
        for f in (-.33,0,.33):
            u=along+door["clear_width"]*f
            for h in (.3,1.21,door["clear_height"]-.12):
                if door["axis"]=="x":
                    a,b=(fixed-.42,-u,y+h),(fixed+.42,-u,y+h)
                else:
                    a,b=(u,-fixed-.42,y+h),(u,-fixed+.42,y+h)
                assert ray(a,b) is None,(site,door["id"],a,b)
                apertures+=1
            for delta in (-.19,.19):
                x,z=(fixed+delta,u) if door["axis"]=="x" else (u,fixed+delta)
                stand(x,y,z)
                sills+=1
    for point in controls:
        stand(*point)

    van=place_review_van(layout["staging"])
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.render.resolution_x,scene.render.resolution_y=1250,890
    bpy.ops.object.camera_add()
    cam=bpy.context.object
    scene.camera=cam
    CAPTURES.mkdir(parents=True,exist_ok=True)
    views=(
        [("overview",(65,-94,99),(41,-45,3),118),
         ("roadhead",(27,-81,1.62),(34,-62,1.40),0),
         ("shelter-stair",(12,-45.58,1.62),(12,-36.9,3.30),0),
         ("upper-stair",(23.625,-31.01,5.62),(23.62,-21.15,8.32),0),
         ("upper-terrace",(32,-26,9.62),(46,-31,9.14),0),
         ("lower-cave-sill",(52.05,-43.8,5.62),(52,-38.5,5.37),0),
         ("terrace-section",(52,-43,28),(42,-26,5.3),37)]
        if site==CANYON else
        [("overview",(62,-99,95),(36,-43,0),122),
         ("levee-arrival",(26.5,-76.3,1.62),(39,-67,1.25),0),
         ("settlement-stair",(39.96,-67.65,1.62),(39.96,-60,2.50),0),
         ("main-boardwalk",(29.87,-53.5,2.82),(29.87,-39.2,2.70),0),
         ("warehouse-sill",(30,-25.06,2.82),(33.8,-25.03,2.5),0),
         ("piles-section",(65,-50,19),(44,-52,-.75),42)])
    captures=[]
    for label,pos,target,scale in views:
        hidden=[]
        if label=="piles-section":
            hidden=meta["native_visual_only"]
        for name in hidden:
            objects[name].hide_render=True
        cam.data.type="ORTHO" if scale else "PERSP"
        cam.data.ortho_scale=scale or 1
        cam.data.lens=22
        cam.location=pos
        cam.rotation_euler=(Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        image=CAPTURES/(site.lower()+"-"+label+".png")
        scene.render.filepath=str(image)
        bpy.ops.render.render(write_still=True)
        for name in hidden:
            objects[name].hide_render=False
        captures.append(dict(path=rel(image),sha256=sha(image),source_glb_sha256=sha(glb),hidden=hidden))
    write(out/"export-verification.json",dict(glb_sha256=sha(glb),native_sha256=sha(native),
          retained_native_records=len(expected_parts),new_closed_parts=len(meta["new_interface_parts"]),
          aperture_rays=apertures,sill_controls=sills,independent_standing=len(controls),
          canonical_van_triangles=van,canonical_van_source=sha(VAN/"geometry.json"),captures=captures))
    print("CANYON MARSH EXPORT REVIEW",site,apertures,sills,len(controls),flush=True)


def build(source):
    site=source["id"]
    folder=PASS/site.lower()
    folder.mkdir(parents=True,exist_ok=True)
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    terrain_parts=[part for part in split_native(ROOT/source["native"]) if part[0].startswith("terrain-")]
    assert terrain_parts==split_native(SOURCE_WORLDS[site]/"terrain-physics.hcs2")
    objects=import_scene(ROOT/source["glb"])
    source_terrain=next(obj for obj in objects.values() if obj.get("role","").startswith("saved editor terrain"))
    world=saved_world(site)
    ground=terrain_surface(site,source_terrain,world,program(site))
    ground["role"]="saved editor terrain / authored terraced canyon" if site==CANYON else "saved editor terrain / marsh basin and founded dry approach"
    aggregate=ground.data.materials[-1]
    aggregate.name="Red Mesa compacted gravel" if site==CANYON else "Mourning Marsh levee gravel"
    aggregate.diffuse_color=(.49,.36,.25,1) if site==CANYON else (.39,.36,.29,1)
    structure=Structure("Red Mesa" if site==CANYON else "Mourning Marsh",objects)
    structure.visual_additions=[]
    structure.retired.add(source_terrain.name)
    if site==CANYON:
        canyon(structure)
    else:
        marsh(structure,ground,layout)
    original,changes,added=guidance(site,structure,layout)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    assert structure.retired<=objects.keys(),structure.retired-objects.keys()
    for name in structure.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    files={rel(path):sha(path) for path in world.rglob("*") if path.is_file() and not path.name.endswith(".lock")}
    layout["authoritative_saved_terrain"]=dict(world=rel(world),files_sha256=files)
    laid=folder/"authored-layout.json"
    write(laid,layout)
    glb,native,blend=(folder/(STEM+extension) for extension in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*structure.built,*structure.visual_additions]:
        obj.select_set(True)
    addition=ROOT/"build"/(site.lower()+"-canyon-marsh-additions.glb")
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],addition,glb,remove_mesh_names=sorted(structure.retired))
    replace_terrain_on_export(ROOT/source["native"],native,structure.retired,structure.built,world,False)
    previous=copy.deepcopy(meta["stair_components"])
    preserved=[name for name in previous if name.split(":",1)[-1] not in structure.retired]
    old_map=copy.deepcopy(meta.get("source_segment_map",{}))
    meta.update(status="CANYON_AND_MARSH_FOUNDED_ARCHITECTURE_RUNTIME_OPEN",
                source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                layout=rel(laid),layout_sha256=sha(laid),
                before_stair_components=previous,stair_components=preserved+structure.stair_additions,
                before_segment_map=old_map,source_segment_map={**old_map,**changes},interface_added_flights=added,
                saved_world=rel(world),saved_world_files_sha256=files,terrain_mesh=ground.name,
                retired_study_strips=sorted(structure.strips),removed_source_meshes=sorted(structure.retired),
                new_interface_parts=[obj.name for obj in structure.built],
                native_visual_only=[obj.name for obj in structure.visual_additions],
                new_portals=structure.portals,builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(folder/"construction.json",meta)
    write(folder/"quarantine.json",dict(source=source,retired=sorted(structure.retired),superseded_guides=original))
    if "--build-only" not in sys.argv:
        review(site,folder,meta,layout,structure.controls)
    return dict(source,construction=rel(folder/"construction.json"),layout=rel(laid),glb=rel(glb),
                native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))


if __name__=="__main__":
    roster=json.loads(BASELINE.read_bytes())
    roster["active_sources"]=[build(row) if row["id"] in SITES else row for row in roster["active_sources"]]
    for row in roster["sites"]:
        if row["id"] in SITES:
            row["decision"]="CANYON_AND_MARSH_FOUNDED_ARCHITECTURE_RUNTIME_OPEN"
    roster["baseline"]=rel(BASELINE)
    write(PASS/"roster.json",roster)
