"""Join Blackwater's frozen site model to its valley, shore and closed roadhead."""
import argparse
from collections import Counter
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Vector
from mathutils.geometry import delaunay_2d_cdt

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_circulation_construction import native_write
from build_college_containment import (MeshParts, distance_to_edge_squared, inside, loop_area,
    post_foot, station, strip, surveyed_terrain_loops)
from build_college_electrical import point
from build_college_grounds import import_scene
from build_deliberate_entrances import write
from build_dam_east_hall import rel, render, sha
from build_northwatch_boundary import closed_ring
from build_prison_sally_gate import DEPTH, GATE, export, moving_panel
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys
from review_roster_macroterrain import records
from verify_college_services import bvh

BASE=ROOT/"development/levels/rebuild"
PRIOR=BASE/"dam-tailrace-pass/roster.json"
STAGE=BASE/"dam-valley-pass"
OUT=STAGE/"wld-09"
GLB,NATIVE,BLEND=(OUT/("bounded-dam"+ext) for ext in (".glb",".hcs2",".blend"))
STATUS="DAM_JOINED_VALLEY_AND_FOUNDED_PHYSICAL_PERIMETER_STAGED"

GATE_Z=89.6
GATE_EDGE=((40,GATE_Z),(32,GATE_Z))
# The compound pinches in at the roadhead. The main retaining runs sit behind
# the powerhouse, above the headwater shore, and beyond the lower bank.
BOUNDARY=(GATE_EDGE[0],GATE_EDGE[1],(27,88.9),(23,85),(18,78),(12,72),(5,63),
    (1.2,54),(-.9,44),(-1.9,31),(-1.8,19),(1,13),(7,8),(16,2),(26,-2),
    (36,-3),(48,-4),(60,-3),(69,1),(76,6),(82,13),(87,22),(90,33),(90,47),
    (87,56),(82,64),(74,73),(64,80),(53,86),(46,89))
ROAD=((41,96,.01,3.0),(43,104,.06,3.6),(46,115,-.20,3.65),
      (50,125,-.43,3.6),(55,134,-.77,3.6),(58.5,142,-1.15,3.5))
VALLEY=((-46,-23),(-23,-48),(15,-57),(57,-62),(96,-51),(128,-28),(144,11),
    (141,58),(130,91),(110,123),(76,144),(40,153),(13,146),(-16,132),
    (-39,105),(-53,63),(-56,17))
VIEWS=(
    ("blackwater-valley",(147,-123,117),(41,-48,-2),176),
    ("east-cut-and-tailrace",(108,-58,24),(74,-34,-4),70),
    ("south-roadhead",(48,-111,18),(36,-83,1),47),
    ("arrival-standing",(39.8,-80.3,1.62),(36,-89.6,1.25),0),
)


def authority():
    roster=json.loads(PRIOR.read_bytes())
    row=next(r for r in roster["active_sources"] if r["id"]=="WLD-09")
    for key in ("glb","native"):
        assert sha(ROOT/row[key])==row[key+"_sha256"],key
    return roster,row


def valley_height(rim,x,z):
    q=min(rim,key=lambda p:(x-p[0])**2+(z-p[1])**2)
    blend=min(1,math.hypot(x-q[0],z-q[1])/25)
    west=15.5*math.exp(-((x+32)/26)**2-((z-44)/63)**2)
    east=17.5*math.exp(-((x-116)/26)**2-((z-47)/47)**2)
    south=10.0*math.exp(-((x-93)/42)**2-((z-110)/33)**2)
    north=8.0*math.exp(-((x-16)/62)**2-((z+55)/16)**2)
    far=2.0*math.sin(.045*x+.061*z)
    outlet=-6.5*math.exp(-((x-(74-.19*(z-12)))/9)**2-((z-6)/28)**2)
    target=-4.1+west+east+south+north+far+outlet
    return q[2]+(target-q[2])*blend


