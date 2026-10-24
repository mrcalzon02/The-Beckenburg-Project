"""Grounded layered scenery beyond the desquareified berms; no gameplay hiding grants."""
import bpy,sys,json,math,hashlib,random
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import hollow_creek_organic as organic
import rebuild_support as s
OUT=ROOT/'development/levels/hollow-creek';PRE=ROOT/'development/levels/previews/hollow-creek';d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'))
source=OUT/'hollow-creek-boundaries.blend';protected={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest() for p in [source,OUT/'organic-terrain.json',ROOT/'assets/runtime/models/level_tree_conifer_worn.glb',ROOT/'assets/runtime/models/level_tree_broadleaf_worn.glb']}
bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0
templates={}
for kind in ['conifer','broadleaf']:
 file=ROOT/f'development/levels/blender/level_tree_{kind}_worn.blend'
 with bpy.data.libraries.load(str(file),link=False) as (src,dst):dst.objects=[name for name in src.objects if name==f'level_tree_{kind}_worn']
 obj=dst.objects[0];assert obj is not None;templates[kind]=obj
 # Library sources already have floor pivots and correct native metre geometry.
rng=random.Random(23024);placements=[];patches=[]
soil=s.material('Scenery rooted forest floor',(.15,.20,.10),'paint')
def scenery_height(x,z,off):
 t=max(0,min(1,(off-3)/20));t=t*t*(3-2*t)
 return organic.height(x,z,d['routes'])+t*(.025*max(0,off-5)+.45*math.sin(x*.07+z*.03))
def envelope(x,z):
 # All tree origins remain outside the entire proposed land/play envelope.
 return not organic.inside(x,z) and z>8
ground_objects=[]
for side,path in [('west',organic.WEST),('east',organic.EAST)]:
 vv=[];ff=[];stations=[]
 for a,b in zip(path,path[1:]):
  n=max(1,math.ceil(math.dist(a,b)/2))
  stations.extend([[a[0]+(b[0]-a[0])*i/n,a[1]+(b[1]-a[1])*i/n] for i in range(n)])
 stations.append(path[-1]);offsets=[1.5,3,6,12,24,45,75]
 for i,(x,z) in enumerate(stations):
  a=stations[max(0,i-1)];b=stations[min(len(stations)-1,i+1)];dx=b[0]-a[0];dz=b[1]-a[1];length=math.hypot(dx,dz);nx=-dz/length;nz=dx/length
  for off in offsets:
   px=x+nx*off;pz=z+nz*off;vv.append((px,-pz,scenery_height(px,pz,off)))
  if i:
   for j in range(len(offsets)-1):
    k=(i-1)*len(offsets)+j;ff.extend([(k,k+len(offsets),k+len(offsets)+1),(k,k+len(offsets)+1,k+1)])
 ground_obj=s.mesh('Non-playable '+side+' forest terrain apron',vv,ff,soil);ground_obj['role']='Scenery-only terrain extension beyond berm; not navigable map support';ground_objects.append(ground_obj)

 # Sample outward from real, irregular boundaries. Omit lake-facing end sectors.
 for layer,offset,spacing in [('near',6.5,5.0),('middle',20,9.0),('far',55,14.0)]:
  for a,b in zip(path,path[1:]):
   dx=b[0]-a[0];dz=b[1]-a[1];length=math.hypot(dx,dz);nx=-dz/length;nz=dx/length;n=max(1,int(length/spacing))
   for i in range(n):
    t=(i+.5)/n;off=offset+rng.uniform(-1.3,1.3);x=a[0]+dx*t+nx*off;z=a[1]+dz*t+nz*off
    if not envelope(x,z):continue
    if any(math.hypot(x-p['x'],z-p['z'])<3 for p in placements):continue
    y=scenery_height(x,z,off)
    kind='broadleaf' if rng.random()<.24 else 'conifer';scale=rng.uniform(1.05,1.55) if layer=='near' else rng.uniform(1.35,2.1)
    obj=templates[kind].copy();obj.data=templates[kind].data;bpy.context.scene.collection.objects.link(obj);obj.name=f'Forest {layer} {side} {len(placements):03}';obj.location=(x,-z,y-.08);obj.rotation_euler.z=rng.random()*math.tau;obj.scale=(scale,scale,scale);obj['role']='Scenery only beyond physical berm; no collision/hiding/evidence grant';obj['forest_layer']=layer
    radius=max(2.2,scale*1.7);verts=[(x,-z,y)]
    for j in range(12):
     angle=j*math.tau/12;r=radius*(1+.12*math.sin(j*2.3));verts.append((x+r*math.cos(angle),-z+r*math.sin(angle),y-.04-.06*math.sin(angle)**2))
    faces=[(0,j+1,(j+1)%12+1) for j in range(12)];patch=s.mesh('Grounded scenery root mound '+str(len(placements)),verts,faces,soil);patch['role']='Non-playable root support; scenery-only terrain extension';patches.append(patch)
    placements.append(dict(name=obj.name,kind=kind,layer=layer,x=x,z=z,ground=y,bottom=y-.08,scale=scale,source=f'development/levels/blender/level_tree_{kind}_worn.blend',outside_play_area=True))
