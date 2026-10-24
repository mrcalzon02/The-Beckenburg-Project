"""Refresh readable terrain review views without regenerating mesh exports or sources."""
import bpy,json,sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/terrain-foundations';PRE=ROOT/'development/levels/previews/terrain-foundations'
keys=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else sorted(p.name for p in OUT.iterdir() if (p/'contract.json').exists())
for key in keys:
 folder=OUT/key;d=json.loads((folder/'contract.json').read_text(encoding='utf-8'));bpy.ops.wm.open_mainfile(filepath=str(folder/'site-terrain.blend'));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene
 deck=set(d['profile']['deck']);below=set(d['profile']['below']);layers={q['id']:q for q in d['source_floor_layers']}
 floors={o.name for o in scene.objects if o.type=='MESH' and o.name.startswith('Floor ') and (layers[o.get('zone',o.name[6:].split('.')[0])]['kind'] in ['inside','circulation','staging'] or o.get('zone',o.name[6:].split('.')[0]) in deck)}
 d['retained_native_floors']=sorted(floors);(folder/'contract.json').write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
 for o in scene.objects:
  if o.type in ['FONT','CURVE']:o.hide_render=True
  if o.type!='MESH':continue
  if o.name.startswith('Floor '):
   q=layers[o.get('zone',o.name[6:].split('.')[0])];a,b,c,e=q['bounds'];cx,cz=(a+c)/2,(b+e)/2
   base=min([p['y'] for p in d['pads'] if p['bounds'][0]<=cx<=p['bounds'][2] and p['bounds'][1]<=cz<=p['bounds'][3]] or [q['y']])
   o.hide_render=o.name not in floors or (q['id'] not in deck and q['y']!=base)
   if q['id'] in deck and q['y']>0 and 'water' not in d['profile']:o.hide_render=True
  else:o.hide_render=not (o.name==d['asset']['id'] or o.name.startswith(('Water foundation ','Foundation pile ')))
 x0,z0,x1,z1=d['bounds'];cx,cz=(x0+x1)/2,(z0+z1)/2;span=max(x1-x0,z1-z0)
 def camera(name,position,target,lens=42):
  scene.camera.location=position;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=lens;scene.render.filepath=str(PRE/key/(name+'.png'))
 camera('site-native-fitout',(cx+span*.72,-cz-span*.80,span*.95),(cx,-cz,1))
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'site-terrain.blend'),compress=True);bpy.ops.render.render(write_still=True)
 low=[q for q in d['source_floor_layers'] if q['id'] in below]
 if low:
  bpy.data.objects[d['asset']['id']].hide_render=True
  for o in scene.objects:
   if o.type=='MESH' and o.name.startswith('Floor '):o.hide_render=o.get('zone',o.name[6:].split('.')[0]) not in below
  q=low[0];a,b,c,e=q['bounds'];camera('layer-study-native-fitout',(cx+span*.55,-cz-span*.6,span*.65),((a+c)/2,-(b+e)/2,q['y']),48)
 else:
  layout=json.loads((ROOT/f'development/levels/layouts/{key}/layout.json').read_text(encoding='utf-8'));p=layout['staging']['point'];camera('approach-native-fitout',(p[0]+16,-p[2]-14,12),(cx,-cz,1),38)
 bpy.ops.render.render(write_still=True);print('REVIEW',key,'refreshed; source geometry untouched',flush=True)
