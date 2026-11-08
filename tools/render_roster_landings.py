"""Actual-export landing/turn join reviews; background Blender only."""
from pathlib import Path
import hashlib,json,subprocess
import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development/levels/rebuild/captures/landing-batch'
CASES={'college':['L84'],'mansion':['L59','L60'],'wld-14':['route-24']}
def main():
    OUT.mkdir(parents=True,exist_ok=True);temp=ROOT/'build/roster-landing-pass';temp.mkdir(parents=True,exist_ok=True)
    source_ref='fe7f4972330a433473ce80428785c3cc597fba87';views=[]
    for site,links in CASES.items():
        relative=f'development/levels/rebuild/structures/{site}/rough-shell.glb';previous=temp/(site+'-prior.glb')
        previous.write_bytes(subprocess.check_output(['git','show',source_ref+':'+relative],cwd=ROOT))
        # Fixed framing comes from both actual exports, preserving comparison.
        candidates=[]
        for label,path in [('rejected-prior',previous),('landing-candidate',ROOT/relative)]:
            bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
            selected=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.get('link') in links and o.get('role') in ['structural-stair-step','structural-stair-landing']]
            assert selected
            points=[o.matrix_world@v.co for o in selected for v in o.data.vertices]
            candidates.extend(points)
        lo=Vector(tuple(min(p[i] for p in candidates) for i in range(3)));hi=Vector(tuple(max(p[i] for p in candidates) for i in range(3)));centre=(lo+hi)/2;span=max(hi-lo)*1.5
        for label,path in [('rejected-prior',previous),('landing-candidate',ROOT/relative)]:
            bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path))
            for obj in bpy.context.scene.objects:
                if obj.type=='MESH':obj.hide_render=not(obj.get('link') in links and obj.get('role') in ['structural-stair-step','structural-stair-landing'])
            scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True
            scene.render.resolution_x=1100;scene.render.resolution_y=900;scene.render.resolution_percentage=100
            bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span;scene.camera=cam
            for angle,offset in [('construction',Vector((span,-span,span*.9))),('top',Vector((0,-.001,span*2)))]:
                cam.location=centre+offset;cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();target=OUT/f'{site}-{label}-{angle}.png';scene.render.filepath=str(target);bpy.ops.render.render(write_still=True)
                views.append(dict(path=target.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(target.read_bytes()).hexdigest(),site=site,links=links,type='NATIVE_ACTUAL_EXPORT_ISOLATED_LANDING_CONSTRUCTION',status='REJECTED_PRIOR_TRANSITIONS' if label.startswith('rejected') else 'CANDIDATE_FITTED_LANDINGS_GUARDS_AND_RUNTIME_OPEN',source_glb_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),source_ref=source_ref if path==previous else 'CURRENT_CANDIDATE'))
    (OUT/'capture-manifest.json').write_bytes((json.dumps(dict(views=views,limitation='Actual export inspection; isolated geometry has surroundings hidden. No finished material/guards/runtime acceptance.'),indent=2)+'\n').encode())

if __name__=='__main__':main()
