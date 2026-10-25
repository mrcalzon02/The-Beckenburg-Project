"""Export the actual visible native site into bounded local meshes + world poses.
No generated room slabs/walls, replacement van, hidden UV evidence or water slab.
Water and the canonical van are owned by their independent runtime modules.
"""
import bpy,json,hashlib,sys,array,struct,collections,math
from pathlib import Path
from mathutils import Matrix,Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'green-grass.json').read_text(encoding='utf-8'));source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene;deps=bpy.context.evaluated_depsgraph_get();C=Matrix(((1,0,0,0),(0,0,1,0),(0,-1,0,0),(0,0,0,1)));Ci=C.inverted();assets={};instances=[];flat={};alpha={};excluded=[]
def material(m):
 if m is None:raise AssertionError('Missing native material')
 p=next((n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED'),None) if m.use_nodes else None
 if p and p.inputs['Base Color'].is_linked and p.inputs['Base Color'].links[0].from_node.type=='TEX_IMAGE':return m
 if m.name in flat:return flat[m.name]
 n=bpy.data.materials.new(m.name+' / portable original');n.use_nodes=True;shader=n.node_tree.nodes['Principled BSDF'];shader.inputs['Roughness'].default_value=.94;tex=n.node_tree.nodes.new('ShaderNodeTexImage')
 images=[node.image for node in m.node_tree.nodes if node.type=='TEX_IMAGE' and node.image] if m.use_nodes else []
 if images:
  assert m.name.startswith('Supplemental alpha crown'),('Unsupported procedural source material',m.name);image=images[0];n.node_tree.links.new(tex.outputs['Alpha'],shader.inputs['Alpha']);n.surface_render_method='DITHERED';alpha[m.name]=image.name
 else:
  assert p is None or not p.inputs['Base Color'].is_linked,('Unsupported native procedural material',m.name)
  color=tuple(p.inputs['Base Color'].default_value) if p else tuple(m.diffuse_color);image=bpy.data.images.new(m.name+' constant diffuse',width=2,height=2,alpha=True);image.pixels.foreach_set(list(color)*4);image.pack()
 tex.image=image;n.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color']);flat[m.name]=n;return n
def signature(o):
 h=hashlib.sha256()
 for data,field,code,n in [(o.data.vertices,'co','f',3),(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  a=array.array(code,[0])*(len(data)*n);data.foreach_get(field,a);h.update(a.tobytes())
 for uv in o.data.uv_layers:
  if uv.active_render:
   a=array.array('f',[0])*(len(uv.data)*2);uv.data.foreach_get('uv',a);h.update(a.tobytes())
 h.update(repr([m.name for m in o.data.materials]).encode());h.update(bytes(p.use_smooth for p in o.data.polygons));return h.hexdigest()
for original in sorted(scene.objects,key=lambda o:o.name):
 if original.type!='MESH':continue
 if original.hide_render or original.name=='Lake water boundary' or any(m and m.name.startswith('Canonical van material') for m in original.data.materials):excluded.append(original.name);continue
 assert not original.modifiers,('Bake evaluated native modifier explicitly',original.name)
 original.data.calc_loop_triangles();key=signature(original);matrix=original.evaluated_get(deps).matrix_world.copy();world=C@matrix@Ci;parts=[]
 if key not in assets:
  uv=next((u for u in original.data.uv_layers if u.active_render),None);parts=[]
  for chunk,start in enumerate(range(0,len(original.data.loop_triangles),24000)):
   tris=list(original.data.loop_triangles)[start:start+24000];assert tris;ids=sorted({i for t in tris for i in t.vertices});remap={old:new for new,old in enumerate(ids)};mesh=bpy.data.meshes.new('Exact native triangles');mesh.from_pydata([original.data.vertices[i].co for i in ids],[],[tuple(remap[i] for i in t.vertices) for t in tris]);mesh.update();mesh.materials.clear()
   for m in original.data.materials:mesh.materials.append(material(m))
   layer=mesh.uv_layers.new(name='Original native render UV0');normals=[]
   for p,t in zip(mesh.polygons,tris):
    p.material_index=original.data.polygons[t.polygon_index].material_index;p.use_smooth=True
    for loop,old_loop in zip(p.loop_indices,t.loops):layer.data[loop].uv=uv.data[old_loop].uv if uv else (0,0);normals.append(tuple(original.data.corner_normals[old_loop].vector))
   mesh.normals_split_custom_set(normals);id='level_hc_native_'+str(len(assets)).zfill(3)+'_'+str(chunk);o=bpy.data.objects.new(id,mesh);scene.collection.objects.link(o);bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o
   file=ROOT/f'assets/runtime/models/{id}.glb';bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True)
   assert file.stat().st_size<4000000,(id,file.stat().st_size)
   parts.append(dict(id=id,triangles=len(tris),bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest(),source_object=original.name,source_triangle_start=start));bpy.data.objects.remove(o,do_unlink=True);bpy.data.meshes.remove(mesh)
  assets[key]=parts
 parts=assets[key];vv=[matrix@v.co for v in original.data.vertices];bounds=[min(v.x for v in vv),min(v.z for v in vv),min(-v.y for v in vv),max(v.x for v in vv),max(v.z for v in vv),max(-v.y for v in vv)];billboard=any(c.type=='LOCKED_TRACK' for c in original.constraints)
 for part in parts:instances.append(dict(id=part['id'],sha256=part['sha256'],matrix=[v for row in world for v in row],bounds=bounds,billboard=billboard,object=original.name))
# Derive the real interior waterline from actual hull triangles, not an AABB mask.
boat=bpy.data.objects['West moored rowing boat'];boat.data.calc_loop_triangles();segments=[];level=-1.95
hull_groups={g.index for g in boat.vertex_groups if g.name.startswith('Continuous double-skin hull')}
hull_ids={v.index for v in boat.data.vertices if any(a.group in hull_groups and a.weight>.5 for a in v.groups)}
assert hull_ids,'No actual inner/outer hull source group'
for tri in boat.data.loop_triangles:
 if not all(i in hull_ids for i in tri.vertices):continue
 points=[boat.matrix_world@boat.data.vertices[i].co for i in tri.vertices];hits=[]
 for a,b in zip(points,points[1:]+points[:1]):
  if (a.z<level)!=(b.z<level):
   t=(level-a.z)/(b.z-a.z);p=a+(b-a)*t;hits.append((round(p.x,5),round(-p.y,5)))
 if len(hits)==2 and hits[0]!=hits[1]:segments.append(tuple(hits))
neighbors=collections.defaultdict(set)
for a,b in segments:neighbors[a].add(b);neighbors[b].add(a)
assert all(len(v)==2 for v in neighbors.values()),'Native hull waterline is not closed'
remaining=set(neighbors);loops=[]
while remaining:
 first=min(remaining);loop=[first];previous=None;at=first
 while True:
  nxt=next(p for p in neighbors[at] if p!=previous)
  if nxt==first:break
  assert nxt not in loop;loop.append(nxt);previous,at=at,nxt
 remaining.difference_update(loop);loops.append(loop)
def area(loop):return abs(sum(a[0]*b[1]-b[0]*a[1] for a,b in zip(loop,loop[1:]+loop[:1]))/2)
assert len(loops)==2,('Expected outer and inner native hull waterlines',len(loops));interior=min(loops,key=area)
manifest=['HOLLOW_CREEK_SCENE_1\t'+r['scene_sha256']]
for i in instances:manifest.append('\t'.join([i['id'],i['sha256']]+[repr(float(v)) for v in i['matrix']+i['bounds']]+[str(int(i['billboard'])),i['object']]))
file=ROOT/'assets/runtime/sites/hollow-creek-scene.tsv';file.write_text('\n'.join(manifest)+'\n',encoding='utf-8')
report=dict(status='actual visible native scene export; runtime review pending',scene=r['scene'],source_sha256=r['scene_sha256'],manifest_sha256=hashlib.sha256(file.read_bytes()).hexdigest(),assets=[a for group in assets.values() for a in group],instances=instances,excluded_native_helpers_van_water=excluded,constant_materials=list(flat),alpha_crowns=alpha,water=dict(y=level,outline=[[-105,-120],[145,-120],[145,30],[-105,30]],dry_hull=dict(object=boat.name,waterline=interior,inner_area_m2=area(interior),outer_area_m2=max(map(area,loops)),bottom=-2.2,top=-1.45,source_cross_section_segments=len(segments))))
(OUT/'runtime-scene.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');assert hashlib.sha256(source.read_bytes()).hexdigest()==r['scene_sha256'];print('EXPORTED NATIVE SITE',len(report['assets']),'bounded meshes;',len(instances),'instances; actual hull hole',len(interior),'waterline points',flush=True)
