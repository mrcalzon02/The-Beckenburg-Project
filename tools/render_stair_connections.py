"""Actual exported before/after connection views; never gameplay acceptance."""
from pathlib import Path
import json,sys,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/stair-connection-pass';CAP=ROOT/'development/levels/rebuild/captures/stair-connection-batch';CAP.mkdir(parents=True,exist_ok=True)
VIEWS={'WLD-04':[('clearing-hub',[30,7.62,21],[30,5,29]),('hut-entry',[12,7.62,23],[12,6.9,28])],'WLD-05':[('adit-hub',[30,1.62,55],[27,-.5,49])],'WLD-15':[('crew-approach',[26,1.62,48],[23,1,43])],'WLD-17':[('battery-approach',[32,3.62,59],[39,3.2,56])],'WLD-18':[('exhibit-threshold',[48,-2.38,34],[52,-2.9,34])]}
def point(p):return Vector((p[0],-p[2],p[1]))
selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else None
prior=BASE/'verification/captures.json'
rows=[r for r in json.loads(prior.read_text()) if r['site'] not in selected] if selected and prior.exists() else []
for p in sorted(BASE.glob('*/construction.json')):
 r=json.loads(p.read_text())
 if selected and r['id'] not in selected:continue
 data=json.loads((ROOT/r['layout']).read_text())
 for phase,source in [('before',ROOT/r['source_glb']),('after',p.parent/'connected-stairs.glb')]:
  bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral intact connection review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2;bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.data.clip_start=.03;scene.camera=cam;bpy.ops.object.light_add(type='POINT');fill=bpy.context.object;fill.data.energy=150;fill.data.shadow_soft_size=.1;scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=1000;scene.render.resolution_y=750;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.render.image_settings.file_format='PNG'
  for label,eye,target in VIEWS[r['id']]:
   cam.data.type='PERSP';cam.location=point(eye);cam.rotation_euler=(point(target)-cam.location).to_track_quat('-Z','Y').to_euler();fill.location=cam.location;path=CAP/(r['id'].lower()+'-'+label+'-'+phase+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site=r['id'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),eye=eye,target=target,type='Native intact paired construction review, neutral150Wfill; not runtime or verified player spawn'))
  if phase=='before':continue
  scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);centre=Vector(((a+c)/2,-(b+d)/2,1));cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;cam.location=centre+Vector((span*.8,-span*.9,span*.8));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/(r['id'].lower()+'-whole-site.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site=r['id'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),type='Native whole-site construction context; no canonical van arrival or landscape acceptance'))
  if r['id'] in ['WLD-04','WLD-05']:
   targets=['L04','L11','L06'] if r['id']=='WLD-04' else ['L09','L17']
   for obj in bpy.context.scene.objects:
    if obj.type=='MESH':obj.hide_render=obj.get('link') not in targets or obj.get('role') not in ['structural-stair-step','structural-stair-landing']
   centre=point([28,4.5,30] if r['id']=='WLD-04' else [23,-2,44]);cam.data.ortho_scale=38;cam.location=centre+Vector((18,-20,22));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/(r['id'].lower()+'-isolated-stair-hub.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site=r['id'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),type='Native isolated structural stair/landing topology; all other meshes hidden, not full support or map acceptance'))
(BASE/'verification/captures.json').write_bytes((json.dumps(rows,indent=2)+'\n').encode())
