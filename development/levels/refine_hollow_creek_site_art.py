"""Second-pass surface/shore/rock art; immutable fitted parent and terrain shape.

Creates a derived native scene and portable, individually bounded GLBs. UV lore
marks are a separate hidden inspection layer, not ghost evidence or gameplay.
"""
import bpy,sys,json,math,random,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
parent=OUT/'hollow-creek-fitted.blend';protected=hashlib.sha256(parent.read_bytes()).hexdigest()
bpy.ops.wm.open_mainfile(filepath=str(parent));bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene;records=[]
def digest(o):
 return hashlib.sha256(repr((tuple(tuple(v.co) for v in o.data.vertices),tuple(tuple(p.vertices) for p in o.data.polygons),tuple(o.matrix_world))).encode()).hexdigest()
terrain=bpy.data.objects['level_camp_terrain_amenities'];before=digest(terrain)
terrain.data.calc_loop_triangles();ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(t.vertices) for t in terrain.data.loop_triangles],all_triangles=True)
def height(x,y):
 hit=ground.ray_cast(Vector((x,y,20)),Vector((0,0,-1)),100)[0]
 return None if hit is None else hit.z
def export(o,id):
 old=o.data;o.data=old.copy()
 active=o.data.uv_layers.active.name if o.data.uv_layers.active else None
 for name in [u.name for u in o.data.uv_layers if u.name!=active]:o.data.uv_layers.remove(o.data.uv_layers[name])
 bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o
 path=ROOT/f'assets/runtime/models/{id}.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
 o.data.calc_loop_triangles();assert len(o.data.loop_triangles)<50000,(id,len(o.data.loop_triangles));assert path.stat().st_size<4_000_000,(id,path.stat().st_size)
 disposable=o.data;o.data=old;bpy.data.meshes.remove(disposable)
 source=ROOT/f'development/levels/blender/{id}.blend';bpy.data.libraries.write(str(source),{o},compress=True)
 records.append(dict(id=id,model=path.relative_to(ROOT).as_posix(),editable=source.relative_to(ROOT).as_posix(),bytes=path.stat().st_size,sha256=hashlib.sha256(path.read_bytes()).hexdigest(),native_pose=[list(o.location),list(o.rotation_euler),list(o.scale)]))
def image(name):
 original=ROOT/f'development/levels/textures/hollow-creek/{name}-v2.png';im=bpy.data.images.load(str(original));im.name=name+' runtime512 derivative';im.scale(512,512);im.pack();return im
grass=image('grass-dirty');dirt=image('trail-dirt');shore=image('shore-grit')
def portable(name,im):
 mat=bpy.data.materials.new(name);mat.use_nodes=True;p=mat.node_tree.nodes['Principled BSDF'];p.inputs['Roughness'].default_value=.97
 tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=im;tex.extension='REPEAT';mat.node_tree.links.new(tex.outputs['Color'],p.inputs['Base Color']);return mat
materials=[portable('Dirty irregular grass at world scale',grass),portable('Compacted gritty worn footpath',dirt),portable('Exposed silty pebbled water margin',shore)]
# Portable precomposed derivatives, not glTF-unsupported live shader mixing.
for weight in [.25,.5,.75]:
 im=bpy.data.images.new('Grass tread transition '+str(weight),width=256,height=256)
 a=grass.copy();a.scale(256,256);c=dirt.copy();c.scale(256,256)
 import numpy as np
 aa=np.array(a.pixels[:],dtype=np.float32);cc=np.array(c.pixels[:],dtype=np.float32);im.pixels.foreach_set(aa*(1-weight)+cc*weight);im.pack();bpy.data.images.remove(a);bpy.data.images.remove(c)
 materials.append(portable('Feathered dirty tread '+str(weight),im))
routes=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))['routes']
segments=[((a[0],-a[2]),(c[0],-c[2])) for r in routes for a,c in zip(r['points'],r['points'][1:])]
def distance(p,a,c):
 dx=c[0]-a[0];dy=c[1]-a[1];t=max(0,min(1,((p[0]-a[0])*dx+(p[1]-a[1])*dy)/(dx*dx+dy*dy)))
 return math.hypot(p[0]-a[0]-dx*t,p[1]-a[1]-dy*t)
