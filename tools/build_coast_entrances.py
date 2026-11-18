"""Rebuild the lightstation entrance and the vessel's fixed quay and weather decks."""
import copy
import json
import math
from pathlib import Path
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/"tools"))
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_circulation_construction import native_write
from build_college_grounds import import_scene, place_review_van, terrain_tree
from build_deliberate_entrances import write
from build_mine_yard import punched_plate
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from planar_structure import plate
from prepare_coast_entrances import BASE, BASELINE, OLD_WORLD, PASS, PROGRAM, VAN, WORLD
from review_roster_macroterrain import records, sha

STEM = "coast-entrances"
CAPTURES = BASE / "captures/coast-entrances-batch"
LIGHT = "WLD-14"
SHIP = "WLD-15"
UPPER_WELLS = ((44.1,40,45.9,48),(13.1,36,14.9,44))
FORE_WELL = (28.6,.1,34.6,1.9)
AFT = [(22,40),(42,40),(42,50),(39,56),(25,56),(22,50)]
STARBOARD = (40,8,42,40)
QUAY = (16,63.72,42,64)
PIER = (16,40,22,64)
GANGWAY = (30.5,56,33.5,64)


def rectangle(box):
    a,b,c,d = box
    return [(a,b),(c,b),(c,d),(a,d)]


class Structure:
    def __init__(self, prefix, old):
        self.prefix = prefix
        self.old = old
        self.areas = {}
        self.built = []
        self.portals = []
        self.controls = []
        self.stair_additions = []
        self.template = frame_template()
        self.strips = {name for name,obj in old.items() if obj.get("role","").startswith("rough route support")}
        self.retired = set(self.strips)

    def part(self, label, polygons, low, high, mat, role, zone):
        solids = plate(self.prefix+" "+label, polygons, high, high-low, mat)
        assert len(solids) == 1, label
        obj = solids[0]
        obj["role"], obj["zone"] = role, zone
        self.built.append(obj)
        return obj

    def box(self, label, box, low, high, mat, role, zone):
        return self.part(label,[rectangle(box)],low,high,mat,role,zone)

    def close(self, label, extent, y, height, mat, zone):
        self.box(label,extent,y,y+height,mat,"continuous solid architectural wall",zone)

    def retire_faces(self, *faces):
        for room, side in faces:
            key = f"{room} {side}"
            self.retired.update(name for name in self.old if any(name == prefix+key or name.startswith(prefix+key+".")
                                for prefix in ("Wall ","Header ","Flat route shell Wall ")))

    def face(self, name, axis, fixed, ends, y, height, mat, openings, zone):
        depth = .12

        def masonry(label, low, high, bottom, top):
            dimensions = (low,fixed-depth,high,fixed+depth) if axis=="z" else (fixed-depth,low,fixed+depth,high)
            self.close(name+" "+label,dimensions,bottom,top-bottom,mat,zone)

        cursor = ends[0]
        for label, center, width, clear, rooms in openings:
            lo,hi = center-.62*width,center+.62*width
            assert cursor < lo < hi < ends[1], label
            lintel = y+clear*2.275/2.125
            masonry(label+" preceding pier",cursor,lo,y,y+height)
            masonry(label+" lintel",lo,hi,lintel,y+height)
            frame = self.template.copy()
            frame.data = self.template.data.copy()
            bpy.context.collection.objects.link(frame)
            frame.name = self.prefix+" "+label+" fitted frame"
            frame.data.materials.clear()
            frame.data.materials.append(mat)
            for v in frame.data.vertices:
                a,b,h = v.co
                a,b,h = a*width,b*.24/.18,y+h*clear/2.125
                v.co = (center-a,-fixed-b,h) if axis=="z" else (fixed+b,-center-a,h)
            frame["role"],frame["source_asset"] = "indexed architectural doorway","level_blockout_doorway"
            self.built.append(frame)
            self.portals.append(dict(id=label,axis=axis,boundary=fixed,along=center,sill_y=y,
                                     clear_width=width,clear_height=clear,rooms=rooms))
            cursor = hi
        masonry("end pier",cursor,ends[1],y,y+height)

    def rail(self, label, a, b, y, mat, zone):
        dx,dz=b[0]-a[0],b[1]-a[1]
        length=math.hypot(dx,dz)
        normal=(-dz/length*.054,dx/length*.054)
        footprint=[(x+sign*normal[0],z+sign*normal[1]) for (x,z),sign in ((a,-1),(b,-1),(b,1),(a,1))]
        self.part(label+" top rail", [footprint], y+.98, y+1.10, mat, "deck edge guard", zone)
        self.part(label+" mid rail", [footprint], y+.43, y+.52, mat, "deck edge guard", zone)
        posts=max(1,math.ceil(length/2.2))
        for index in range(posts+1):
            x,z=a[0]+dx*index/posts,a[1]+dz*index/posts
            self.box(f"{label} post {index}",(x-.055,z-.055,x+.055,z+.055),y-.04,y+.98,mat,
                     "edge guard fixed to deck",zone)


