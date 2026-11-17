"""Build the Mine headhouse and two orthogonal stair chambers, then derive circulation."""
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
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_cemetery_grounds import rel, replace_terrain_on_export, terrain_surface
from build_college_grounds import import_scene, place_review_van
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from planar_structure import plate
from prepare_mine_yard import BASE, BASELINE, HEADHOUSE, PASS, PROGRAM, VAN, WORLD
from review_roster_macroterrain import records, sha

SITE="WLD-05"
OUT=PASS/"wld-05"
STEM="mine-yard"
CAPTURES=BASE/"captures/mine-yard-batch"
MAIN_X=28.35
MAIN_RUNS=((32.4,39.8,-6,-3),(41.4,48.8,-3,0))
VENT_Z=52
VENT_RUNS=((11.85,18.15,-6,-3),(19.85,26.25,-3,0))
WIDTH=2.0
TREADS=17


def combined_plate(name,cells,top,depth,mat):
    result=plate(name,[[(a,b),(c,b),(c,d),(a,d)] for a,b,c,d in cells if c>a and d>b],
                 top,depth,mat)
    assert len(result)==1,name
    return result[0]


def punched_plate(outer,hole):
    a,b,c,d=outer
    e,f,g,h=hole
    assert a<=e<g<=c and b<=f<h<=d
    return [(a,b,c,f),(a,f,e,h),(g,f,c,h),(a,h,c,d)]