surfaces=[terrain]+[o for o in scene.objects if o.type=='MESH' and o.name.startswith(('Continuous west forest lakeshore return','Continuous east forest lakeshore return'))]
counts={}
for o in surfaces:
 o.data=o.data.copy();o.data.materials.clear()
 for m in materials:o.data.materials.append(m)
 uv=o.data.uv_layers.new(name='SiteArtWorldTile2m');o.data.uv_layers.active=uv
 for layer in o.data.uv_layers:layer.active_render=layer.name==uv.name
 for loop in o.data.loops:
  p=o.matrix_world@o.data.vertices[loop.vertex_index].co;uv.data[loop.index].uv=(p.x/2,p.y/2)
 for p in o.data.polygons:
  points=[o.matrix_world@o.data.vertices[i].co for i in p.vertices];center=sum(points,Vector())/len(points)
  # Whole face becomes bare shore if any corner enters a .35m water buffer.
  if min(v.z for v in points)<=-1.60:p.material_index=2
  else:
   d=min(distance(center,a,c) for a,c in segments);noise=.11*math.sin(center.x*2.7+math.sin(center.y*2.1))
   p.material_index=1 if d<.42+noise else 5 if d<.64+noise else 4 if d<.86+noise else 3 if d<1.10+noise else 0
  counts[p.material_index]=counts.get(p.material_index,0)+1
 o['site_art_surface']='2m tiled dirty grass/tread; actual-height bare shore; preserved terrain geometry'
 # Terrain already has many primitives; material partition keeps loader indices bounded.
 export(o,'level_camp_siteart_ground' if o==terrain else 'level_camp_siteart_'+('west' if 'west' in o.name else 'east')+'_shore')
assert before==digest(terrain),'Material pass changed physical terrain'
# Replace only the memorial mesh, preserving its actual supporting foundation/pose.
o=bpy.data.objects['Camp wardens heron memorial'];pose=o.matrix_world.copy()
with bpy.data.libraries.load(str(ROOT/'development/levels/blender/level_camp_heron_memorial_organic.blend'),link=False) as (src,dst):dst.objects=['level_camp_heron_memorial_organic']
t=dst.objects[0];o.vertex_groups.clear();o.data=t.data.copy()
for g in t.vertex_groups:o.vertex_groups.new(name=g.name)
bpy.data.objects.remove(t,do_unlink=True);o['organic_source']='level_camp_heron_memorial_organic';assert o.matrix_world==pose
# Geologically varied low-profile stones, all grounded on actual triangles.
rockmat=portable('Weathered lake stones with grit and lichen',shore)
rng=random.Random(241024);rocks=[];attempts=0
props=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'))['placements'];occupied=[]
for name in [p['name'] for p in props]+['west-pitched-ridge','east-pitched-dome','Camp wardens heron memorial']:
 prop=bpy.data.objects[name];bounds=[prop.matrix_world@Vector(c) for c in prop.bound_box]
 occupied.append((min(p.x for p in bounds)-1.4,max(p.x for p in bounds)+1.4,min(p.y for p in bounds)-1.4,max(p.y for p in bounds)+1.4))