def valley(original):
    rim,*cuts=surveyed_terrain_loops(original)
    assert len(rim)==1159 and list(map(len,cuts))==[185]
    outer=[(x,z,valley_height(rim,x,z)) for x,z in VALLEY]
    points=list(rim)+outer
    final=len(points)
    boundary_edges=[(i,(i+1)%len(rim)) for i in range(len(rim))]
    boundary_edges.extend((i,len(rim)+(i+1-len(rim))%len(outer)) for i in range(len(rim),final))

    # The service road is a material and grade corridor in the same terrain.
    # It meets existing rim vertices and bends beyond the locked roadhead.
    road=[]
    shoulders=[]
    for side in (-1,1):
        start_x=ROAD[0][0]+side*ROAD[0][3]
        start=next(i for i,(x,z,_) in enumerate(rim) if (x,z)==(start_x,96))
        shoulder_x=start_x+2*side
        shoulder=next(i for i,(x,z,_) in enumerate(rim) if (x,z)==(shoulder_x,96))
        indices=[start]
        side_indices=[shoulder]
        for i,(x,z,y,half) in enumerate(ROAD[1:],1):
            indices.append(len(points))
            points.append((x+side*half,z,y-.085*side))
            width=half+min(5.6,2.8+i*.55)
            at=x+side*width
            side_indices.append(len(points))
            natural=valley_height(rim,at,z)
            shoulder_y=y+max(-1.15,min(1.15,natural-y))
            points.append((at,z,shoulder_y))
        road.append(indices)
        shoulders.append(side_indices)
    road_edges=[edge for line in road for edge in zip(line,line[1:])]
    road_edges.append((road[0][-1],road[1][-1]))
    shoulder_edges=[edge for line in shoulders for edge in zip(line,line[1:])]
    shoulder_edges.extend((road[i][-1],shoulders[i][-1]) for i in range(2))
    road_shape=[points[i] for i in road[0]+road[1][::-1]]
    grade_buffer=[points[i] for i in shoulders[0]+shoulders[1][::-1]]
    # North/east low ground continues the visible drainage depression past
    # the reserved tailrace, keeping enough new bank for the physical fence.
    drainage=((74,11,-7.63),(73.5,8,-7.77),(74,3,-7.95),(76,-4,-8.21),
              (79.5,-12,-8.46),(84,-22,-8.66))
    drain_ids=[]
    for x,z,y in drainage:
        if not inside(rim,x,z):
            drain_ids.append(len(points))
            points.append((x,z,y))
    drain_edges=list(zip(drain_ids,drain_ids[1:]))
    constrained=boundary_edges+road_edges+shoulder_edges+drain_edges
    segments=[(points[a],points[b]) for a,b in constrained]

    for x in range(-56,145,6):
        for z in range(-61,154,6):
            if inside(VALLEY,x,z) and not inside(rim,x,z) and not inside(grade_buffer,x,z):
                if min(distance_to_edge_squared((x,z),a,b) for a,b in segments)>.1225:
                    points.append((x,z,valley_height(rim,x,z)))
    _,_,triangles,owners,_,_=delaunay_2d_cdt([Vector(p[:2]) for p in points],constrained,[],0,1e-7)
    assert all(len(source)==1 for source in owners)
    vertices=[points[source[0]] for source in owners]
    faces=[]
    paving=[]
    for tri in triangles:
        coords=[vertices[i] for i in tri]
        x,z=(sum(v[k] for v in coords)/3 for k in (0,1))
        if inside(VALLEY,x,z) and not inside(rim,x,z):
            faces.append(tri[::-1] if loop_area(coords)>0 else tri)
            paving.append(inside(road_shape,x,z))
    counts=Counter(tuple(sorted((a,b))) for face in faces for a,b in closed_ring(face))
    perimeter={tuple(sorted(owners[i][0] for i in edge)) for edge,count in counts.items() if count==1}
    assert perimeter=={tuple(sorted(edge)) for edge in boundary_edges}
    area=sum(abs(loop_area([vertices[i] for i in face])) for face in faces)
    assert abs(area-(abs(loop_area(VALLEY))-abs(loop_area(rim))))<.003

    mesh=bpy.data.meshes.new("Blackwater joined valley terrain")
    mesh.from_pydata([point(x,y,z) for x,z,y in vertices],[],faces)
    for finish in original.data.materials:
        mesh.materials.append(finish)
    assert len(mesh.materials)==2
    mesh.update()
    uv=mesh.uv_layers.new(name="Terrain metres")
    for face,paved in zip(mesh.polygons,paving):
        face.material_index=int(paved)
        for i in face.loop_indices:
            p=mesh.vertices[mesh.loops[i].vertex_index].co
            uv.data[i].uv=(p.x/8,p.y/8)
    obj=bpy.data.objects.new(mesh.name,mesh)
    bpy.context.collection.objects.link(obj)
    obj["role"]="Joined exterior valley, north drainage and graded service road"
    paved_existing=0
    for face in original.data.polygons:
        x,z=face.center.x,-face.center.y
        if GATE_Z<z<=96:
            t=max(0,min(1,(z-90)/6))
            centre=36+5*t
            half=4-t
            if abs(x-centre)<half:
                face.material_index=1
                paved_existing+=1
    return obj,dict(exact_joined_rim_edges=len(rim),preserved_excavation_edges=len(cuts[0]),
                    extension_triangles=len(faces),road_triangles=sum(paving),retained_road_surface_faces=paved_existing,area_m2=round(area,2))


