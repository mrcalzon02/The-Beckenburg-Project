"""Paired intact source/candidate plate views with neutral native review fill."""
from pathlib import Path
import json,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
BASE=ROOT/'development/levels/rebuild/floor-closure-pass';CAP=ROOT/'development/levels/rebuild/captures/floor-closure-batch'


def point(p):return Vector((p[0],-p[2],p[1]))


def main():
    result=[]
    for contract in sorted(BASE.glob('*/construction.json')):
        r=json.loads(contract.read_text())
        if not r['plates']:continue
        patch=max(r['plates'],key=lambda p:p['area_m2']);source=ROOT/r['source_glb'];candidate=contract.parent/'closed-floors.glb';data=json.loads((ROOT/r['layout']).read_text());zone=next(z for z in data['zones'] if z['id']==patch['zone'])
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(candidate));obj=next(o for o in bpy.context.scene.objects if o.name==patch['name']);triangles=[xyz for xyz,flags in records(obj) if flags&1 and all(abs(xyz[i]-patch['top_y'])<1e-4 for i in [1,4,7])]
        def surface(xyz):return (Vector(xyz[3:6])-Vector(xyz[:3])).cross(Vector(xyz[6:9])-Vector(xyz[:3])).length_squared
        tri=max(triangles,key=surface);x=sum(tri[i] for i in [0,3,6])/3;z=sum(tri[i] for i in [2,5,8])/3;e=patch['top_y']
        eye=[x,zone['y']+1.62,z];target=[x+.3,e-.16 if patch['role']=='ceiling-cap' else e,z+.3]
        for phase,path in [('before',source),('after',candidate)]:
            bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral plate inspection');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2
            bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.data.clip_start=.03;scene.camera=cam;cam.location=point(eye);cam.rotation_euler=(point(target)-cam.location).to_track_quat('-Z','Y').to_euler()
            bpy.ops.object.light_add(type='POINT');light=bpy.context.object;light.name='150W neutral review fill, not installed fixture';light.location=cam.location;light.data.energy=150;light.data.shadow_soft_size=.1
            scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=1000;scene.render.resolution_y=750;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';output=CAP/(r['id'].lower()+'-room-'+phase+'.png');scene.render.filepath=str(output);bpy.ops.render.render(write_still=True)
            result.append(dict(site=r['id'],zone=patch['zone'],role=patch['role'],phase=phase,path=output.relative_to(ROOT).as_posix(),sha256=sha(output),source_glb_sha256=sha(path),eye=eye,target=target,type='NATIVE_INTACT_PAIRED_STRUCTURAL_VIEW_NEUTRAL150W_FILL',limitation='Authored inspection eye, not verified avatar/fixture/runtime acceptance. Geometry intact; no render mask.'))
    (CAP/'paired-room-captures.json').write_bytes((json.dumps(result,indent=2)+'\n').encode())
if __name__=='__main__':main()