def tower_structure(s):
    steel=material("Lightstation cast iron tower stair supports",(.26,.30,.31))
    column=[(32+.54*math.cos(i*math.tau/32),10+.54*math.sin(i*math.tau/32)) for i in range(32)]
    s.part("tower central structural column",[column],-.60,21.44,steel,
           "founded column and radial stair load path","tower0")
    for name,r0,r1 in (("inner",2.08,2.25),("outer",3.33,3.49)):
        vertices=[]
        for i in range(141):
            angle=i*math.tau/28
            y=3.6*i/28-.16
            for radius,depth in ((r0,.24),(r1,.24),(r1,0),(r0,0)):
                vertices.append((32+math.cos(angle)*radius,-10-math.sin(angle)*radius,y-depth))
        faces=[(0,1,2,3)]
        for i in range(140):
            a=i*4
            faces.extend((a+j,a+(j+1)%4,a+4+(j+1)%4,a+4+j) for j in range(4))
        faces.append(tuple(range(563,559,-1)))
        mesh=bpy.data.meshes.new("tower "+name+" helical curved girder")
        mesh.from_pydata(vertices,[],faces)
        mesh.update()
        bm=bmesh.new()
        bm.from_mesh(mesh)
        bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
        bmesh.ops.triangulate(bm,faces=list(bm.faces))
        assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0
        bm.to_mesh(mesh)
        bm.free()
        obj=bpy.data.objects.new("Lightstation "+mesh.name,mesh)
        bpy.context.collection.objects.link(obj)
        mesh.materials.append(steel)
        obj["role"],obj["zone"]="full-height helical stair supporting girder","tower0"
        s.built.append(obj)
    for i in range(4,140,7):
        angle=(i+.5)*math.tau/28
        dx,dz=math.cos(angle),math.sin(angle)
        y=3.6*(i+.5)/28-.16
        quadrilateral=[(32+dx*r-dz*w,10+dz*r+dx*w) for r,w in ((.46,-.11),(3.47,-.11),(3.47,.11),(.46,.11))]
        s.part(f"tower radial stair arm {i}",[quadrilateral],y-.33,y-.12,steel,
               "bolted stair beam from column to curved girders","tower0")


