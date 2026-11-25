"""Independently measure installed jambs and true open profiles on each exported scene."""
import json
from pathlib import Path
import sys

import bpy

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from build_college_electrical import point, world
from prepare_cemetery_doors import BOUNDARY, OUTPUT, load
from review_roster_macroterrain import sha
from verify_college_services import bvh


def survey(site):
    packet = OUTPUT / site
    authority = load(BOUNDARY / site / "construction.json")
    source = BOUNDARY / site / "bounded-grounds.glb"
    assert sha(source) == authority["glb_sha256"]
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source))
    tree = bvh([obj for obj in bpy.context.scene.objects if obj.type == "MESH"])
    schedule = packet / "door-schedule.tsv"
    rows = []
    for line in schedule.read_text(encoding="utf-8").splitlines():
        r = line.split("\t")
        x, y, z, width = map(float, r[6:10])
        along = r[10] == "z"
        near, far, height = map(float, r[11:14])
        fixed, center = (z,x) if along else (x,z)
        axis = 2 if along else 0

        def at(offset, above, tangent):
            return point(tangent,y+above,fixed+offset) if along else point(fixed+offset,y+above,tangent)

        def ray(start, end):
            distance=(end-start).length
            hit,_,_,_=tree.ray_cast(start,(end-start).normalized(),distance)
            return None if hit is None else world(hit)

        def profile(tangent, above):
            return [ray(at(sign*.8,above,tangent),at(-sign*.8,above,tangent)) for sign in (-1,1)]

        jamb = []
        for side in (-1,1):
            for above in (.65,1.25,1.82):
                first,last = profile(center+side*(width/2+.085),above)
                assert first is not None and last is not None,(site,r[1],side,above)
                jamb.append([first[axis],last[axis]])
        mean_near=sum(p[0] for p in jamb)/len(jamb)
        mean_far=sum(p[1] for p in jamb)/len(jamb)
        assert abs(mean_near-near)<.003 and abs(mean_far-far)<.003,(site,r[1],jamb)
        for tangent in (center-width*.35,center,center+width*.35):
            for above in (.43,1.2,min(height-.08,2.22)):
                assert profile(tangent,above)==[None,None],(site,r[1],tangent,above)
        header=ray(at(0,height-.09,center),at(0,height+.26,center))
        sill=ray(at(0,.19,center),at(0,-.17,center))
        assert header is not None and abs(header[1]-y-height)<.025,(site,r[1],header,height)
        assert sill is not None and abs(sill[1]-y)<.03,(site,r[1],sill,y)
        rows.append(dict(id=int(r[0]),frame=r[1],rooms=r[2:4],sill_y=sill[1],header_y=header[1],
                         measured_jamb_faces=jamb,clear_profile_rays=18))
    result=dict(source_scene_sha256=sha(source),door_schedule_sha256=sha(schedule),frames=rows)
    (packet/"frame-survey.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("PASS",site,len(rows),"frame bearings, headers, floor sills and clear apertures")


if __name__=="__main__":
    for name in ("wld-07","wld-07-mortuary"):
        survey(name)
