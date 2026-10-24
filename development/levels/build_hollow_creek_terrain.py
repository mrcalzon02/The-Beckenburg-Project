"""Build site foundation first; retain structures and prior files without mutation."""
import bpy,bmesh,sys,json,hashlib,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import hollow_creek_ground as ground
import build_lived_assets as b
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek'
protected_paths=[OUT/'hollow-creek-pavilion.blend',OUT/'pavilion.json',ROOT/'development/levels/layouts/wld-02/layout.json',*sorted((ROOT/'development/levels/blockouts').glob('*/rough-shell.glb'))]
protected={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in protected_paths}
d=ground.make();(OUT/'terrain.json').write_text(json.dumps(d,separators=(',',':'))+'\n',encoding='utf-8')
m=b.init();soil=s.material('Camp earth and moss',(.17,.21,.12),'paint');gravel=s.material('Camp graded path',(.30,.27,.20),'paint')
nx,nz=d['nx'],d['nz'];x0,z0,_,_=d['bounds'];step=d['step']
vertices=[(d['xs'][i],-d['zs'][j],d['heights'][j*nx+i]) for j in range(nz) for i in range(nx)];faces=[];materials=[]
for j in range(nz-1):
 for i in range(nx-1):
  x,z=(d['xs'][i]+d['xs'][i+1])/2,(d['zs'][j]+d['zs'][j+1])/2
  a,c,e,f=j*nx+i,j*nx+i+1,(j+1)*nx+i+1,(j+1)*nx+i
  # The pavilion's physical slab/apron own the surface; omit terrain directly beneath its visible floor.
  if 24<x<40 and 34<z<50:continue
  faces.extend([(a,e,c),(a,f,e)])
  path=min(ground.distance(x,z,a,b) for l in d['routes'] for a,b in zip(l['points'],l['points'][1:]))<.55
  materials.extend([(1 if path else 0)+(2 if x>=30 else 0)]*2)