def lighthouse(s):
    ashlar = material("Lightstation limewashed masonry", (.61,.58,.50))
    stone = material("Lightstation cut headland stone", (.43,.42,.38))
    joists = material("Lightstation floor with timber edges", (.42,.34,.24))
    slate = material("Lightstation weather slate", (.22,.26,.28))
    guard = material("Lightstation painted timber guards", (.42,.36,.28))
    assert len(s.strips)==6
    old_terrain=next(obj for obj in s.old.values() if obj.get("role","").startswith("saved editor terrain"))
    s.retired.add(old_terrain.name)
    s.retired.update(name for name,obj in s.old.items() if obj.get("role")=="structural-arrival-end-bearing"
                     or obj.get("role")=="floor" and obj.get("zone") in {"porch","upperhall","service-upper","tower0","passage"})

    s.retire_faces(("foyer","S"),("kitchen","S"),("generator","S"),("passage","N"),
                   ("study","E"),("upperhall","W"),("bed","W"),("service-upper","E"))
    s.face("keeper entrance elevation","z",54,(24,32),0,3,ashlar,
           [("keeper front door",30,1.82,2.25,["porch","foyer"])],"foyer")
    s.face("kitchen court elevation","z",54,(32,42),0,3,ashlar,
           [("kitchen service door",38.5,1.82,2.25,["kitchen","yard"])],"kitchen")
    s.face("oil house south elevation","z",46,(48,58),0,3,stone,
           [("oil house delivery door",54,1.86,2.26,["yard","generator"])],"generator")
    s.face("tower passage end","z",14.55,(28,36),0,3,stone,
           [("tower base doorway",32,1.57,2.2,["passage","tower0"])],"passage")
    s.face("main stair study partition","x",42,(30,54),3.2,3,joists,
           [("upper study doorway",43.80,1.83,2.24,["upperhall","study"])],"upperhall")
    s.face("service landing bedroom partition","x",16,(30,54),3.2,3,joists,
           [("keeper bedroom doorway",43.5,1.82,2.24,["bed","service-upper"])],"service-upper")

    # The entry porch has a weather cover with two founded stone piers.
    for x in (27.5,32.5):
        s.box(f"entrance porch pier at {x}",(x-.20,57.95,x+.20,58.35),-.6,2.64,ashlar,
              "founded porch roof pier","porch")
    s.box("entrance porch cross beam",(27.15,57.92,32.85,58.38),2.60,2.78,joists,
          "beam supported by the porch piers","porch")
    s.box("keeper porch roof",(27.05,53.94,32.95,58.55),2.78,3.0,slate,
          "weather roof bearing on wall and porch piers","porch")

    for label,zone,outer,well,low_end in (
        ("main","upperhall",(42,30,48,54),UPPER_WELLS[0],48),
        ("service","service-upper",(12,30,16,54),UPPER_WELLS[1],44)):
        s.part(label+" continuous upper deck", [rectangle(b) for b in punched_plate(outer,well)
               if b[2]>b[0] and b[3]>b[1]], 3.04,3.2,joists,"floor around physical stair well",zone)
        a,b,c,d=well
        for edge,x in (("west",a-.058),("east",c+.058)):
            s.rail(label+" well "+edge,(x,b+.08),(x,d+.048),3.2,guard,zone)
        s.rail(label+" lower end guard",(a-.055,low_end+.048),(c+.055,low_end+.048),3.2,guard,zone)
    # Merge the level tower slab with a short masonry neck at the actual
    # round-shell aperture and the enclosed connection to the house.
    circle=[(32+4.5*math.cos(i*math.tau/64),10+4.5*math.sin(i*math.tau/64)) for i in range(64)]
    passage=[(28,14.54),(31,14.54),(31,14.30),(33,14.30),(33,14.54),(36,14.54),(36,30),(28,30)]
    s.part("continuous passage and tower slab",[circle,passage],-.24,0,stone,
           "founded passage and circular tower floor","passage")
    s.box("tower portal sill masonry", (30.84,14.18,33.16,14.62),-.66,-.24,stone,
          "full bearing beneath tower access neck","tower0")
    tower_structure(s)
    s.controls.extend([(32,0,80.372),(30,0,57),(54,0,52),(32,0,14.46),(45,3.2,39),(14,3.2,35)])
    s.areas.update(porch=(27.05,53.94,32.95,58.55),upper_wells=UPPER_WELLS,tower_center=(32,10),
                   tower_deck_radius=4.5, passage_outline=passage)
    return old_terrain


