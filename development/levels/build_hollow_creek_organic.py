"""Build the user-requested desquareification, retaining old sources unchanged."""
import bpy,bmesh,sys,json,math,hashlib,collections
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import hollow_creek_organic as organic
import hollow_creek_ground as old
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';d=organic.make()
protected={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [OUT/'hollow-creek-terrain.blend',OUT/'terrain.json',ROOT/'assets/runtime/models/level_camp_terrain.glb',ROOT/'development/levels/layouts/wld-02/layout.json']}
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-terrain.blend'));bpy.context.preferences.filepaths.save_version=0
removed=[]
for o in list(bpy.context.scene.objects):
 if o.name=='level_camp_terrain' or o.name.startswith('Pavilion apron '):removed.append(o.name);bpy.data.objects.remove(o,do_unlink=True)
xs=sorted(set(d['xs']+[25,39]));zs=sorted(set(d['zs']+[35,49]));nx=len(xs);vv=[(x,-z,organic.height(x,z,d['routes'])) for z in zs for x in xs];ff=[]
for j in range(len(zs)-1):
 for i in range(nx-1):
  x=(xs[i]+xs[i+1])/2;z=(zs[j]+zs[j+1])/2
  if 25<x<39 and 35<z<49 or not organic.keep(x,z):continue
  a=j*nx+i;b=a+1;c=a+nx+1;e=a+nx;ff.extend([(a,c,b),(a,e,c)])
mat=bpy.data.materials.new('Organic ground without zone-shaped color stamps');mat.use_nodes=True;shader=mat.node_tree.nodes['Principled BSDF'];shader.inputs['Roughness'].default_value=.95
image=bpy.data.images.new('Original irregular soil and feathered tread',width=512,height=800);pixels=[]
segments=[(p,q,l['width']) for l in d['routes'] for p,q in zip(l['points'],l['points'][1:])]
for j in range(800):
 z=d['bounds'][3]-(d['bounds'][3]-d['bounds'][1])*(j+.5)/800
 for i in range(512):
  x=d['bounds'][0]+(d['bounds'][2]-d['bounds'][0])*(i+.5)/512
  distance=min(old.distance(x,z,a,b) for a,b,w in segments);noise=.10*math.sin(x*3.1+math.sin(z*1.7))+.08*math.sin(z*3.9+x*.8)
  wear=1-old.smooth((distance-.38-noise)/.65);grain=.02*math.sin(x*11.3+z*7.7)
  earth=(.18,.22,.125);tread=(.30,.265,.18)
  pixels.extend([max(0,c*(1+grain)*(1-wear)+p*wear) for c,p in zip(earth,tread)]+[1])
image.pixels.foreach_set(pixels);image.pack();tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Linear';mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
o=s.mesh('level_camp_terrain_organic',vv,ff,mat)
uv=o.data.uv_layers.active
for loop in o.data.loops:
 v=o.data.vertices[loop.vertex_index].co;uv.data[loop.index].uv=((v.x-d['bounds'][0])/(d['bounds'][2]-d['bounds'][0]),(v.y+d['bounds'][3])/(d['bounds'][3]-d['bounds'][1]))
# Original world-space vertices retained. Partition primitives spatially to stay
# within the unchanged loader's per-primitive vertex limit.
for k in range(1,4):o.data.materials.append(mat.copy())
for p in o.data.polygons:p.material_index=(1 if p.center.x>=30 else 0)+(2 if -p.center.y>=42 else 0)
bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o;o.data.calc_loop_triangles();assert len(o.data.loop_triangles)<50000
asset=ROOT/'assets/runtime/models/level_camp_terrain_organic.glb';bpy.ops.export_scene.gltf(filepath=str(asset),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);assert asset.stat().st_size<4_000_000
o['role']='Desquareified authored terrain; actual pavilion slab only';o['license']='CC0-1.0';o['origin_policy']='World-site origin; no centering/floor normalization'
bpy.data.libraries.write(str(ROOT/'development/levels/blender/level_camp_terrain_organic.blend'),{o,mat},compress=True)
# Reuse exact runtime van triangles/material UVs at the authored ground transform.
src=ROOT/'build/level-verification/scenes/hollow-creek-canonical-van';faces=json.loads((src/'geometry.json').read_text(encoding='utf-8'));groups=collections.defaultdict(list)
for f in faces:groups[(f['material'],f['color'])].append(f)
for (mid,color),group in groups.items():
 m=bpy.data.materials.new(f'Canonical van material {mid}/{color}');m.use_nodes=True;p=m.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.85;tint=tuple(((color>>s)&255)/255 for s in [16,8,0])+(1,)
 if mid>=0:
  im=bpy.data.images.load(str(src/f'textures/{mid}.png'),check_existing=True);im.pack();tx=m.node_tree.nodes.new('ShaderNodeTexImage');tx.image=im;tx.interpolation='Closest';mix=m.node_tree.nodes.new('ShaderNodeMixRGB');mix.blend_type='MULTIPLY';mix.inputs[0].default_value=1;mix.inputs[2].default_value=tint;m.node_tree.links.new(tx.outputs['Color'],mix.inputs[1]);m.node_tree.links.new(mix.outputs[0],p.inputs['Base Color'])
 else:p.inputs['Base Color'].default_value=tint
 verts=[];tri=[];uvs=[]
 for f in group:
  n=len(verts)
  for key in ['a','b','c']:v=f[key];verts.append((32+v['x'],-78-v['z'],1.5+v['y']))
  tri.append((n,n+1,n+2));uvs.extend([(f['au'],f['av']),(f['bu'],f['bv']),(f['cu'],f['cv'])])
 obj=s.mesh(f'Canonical van runtime material {mid} tint {color}',verts,tri,m)
 for loop in obj.data.loops:obj.data.uv_layers.active.data[loop.index].uv=uvs[loop.vertex_index]
 obj['role']='Exact canonical runtime van; source textures/UVs retained';obj['source']='SceneRenderer.vanModel and VanStaging';obj['placement']='X32 Y1.5 Z78 yaw0; no replacement vehicle';obj['canonical_van']=True
d['xs']=xs;d['zs']=zs;d['nx']=len(xs);d['nz']=len(zs);d['heights']=[v[2] for v in vv]
(OUT/'organic-terrain.json').write_text(json.dumps(d,separators=(',',':'))+'\n',encoding='utf-8')
report=dict(status='desquareified terrain native construction',protected_sources=protected,removed_in_derived_scene=removed,terrain_triangles=len(o.data.loop_triangles),terrain_sha256=hashlib.sha256(asset.read_bytes()).hexdigest(),canonical_van_triangles=len(faces),canonical_van_source_sha256=hashlib.sha256((src/'geometry.json').read_bytes()).hexdigest(),rectangular_landscape_pads=0,retained_physical_rectangular_foundation='14m pavilion slab only',limitations=d['limitations'])
(OUT/'organic-assets.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
scene=bpy.context.scene;scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.38,.45,.52,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.7;scene['production_status']='Desquareified terrain and real canonical van; runtime integration pending';bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-organic.blend'),compress=True)
for path,h in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==h,path
print('BUILT organic terrain',len(o.data.loop_triangles),'triangles; canonical van',len(faces),'triangles; original sources unchanged',flush=True)