while len(rocks)<78 and attempts<4000:
 attempts+=1;x=rng.uniform(1,61);y=rng.uniform(-67,-11);z=height(x,y)
 if z is None or z< -2.05:continue
 if any(a<x<b and c<y<d for a,b,c,d in occupied):continue
 d=min(distance((x,y),a,c) for a,c in segments)
 if d<2.3 or (23<x<41 and -52<y< -32):continue
 if any((x-a)**2+(y-b)**2<r*r for a,b,r in [(8.8,-32.8,3.8),(51,-43,3.8),(8.5,-22,3.8),(20.2,-20.5,3.8),(20.4,-46,3.8),(45.3,-30.3,3.8),(23,-52,3.8),(46.3,-61.2,3.8),(5,-50.7,3),(8,-12,4),(49,-17,5)]):continue
 bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2,radius=1,location=(x,y,z));rock=bpy.context.object;rock.name='Grounded lake stone '+str(len(rocks)+1).zfill(2)
 scale=rng.uniform(.16,.63) if len(rocks)%5 else rng.uniform(.75,1.1)
 for v in rock.data.vertices:
  v.co*=rng.uniform(.86,1.12);v.co.x*=scale;v.co.y*=scale*rng.uniform(.70,1.15);v.co.z*=scale*.65
 low=min(v.co.z for v in rock.data.vertices);rock.location.z-=low;rock.location.z-=.045
 rock.rotation_euler.z=rng.uniform(0,math.tau);rock.data.materials.append(rockmat);s.uv_project(rock)
 bpy.context.view_layer.update();clearances=[]
 for v in rock.data.vertices:
  w=rock.matrix_world@v.co;g=height(w.x,w.y)
  if g is not None:clearances.append(w.z-g)
 assert clearances;rock.location.z-=min(clearances)+.025
 for p in rock.data.polygons:p.use_smooth=True
 rock['role']='Decorative rock; no hiding socket';rocks.append(rock)
# Far bank grounded on its actual return terrain, not a made-up flat height.
for bank in surfaces[1:]:
 bank.data.calc_loop_triangles();tree=BVHTree.FromPolygons([bank.matrix_world@v.co for v in bank.data.vertices],[tuple(t.vertices) for t in bank.data.loop_triangles],all_triangles=True)
 lo=[min((bank.matrix_world@v.co)[a] for v in bank.data.vertices) for a in range(3)];hi=[max((bank.matrix_world@v.co)[a] for v in bank.data.vertices) for a in range(3)]
 for i in range(20):
  x=rng.uniform(lo[0],hi[0]);y=rng.uniform(lo[1],hi[1]);hit=tree.ray_cast(Vector((x,y,50)),Vector((0,0,-1)),100)[0]
  if hit is None:continue
  rock=rocks[i%len(rocks)].copy();rock.data=rock.data.copy();scene.collection.objects.link(rock);rock.name='Distant bank boulder '+bank.name+' '+str(i);rock.scale=(1.5,1.3,1.2);rock.location=(x,y,hit.z-min(v.co.z for v in rock.data.vertices)*rock.scale.z-.10)
  bpy.context.view_layer.update();gaps=[]
  for v in rock.data.vertices:
   w=rock.matrix_world@v.co;g=tree.ray_cast(Vector((w.x,w.y,50)),Vector((0,0,-1)),100)[0]
   if g is not None:gaps.append(w.z-g.z)
  if len(gaps)!=len(rock.data.vertices):bpy.data.objects.remove(rock,do_unlink=True);continue
  rock.location.z-=min(gaps)+.025;rocks.append(rock)
# Keep rocks as individually inspectable native instances; export combined world mesh.
bpy.ops.object.select_all(action='DESELECT')
for rock in rocks:rock.select_set(True)
copies=[]
for rock in rocks:
 c=rock.copy();c.data=rock.data.copy();scene.collection.objects.link(c);copies.append(c);rock.select_set(False);c.select_set(True)
