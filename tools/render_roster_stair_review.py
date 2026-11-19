"""Independent actual-export before/after curved stair construction captures."""
from pathlib import Path
import hashlib,json,subprocess,sys
import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development/levels/rebuild/captures/stair-movement-batch'
def main():
    OUT.mkdir(parents=True,exist_ok=True);temp=ROOT/'build/roster-stair-probe';temp.mkdir(parents=True,exist_ok=True)
    ref='66c5e947b5c49b087d3d0b2eaf9932eaf926db52';relative='development/levels/rebuild/structures/wld-14/rough-shell.glb'
    previous=temp/'wld-14-before.glb';previous.write_bytes(subprocess.check_output(['git','show',ref+':'+relative],cwd=ROOT))
    rows=[]
    for label,path in [('rejected-rectangular',previous),('mitered-candidate',ROOT/relative)]:
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
        for obj in bpy.context.scene.objects:
            if obj.type=='MESH':obj.hide_render=not(obj.get('link')=='route-20' and obj.get('role')=='structural-stair-step')
        scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True
        scene.render.resolution_x=1100;scene.render.resolution_y=900;scene.render.resolution_percentage=100
        centre=Vector((32,-10,1.7));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=9;scene.camera=cam
        for angle,offset in [('construction',Vector((8,-10,8))),('top',Vector((0,-.001,16)))]:
            cam.location=centre+offset;cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler()
            target=OUT/f'wld-14-{label}-{angle}.png';scene.render.filepath=str(target);bpy.ops.render.render(write_still=True)
            rows.append({'path':target.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'type':'NATIVE_ACTUAL_EXPORT_ISOLATED_STAIR_CONSTRUCTION','status':'REJECTED_PRIOR_RECTANGULAR_TREADS' if label.startswith('rejected') else 'CANDIDATE_MITERED_JOINS_GUARDS_AND_COMPLETE_ROUTE_ACCEPTANCE_OPEN','source_glb_sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'source_ref':ref if path==previous else 'WORKING_CANDIDATE'})
    (OUT/'capture-manifest.json').write_bytes((json.dumps({'views':rows,'limitation':'Isolated rough geometry inspection, no runtime image, guards, complete building or visual finish acceptance.'},indent=2)+'\n').encode())

if __name__=='__main__':main()
