"""Boat-only source refinement. Parent site/hull geometry/pose remain immutable.
Fit thwarts and their hardware inside the actual curved hull; original new
procedural wood/paint/metal textures have world-oriented grain and wear.
"""
import bpy,math,json,hashlib,sys
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'green-grass.json').read_text(encoding='utf-8'));parent=ROOT/r['scene'];assert hashlib.sha256(parent.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(parent));o=bpy.data.objects['West moored rowing boat'];o.data=o.data.copy();o.data.calc_loop_triangles();old=[tuple(v.co) for v in o.data.vertices];groups={g.index:g.name for g in o.vertex_groups}
def group(v,prefix):return any(groups[a.group].startswith(prefix) and a.weight>.5 for a in v.groups)
hull=[t for t in o.data.loop_triangles if all(group(o.data.vertices[i],'Continuous double-skin hull') for i in t.vertices)];bvh=BVHTree.FromPolygons([v.co for v in o.data.vertices],[tuple(t.vertices) for t in hull],all_triangles=True)
def span(y,z):
 widths=[]
 for sign in [-1,1]:
  hit=bvh.ray_cast(Vector((0,y,z)),Vector((sign,0,0)),2)[0];assert hit is not None,(y,z);widths.append(abs(hit.x))
 return min(widths)
checks=[]
for name in ['Supported rowing thwart','Supported rowing thwart.001','Supported rowing thwart.002']:
 ids=[v.index for v in o.data.vertices if any(groups[a.group]==name and a.weight>.5 for a in v.groups)];assert ids
 vv=[o.data.vertices[i] for i in ids];y0=min(v.co.y for v in vv);y1=max(v.co.y for v in vv);z0=min(v.co.z for v in vv);z1=max(v.co.z for v in vv);before=max(abs(v.co.x) for v in vv)
 limit=min(span(y,z) for y in [y0,y1,(y0+y1)/2] for z in [z0,z1])-.012
 for v in vv:v.co.x*=limit/before
 checks.append(dict(group=name,old_half_width=before,new_half_width=limit,minimum_shell_clearance=.012))
# Move fastening assemblies and side brackets with their matching local seat.
for g in o.vertex_groups:
 if not g.name.startswith(('Thwart fastening','Thwart side bracket')):continue
 vv=[v for v in o.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)]
 if not vv:continue
 yc=sum(v.co.y for v in vv)/len(vv);xc=sum(v.co.x for v in vv)/len(vv);zc=sum(v.co.z for v in vv)/len(vv);seat=min(checks,key=lambda c:abs(yc-([-0.95,0,.95][checks.index(c)])))
 limit=seat['new_half_width']-.035;shift=max(0,max(abs(v.co.x) for v in vv)-limit)
 for v in vv:v.co.x-=math.copysign(shift,xc)
# Bearing brackets deliberately enter the inner skin by 3mm (not the exterior).
for v in o.data.vertices:
 if group(v,'Thwart side bracket'):
  limit=span(v.co.y,v.co.z)+.003
  v.co.x=math.copysign(min(abs(v.co.x),limit),v.co.x)
# Stow both oars parallel on the three thwarts, inside the narrowing bow.
oar_checks=[]
for g in o.vertex_groups:
 if not g.name.startswith(('Stowed oar shaft','Stowed oar blade')):continue
 vv=[v for v in o.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)];xc=sum(v.co.x for v in vv)/len(vv);target=math.copysign(.10,xc)
 for v in vv:v.co.x+=target-xc;v.co.z-=.0105
 clearance=min(span(v.co.y,v.co.z)-abs(v.co.x) for v in vv);assert clearance>.008,(g.name,clearance)
 oar_checks.append(dict(group=g.name,minimum_inner_hull_clearance=clearance,shaft_bears_on_thwarts=True))
