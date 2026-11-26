"""Actual flat-aperture pilots and complete exported native site context."""
from pathlib import Path
import json,sys,bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass';CAP=ROOT/'development/levels/rebuild/captures/flat-circulation-batch';CAP.mkdir(parents=True,exist_ok=True)
VIEWS={'ACADEMY':([43.5,1.62,48],[44.3,1.62,54]),'WLD-08':([23,1.62,19],[18,1.62,19])}
def point(p):return Vector((p[0],-p[2],p[1]))
selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else None
previous=BASE/'verification/captures.json'
rows=[r for r in json.loads(previous.read_text()) if r['site'] not in selected] if selected and previous.exists() else []
for p in sorted(BASE.glob('*/construction.json')):
 r=json.loads(p.read_text())
 if selected and r['id'] not in selected:continue
 data=json.loads((ROOT/r['layout']).read_text());sources=[('before',ROOT/r['source_glb']),('after',p.parent/'open-circulation.glb')] if r['id'] in VIEWS else [('after',p.parent/'open-circulation.glb')]
 for phase,source in sources:
  bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral flat aperture review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.2;bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.lens=25;cam.data.clip_start=.03;scene.camera=cam;scene.render.resolution_x=800;scene.render.resolution_y=600;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';scene.render.image_settings.file_format='PNG'
  if r['id'] in VIEWS:
   eye,target=VIEWS[r['id']];cam.location=point(eye);cam.rotation_euler=(point(target)-cam.location).to_track_quat('-Z','Y').to_euler();bpy.ops.object.light_add(type='POINT');fill=bpy.context.object;fill.location=cam.location;fill.data.energy=150;fill.data.shadow_soft_size=.1;scene.render.engine='CYCLES';scene.cycles.samples=4;path=CAP/(r['id'].lower()+'-threshold-'+phase+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site=r['id'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),eye=eye,target=target,type='Native intact threshold/cap inspection, neutral150Wfill; not runtime or verified player spawn'))
  if phase=='before':continue
  scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);centre=point([(a+c)/2,1,(b+d)/2]);cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;cam.location=centre+Vector((span*.8,-span*.9,span*.8));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/(r['id'].lower()+'-whole-site.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);rows.append(dict(site=r['id'],phase=phase,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),source_glb_sha256=sha(source),type='Native whole-site architectural construction; no cutaway, no canonical van or complete join/landscape/runtime acceptance'))
(BASE/'verification/captures.json').write_bytes((json.dumps(rows,indent=2)+'\n').encode())