def grades(*grounds):
    tree=bvh(grounds)

    def query(x,z):
        hit,_,_,_=tree.ray_cast(point(x,35,z),Vector((0,0,-1)),58)
        assert hit is not None,(x,z,"Unsupported site limit")
        return hit.z
    return query


def enclosure(grade):
    concrete=material("Blackwater bank cast concrete",(.39,.40,.36))
    steel=material("Blackwater galvanised shore steel",(.29,.33,.33))
    cap=material("Blackwater retaining coping stone",(.45,.46,.41))
    finishes=(concrete,steel,cap)
    parts=[]
    stations=[]
    joins={}
    runs=[]
    for p,q in closed_ring(BOUNDARY):
        if (p,q)==GATE_EDGE:
            continue
        length=math.dist(p,q)
        number=math.ceil(length/3.6)
        for i in range(number):
            a=p if i==0 else station(p,q,i/number)
            b=q if i+1==number else station(p,q,(i+1)/number)
            length=math.dist(a,b)
            dx,dz=(b[0]-a[0])/length,(b[1]-a[1])/length
            nx,nz=dz,-dx
            shore=(a[1]+b[1])/2<16
            ground=strip(a,b,.88)
            samples=[grade(*station(s,t,k/12))
                     for s,t in ((ground[0],ground[1]),(ground[3],ground[2]),(a,b)) for k in range(13)]
            lo,hi=min(samples),max(samples)
            base=hi+(.58 if shore else .16)
            height=base+(1.59 if shore else 1.94)
            mesh=MeshParts(finishes)
            mesh.prism(ground,lo-.38,base,0)
            if shore:
                mesh.prism(strip(a,b,.29),base-.035,base+.075,2)
                for y in (base+.21,height-.06):
                    mesh.prism(strip(a,b,.115),y-.042,y+.042,1)
                bars=max(2,math.ceil(length/.21))
                for j in range(bars):
                    at=(j+.5)/bars
                    start=station(a,b,at-.019/length)
                    end=station(a,b,at+.019/length)
                    mesh.prism(strip(start,end,.086),base+.056,height-.012,1)
            else:
                mesh.prism(strip(a,b,.34),base-.025,height-.095,0)
                mesh.prism(strip(a,b,.46),height-.095,height,2)
            label=f"Blackwater {'shore grille' if shore else 'retaining return'} {len(runs)+1:02}"
            parts.append(mesh.frame(label,"Founded physical valley boundary"))
            for end in (a,b):
                bottom,crest=joins.get(end,(lo,height))
                joins[end]=(min(bottom,lo-.38),max(crest,height))
            runs.append(dict(name=label,side="shore" if shore else "retaining",a=a,b=b,
                             soil_range=(round(lo,4),round(hi,4)),toe=round(lo-.38,4),top=round(height,4)))
            count=math.ceil(length/.65)
            for j in range(count+1):
                x,z=station(a,b,j/count)
                stations.append(("SHORE" if shore else "BANK",x,z,nx,nz,grade(x,z)))

    pillars=MeshParts(finishes)
    for p,(bottom,top) in joins.items():
        if p in GATE_EDGE:
            continue
        foot=post_foot(p,.70)
        local=[grade(x,z) for x,z in foot]
        seat=max(local)+.16
        pillars.prism(foot,min(bottom,min(local)-.37),seat,0)
        pillars.prism(post_foot(p,.39),seat-.025,top+.055,0)
    parts.append(pillars.frame("Blackwater tied boundary piers","Soil-seated span junctions"))

    footing=MeshParts(finishes)
    a,b=GATE_EDGE
    verge=[grade(x,z) for x,z in strip(a,b,.71)]
    assert max(verge)-min(verge)<.04,verge
    datum=max(verge)
    footing.prism(strip(a,b,.71),min(verge)-.31,datum+.016,0)
    for p in (a,b):
        foot=post_foot(p,1.00)
        footing.prism(foot,min(grade(*c) for c in foot)-.44,datum+.13,0)
        footing.prism(post_foot(p,.59),datum+.12,datum+2.72,0)
    parts.append(footing.frame("Blackwater founded gate jambs and sill","Sighted roadhead portal in the retained apron"))

    templates=moving_panel(steel)
    leaf_names=[]
    for name,x in (("west",34),("east",38)):
        scale=Matrix.Diagonal((4/2.396,1,1,1))
        transform=Matrix.Translation(point(x,datum,GATE_Z))@scale@Matrix.Translation(-point(34,0,DEPTH))
        for src in templates:
            label=f"Blackwater {name} vehicle gate "+src.name.removeprefix("Sally gate ")
            valve=bpy.data.objects.new(label,src.data)
            bpy.context.scene.collection.objects.link(valve)
            valve.matrix_world=transform@src.matrix_world
            valve["role"]="Closed roadhead vehicle leaf"
            if src.get("source_asset"):
                valve["source_asset"]=src["source_asset"]
                leaf_names.append(label)
            parts.append(valve)
    for src in templates:
        bpy.data.objects.remove(src,do_unlink=True)
    for i in range(61):
        x,z=station(a,b,i/60)
        stations.append(("GATE",x,z,0,1,grade(x,z)))
    return parts,stations,runs,leaf_names


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    roster,source=authority()
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    objects=import_scene(ROOT/source["glb"])
    terrain=objects["Dam saved east-bank ground"]
    extended,land=valley(terrain)
    bpy.context.view_layer.update()
    pieces,stations,runs,leaves=enclosure(grades(terrain,extended))
    original_mesh=terrain.name
    terrain.name="Blackwater saved ground with road surface"
    parts=[extended,*pieces]
    bpy.ops.wm.save_as_mainfile(filepath=str(BLEND),compress=True)
    fragment=ROOT/"build/dam-valley-addition.glb"
    export(fragment,[terrain,*parts])
    append(ROOT/source["glb"],fragment,GLB,remove_mesh_names=[original_mesh])
    native_write(ROOT/source["native"],NATIVE,[],parts,open_components={extended.name,*leaves})
    (OUT/"boundary-stations.tsv").write_bytes("".join("\t".join(map(str,row))+"\n" for row in stations).encode())
    meta.update(status=STATUS,source_glb=source["glb"],source_glb_sha256=source["glb_sha256"],
        source_physics=source["native"],source_physics_sha256=source["native_sha256"],
        before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
        before_segment_map=meta["source_segment_map"],before_stair_components=meta["stair_components"],
        removed_source_meshes=[],resurfaced_source_mesh=original_mesh,resurfaced_render_mesh=terrain.name,
        new_interface_parts=[p.name for p in parts],
        valley=land,perimeter=dict(alignment=BOUNDARY,spans=runs,station_count=len(stations),
            gate_asset=rel(GATE),gate_asset_sha256=sha(GATE),road_gate=GATE_EDGE),
        builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
        glb_sha256=sha(GLB),physics_sha256=sha(NATIVE),blend_sha256=sha(BLEND))
    write(OUT/"construction.json",meta)
    source.update(construction=rel(OUT/"construction.json"),glb=rel(GLB),native=rel(NATIVE),
                  blend=rel(BLEND),glb_sha256=sha(GLB),native_sha256=sha(NATIVE))
    next(row for row in roster["sites"] if row["id"]=="WLD-09")["decision"]=STATUS
    roster["baseline"]=rel(PRIOR)
    write(STAGE/"roster.json",roster)