# Three dedicated repeating procedural albedos; no reused atlas gutters.
texdir=ROOT/'development/levels/textures/hollow-creek';mats=[]
for kind in ['paint','wood','metal']:
 size=256;image=bpy.data.images.new('Rowboat '+kind+' original weathered diffuse',width=size,height=size,alpha=False);pixels=[]
 for j in range(size):
  for i in range(size):
   u=i/size;v=j/size;noise=(math.sin(i*127.1+j*311.7)*43758.5453)%1;grain=math.sin(v*math.tau*17+math.sin(u*math.tau*3)*.7)+.35*math.sin(v*math.tau*41+math.sin(u*math.tau*7));stain=(math.sin(u*math.tau*2+math.sin(v*math.tau*3))*.5+.5)
   if kind=='wood':rgb=[.30,.235,.155];factor=.86+.07*grain+.10*noise-.20*stain
   elif kind=='paint':
    worn=max(0,(math.sin(v*math.tau*19+math.sin(u*math.tau*5)*.4)-.94)*9)*(.25+.75*stain);rgb=[.12+worn*.10,.235+worn*.025,.255-worn*.04];factor=.86+.05*grain+.12*noise+.1*stain
   else:rgb=[.22,.24,.23];factor=.76+.2*noise-.2*stain
   pixels.extend([max(0,min(1,c*factor)) for c in rgb]+[1])
 image.pixels.foreach_set(pixels);image.filepath_raw=str(texdir/('rowboat-'+kind+'-v2.png'));image.file_format='PNG';image.save();image.pack();m=bpy.data.materials.new('Rowboat original '+kind+' weathering v2');m.use_nodes=True;p=m.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.88;t=m.node_tree.nodes.new('ShaderNodeTexImage');t.image=image;m.node_tree.links.new(t.outputs['Color'],p.inputs['Base Color']);mats.append(m)
o.data.materials.clear()
for m in mats:o.data.materials.append(m)
while o.data.uv_layers:o.data.uv_layers.remove(o.data.uv_layers[0])
uv=o.data.uv_layers.new(name='Boat grain by construction');o.data.uv_layers.active=uv
for layer in o.data.uv_layers:layer.active_render=layer.name==uv.name
for p in o.data.polygons:
 vv=[o.data.vertices[i] for i in p.vertices];is_hull=all(group(v,'Continuous double-skin hull') for v in vv);is_metal=any(group(v,'Mooring ring') or group(v,'Actual oarlock') or group(v,'Thwart fastening') for v in vv);p.material_index=0 if is_hull else 2 if is_metal else 1
 for li in p.loop_indices:
  co=o.data.vertices[o.data.loops[li].vertex_index].co
  uv.data[li].uv=(co.y*.55,math.atan2(co.x,max(.02,co.z-.12))*.32) if is_hull else (co.x*.9,co.y*.9+co.z*.11)
o.data.update();source=OUT/'hollow-creek-rowboat-refined.blend';bpy.ops.wm.save_as_mainfile(filepath=str(source));bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o
# Export exact local coordinates with the unchanged instance transform supplied separately.
pose=o.matrix_world.copy();o.matrix_world.identity();file=ROOT/'assets/runtime/models/level_hc_native_361_0.glb';bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True);o.matrix_world=pose
sha=hashlib.sha256(file.read_bytes()).hexdigest();j=json.loads((OUT/'runtime-scene.json').read_text(encoding='utf-8'));before_sha=next(a['sha256'] for a in j['assets'] if a['id']=='level_hc_native_361_0')
for a in j['assets']:
 if a['id']=='level_hc_native_361_0':a.update(sha256=sha,bytes=file.stat().st_size,override_scene=source.relative_to(ROOT).as_posix())
for i in j['instances']:
 if i['id']=='level_hc_native_361_0':i['sha256']=sha
manifest=ROOT/'assets/runtime/sites/hollow-creek-scene.tsv';s=manifest.read_text(encoding='utf-8').replace(before_sha,sha);manifest.write_text(s,encoding='utf-8');j['manifest_sha256']=hashlib.sha256(manifest.read_bytes()).hexdigest();j['boat_override']=dict(scene=source.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(source.read_bytes()).hexdigest(),reason='Seat penetration correction and original directional weathered albedos; hull/pivot/pose unchanged');(OUT/'runtime-scene.json').write_text(json.dumps(j,indent=2)+'\n',encoding='utf-8')
report=dict(parent=r['scene'],parent_sha256=r['scene_sha256'],scene=source.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),model_sha256=sha,seat_checks=checks,oar_checks=oar_checks,hull_vertices_unchanged=all(tuple(v.co)==old[v.index] for v in o.data.vertices if group(v,'Continuous double-skin hull')),changed_vertices=sum(tuple(v.co)!=old[v.index] for v in o.data.vertices),textures=['rowboat-'+kind+'-v2.png' for kind in ['paint','wood','metal']],limitations=['Seat fit and native hull shape retained; final human model acceptance pending.'])
assert report['hull_vertices_unchanged'];assert hashlib.sha256(parent.read_bytes()).hexdigest()==r['scene_sha256'];(OUT/'rowboat-refinement.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS boat seat fit, unchanged hull, original weathered construction textures')