# Fit every tree to the actual extended native triangles rather than a height estimate.
vv=[];ff=[]
for obj in ground_objects:
 obj.data.calc_loop_triangles();base=len(vv);vv.extend(obj.matrix_world@v.co for v in obj.data.vertices);ff.extend(tuple(base+i for i in f.vertices) for f in obj.data.loop_triangles)
ground_bvh=BVHTree.FromPolygons(vv,ff,all_triangles=True)
for place,patch in zip(placements,patches):
 hit=ground_bvh.ray_cast(Vector((place['x'],-place['z'],40)),Vector((0,0,-1)),100)[0]
 if hit is None:raise ValueError(('Tree outside actual scenery apron',place['name']))
 delta=hit.z-place['ground'];bpy.data.objects[place['name']].location.z+=delta
 for v in patch.data.vertices:v.co.z+=delta
 place['ground']=hit.z;place['bottom']=hit.z-.08;place['native_apron_contact']=True
# A curved rear treeline leaves the van gate/road vista readable.
# No lake-front curtain: north/water sectors deliberately remain open.
scene=bpy.context.scene;scene['production_status']='Desquareified terrain, canonical van and layered off-map forest; runtime pending';scene.render.engine='CYCLES';scene.cycles.samples=16;scene.cycles.use_denoising=True;scene.render.resolution_x=1200;scene.render.resolution_y=800;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
fill=bpy.data.objects.get('Boundary inspection fill only')
def camera(name,pos,target,lens):
 scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hollow-creek-forest.blend'),compress=True)
camera('forest-organic-site-native-fitout',(105,-125,100),(31,-42,1),40)
camera('forest-van-and-roadhead-native-fitout',(43,-85,5),(32,-76,2.8),30)
camera('forest-camp-trail-native-fitout',(32,-64,2.5),(20,-40,1.5),24)
camera('forest-west-berm-native-fitout',(8,-37,1.7),(1,-40,3.2),23)
camera('forest-open-lake-native-fitout',(32,-14,.32),(30,10,.2),24)
report=dict(status='native layered off-map foliage; runtime scenery integration pending',placements=placements,root_mounds=len(patches),continuous_scenery_aprons=len(ground_objects),layers={key:sum(p['layer']==key for p in placements) for key in ['near','middle','far']},source_hashes=protected,canonical_van_visible=True,open_lake_sector=True,limitations=['No new gameplay hiding pockets or inside-map forest collision.','Root support mounds are scenery-only extensions, not navigable ground.','Distant native geometry is not yet exported/instanced in runtime; source tree GLBs are reused unchanged.','No per-map runtime sky mask or full human/LAN/performance acceptance.'])
(OUT/'forest.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
for path,h in protected.items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==h,path
print('BUILT',len(placements),'grounded reused trees in three off-map layers',report['layers'],flush=True)