def vessel(s):
    assert len(s.strips)==10
    terrain=s.old["Authored quay terrain"]
    ground=terrain_tree(terrain)
    concrete=material("Harbour sea-worn quay concrete",(.46,.44,.39))
    steel=material("Saint Elara weather deck steel",(.35,.40,.39))
    steel_dark=material("Saint Elara welded steel structure",(.25,.31,.31))
    timber=material("Saint Elara timber service pier",(.41,.34,.26))
    paint=material("Saint Elara painted bulkheads",(.59,.61,.57))
    s.retired.update(name for name,obj in s.old.items() if obj.get("role")=="floor"
                     and obj.get("zone") in {"quay","portpier","fore"})
    s.retired.update(name for name in s.old if name.startswith("Bearing pile "))
    s.retire_faces(("galley","S"),("mess","S"))
    s.face("aft deckhouse bulkhead","z",40,(24,40),0,3,paint,
           [("galley weather door",29.6,1.80,2.23,["aft","galley"]),
            ("mess weather door",34.4,1.80,2.23,["aft","mess"])],"aft")

    # The land behind the seawall is already a continuous level shelf.
    # Its old coplanar quay fragments are replaced by a retaining head.
    s.box("retaining quay head",QUAY,-2.48,0,concrete,"founded shore retaining head","quay")
    s.box("port work pier",PIER,-.34,0,timber,"continuous supported port pier deck","portpier")
    for z in (41,45.25,49,53,57,61.4):
        for x in (16.52,21.41):
            hit=ground.ray_cast(Vector((x,-z,4)),Vector((0,0,-1)))[0]
            assert hit is not None
            s.box(f"port pier pile at {x:g} {z:g}",(x-.17,z-.17,x+.17,z+.17),
                  hit.z-.55,-.35,timber,"pier pile bearing into measured bed","portpier")
        s.box(f"port pier transverse bearer at {z:g}",(16.31,z-.16,21.71,z+.16),-.56,-.34,timber,
              "pier beam over paired founded piles","portpier")

    # One continuous weather deck wraps the existing deckhouse. The bow
    # plate starts on the hull edge and has the original rectangular hatch.
    fore=[(32,-10),(39,-4),(42,6),(42,8),(22,8),(22,6),(25,-4)]
    a,b,c,d=FORE_WELL
    left=lambda z:25-(z+4)*.3
    right=lambda z:39+(z+4)*.3
    fore_pieces = [
        [(32,-10),(39,-4),(25,-4)],
        [(25,-4),(39,-4),(right(b),b),(left(b),b)],
        [(left(b),b),(a,b),(a,d),(left(d),d)],
        [(c,b),(right(b),b),(right(d),d),(c,d)],
        [(left(d),d),(right(d),d),(42,6),(42,8),(22,8),(22,6)],
    ]
    s.part("joined aft starboard and fore weather deck",[AFT,rectangle(STARBOARD),*fore_pieces],-.16,0,steel,"continuous weather deck with rectangular pump hatch","aft")
    vessel_stairs = (
        ("engine upper landing","route-13 landing0",(22.25,21.94,24,24.0),0,"engine"),
        ("engine lower landing","route-13 landing2",(22.25,33.70,24,35.52),-3,"engine"),
        ("crew upper landing","route-17 landing2",(22.25,33.43,24,36.0),3,"crew"),
        ("net deck stair landing","route-19 landing0",(22.25,12.47,24,14.20),3,"net"),
        ("bridge stair landing","route-19 landing2",(22.25,24.0,24,26.33),6,"bridge"),
    )
    for label,retired,dimensions,y,zone in vessel_stairs:
        s.retired.add(retired)
        landing=s.box(label,dimensions,y-.18,y,steel,"complete level ship stair receiver",zone)
        s.stair_additions.append(landing.name)
        a,b,c,d=dimensions
        s.box(label+" bulkhead bracket",(22.21,(b+d)/2-.16,24.14,(b+d)/2+.16),y-.50,y-.18,steel_dark,
              "steel receiver beam borne by deck structure",zone)
        s.rail(label+" port guard",(a+.07,b+.07),(a+.07,d-.07),y,steel_dark,zone)
    s.retired.update(("route-17 landing0","Bay landing WLD-15 route-17 1 region0",
                      "Bay landing WLD-15 route-17 2 region0","Bay landing WLD-15 route-17 4 region0"))
    s.retire_faces(("engine","W"),("net","W"),("crew","W"),("processing","W"),("bridge","W"))
    s.face("engine service bulkhead","x",24,(24,40),-3,3,paint,
           [("engine stair door",34.40,1.83,2.26,["port stair","engine"])],"engine")
    s.face("net store port bulkhead","x",24,(8,18),3,3,paint,
           [("net stair door",13.45,1.85,2.27,["net","port stair"])],"net")
    s.face("processing stair bulkhead","x",24,(8,30),0,3,paint,
           [("processing stair door",23.20,1.81,2.26,["processing","port stair"])],"processing")
    s.face("crew deck port bulkhead","x",24,(30,40),3,3,paint,
           [("crew stair door",34.78,1.81,2.27,["port stair","crew"])],"crew")
    s.face("bridge deck port bulkhead","x",24,(18,30),6,3,paint,
           [("bridge stair door",25.15,1.81,2.27,["port stair","bridge"])],"bridge")
    # The intermediate upper weather deck is the real processing deck head.
    # Its complete plate bears on the two long processing bulkheads.
    s.retired.add("Ceiling cap processing")
    s.box("processing deck head and upper weather deck",(24,18,40,30),2.82,3,steel,
          "continuous deck head and supported working deck","processing")
    s.box("main stern gangway",GANGWAY,-.24,0,steel,"fixed gangway from quay head to stern","quay")
    for x in (30.64,33.36):
        s.box(f"gangway girder at {x:g}",(x-.11,55.88,x+.11,64.02),-.68,-.24,steel_dark,
              "bearing gangway girder with deck and quay seats","quay")
    for z in (55.99,63.88):
        s.box(f"gangway bearing seat at {z:g}",(30.52,z-.13,33.48,z+.13),-.92,-.68,steel_dark,
              "structural seat at vessel or shore","quay")
    # The pier and the ship meet at the service platform through a break
    # in the guard. The original port stair mouths stay clear.
    s.box("port service deck sill beam",(21.78,44.64,22.32,47.56),-.54,-.16,steel_dark,
          "transfer beam at the supported port boarding point","portpier")
    for label,a,b in (
        ("quay east guard",(33.56,63.9),(41.95,63.9)),
        ("quay west guard",(22.06,63.9),(30.44,63.9)),
        ("pier outer guard",(16.1,40.1),(16.1,64)),
        ("pier head guard",(16.1,40.1),(21.91,40.1)),
        ("pier inner forward guard",(21.91,40.1),(21.91,44.52)),
        ("pier inner aft guard",(21.91,47.69),(21.91,63.9)),
        ("gangway port guard",(30.56,56),(30.56,63.9)),
        ("gangway starboard guard",(33.44,56),(33.44,63.9)),
        ("stern port guard",(25.09,55.91),(30.43,55.91)),
        ("stern starboard guard",(33.57,55.91),(38.91,55.91)),
        ("stern starboard return",(38.91,55.91),(41.89,50)),
        ("stern port return",(25.09,55.91),(22.11,50)),
        ("aft port guard",(22.11,50),(22.11,47.69)),
        ("forward port guard",(22.11,44.52),(22.11,40)),
        ("starboard gangway edge",(41.89,50),(41.89,6.1)),
        ("fore starboard return",(41.89,6.1),(38.9,-3.96)),
        ("fore starboard bow",(38.9,-3.96),(32,-9.81)),
        ("fore port bow",(32,-9.81),(25.1,-3.96)),
        ("fore port edge",(25.1,-3.96),(22.11,6.1)),
        ("fore port after edge",(22.11,6.1),(22.11,7.94)),
        ("fore port enclosure return",(22.11,7.94),(24,7.94))):
        s.rail(label,a,b,0,steel_dark,"weather edge")
    for x0,z0,x1,z1 in ((24.1,18,24.1,30),(39.9,18,39.9,30)):
        s.rail("upper deck edge "+str(x0),(x0,z0),(x1,z1),3,steel_dark,"processing roof")
    s.controls.extend([(32,0,82.372),(32,0,63.4),(31,0,60),(33,0,60),(32,0,55.8),
                       (37,0,50),(21,0,51),(19,0,44),(41,0,12),(41,0,32),(32,3,25)])
    s.areas.update(port_pier=PIER,gangway=GANGWAY,aft_deck=AFT,fore_deck=fore,fore_hatch=FORE_WELL,
                   starboard_deck=STARBOARD,upper_work_deck=(24,18,40,30))
    return terrain