def revise_geometry(old,layout):
    initial_ground=next(o for o in old.values() if o.get("role","").startswith("saved editor terrain"))
    retained_exterior={name for name in old if name.startswith("Retaining shoulder WLD-05 L08")}
    retired={initial_ground.name,*retained_exterior}
    retired.update(name for name in old if name.startswith(("Rooted cut walkway WLD-05 ",
                     "East stair hub bearing ledge ","Bay landing WLD-05 L09 ")))
    retired.update(o.name for o in old.values() if o.get("role","").startswith("rough route support")
                   or o.get("role")=="structural-arrival-end-bearing"
                   or o.name.startswith(("Bayed stair WLD-05 L09 ","Bayed stair WLD-05 L17 ",
                                         "Authored stair end WLD-05 L09 ","Authored stair end WLD-05 L17 ")))
    retired.update(o.name for o in old.values() if (o.get("zone"),o.get("role")) in
                   {("shack","floor"),("vent","floor"),("haul","floor"),
                    ("vent","ceiling-cap"),("haul","ceiling-cap")})
    assert len([o for o in old.values() if o.get("role","").startswith("rough route support")])==11
    concrete=material("Mine board formed retaining concrete",(.42,.41,.36))
    floor=material("Mine worn concrete floor",(.40,.41,.39))
    timber=material("Mine headhouse general timber boarding",(.32,.25,.18))
    roofing=material("Mine shaft rolled steel roof",(.25,.28,.27))
    lining=material("Mine tunnel dark lining",(.35,.35,.31))
    template=frame_template()
    components,steps,doors=[],[],[]

    def part(name,cells,bottom,top,material,role,zone):
        obj=combined_plate("Mine "+name,cells,top,top-bottom,material)
        obj["role"],obj["zone"]=role,zone
        components.append(obj)
        return obj

    def solid(name,box,bottom,top,material,role,zone):
        return part(name,[box],bottom,top,material,role,zone)

    def retire_face(room,face):
        names=[n for n in old if f"Wall {room} {face}" in n or f"Header {room} {face}" in n]
        assert names,(room,face)
        retired.update(names)

    def wall(name,axis,fixed,lo,hi,y,height,opening,mat,rooms):
        def face(label,a,b,low,high):
            box=(a,fixed-.12,b,fixed+.12) if axis=="z" else (fixed-.12,a,fixed+.12,b)
            solid(name+" "+label,box,low,high,mat,"whole wall bearing and lintel",rooms[0])
        if not opening:
            face("solid",lo,hi,y,y+height)
            return
        centre,width,clear=opening
        left,right=centre-.62*width,centre+.62*width
        assert lo<left<right<hi,(name,opening)
        face("west or north pier",lo,left,y,y+height)
        face("east or south pier",right,hi,y,y+height)
        face("head lintel",left,right,y+clear*2.275/2.125,y+height)
        frame=template.copy()
        frame.data=template.data.copy()
        bpy.context.collection.objects.link(frame)
        frame.name="Mine "+name+" fitted frame"
        for vertex in frame.data.vertices:
            x,z,h=vertex.co
            u,n,v=x*width,z*.24/.18,y+h*clear/2.125
            vertex.co=(centre-u,-fixed-n,v) if axis=="z" else (fixed+n,-centre-u,v)
        frame["role"],frame["source_asset"]="indexed doorway in a complete building face","level_blockout_doorway"
        components.append(frame)
        doors.append(dict(id=name,axis=axis,boundary=fixed,along=centre,sill_y=y,
                          clear_width=width,clear_height=clear,rooms=rooms))

    # Full building bases. The surface crew building completely spans the
    # old shaft scar; actual flight remains under it until the east wall.
    solid("crew house full floor",(2,46,20,62),-.24,0,floor,"continuous crew slab","shack")
    solid("haul level continuous floor",(20,20,36,42),-6.24,-6,floor,"continuous underground slab","haul")
    solid("ventilation full floor",(2,42,20,62),-6.24,-6,floor,"continuous underground slab","vent")
    part("haul cover with rectangular shaft mouth",punched_plate((20,20,36,42),(27.25,32.4,29.45,42)),
         -3.26,-3,lining,"vault cap around actual haul stair cell","haul")
    part("vent cover with rectangular shaft mouth",punched_plate((2,42,20,62),(11.85,50.9,20,53.1)),
         -3.26,-3,lining,"vault cap around actual vent stair cell","vent")

    for room,side in (("shack","E"),("shack","N"),("stores","S"),("stores","E"),("assay","W"),("wash","W")):
        retire_face(room,side)
    wall("crew house yard door","x",20,46,62,0,3,(57,1.8,2.24),timber,["shack","adit-court"])
    wall("crew house and stores shared face","z",46,2,20,0,3,(8,1.55,2.16),timber,["shack","stores"])
    wall("stores working door","x",14,30,46,0,3,(42,1.8,2.22),timber,["stores","adit-court"])
    wall("assay yard door","x",40,46,62,0,3,(57,1.8,2.24),timber,["assay","adit-court"])
    wall("wash house yard door","x",40,30,46,0,3,(43,1.8,2.22),timber,["wash","adit-court"])

    x0,z0,x1,z1=HEADHOUSE
    solid("headhouse founded deck",HEADHOUSE,-.34,0,concrete,"whole headhouse floor","adit")
    # Deep end-bearing structures continue under the edges but stay outside
    # both shaft openings. The side walls meet the buried chamber walls.
    solid("headhouse east bearing",(31,49,31.25,54.25),-4.4,-.34,concrete,"east footing","adit")
    solid("headhouse south deep bearing",(26.5,54,31.25,54.25),-4.4,-.34,concrete,"south footing","adit")
    wall("headhouse public mouth","z",z1,x0,x1,0,3,(29,1.8,2.24),timber,["adit","adit-court"])
    wall("headhouse haul stair door","z",z0,x0,x1,0,3,(MAIN_X,1.8,2.24),timber,["adit","haul-stair"])
    wall("headhouse ventilation door","x",x0,z0,z1,0,3,(VENT_Z,1.8,2.24),timber,["adit","vent-stair"])
    wall("headhouse east elevation","x",x1,z0,z1,0,3,None,timber,["adit"])
    solid("headhouse roof",(26.38,48.88,31.38,54.40),3,3.26,roofing,"enclosed weather roof","adit")

    def stair_axis(axis,centre,spans,low,top,label,zone):
        offset=WIDTH/2
        def tread_box(a,b):
            return (centre-offset,a,centre+offset,b) if axis=="z" else (a,centre-offset,b,centre+offset)
        for i,(start,end,bottom,upper) in enumerate(spans):
            for step in range(TREADS):
                a=start+(end-start)*step/TREADS
                b=start+(end-start)*(step+1)/TREADS
                y=bottom+(upper-bottom)*(step+1)/TREADS
                t=solid(f"{label} flight {i+1} riser {step+1:02}",tread_box(a,b),-6.22,y,
                        concrete,"closed founded stair tread",zone)
                steps.append(t.name)
        mid=solid(label+" supported middle landing",tread_box(spans[0][1],spans[1][0]),-6.22,-3,
                  concrete,"full intermediate stair landing",zone)
        final=solid(label+" high landing at headhouse",tread_box(spans[1][1],top),-.34,0,
                    concrete,"level high threshold landing",zone)
        steps.extend([mid.name,final.name])
        # The low approach is on the whole lower floor. No strip is added.
        assert low < spans[0][0]
    stair_axis("z",MAIN_X,MAIN_RUNS,31,49,"haul stair","haul-stair")
    stair_axis("x",VENT_Z,VENT_RUNS,11,26.5,"vent stair","vent-stair")

    # The larger old slot is now contained in founded masonry shoulders. Only
    # the measured stair chamber lies between them. An end wall closes the
    # roof above the original underground hall instead of a terrain cut.
    for side,a,b in (("west",26.62,27.35),("east",29.35,31.38)):
        solid(f"haul {side} retaining wall",(a,32.4,b,49),-6.24,3.10,lining,
              "founded shaft wall beyond the lower crossing","haul-stair")
        solid(f"haul {side} return over the crossing",(a,30.70,b,32.4),-3,3.10,lining,
              "roof-bearing return above the retained haul aisle","haul-stair")
    solid("haul north upper closure",(27.35,30.70,29.35,30.91),-3,3.10,lining,"shaft end wall above haul ceiling","haul-stair")
    solid("haul stair weather roof",(26.62,30.70,31.38,49),3.10,3.36,roofing,"closed shaft roof","haul-stair")
    for name,a,b in (("north",50.72,51),("south",53,53.30)):
        solid(f"vent {name} wall below crew house",(11.85,a,20,b),-6.24,-.24,lining,"stair shaft lining below crew slab","vent-stair")
        solid(f"vent {name} outdoor shaft wall",(20,a,26.5,b),-6.24,3.10,lining,"buried and outdoor shaft lining","vent-stair")
    solid("vent outside weather roof",(20,50.72,26.5,53.30),3.10,3.36,roofing,"closed shaft roof","vent-stair")
    solid("vent front raised roof face",(19.91,51,20.12,53),0,3.10,lining,"weather roof return above crew floor","vent-stair")
    bpy.data.objects.remove(template,do_unlink=True)

    links={l["id"]:l for l in layout["links"]}
    old_guidance={}
    def guide(id,points,width):
        old_guidance[id]=copy.deepcopy(links[id])
        links[id]["points"]=[list(p) for p in points]
        links[id]["width"]=width
        links[id]["note"]="Measured guide through built sills, rooms and stair landings."
    def along(id,points,width):
        guide(id,[(x,0,z) for x,z in points],width)

    along("L01",[(28,78.372),(28,76),(28,71),(30,69)],1.55)
    along("L02",[(30,69),(25,64),(23,60),(23,57),(20,57),(16,57),(11,54)],1.55)
    along("L03",[(30,69),(36,64),(37,60),(37,57),(40,57),(43,57),(47,54)],1.55)
    along("L04",[(11,54),(8,51),(8,46),(8,38)],1.40)
    along("L05",[(8,38),(8,46),(8,51),(13,55),(16,57),(20,57),(23,57),
                 (29,57),(29,54.25),(29,52),(30,52)],1.40)
    along("L06",[(30,52),(29,52),(29,54.25),(29,58),(30,63),(30,69)],1.55)
    along("L08",[(47,38),(45,43),(40,43),(36,43),(34,46),(34,55.8),
                 (29,56.5),(29,54.25),(29,52),(30,52)],1.45)
    guide("L09",[(30,0,52),(MAIN_X,0,52),(MAIN_X,0,49),(MAIN_X,0,48.8),
                   (MAIN_X,-3,41.4),(MAIN_X,-3,39.8),(MAIN_X,-6,32.4),
                   (MAIN_X,-6,31),(28,-6,31)],1.55)
    guide("L17",[(11,-6,VENT_Z),(11.85,-6,VENT_Z),(18.15,-3,VENT_Z),
                   (19.85,-3,VENT_Z),(26.25,0,VENT_Z),(26.5,0,VENT_Z),(30,0,VENT_Z)],1.55)
    old_maps={"L09":{3:0,5:19},"L17":{1:0,3:17}}
    added=[]
    for id,segment,new_id in (("L09",5,19),("L17",3,17)):
        added.append(dict(link=id,segment=new_id,layout_segment=segment,points=links[id]["points"][segment:segment+2]))
    adit=next(z for z in layout["zones"] if z["id"]=="adit")
    adit.update(name="Mine headhouse",kind="inside",bounds=list(HEADHOUSE),purpose="Controlled access to the haul and ventilation stair chambers")
    layout["zones"].extend([
        dict(id="adit-court",name="Mine work yard",bounds=[14,41,40,63],y=0,height=3,kind="outside",
             purpose="Shared sorting and building approaches"),
        dict(id="haul-stair",name="Main haul stair",bounds=[27.35,32.4,29.35,49],y=-6,height=9,kind="circulation",
             purpose="Two flights between the headhouse and main haul level"),
        dict(id="vent-stair",name="Ventilation return stair",bounds=[11.85,51,26.5,53],y=-6,height=9,kind="circulation",
             purpose="Separate two-flight return to the headhouse"),
    ])
    layout["authored_doorways"]=doors
    layout["staging"]["note"]="Canonical van at original yaw and translation, rear ramp meets the saved roadhead."
    layout["mine_headhouse"]=dict(footprint=list(HEADHOUSE),main_runs=MAIN_RUNS,vent_runs=VENT_RUNS,
                                   tread_count_per_flight=TREADS,shaft_clear_width=WIDTH,
                                   yard_tracks_are_nonphysical_diagram_guidance=True)
    return initial_ground,retired,components,steps,old_guidance,old_maps,added


