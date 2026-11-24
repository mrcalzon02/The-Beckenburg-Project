"""Seat Northwatch's room furnishings and two original readable station records."""
import bmesh
import math
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parent))
from build_northwatch_distribution import BASE, build
from build_college_electrical import point, world
from build_mine_case import Case, DOWN
from verify_college_services import bvh

PARENT=BASE/"northwatch-water-pass"
OUT=BASE/"northwatch-furnishing-pass/wld-08"
PIECES=(
    ("generator","service workbench","level_work_repair_bench",3.46,20.05,math.pi/2),
    ("records","relay log desk","level_student_desk",35.86,14.85,0),
    ("store","cold-store shelving","shelf_mp_6",3.0,49.8,math.pi/2),
    ("mess","mess table","dinner_table_1",49.8,50.85,0),
    ("dorm","dorm gear lockers","level_lockers",57.7,27.1,0),
    ("shelter","shelter equipment shelf","shelf_mp_6",66.5,9.25,0),
    ("lab","science instrument table","level_blockout_science_instrument_table",13.6,29.25,0),
    ("array","weather instrument array","level_blockout_weather_instrument_array",57.8,8.0,0),
)
RECORDS=(
    ("WINTER_RELAY_LOG","records",4,"level_trace_journal",(35.86,14.82),(35.86,0,16.10),
     ((38,0,19),(38,0,16.10),(35.86,0,16.10))),
    ("FUEL_AND_HEAT_WORKSHEET","generator",5,"level_story_letters",(3.55,20.18),(5.10,0,20.18),
     ((11,0,16),(7,0,16),(5.1,0,16),(5.1,0,20.18))),
)


def probes():
    rows=(PARENT/"wld-08/runtime-input.tsv").read_text().splitlines()
    tracks={}
    for raw in rows:
        row=raw.split("\t")
        if row[0]=="POINT":
            tracks.setdefault(row[1],[]).append(tuple(map(float,row[2:5])))
    for path,positions in tracks.items():
        for start,end in zip(positions,positions[1:]):
            count=max(1,math.ceil(math.dist(start,end)/.10))
            for i in range(count+1):
                yield path,tuple(a+(b-a)*i/count for a,b in zip(start,end))


def install(case):
    existing=[o for o in case.scene.objects if o.type=="MESH"]
    # The receiving slabs already carry the feet. Solid wall, guard,
    # furnishing, service and cupboard triangles remain collision controls.
    obstructions=bvh([o for o in existing if "floor" not in o.get("role","")
                      and "terrain" not in o.get("role","")])
    tracks=list(probes())
    furnishings={}
    placements=[]
    anchors=[]
    for room,name,asset,x,z,yaw in PIECES:
        meshes,info=case.fit(asset,name,room,x,0,z,yaw=yaw)
        if asset=="level_student_desk":
            removed=0
            for obj in meshes:
                mesh=bmesh.new()
                mesh.from_mesh(obj.data)
                empty=[face for face in mesh.faces if face.calc_area()<1e-12]
                removed+=len(empty)
                bmesh.ops.delete(mesh,geom=empty,context="FACES_ONLY")
                mesh.to_mesh(obj.data)
                mesh.free()
                obj.data.update()
            assert removed==16,removed
            info["discarded_zero_area_source_triangles"]=removed
        assert not obstructions.overlap(bvh(meshes)),name
        (a,b,c),(d,e,f)=info["bounds"]
        gaps=[(math.hypot(max(a-p,0,p-d),max(c-r,0,r-f)),path)
              for path,(p,q,r) in tracks if b-1.72<=q<=e+.06]
        clearance,nearest=min(gaps,default=(999,"none"))
        assert clearance>.42,(name,nearest,clearance)
        placements.append(dict(room=room,name=name,nearest_route=nearest,
                               minimum_route_margin=round(clearance,4),**info))
        anchors.append(dict(room=room,name=name,measured_floor_bearings=4))
        furnishings[room]=meshes
        for o in meshes:
            o.name=o.name.replace("Mine case ","Northwatch ")
            o["role"]="supported station furnishing"

    rows=[]
    documents=[]
    for name,room,owner,asset,(x,z),feet,walk in RECORDS:
        desk=furnishings[room]
        top,n,_,_=bvh(desk).ray_cast(point(x,1.70,z),DOWN,1.65)
        assert top is not None and n.z>.98,(name,top,n)
        paper,placement=case.fit(asset,name.lower().replace("_"," "),room,x,top.z+.001,z,scale=.62,support=desk)
        hit,*_=bvh(paper).ray_cast(point(x+.018,top.z+.10,z+.012),DOWN,.12)
        assert hit is not None,name
        eye=point(feet[0],feet[1]+1.63,feet[2])
        sight=hit-eye
        remaining=bvh([o for o in case.scene.objects if o.type=="MESH" and o not in paper])
        assert remaining.ray_cast(eye,sight.normalized(),sight.length-.004)[0] is None,name
        aim=world(hit)
        for o in paper:
            o.name=o.name.replace("Mine case ","Northwatch ")
            o["role"]="desktop-supported readable station record"
        rows.append(("STORY","northwatch-"+name.lower().replace("_","-"),name,owner,asset,
                     *placement["pivot"],placement["yaw"],placement["scale"],*feet,*aim))
        documents.append(dict(id=name,room=room,owner=owner,desktop=round(top.z,5),
                              reading_stance=feet,aim=aim,placement=placement))
        anchors.append(dict(room=room,name=name,measured_desktop_bearings=4))
    return dict(anchors=anchors,sleeves=[],furnishings=placements,documents=documents,runtime_rows=rows)


if __name__=="__main__":
    approaches=[(record.lower().replace("_","-"),owner,None,None,walk)
                for record,_,owner,_,_,_,walk in RECORDS]
    build(PARENT,OUT,approaches,install,"station-with-furnishings",assembly_factory=Case)