def guidance(site, layout, s):
    links={row["id"]:row for row in layout["links"]}
    previous={}

    def set_route(number, points, width=None, y=0):
        link=links[f"route-{number:02}"]
        previous[link["id"]]=copy.deepcopy(link)
        link["points"]=[list(p) if len(p)==3 else [p[0],y,p[1]] for p in points]
        if width is not None:
            link["width"]=width
        link["note"]="Nonphysical guidance through the built openings, decks and terrain."

    if site==LIGHT:
        set_route(1,[(32,80.372),(32,72)])
        set_route(3,[(32,60),(30,57.4),(30,54),(30,52),(28,48)])
        set_route(11,[(45,39),(43.15,39),(43.15,43.8),(42,43.8),(40.8,43.8),(35,48)],y=3.2)
        set_route(13,[(22,48),(17.3,43.5),(16,43.5),(15.448,43.5),(15.448,35),(14,35)],
                  width=.90,y=3.2)
        annulus=[(round(32+4.01*math.cos(math.radians(a)),4),round(10+4.01*math.sin(math.radians(a)),4))
                 for a in (90,77.5,65,52.5,40,27.5,15,1.45)]
        set_route(17,[(32,22),(32,14.55),*annulus,(34.8,10)],width=.84)
        set_route(18,[(37,48),(38.5,52),(38.5,54),(38.5,56.1),(47,56.1),(54,53)])
        set_route(19,[(54,53),(54,46),(54,43.8),(53,40)])
    else:
        set_route(1,[(32,82.372),(32,78)])
        set_route(2,[(32,78),(32,70)])
        set_route(3,[(32,70),(32,64),(32,56),(32,47)])
        set_route(4,[(32,70),(19,70),(19,64),(19,46.75)])
        set_route(5,[(19,46.75),(22,46.75),(26,46.75)])
        set_route(6,[(31.5,45.5),(29.6,42),(29.6,40),(29.6,38.4),(28,35)])
        set_route(7,[(32.5,45.5),(34.4,42),(34.4,40),(34.4,38.4),(36,35)])
        set_route(11,[(32,3),(32,6.35),(41,6.35),(41,10),(41,24)])
        set_route(12,[(41,24),(41,40),(41,45),(38.6,47),(32,47)])
        set_route(13,[(25,0,24),(24.8,0,23.20),(23,0,23.20),(23,0,24),(23,-3,34),(23,-3,34.40),
                      (24,-3,34.40),(25,-3,34.40),(25,-3,34)],width=1.15)
        set_route(17,[(26,0,46.75),(23,0,46.75),(23,0,46),(23,3,36),(23,3,34.78),(24,3,34.78),(25.15,3,34.78),(26,3,36)],width=1.14)
        set_route(19,[(30,3,14),(25.40,3,13.45),(24,3,13.45),(23,3,13.45),(23,3,14),
                      (23,6,24),(23,6,25.15),(24,6,25.15),(25.16,6,25.15),(30,6,24)],width=1.12)
    layout["authored_doorways"]=s.portals
    layout["architectural_interfaces"]=dict(s.areas,nonphysical_guidance=True)
    return previous