def build():
    roster=json.loads(BASELINE.read_bytes())
    source=next(r for r in roster["active_sources"] if r["id"]==SITE)
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    OUT.mkdir(parents=True,exist_ok=True)
    objects=import_scene(ROOT/source["glb"])
    original,retired,built,new_steps,old_guides,mapping,added=revise_geometry(objects,layout)
    ground=terrain_surface(SITE,original,WORLD,PROGRAM)
    ground["role"]="saved editor terrain / mine geographic workyard"
    ground.data.materials[-1].name="Compacted mine road ballast"
    for name in retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    source_world={rel(p):sha(p) for p in WORLD.rglob("*") if p.is_file() and not p.name.endswith(".lock")}
    layout["authoritative_saved_terrain"]=dict(world=rel(WORLD),files_sha256=source_world)
    layout_file=OUT/"authored-layout.json"
    write(layout_file,layout)
    glb,native,blend=(OUT/(STEM+ext) for ext in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [ground,*built]:
        obj.select_set(True)
    temp=ROOT/"build/mine-yard-additions.glb"
    bpy.ops.export_scene.gltf(filepath=str(temp),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],temp,glb,remove_mesh_names=sorted(retired))
    replace_terrain_on_export(ROOT/source["native"],native,retired,built,WORLD,False)
    prior_components=copy.deepcopy(meta["stair_components"])
    originals=[n for n in prior_components if n.split(":",1)[-1] not in retired]
    assert any("L15" in n for n in originals)
    old_map=copy.deepcopy(meta.get("source_segment_map",{}))
    revised_map=copy.deepcopy(old_map)
    revised_map.update(mapping)
    meta.update(status="MINE_GEOGRAPHIC_YARD_AND_ENCLOSED_STAIR_CELLS_RUNTIME_OPEN",source_glb=source["glb"],
                source_glb_sha256=source["glb_sha256"],source_physics=source["native"],
                source_physics_sha256=source["native_sha256"],before_layout=source["layout"],
                before_layout_sha256=sha(ROOT/source["layout"]),layout=rel(layout_file),
                layout_sha256=sha(layout_file),before_stair_components=prior_components,stair_components=originals+new_steps,
                before_segment_map=old_map,source_segment_map=revised_map,interface_added_flights=added,
                saved_world=rel(WORLD),saved_world_files_sha256=source_world,
                terrain_mesh=ground.name,removed_source_meshes=sorted(retired),new_portals=layout["authored_doorways"],
                new_interface_parts=[o.name for o in built],builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(OUT/"construction.json",meta)
    write(OUT/"quarantine.json",dict(source=source,retired=sorted(retired),superseded_guides=old_guides))
    row=dict(source,construction=rel(OUT/"construction.json"),layout=rel(layout_file),glb=rel(glb),
             native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))
    roster["active_sources"]=[row if old["id"]==SITE else old for old in roster["active_sources"]]
    roster["baseline"]=rel(BASELINE)
    next(r for r in roster["sites"] if r["id"]==SITE)["decision"]=meta["status"]
    write(PASS/"roster.json",roster)



