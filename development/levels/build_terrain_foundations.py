"""Breadth-first native site foundations. Original layouts/blockouts are read-only.
Blender --background --factory-startup --python-exit-code 1 --python this.py -- keys
"""
import bpy,json,sys,math,hashlib,random
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import terrain_foundation_profiles as profiles
import build_lived_assets as b
import rebuild_support as s
OUT=ROOT/'development/levels/terrain-foundations';OUT.mkdir(exist_ok=True)
PRE=ROOT/'development/levels/previews/terrain-foundations';PRE.mkdir(exist_ok=True)
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def rd(x,z,r):return math.hypot(max(r[0]-x,0,x-r[2]),max(r[1]-z,0,z-r[3]))
def distance(x,z,a,c):
 dx,dz=c[0]-a[0],c[2]-a[2];n=dx*dx+dz*dz;t=max(0,min(1,((x-a[0])*dx+(z-a[2])*dz)/n)) if n else 0
 return math.hypot(x-a[0]-dx*t,z-a[2]-dz*t),a[1]*(1-t)+c[1]*t

def build(key):
 layout_path=ROOT/f'development/levels/layouts/{key}/layout.json';rough=ROOT/f'development/levels/blockouts/{key}/rough-shell.blend';glb=rough.with_suffix('.glb')
 profile=profiles.PROFILES[key];layout=json.loads(layout_path.read_text(encoding='utf-8'));zones={q['id']:q for q in layout['zones']}
 folder=OUT/key;folder.mkdir(exist_ok=True);preview=PRE/key;preview.mkdir(exist_ok=True)
 protected={p.relative_to(ROOT).as_posix():digest(p) for p in [layout_path,rough,glb,ROOT/'src/main/java/org/beckenburg/LevelPlans.java']}
 below=set(profile['below']);deck=set(profile['deck'])
 # Select the lowest structural foundation when source floors overlap in X/Z.
 pads=[dict(id=q['id'],bounds=q['bounds'],y=q['y']) for q in layout['zones'] if (q['kind'] in ['inside','circulation','staging']) and q['id'] not in below|deck and q['y']>=0]
 def pad_at(x,z):
  inside=[p for p in pads if rd(x,z,p['bounds'])<1e-7]
  return min(inside,key=lambda p:p['y']) if inside else None
 def target(q):
  if q['kind'] in ['inside','circulation','staging'] or q['id'] in deck|below or q['y']!=0 or key in ['wld-03','wld-04','wld-09']:return q['y']
  a,c,e,f=q['bounds'];base=profiles.base(key,(a+e)/2,(c+f)/2)
  return round(max(-1.2,min(1.2,base*(.25 if q['id']=='entry' else .65))),4)
 levels={q['id']:target(q) for q in layout['zones']};links=[];surface=[]
 for link in layout['links']:
  obj=json.loads(json.dumps(link));points=obj['points'];lengths=[math.dist(a,c) for a,c in zip(points,points[1:])];total=sum(lengths) or 1;travel=0
  revised=[]
  for i,point in enumerate(points):
   if i:travel+=lengths[i-1]
   if all(abs(p[1])<1e-8 for p in points):y=levels[link['a']]*(1-travel/total)+levels[link['b']]*(travel/total)
   else:y=point[1]
   fixed=pad_at(point[0],point[2])
   if fixed and link['a'] not in below|deck and link['b'] not in below|deck:y=fixed['y']
   revised.append([point[0],round(y,5),point[2]])
  obj['points']=revised;links.append(obj)
  if link['a'] not in below|deck and link['b'] not in below|deck:
   for a,c in zip(revised,revised[1:]):
    if math.hypot(c[0]-a[0],c[2]-a[2])>.05:surface.append((a,c,link['width'],link['id']))
 def h(x,z):
  value=profiles.base(key,x,z);best=0;level=0
  for pad in sorted(pads,key=lambda p:p['y']):
   w=1-profiles.smooth(rd(x,z,pad['bounds'])/2.5)
   if w>best:best=w;level=pad['y']
  value=value*(1-best)+level*best
  candidates=[]
  for a,c,width,ident in surface:
   dist,y=distance(x,z,a,c);candidates.append((dist-width/2,dist,y))
  if candidates:
   nearest=min(candidates);weights=[]
   for gap,dist,y in candidates:
    if gap<nearest[0]+2 and gap<3:weights.append(((1-profiles.smooth((gap-nearest[0])/2))/(dist*dist+1),y))
   path=sum(w*y for w,y in weights)/sum(w for w,y in weights) if weights else nearest[2]
   blend=1-profiles.smooth((nearest[0]-.5)/1.4)
   fixed=pad_at(x,z);depth=.1*profiles.smooth(min([rd(x,z,p['bounds']) for p in pads] or [3])/2)
   value=value*(1-blend)+(path+depth)*blend-depth*(1-profiles.smooth((nearest[1]-.45)/1.25))
   if fixed:value=fixed['y']
  # Never turn a water site into dry land to fake deck support.
  if 'north_water' in profile:
   shore=profile['north_water']+.7*math.sin((x-32)*.14)
   if z<shore:value=min(value,profile['water']-1.0*profiles.smooth((shore-z)/3))
  if 'water_rect' in profile and rd(x,z,profile['water_rect'])<1e-8:value=min(value,profile['water']-1)
  if key=='wld-12':
   if 26<x<42 and 23<z<41:value=min(value,-5+.25*math.sin(z*.3))
  for zid in deck-below:
   q=zones[zid]
   if rd(x,z,q['bounds'])<1e-7:value=min(value,q['y']-.18)
  return round(value,6)
 x0,z0,x1,z1=layout['site_bounds'];nx=round(x1-x0)+1;nz=round(z1-z0)+1
 verts=[(x0+i,-(z0+j),h(x0+i,z0+j)) for j in range(nz) for i in range(nx)];faces=[];mats=[]
 def baked_h(x,z):
  gx=max(0,min(nx-1,x-x0));gz=max(0,min(nz-1,z-z0));i=min(nx-2,int(gx));j=min(nz-2,int(gz));tx=gx-i;tz=gz-j
  a=verts[j*nx+i][2];c=verts[j*nx+i+1][2];e=verts[(j+1)*nx+i+1][2];f=verts[(j+1)*nx+i][2]
  return a+(c-a)*tx+(e-c)*tz if tz<=tx else a+(e-f)*tx+(f-a)*tz
 # Only building foundations own/cut the visible soil. Raised piers and lower
 # floors retain independent source layers over/under the continuous ground.
 for j in range(nz-1):
  for i in range(nx-1):
   x,z=x0+i+.5,z0+j+.5
   if pad_at(x,z):continue
   a=j*nx+i;c=a+1;e=a+nx+1;f=a+nx;faces.extend([(a,e,c),(a,f,e)])
   trail=min([distance(x,z,a,c)[0] for a,c,w,k in surface] or [999])<.6;mats.extend([1 if trail else 0]*2)
 materials=b.init();soil=s.material('Terrain '+key,profile['color'],'paint');track=s.material('Tread '+key,tuple(v*.82 for v in profile['color']),'paint')
 for mat in [soil,track]:
  image=next(n.image for n in mat.node_tree.nodes if n.type=='TEX_IMAGE');rng=random.Random(key+mat.name);pixels=[]
  for y in range(64):
   for x in range(64):
    factor=1+rng.uniform(-.045,.045);color=profile['color'] if mat==soil else tuple(v*.82 for v in profile['color']);pixels.extend([c*factor for c in color]+[1])
  image.pixels.foreach_set(pixels);image.pack()
 mesh=bpy.data.meshes.new('Site ground '+key);mesh.from_pydata(verts,[],faces);mesh.update();obj=bpy.data.objects.new('Terrain '+key,mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(soil);mesh.materials.append(track)
 for face,material in zip(mesh.polygons,mats):face.material_index=material;face.use_smooth=True
 uv=mesh.uv_layers.new(name='World metre UV')
 for loop in mesh.loops:
  v=mesh.vertices[loop.vertex_index].co;uv.data[loop.index].uv=(v.x*.25,v.y*.25)
 model='level_site_'+key.replace('-','_')+'_terrain';b.export(model,[obj],'terrain-foundation',view=(150,-200,170));asset=b.LOG[-1]
 bpy.ops.wm.open_mainfile(filepath=str(rough));bpy.context.preferences.filepaths.save_version=0
 floors=[];masked=[]
 for o in list(bpy.context.scene.objects):
  if o.type in ['FONT','CURVE']:o.hide_render=True
  if o.type!='MESH':continue
  zone=o.get('zone');role=o.get('role','')
  if o.name.startswith('Floor '):
   zid=o.get('zone',o.name[6:].split('.')[0]);q=zones.get(zid)
   keep=q and (q['kind'] in ['inside','circulation','staging'] or zid in deck)
   o.hide_render=not keep
   if keep:floors.append(o.name)
  elif role in ['rough shell wall','rough opening header'] or o.name.startswith(('Wall ','Header ')):o.hide_render=True
  else:o.hide_render=True
 with bpy.data.libraries.load(str(ROOT/asset['editable']),link=False) as (incoming,outgoing):outgoing.objects=[model]
 ground=outgoing.objects[0];bpy.context.scene.collection.objects.link(ground);ground['role']='broad terrain foundation; runtime integration pending'
 # Add buried footings under over-water source deck/floor corners. Do not import
 # investigator equipment, unlock maps or replace source blockout geometry.
 watermat=s.material('Water foundation '+key,(.045,.13,.16),'paint');support=s.material('Support foundation '+key,(.21,.18,.13),'wood');piles=[]
 if 'north_water' in profile:
  high=profile['north_water']+1;low=z0-2
  wo=s.box('Water foundation '+key,((x0+x1)/2,-(low+high)/2,profile['water']-.02),(x1-x0+4,high-low,.04),watermat);wo['role']='opaque native flood/shore preview; not walkable'
 elif 'water_rect' in profile:
  a,c,e,f=profile['water_rect'];wo=s.box('Water foundation '+key,((a+e)/2,-(c+f)/2,profile['water']-.02),(e-a,f-c,.04),watermat);wo['role']='opaque native tailrace preview; not walkable'
 if 'water' in profile:
  for zid in deck:
   q=zones[zid];a,c,e,f=q['bounds']
   for x,z in [(a+.4,c+.4),(e-.4,c+.4),(a+.4,f-.4),(e-.4,f-.4)]:
    bed=baked_h(x,z)
    if bed>=q['y']-.2:continue
    low=bed-.2;high=q['y']-.08
    o=s.box('Foundation pile '+zid,(x,-z,(low+high)/2),(.22,.22,high-low),support);o['role']='prototype piled support; guard and connection detailing pending';piles.append(dict(id=o.name,zone=zid,x=x,z=z,bottom=low,top=high))
 scene=bpy.context.scene;scene['production_status']='Broad terrain pass only; original blockout and gameplay acceptance not replaced'
 if scene.camera is None:bpy.ops.object.camera_add();scene.camera=bpy.context.object
 if not any(o.type=='LIGHT' for o in scene.objects):bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.45,-.5,-.6);bpy.context.object.data.energy=2.5
 if scene.world is None:scene.world=bpy.data.worlds.new('Terrain inspection world')
 scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.6
 scene.render.engine='CYCLES';scene.cycles.samples=16;scene.cycles.use_denoising=True;scene.render.resolution_x=1000;scene.render.resolution_y=700;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
 cx,cz=(x0+x1)/2,(z0+z1)/2;span=max(x1-x0,z1-z0)
 def camera(name,position,target,lens=42):
  scene.camera.location=position;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.type='PERSP';scene.camera.data.lens=lens;scene.render.filepath=str(preview/(name+'.png'))
 for o in scene.objects:
  if o.type=='MESH' and o.name.startswith('Floor '):
   q=zones.get(o.get('zone',o.name[6:].split('.')[0]));o.hide_render=o.name not in floors or (q['id'] not in deck and q['y']!=min([p['y'] for p in pads if rd((q['bounds'][0]+q['bounds'][2])/2,(q['bounds'][1]+q['bounds'][3])/2,p['bounds'])<1e-7] or [q['y']]))
 for o in scene.objects:
  if o.type=='MESH' and o.name.startswith('Floor '):
   q=zones.get(o.get('zone',o.name[6:].split('.')[0]))
   if q['id'] in deck and q['y']>0 and 'water' not in profile:o.hide_render=True
 camera('site-native-fitout',(cx+span*.72,-cz-span*.80,span*.95),(cx,-cz,1))
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'site-terrain.blend'),compress=True);bpy.ops.render.render(write_still=True)
 lowzones=[q for q in layout['zones'] if q['id'] in below]
 if lowzones:
  ground.hide_render=True
  for o in scene.objects:
   if o.type=='MESH' and o.name.startswith('Floor '):o.hide_render=o.get('zone',o.name[6:].split('.')[0]) not in below
  center=lowzones[0];a,c,e,f=center['bounds'];target=((a+e)/2,-(c+f)/2,center['y'])
  camera('layer-study-native-fitout',(cx+span*.55,-cz-span*.6,span*.65),target,48)
 else:
  p=layout['staging']['point'];camera('approach-native-fitout',(p[0]+16,-p[2]-14,12),(cx,-cz,1),38)
 bpy.ops.render.render(write_still=True)
 contract=dict(status='broad-native-terrain-only',key=key,title=layout['title'],profile=profile,asset=asset,bounds=layout['site_bounds'],staging=layout['staging'],nx=nx,nz=nz,step=1,heights=[v[2] for v in verts],pads=pads,zone_levels=levels,routes=links,source_floor_layers=[dict(id=q['id'],y=q['y'],height=q['height'],bounds=q['bounds'],kind=q['kind']) for q in layout['zones']],excavation_reservations=[dict(id=q['id'],bounds=q['bounds'],floor_y=q['y'],height=q['height'],status='solid-cut/headroom/portal engineering pending') for q in layout['zones'] if q['id'] in below],retained_native_floors=floors,piles=piles,protected_sources=protected,height_range=[min(v[2] for v in verts),max(v[2] for v in verts)],limits=['Rough terrain foundation, not completed architecture, collision, navigation or mission unlock.','Original elevated/underground layers and transition reservations preserved; stairs/headroom/retaining/guard design still needs per-site engineering.','Opaque water previews are not the layered/translucent runtime water implementation.','Native floor/pile support does not certify full-width gameplay traversal, LAN, circuit lighting or performance.'])
 (folder/'contract.json').write_text(json.dumps(contract,indent=2)+'\n',encoding='utf-8')
 for path,sha in protected.items():assert digest(ROOT/path)==sha,path
 print('COMPLETE terrain foundation',key,'triangles',asset['triangles'],'range',contract['height_range'],flush=True)

if __name__=='__main__':
 selected=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else list(profiles.PROFILES)
 for key in selected:build(key)
