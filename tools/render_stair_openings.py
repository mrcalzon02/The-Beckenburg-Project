"""Two-sided native inspection of actual exported structural apertures."""
from pathlib import Path
import json,bpy,sys
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/stair-opening-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-opening-batch';CAP.mkdir(parents=True,exist_ok=True)
for p in sorted(BASE.glob('*/construction.json')):
    r=json.loads(p.read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(p.parent/'opened-stairs.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=900;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Native aperture inspection');captures=[]
    for index,change in enumerate(r['changes']):
        if not change['new_mesh'] or change['source_role']!='rough shell wall':continue
        obj=objects[change['new_mesh']];ps=[obj.matrix_world@Vector(v) for v in obj.bound_box];ranges=[max(v[j] for v in ps)-min(v[j] for v in ps) for j in [0,1]];axis=0 if ranges[0]<ranges[1] else 2
        for corridor in r['corridors']:
            if not any(n.startswith(corridor['cutter_name']) for n in change['cutters']):continue
            a,b=corridor['points'];plane=change['source_origin'][0] if axis==0 else -change['source_origin'][1];t=max(0,min(1,(plane-a[axis])/(b[axis]-a[axis]))) if abs(b[axis]-a[axis])>.001 else .5;x=a[0]+(b[0]-a[0])*t;z=a[2]+(b[2]-a[2])*t;y=a[1]+(b[1]-a[1])*t;target=Vector((x,-z,y+1));direction=Vector((b[0]-a[0],a[2]-b[2],0)).normalized()
            for side in [-1,1]:
                bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=28;cam.location=target+direction*side*3.5+Vector((0,0,.3));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();scene.camera=cam;out=CAP/(r['id'].lower()+'-'+str(index)+'-'+corridor['link']+'-'+str(side)+'.png');scene.render.filepath=str(out);bpy.ops.render.render(write_still=True);captures.append(dict(path=out.relative_to(ROOT).as_posix(),sha256=sha(out),type='native aperture inspection; construction camera, not runtime'))
    r['captures']=captures;(p.parent/'captures.json').write_bytes((json.dumps(dict(id=r['id'],captures=captures,renderer='Blender native Workbench; actual GLB reimport, not runtime'),indent=2)+'\n').encode())