def review():
    layout=json.loads((OUT/"authored-layout.json").read_bytes())
    meta=json.loads((OUT/"construction.json").read_bytes())
    glb,native=(OUT/(STEM+x) for x in (".glb",".hcs2"))
    old,old_bin=read(ROOT/meta["source_glb"])
    new,new_bin=read(glb)
    assert new_bin[:len(old_bin)]==old_bin
    for key in ("meshes","materials","accessors","bufferViews","textures","images"):
        records_before=old.get(key,[])
        assert new.get(key,[])[:len(records_before)]==records_before,key
    removed=set(meta["removed_source_meshes"])
    for index,node in enumerate(old["nodes"]):
        expected=dict(node)
        if node.get("name") in removed:
            expected.pop("mesh",None)
        assert new["nodes"][index]==expected,node.get("name")
    original_parts=[p for p in split_native(ROOT/meta["source_physics"]) if not p[0].startswith("terrain-")
                    and p[0].split(":",1)[-1] not in removed]
    assert split_native(native)[:len(original_parts)]==original_parts
    objects=import_scene(glb)
    ground=objects[meta["terrain_mesh"]]
    assert face_keys(records(ground))==face_keys(native_faces(WORLD/"terrain-physics.hcs2"))
    native_parts=read_parts(native)
    for name in meta["new_interface_parts"]:
        obj=objects[name]
        assert face_keys(records(obj))==face_keys(native_parts[name][1]),name
        mesh=bmesh.new()
        mesh.from_mesh(obj.data)
        bmesh.ops.remove_doubles(mesh,verts=list(mesh.verts),dist=.00001)
        assert all(edge.is_manifold for edge in mesh.edges) and mesh.calc_volume(signed=True)>0,name
        mesh.free()
    vertices,faces=[],[]
    for obj in objects.values():
        start=len(vertices)
        vertices.extend(obj.matrix_world@vertex.co for vertex in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
    tree=BVHTree.FromPolygons(vertices,faces)

    def trace(a,b):
        a,b=Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    aperture,sill=0,0
    for door in meta["new_portals"]:
        fixed,centre=door["boundary"],door["along"]
        for multiplier in (-.34,0,.34):
            offset=centre+door["clear_width"]*multiplier
            for h in (.28,1.2,door["clear_height"]-.10):
                a,b=((offset,-fixed-.44,h),(offset,-fixed+.44,h)) if door["axis"]=="z" else (
                     (fixed-.44,-offset,h),(fixed+.44,-offset,h))
                assert trace(a,b) is None,(door["id"],a,b)
                aperture+=1
            for d in (-.20,-.06,.06,.20):
                x,z=(offset,fixed+d) if door["axis"]=="z" else (fixed+d,offset)
                hit=trace((x,-z,.10),(x,-z,-.22))
                assert hit is not None and abs(hit.z)<.041,(door["id"],x,z,hit)
                sill+=1
    tread,head=0,0
    for spans,n,c in ((MAIN_RUNS,"z",MAIN_X),(VENT_RUNS,"x",VENT_Z)):
        for start,end,base,top in spans:
            for step in range(TREADS):
                t=(step+.5)/TREADS
                v=start+(end-start)*t
                y=base+(top-base)*(step+1)/TREADS
                x,z=(c,v) if n=="z" else (v,c)
                hit=trace((x,-z,y+.045),(x,-z,y-.05))
                assert hit is not None and abs(hit.z-y)<.0001,(x,y,z,hit)
                tread+=1
                assert trace((x,-z,y+.07),(x,-z,y+1.9)) is None,(x,y,z,"headroom obstructed")
                head+=1

    roadhead=0
    for x in (27.2,28,28.8):
        for z in (78.2,78.37,80.0,82.0,84.0):
            p=trace((x,-z,.15),(x,-z,-.20))
            assert p is not None and abs(p.z)<.013,(x,z,p)
            roadhead+=1
    samples=[(32.1,-47.5,1.62),(22.6,-57,1.62),(29.6,-53,1.62),
             (MAIN_X,-31.8,-4.38),(11.2,-VENT_Z,-4.38)]
    for x,z,y in samples:
        support=trace((x,z,y-1.45),(x,z,y-1.82))
        assert support is not None and abs(support.z-(y-1.62))<.047,(x,z,y,support)
    van_count=place_review_van(layout["staging"])
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.render.resolution_x,scene.render.resolution_y=1280,920
    bpy.ops.object.camera_add()
    cam=bpy.context.object
    scene.camera=cam
    CAPTURES.mkdir(parents=True,exist_ok=True)
    views=[
        ("overview",(46,-108,105),(30,-44,-1),113),
        ("canonical-arrival",(22,-73,1.62),(28,-82,1.35),0),
        ("yard-and-headhouse",(32.1,-60.5,1.62),(28.5,-48,1.5),0),
        ("headhouse-south",(29,-57.25,1.62),(29,-51.0,1.48),0),
        ("haul-stair",(MAIN_X,-31.8,-4.38),(MAIN_X,-38.9,-2.55),0),
        ("ventilation-stair",(11.2,-VENT_Z,-4.38),(16,-VENT_Z,-2.75),0),
        ("shaft-section",(50,-56,31),(28,-40,-1),31),
    ]
    captures=[]
    for label,pos,goal,scale in views:
        hide=[]
        if label=="shaft-section":
            hide=[n for n in objects if n==meta["terrain_mesh"] or n.startswith(
                 ("Mine haul stair weather roof","Mine haul east retaining wall","Mine haul east return",
                  "Mine headhouse east elevation","Mine headhouse roof","Mine haul cover with",
                  "Ceiling cap ore-east","Wall haul S","Flat route shell Wall haul E","Header haul E"))]
        for name in hide:
            objects[name].hide_render=True
        cam.data.type="ORTHO" if scale else "PERSP"
        cam.data.ortho_scale=scale or 1
        cam.data.lens=26
        cam.location=pos
        cam.rotation_euler=(Vector(goal)-cam.location).to_track_quat("-Z","Y").to_euler()
        png=CAPTURES/(label+".png")
        scene.render.filepath=str(png)
        bpy.ops.render.render(write_still=True)
        for name in hide:
            objects[name].hide_render=False
        captures.append(dict(path=rel(png),sha256=sha(png),source_glb_sha256=sha(glb),hidden=hide))
    write(OUT/"export-verification.json",dict(glb_sha256=sha(glb),native_sha256=sha(native),
          retained_native_records=len(original_parts),added_closed_parts=len(meta["new_interface_parts"]),
          terrain_matches_editor=True,door_clearance_controls=aperture,door_sill_controls=sill,
          tread_controls=tread,standing_headroom_controls=head,roadhead_controls=roadhead,
          van_triangle_count=van_count,van_source_sha256=sha(VAN/"geometry.json"),captures=captures))
    print("MINE EXPORT REVIEW",aperture,sill,tread,head,roadhead,flush=True)


if __name__=="__main__":
    if "--review-only" not in sys.argv:
        build()
    if "--build-only" not in sys.argv:
        review()