def review():
    import build_dam_east_hall
    build_dam_east_hall.CAPTURES=OUT/"captures/structure"
    _,source=authority()
    meta=json.loads((OUT/"construction.json").read_bytes())
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    old,old_buffer=read(ROOT/source["glb"])
    current,new_buffer=read(GLB)
    assert new_buffer.startswith(old_buffer)
    for field in ("meshes","materials","accessors","bufferViews","textures","images"):
        assert current.get(field,[])[:len(old.get(field,[]))]==old.get(field,[]),field
    for i,node in enumerate(old["nodes"]):
        expected=dict(node)
        if node.get("name")==meta["resurfaced_source_mesh"]:
            expected.pop("mesh",None)
        assert current["nodes"][i]==expected,node.get("name")
    retained=split_native(ROOT/source["native"])
    records_now=split_native(NATIVE)
    assert records_now[:len(retained)]==retained
    objects=import_scene(GLB)
    native=read_parts(NATIVE)
    terrain=objects["Blackwater joined valley terrain"]
    old_terrain=objects[meta["resurfaced_render_mesh"]]
    outer,join=surveyed_terrain_loops(terrain)
    assert len(outer)==len(VALLEY) and len(join)==1159,(len(outer),len(join))
    point_keys=lambda pts:{tuple(round(v,5) for v in p) for p in pts}
    old_geom=[part[1] for name,part in native.items() if name.startswith("terrain-")]
    old_faces=[triangle for triangles in old_geom for triangle in triangles]
    assert face_keys(records(old_terrain))==face_keys(old_faces)
    old_rim,_=surveyed_terrain_loops(old_terrain)
    assert point_keys(join)==point_keys(old_rim)
    for name in meta["new_interface_parts"]:
        obj=objects[name]
        if name==terrain.name or obj.get("source_asset"):
            pass
        else:
            # The assemblies consist of independently closed concrete and
            # steel pieces that can meet at shared faces along the run.
            assert obj.data.polygons
        assert face_keys(records(obj))==face_keys(native[name][1]),name
    captures,van=render(layout,VIEWS,sha(GLB))
    write(OUT/"export-verification.json",dict(glb_sha256=sha(GLB),native_sha256=sha(NATIVE),
        retained_native_records=len(retained),added_native_records=len(records_now)-len(retained),
        shared_rim_vertices=len(join),canonical_van_triangles=van,captures=captures))


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--review-only",action="store_true")
    args=parser.parse_args(sys.argv[sys.argv.index("--")+1:] if "--" in sys.argv else [])
    (review if args.review_only else build)()