bpy.context.view_layer.objects.active=copies[0];bpy.ops.object.join();combined=bpy.context.object;combined.name='Site art rock collection world origin';bpy.ops.object.transform_apply(location=True,rotation=True,scale=True);export(combined,'level_camp_siteart_rocks');bpy.data.objects.remove(combined,do_unlink=True)
# Five physically offset cubes, UV mask on each outward face only.
# Their native inspection layer is hidden until a UV review explicitly reveals it.
transparent=bpy.data.materials.new('UV carrier invisible inner and edge faces');transparent.use_nodes=True
p=transparent.node_tree.nodes['Principled BSDF'];p.inputs['Alpha'].default_value=0;transparent.surface_render_method='DITHERED'
clear=bpy.data.images.new('Transparent carrier edge PNG',width=2,height=2,alpha=True);clear.pixels.foreach_set([0.0]*16);clear.pack()
tx=transparent.node_tree.nodes.new('ShaderNodeTexImage');tx.image=clear;transparent.node_tree.links.new(tx.outputs['Color'],p.inputs['Base Color']);transparent.node_tree.links.new(tx.outputs['Alpha'],p.inputs['Alpha'])
foot=bpy.data.images.new('Original webbed goose footprint mask',width=128,height=128,alpha=True);pixels=[]
def tri(x,y,a,b,c):
 def cross(p,q,r):return (q[0]-p[0])*(r[1]-p[1])-(q[1]-p[1])*(r[0]-p[0])
 d=[cross(a,b,(x,y)),cross(b,c,(x,y)),cross(c,a,(x,y))];return min(d)>=0 or max(d)<=0
for j in range(128):
 for i in range(128):
  x=(i+.5)/128;y=(j+.5)/128
  inside=tri(x,y,(.46,.16),(.15,.76),(.5,.66)) or tri(x,y,(.46,.16),(.5,.93),(.65,.67)) or tri(x,y,(.46,.16),(.67,.65),(.89,.73))
  pixels.extend((.12,.95,.39,1 if inside else 0))
foot.pixels.foreach_set(pixels);foot.pack();fm=portable('UV cosmetic goose track outer face only',foot);p=fm.node_tree.nodes['Principled BSDF'];tex=next(n for n in fm.node_tree.nodes if n.type=='TEX_IMAGE');fm.node_tree.links.new(tex.outputs['Alpha'],p.inputs['Alpha']);fm.surface_render_method='DITHERED'
parts=[];attachments=[]
for n,(center,normal) in enumerate([((-.18,-.43,1.08),(0,-1,0)),((.16,-.43,.50),(0,-1,0)),((.43,-.18,.70),(1,0,0)),((.43,.18,.95),(1,0,0)),((.33,-.26,1.27),(0,0,1))]):
 normal=Vector(normal);center=Vector(center);basis=normal.to_track_quat('Z','Y');cube=s.box('UV goose footprint surface carrier '+str(n+1),(0,0,0),(.09,.13,.002),transparent)
 cube.data.materials.append(fm)
 for poly in cube.data.polygons:
  if poly.normal.z>.9:
   poly.material_index=1
   for li in poly.loop_indices:
    co=cube.data.vertices[cube.data.loops[li].vertex_index].co;cube.data.uv_layers.active.data[li].uv=(co.x/.09+.5,co.y/.13+.5)
 for v in cube.data.vertices:v.co=center+normal*.0015+basis@v.co
 parts.append(cube);attachments.append(dict(center=list(center),outward_normal=list(normal),inner_offset_m=.0005,outer_offset_m=.0025))
marks=s.join(parts,'Cosmetic UV goose footprint carriers');marks['role']='Cosmetic location lore; not ghost evidence';export(marks,'level_camp_memorial_uv_tracks');marks.matrix_world=pose;marks.hide_render=True;marks.hide_set(True)
target=OUT/'hollow-creek-siteart.blend';scene['production_status']='Site art second pass; native only, runtime integration remains pending';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
assert hashlib.sha256(parent.read_bytes()).hexdigest()==protected
report=dict(status='native site art; independent visual/geometry checks pending',scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),parent=parent.relative_to(ROOT).as_posix(),parent_sha256=protected,terrain_geometry_sha256=before,terrain_unchanged=True,world_tile_m=2,water_height=-1.95,grass_exclusion_below=-1.6,material_faces=counts,rocks=len(rocks),assets=records,uv_cosmetic_attachments=attachments,remaining=['Tent headings and full renewed placement validation','Actual UV/range/occlusion/host/LAN encounter trigger; marks hidden in ordinary native scene','Independent runtime/source checks; no map unlock'])
(OUT/'site-art.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('SITE ART',len(rocks),'supported stones; terrain geometry preserved',flush=True)
