"""Paired intact native export views; neutral review fill, not runtime capture."""
from pathlib import Path
import json,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/terrain-occupancy-pass';CAP=ROOT/'development/levels/rebuild/captures/room-terrain-batch'


def point(p):return Vector((p[0],-p[2],p[1]))


def main():
    captures=[]
    for view in json.loads((BASE/'review-views.json').read_text()):
        folder=BASE/'assemblies'/view['site'].lower();contract=json.loads((folder/'construction.json').read_text())
        for phase,source in [('before',ROOT/contract['source_glb']),('after',folder/'occupied-terrain.glb')]:
            bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral native terrain inspection');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2
            bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.data.clip_start=.03;scene.camera=cam;cam.location=point(view['eye']);cam.rotation_euler=(point(view['target'])-cam.location).to_track_quat('-Z','Y').to_euler()
            bpy.ops.object.light_add(type='POINT');fill=bpy.context.object;fill.name='Neutral inspection fill, not installed light';fill.data.energy=150;fill.data.shadow_soft_size=.1;fill.location=cam.location
            scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=1000;scene.render.resolution_y=750;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
            path=CAP/(view['site'].lower()+'-room-'+phase+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
            captures.append(dict(site=view['site'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),eye=view['eye'],target=view['target'],pose_source=view['pose_source'],type='NATIVE_PAIRED_FIRST_PERSON_EXPORT_REVIEW_WITH_NEUTRAL150W_FILL',intact_geometry=True,limitation='Offline native review; no installed fixtures, runtime editor UI, gameplay/network or performance claim.'))
    (CAP/'room-capture-manifest.json').write_bytes((json.dumps(captures,indent=2)+'\n').encode())
if __name__=='__main__':main()