mesh=bpy.data.meshes.new('Continuous authored site terrain');mesh.from_pydata(vertices,[],faces);mesh.update();obj=bpy.data.objects.new('Site ground',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(soil);mesh.materials.append(gravel);mesh.materials.append(soil.copy());mesh.materials.append(gravel.copy())
for poly,mat in zip(mesh.polygons,materials):poly.material_index=mat;poly.use_smooth=True
# Shared planar UVs and spatial primitive partitions respect the existing GLB loader
# limits without increasing engine budgets or duplicating face-specific normals.
uv=mesh.uv_layers.new(name='Site metre UV')
for loop in mesh.loops:
 v=mesh.vertices[loop.vertex_index].co;uv.data[loop.index].uv=(v.x*.25,v.y*.25)
b.export('level_camp_terrain',[obj],'campground',view=(100,-160,150));asset=b.LOG[-1];asset['mount']='World-site origin; do not centre/floor normalize. Negative lake and creek bed elevations are intentional.'
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-pavilion.blend'));bpy.context.preferences.filepaths.save_version=0
# Remove schematic geometry only in this new derived scene. Preserve every reviewed facility and pavilion mesh.
removed=[]
for obj in list(bpy.context.scene.objects):
 if obj.type=='MESH' and obj.name!='level_camp_pavilion' and not obj.get('facility_id') and not obj.name.startswith('Pavilion apron '):
  removed.append(obj.name);bpy.data.objects.remove(obj,do_unlink=True)
with bpy.data.libraries.load(str(ROOT/asset['editable']),link=False) as (incoming,outgoing):outgoing.objects=[asset['id']]
terrain=outgoing.objects[0];bpy.context.scene.collection.objects.link(terrain);terrain['role']='authoritative site surface; runtime integration pending'
water=s.material('Camp water foundation',(.035,.12,.15),'paint');timber=s.material('Camp crossing foundation',(.22,.17,.10),'wood')
def cube(name,center,size,mat,role):
 obj=s.box(name,center,size,mat);obj['role']=role;return obj
cube('Lake water boundary',(30,7,-1.97),(68,46,.04),water,'water surface is not walkable ground')
verts=[];quads=[]
for i in range(49):
 z=2+i*.55;center=ground.creek_center(z);verts.extend([(center-1.5,-z,d['water_y']),(center+1.5,-z,d['water_y'])])
 if i:quads.append((2*i-2,2*i,2*i+1,2*i-1))
wm=bpy.data.meshes.new('Meandering creek water');wm.from_pydata(verts,[],quads);wm.update();wo=bpy.data.objects.new('Creek water boundary',wm);bpy.context.scene.collection.objects.link(wo);wm.materials.append(water);s.uv_project(wo);wo['role']='water surface is not walkable ground' 
for deck in d['decks']:
 a,b,c,e=deck['bounds'];cube(deck['id'],((a+c)/2,-(b+e)/2,deck['top']-.09),(c-a,e-b,.18),timber,'temporary supported infrastructure; final guard/detail pending')
 for x in [a+.3,c-.3]:
  for z in [b+.4,e-.4]:
   bed=ground.height(x,z,json.loads(ground.LAYOUT.read_text(encoding='utf-8')),d['routes']);bottom=min(bed-.25,deck['top']-.5)
   cube(deck['id']+' footing', (x,-z,(bottom+deck['top']-.18)/2),(.22,.22,deck['top']-.18-bottom),timber,'foundation post reaches terrain bed')
for name,y in d['facility_elevations'].items():bpy.data.objects[name].location.z=y
# Wire boundaries retain readable reservations without floating floor plates or fake walls.
for zone in d['zones']:
 a,b,c,e=zone['bounds'];curve=bpy.data.curves.new('Reservation '+zone['id'],'CURVE');curve.dimensions='3D';curve.bevel_depth=.012;curve.bevel_resolution=0
 poly=curve.splines.new('POLY');poly.points.add(4)
 for point,(x,z) in zip(poly.points,[(a,b),(c,b),(c,e),(a,e),(a,b)]):point.co=(x,-z,zone['y']+.035,1)
 obj=bpy.data.objects.new('Reservation '+zone['id'],curve);bpy.context.scene.collection.objects.link(obj);obj['role']='plan reservation, not collision geometry';obj.hide_render=True
for o in bpy.context.scene.objects:
 if o.type=='FONT':o.hide_render=True
scene=bpy.context.scene;scene['production_status']='Terrain-first site foundation review; unfinished runtime map'
fill=bpy.data.objects.get('Inspection-only camera fill')
if fill:fill.data.energy=0
for light in [o for o in scene.objects if o.type=='LIGHT' and o.data.type=='SUN']:light.data.energy=3
scene.world.node_tree.nodes['Background'].inputs[1].default_value=.6
scene.render.engine='CYCLES';scene.cycles.samples=24;scene.cycles.use_denoising=True;scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
def camera(name,position,target,lens=40):
 scene.camera.location=position;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=lens;scene.render.filepath=str(PRE/(name+'.png'))
camera('terrain-site-native-fitout',(106,-118,110),(29,-36,0),45)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-terrain.blend'),compress=True);bpy.ops.render.render(write_still=True)
camera('terrain-water-crossings-native-fitout',(70,25,30),(37,-9,-.2),42);bpy.ops.render.render(write_still=True)
camera('terrain-van-approach-native-fitout',(47,-88,13),(30,-65,0),40);bpy.ops.render.render(write_still=True)
for o in scene.objects:
 if o.type in ['CURVE','FONT']:o.hide_render=True
camera('terrain-worn-trail-native-fitout',(16.5,-27.5,0),(13.7,-28,-.6),28);bpy.ops.render.render(write_still=True)
report=dict(status='site-foundation-native-review',asset=asset,protected_sources=protected,removed_schematic_meshes=removed,terrain_contract='development/levels/hollow-creek/terrain.json',limitations=d['limitations'])
(OUT/'terrain-assets.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,digest in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
print('PASS authored ground, carved lake/creek, supported crossing foundations and preserved source checkpoints',flush=True)
