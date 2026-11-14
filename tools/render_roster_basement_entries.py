"""Actual furnished native exports, intact geometry, neutral inspection light.

These are headless Blender first-person studies, never runtime/editor UI captures.
"""
from pathlib import Path
import hashlib,json
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development/levels/rebuild/captures/basement-batch'
def point(p):return Vector((p[0],-p[2],p[1]))
def main():
    OUT.mkdir(parents=True,exist_ok=True);views=[]
    for site in ['wld-07','wld-07-mortuary']:
        source=ROOT/f'development/levels/rebuild/furnishings/{site}/furnished.glb'
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
        scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral native inspection');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2
        bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.data.clip_start=.03;scene.camera=cam
        bpy.ops.object.light_add(type='POINT');fill=bpy.context.object;fill.name='Review illumination only';fill.data.energy=150;fill.data.shadow_soft_size=.1
        scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=1100;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
        for name,eye,target in [('west-entry',[7.5,1.55,16],[13.4,-2.65,16]),('lower-west-landing',[15.5,-1.4,16],[8.5,.6,16]),('east-entry',[48.5,1.55,16],[42.6,-2.65,16])]:
            cam.location=point(eye);cam.rotation_euler=(point(target)-cam.location).to_track_quat('-Z','Y').to_euler();fill.location=cam.location;output=OUT/f'{site}-{name}.png';scene.render.filepath=str(output);bpy.ops.render.render(write_still=True)
            views.append(dict(site=site,path=output.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(output.read_bytes()).hexdigest(),source_glb_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),camera_engine=eye,look_at_engine=target,type='NATIVE_FIRST_PERSON_EXPORT_INSPECTION_WITH_REVIEW_FILL',intact_geometry_not_render_masked=True,limitation='Neutral inspection light is not installed fixture/runtime gameplay evidence. Guards, final surfaces and owner/services integration remain open.'))
    (OUT/'capture-manifest.json').write_bytes((json.dumps(dict(views=views),indent=2)+'\n').encode())
if __name__=='__main__':main()