def review(site, out, meta, layout, controls):
    glb, native = (out/(STEM+suffix) for suffix in (".glb",".hcs2"))
    old, binary = read(ROOT/meta["source_glb"])
    merged, payload = read(glb)
    assert payload[:len(binary)]==binary
    for key in ("meshes","materials","accessors","bufferViews","images","textures"):
        prior=old.get(key,[])
        assert merged.get(key,[])[:len(prior)]==prior,key
    for index,node in enumerate(old["nodes"]):
        expected=dict(node)
        if expected.get("name") in meta["removed_source_meshes"]:
            expected.pop("mesh",None)
        assert merged["nodes"][index]==expected,node.get("name")
    retained=[p for p in split_native(ROOT/meta["source_physics"])
              if p[0].split(":",1)[-1] not in meta["removed_source_meshes"]
              and (site==SHIP or not p[0].startswith("terrain-"))]
    assert split_native(native)[:len(retained)]==retained
    objects=import_scene(glb)
    native_parts=read_parts(native)
    if site==LIGHT:
        assert face_keys(records(objects[meta["terrain_mesh"]]))==face_keys(native_faces(WORLD/"terrain-physics.hcs2"))
    assert not any(obj.get("role","").startswith("rough route support") for obj in objects.values())
    vertices,faces=[],[]
    for name,obj in objects.items():
        start=len(vertices)
        vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
        if name not in meta["new_interface_parts"]:
            continue
        mesh=bmesh.new()
        mesh.from_mesh(obj.data)
        bmesh.ops.remove_doubles(mesh,verts=list(mesh.verts),dist=.00001)
        assert all(edge.is_manifold for edge in mesh.edges) and mesh.calc_volume(signed=True)>0,name
        mesh.free()
        assert face_keys(records(obj))==face_keys(native_parts[name][1]),name
    tree=BVHTree.FromPolygons(vertices,faces)

    def trace(a,b):
        a,b=Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    def standing(x,y,z):
        hit=trace((x,-z,y+.075),(x,-z,y-.23))
        assert hit is not None and abs(y-hit.z)<.047,(site,x,y,z,hit)
        assert trace((x,-z,y+.078),(x,-z,y+1.93)) is None,(site,x,y,z,"standing headroom")

    aperture,sills=0,0
    for door in meta["new_portals"]:
        base,center,fixed=door["sill_y"],door["along"],door["boundary"]
        for factor in (-.33,0,.33):
            u=center+factor*door["clear_width"]
            for given in (.32,1.20,door["clear_height"]-.10):
                h=base+given
                line=((u,-fixed-.42,h),(u,-fixed+.42,h)) if door["axis"]=="z" else (
                     (fixed-.42,-u,h),(fixed+.42,-u,h))
                assert trace(*line) is None,(door["id"],line)
                aperture+=1
            for delta in (-.18,.18):
                x,z=(u,fixed+delta) if door["axis"]=="z" else (fixed+delta,u)
                standing(x,base,z)
                sills+=1
    for x,y,z in controls:
        standing(x,y,z)

    triangles=place_review_van(layout["staging"])
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.render.resolution_x,scene.render.resolution_y=1260,900
    bpy.ops.object.camera_add()
    cam=bpy.context.object
    scene.camera=cam
    CAPTURES.mkdir(parents=True,exist_ok=True)
    views = (
        [("overview",(60,-95,103),(32,-43,2),113),
         ("keeper-arrival",(24,-69,1.65),(30,-54,1.5),0),
         ("oil-house-yard",(53,-54,1.62),(54,-45,1.58),0),
         ("main-stair-landing",(45.1,-37,4.82),(43.3,-43,4.3),0),
         ("tower-sill",(32,-18,1.61),(32,-13.3,1.5),0)]
        if site==LIGHT else
        [("overview",(62,-114,95),(32,-44,-1),123),
         ("stern-boarding",(32,-65.8,1.63),(32,-55,1.45),0),
         ("port-service-pier",(19,-52,1.62),(22,-46,1.44),0),
         ("starboard-foredeck",(41,-17,1.62),(39,-1,1.40),0),
         ("gangway-bearing-section",(48,-59,12),(32,-59,-.1),22)])
    captures=[]
    for label,location,target,scale in views:
        hide=[]
        if label=="gangway-bearing-section":
            hide=[name for name,obj in objects.items() if "water" in obj.get("role","").lower()]
        for name in hide:
            objects[name].hide_render=True
        cam.data.type="ORTHO" if scale else "PERSP"
        cam.data.ortho_scale=scale or 1
        cam.data.lens=24
        cam.location=location
        cam.rotation_euler=(Vector(target)-cam.location).to_track_quat("-Z","Y").to_euler()
        png=CAPTURES/(site.lower()+"-"+label+".png")
        scene.render.filepath=str(png)
        bpy.ops.render.render(write_still=True)
        for name in hide:
            objects[name].hide_render=False
        captures.append(dict(path=rel(png),sha256=sha(png),hidden=hide,source_glb_sha256=sha(glb)))
    write(out/"export-verification.json",dict(glb_sha256=sha(glb),native_sha256=sha(native),
          added_closed_parts=len(meta["new_interface_parts"]),retained_native_records=len(retained),
          aperture_rays=aperture,sill_controls=sills,other_standing=len(controls),canonical_van_triangles=triangles,
          canonical_van_source=sha(VAN/"geometry.json"),captures=captures))
    print("COAST EXPORT REVIEW",site,aperture,sills,len(controls),flush=True)


