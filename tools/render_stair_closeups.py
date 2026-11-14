"""Native exported stair closeups; overhead caps hidden for construction review."""
from pathlib import Path
import json,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/stair-landing-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-landing-fit-batch'
for p in sorted(BASE.glob('*/construction.json')):
    r=json.loads(p.read_text());data=json.loads((ROOT/r['layout']).read_text());links={l['id']:l for l in data['links']}
    targets=[(v['link'],v['new_points']) for v in r['flight_replacements']]
    if not targets:
        v=r['targeted_segments'][0];old=json.loads((ROOT/r['source_layout']).read_text());link=next(l for l in old['links'] if l['id']==v['link']);targets=[(v['link'],link['points'][v['segment']:v['segment']+2])]
    for ordinal,(identity,pair) in enumerate(targets):
        high=max(pair,key=lambda a:a[1]);center=Vector((high[0],-high[2],high[1]-.4))
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(p.parent/'supported-stairs.glb'))
        for obj in bpy.context.scene.objects:
            if obj.type!='MESH':continue
            ps=[obj.matrix_world@Vector(v) for v in obj.bound_box];mid=sum(ps,Vector())/8
            if (mid-center).length>16 or obj.get('role')=='ceiling-cap' and min(v.z for v in ps)>center.z:obj.hide_render=True
        scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=900;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Native construction review')
        bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=8;cam.location=center+Vector((5,-7,5));cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.camera=cam;scene.render.filepath=str(CAP/(r['id'].lower()+'-'+identity+'-closeup-'+str(ordinal)+'.png'));bpy.ops.render.render(write_still=True)