def build(source):
    site=source["id"]
    out=PASS/site.lower()
    out.mkdir(parents=True,exist_ok=True)
    assert sha(ROOT/source["glb"])==source["glb_sha256"]
    assert sha(ROOT/source["native"])==source["native_sha256"]
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    objects=import_scene(ROOT/source["glb"])
    structure=Structure("Lightstation" if site==LIGHT else "Saint Elara",objects)
    terrain=(lighthouse if site==LIGHT else vessel)(structure)
    prior_guides=guidance(site,layout,structure)
    bpy.data.objects.remove(structure.template,do_unlink=True)
    ground=None
    if site==LIGHT:
        old_terrain=[part for part in split_native(ROOT/source["native"]) if part[0].startswith("terrain-")]
        assert old_terrain==split_native(OLD_WORLD/"terrain-physics.hcs2")
        ground=terrain_surface(site,terrain,WORLD,PROGRAM)
        ground["role"]="saved editor terrain / keeper grounds and oil-house court"
        aggregate=ground.data.materials[-1]
        aggregate.name="Lightstation gravel forecourt"
        aggregate.diffuse_color=(.42,.39,.31,1)
        files={rel(p):sha(p) for p in WORLD.rglob("*") if p.is_file() and not p.name.endswith(".lock")}
        layout["authoritative_saved_terrain"]=dict(world=rel(WORLD),files_sha256=files)
        meta.update(saved_world=rel(WORLD),saved_world_files_sha256=files,terrain_mesh=ground.name)
    for name in structure.retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    layout_path=out/"authored-layout.json"
    write(layout_path,layout)
    glb,native,blend=(out/(STEM+suffix) for suffix in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in structure.built+([ground] if ground else []):
        obj.select_set(True)
    addition=ROOT/"build"/(site.lower()+"-coast-additions.glb")
    bpy.ops.export_scene.gltf(filepath=str(addition),export_format="GLB",use_selection=True,export_extras=True)
    append(ROOT/source["glb"],addition,glb,remove_mesh_names=sorted(structure.retired))
    if ground:
        replace_terrain_on_export(ROOT/source["native"],native,structure.retired,structure.built,WORLD,False)
    else:
        native_write(ROOT/source["native"],native,structure.retired,structure.built)
    prior_components=meta["stair_components"]
    kept_components=[n for n in prior_components if n.split(":",1)[-1] not in structure.retired]
    segments=copy.deepcopy(meta.get("source_segment_map",{}))
    new_segments=copy.deepcopy(segments)
    if site==SHIP:
        for key,new_index in (("route-13",3),("route-17",2),("route-19",4)):
            new_segments[key]={new_index:segments.get(key,{}).get("1",1)}
    meta.update(status="VERIFIED_COAST_ENTRANCE_ARCHITECTURE_RUNTIME_OPEN",
                source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
                source_physics=source["native"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                layout=rel(layout_path),layout_sha256=sha(layout_path),before_stair_components=prior_components,
                stair_components=kept_components+structure.stair_additions,
                before_segment_map=segments,source_segment_map=new_segments,
                retired_study_strips=sorted(structure.strips),removed_source_meshes=sorted(structure.retired),
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                new_interface_parts=[obj.name for obj in structure.built],new_portals=structure.portals,
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(out/"construction.json",meta)
    write(out/"quarantine.json",dict(source=source,retired=sorted(structure.retired),superseded_guides=prior_guides))
    if "--build-only" not in sys.argv:
        review(site,out,meta,layout,structure.controls)
    return dict(source,construction=rel(out/"construction.json"),layout=rel(layout_path),glb=rel(glb),
                native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))


if __name__=="__main__":
    roster=json.loads(BASELINE.read_bytes())
    roster["active_sources"]=[build(row) if row["id"] in (LIGHT,SHIP) else row for row in roster["active_sources"]]
    roster["baseline"]=rel(BASELINE)
    for row in roster["sites"]:
        if row["id"] in (LIGHT,SHIP):
            row["decision"]="VERIFIED_COAST_ENTRANCE_ARCHITECTURE_RUNTIME_OPEN"
    write(PASS/"roster.json",roster)
