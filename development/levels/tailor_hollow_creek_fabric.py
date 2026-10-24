"""Additive cloth-shape refinement; keep exact fitted groundsheet geometry.

Subdivide only cloth, author restrained tension folds, and add supported sewn
seams. No terrain, placement, frame, guy anchor or doorway is moved.
"""
import bpy,bmesh,sys,math,json,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
from mathutils.geometry import delaunay_2d_cdt
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
OUT=ROOT/'development/levels/hollow-creek'
sources=['level_camp_tent_ridge_refined','level_camp_tent_dome_refined','level_camp_bedroll_rolled_authored']
scene_source=OUT/'hollow-creek-authored.blend';protected={scene_source.relative_to(ROOT).as_posix():hashlib.sha256(scene_source.read_bytes()).hexdigest()}
for id in sources:
 for p in [ROOT/f'assets/runtime/models/{id}.glb',ROOT/f'development/levels/blender/{id}.blend']:
  protected[p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()

def floor_group(name):return 'groundsheet' in name.lower() or 'physical floor' in name.lower()

def displacement(p,kind,group=''):
 x,y,z=p
 if kind=='ridge':
  if 'Catenary canvas side' in group:
   t=max(0,min(1,abs(x)/1.3));u=max(0,min(1,(y+1.7)/3.4));fade=math.sin(math.pi*t)*math.sin(math.pi*u)
   return Vector((0,0,fade*(-.034+.007*math.sin(t*29+u*9))))
  if 'Tied-open front flap' in group:
   t=max(0,min(1,(abs(x)-.45)/.85));return Vector((0,.021*math.sin(t*math.pi)*math.sin(z*12),0))
 elif kind=='dome':
  theta=math.atan2(y/1.65,x/1.4);front=abs(math.atan2(math.sin(theta+math.pi/2),math.cos(theta+math.pi/2)))
  if front<.50 and z<1.4:return Vector((0,0,0))
  latitude=math.asin(max(0,min(1,(z-.03)/1.65)))
  pole=math.sin(2*(theta-math.pi/4))**2;fade=math.sin(latitude*2)
  sag=-.030*pole*fade+.005*math.sin(16*theta+4*latitude)*fade*pole
  return Vector((x*sag*.28,y*sag*.28,sag*.65))
 elif kind=='roll':
  if z<.045:return Vector((0,0,0))
  theta=math.atan2(y,z-.17);fade=min(1,(z-.045)/.08)*max(0,1-(abs(x)/.325)**4)
  strap=min(1,abs(abs(x)-.21)/.045)
  amount=.005*math.sin(theta*12+x*13)*fade*strap
  return Vector((0,math.sin(theta)*amount,math.cos(theta)*amount))
 return Vector((0,0,0))

def dome_aperture(o):
 # Author two connected curved hemispheres around an actual arched opening.
 # Keep the existing frame/groundsheet; do not staircase by deleting grid cells.
 gid=next(g.index for g in o.vertex_groups if 'Sewn multi-panel dome canopy' in g.name)
 material=o.data.materials[next(p.material_index for p in o.data.polygons if all(any(a.group==gid for a in o.data.vertices[i].groups) for i in p.vertices))]
 bm=bmesh.new();bm.from_mesh(o.data);layer=bm.verts.layers.deform.active
 bmesh.ops.delete(bm,geom=[v for v in bm.verts if gid in v[layer]],context='VERTS');bm.to_mesh(o.data);bm.free()
 outer=[(1.4*math.cos(i*math.pi/96),.03+1.65*math.sin(i*math.pi/96)) for i in range(97)]
 front=[(-1.4+.86*i/24,.03) for i in range(25)]
 front +=[(-.54,.90)]
 front +=[(-.54*math.cos(i*math.pi/40),.90+.45*math.sin(i*math.pi/40)) for i in range(1,40)]
 front +=[(.54,.90),(.54,.03)]+[(.54+.86*i/24,.03) for i in range(1,25)]+outer[1:]
 rear=[(-1.4+2.8*i/64,.03) for i in range(65)]+outer[1:]
 vertices=[];faces=[]
 for outline,side in [(front,-1),(rear,1)]:
  points=[Vector((x,z)) for x,z in outline]
  if (points[0]-points[-1]).length<1e-6:points.pop()
  boundary=len(points)
  for ix in range(-13,14):
   for iz in range(1,17):
    x=ix*.10;z=.03+iz*.10
    if (x/1.4)**2+((z-.03)/1.65)**2>.97:continue
    if side<0 and abs(x)<.56 and z<.92+.45*math.sqrt(max(0,1-(x/.54)**2)):continue
    points.append(Vector((x,z)))
  coords,_,tris,*_=delaunay_2d_cdt(points,[],[tuple(range(boundary))],1,1e-6)
  data=bpy.data.meshes.new('Authored curved hemisphere');data.from_pydata([(p.x,0,p.y) for p in coords],[],tris);data.update()
  mid=bmesh.new();mid.from_mesh(data)
  mid.verts.index_update();offset=len(vertices)
  for v in mid.verts:
   x,_,z=v.co;radius2=(x/1.4)**2+((z-.03)/1.65)**2
   v.co.y=0 if abs(radius2-1)<1e-5 else side*1.65*math.sqrt(max(0,1-radius2));vertices.append(tuple(v.co))
  faces +=[tuple(offset+v.index for v in f.verts) for f in mid.faces];mid.free();bpy.data.meshes.remove(data)
 canopy=s.mesh('Sewn multi-panel dome canopy',vertices,faces,material)
 bm=bmesh.new();bm.from_mesh(canopy.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-5);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
 orientation=sum(f.normal.dot(Vector((f.calc_center_median().x/1.4,f.calc_center_median().y/1.65,(f.calc_center_median().z-.03)/1.65))) for f in bm.faces)
 if orientation<0:bmesh.ops.reverse_faces(bm,faces=list(bm.faces))
 bm.to_mesh(canopy.data);bm.free();canopy.data.update()
 mod=canopy.modifiers.new('Continuous actual8mm arched canopy shell','SOLIDIFY');mod.thickness=.008;mod.offset=0
 bpy.context.view_layer.objects.active=canopy;bpy.ops.object.modifier_apply(modifier=mod.name)
 for p in canopy.data.polygons:p.use_smooth=True
 # s.mesh already gives the new authored panel a semantic component group.
 s.join([o,canopy],o.name)

def deform(o,kind):
 if kind=='dome':dome_aperture(o)
 wanted={g.index:g.name for g in o.vertex_groups if ('Catenary canvas side' in g.name or 'Tied-open front flap' in g.name) and kind=='ridge' or 'Sewn multi-panel dome canopy' in g.name and kind=='dome' or 'Soft rolled sleeping bag' in g.name and kind=='roll'}
 bm=bmesh.new();bm.from_mesh(o.data);layer=bm.verts.layers.deform.active
 faces=[f for f in bm.faces if all(any(i in v[layer] for i in wanted) for v in f.verts)]
 if kind!='dome':bmesh.ops.subdivide_edges(bm,edges=list({e for f in faces for e in f.edges}),cuts=1,use_grid_fill=True)
 bm.to_mesh(o.data);bm.free()
 for v in o.data.vertices:
  group=next((wanted[g.group] for g in v.groups if g.weight>.5 and g.group in wanted),None)
  if group:v.co+=displacement(v.co,kind,group)
 o.data.update()

records=[]
for source in sources:
 s.reset()
 with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{source}.blend'),link=False) as (src,dst):dst.objects=[source]
 o=dst.objects[0];bpy.context.scene.collection.objects.link(o);o.data=o.data.copy();kind='ridge' if 'ridge' in source else 'dome' if 'dome' in source else 'roll'
 old_floor={g.name:[tuple(v.co) for v in o.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)] for g in o.vertex_groups if floor_group(g.name)}
 deform(o,kind);parts=[o];seams=[]
 cloth_ids={g.index for g in o.vertex_groups if any(word in g.name for word in ['Catenary canvas side','Sewn multi-panel dome canopy','Soft rolled sleeping bag'])}
 cloth_vertices={v.index for v in o.data.vertices if any(a.group in cloth_ids and a.weight>.5 for a in v.groups)};o.data.calc_loop_triangles()
 cloth_tree=BVHTree.FromPolygons([v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if all(i in cloth_vertices for i in t.vertices)],all_triangles=True)
 thread=s.material('Tailored '+kind+' canvas thread',(.18,.21,.11) if kind=='ridge' else (.29,.21,.105) if kind=='dome' else (.115,.18,.21),'paint')
 def sewn(name,points,r=.0028):
  fitted=[]
  for p in points:
   nearest,normal,_,distance=cloth_tree.find_nearest(p,.035);assert nearest is not None,('Unsupported sewn seam',kind,name,list(p))
   delta=Vector(p)-nearest;direction=delta.normalized() if delta.length>.00001 else normal
   fitted.append(nearest+direction*(r+.0005))
  for a,c in zip(fitted,fitted[1:]):parts.append(s.rod(name,a,c,r,thread,5));seams.append([list(a),list(c)])
 if kind=='ridge':
  for side in [-1,1]:
   for y in [-1.57,-.56,.56,1.57]:
    points=[]
    for k in range(25):
     t=.04+.92*k/24;x=side*t*1.3;z=(1.90-.045*math.cos(y*math.pi/3.4))*(1-t)+.18*t-.035*math.sin(t*math.pi)
     p=Vector((x,y,z));p+=displacement(p,kind,'Catenary canvas side');p+=Vector((side*.007,0,.007));points.append(p)
    sewn('Sewn roof panel joining tape',points,.003)
 elif kind=='dome':
  for angle in [0,math.pi/2,math.pi,3*math.pi/2]:
   points=[]
   for j in range(25):
    lat=.05+1.43*j/24;point=Vector((1.4*math.cos(lat)*math.cos(angle),1.65*math.cos(lat)*math.sin(angle),.03+1.65*math.sin(lat)))
    if angle==3*math.pi/2 and point.z<1.4:continue
    point+=displacement(point,kind);point+=Vector((math.cos(angle)*.007,math.sin(angle)*.007,.006));points.append(point)
   sewn('Sewn dome gore reinforcement',points,.003)
  hem=[]
  for x,z in [(-.54,.03),(-.54,.90)]+[(-.54*math.cos(i*math.pi/40),.90+.45*math.sin(i*math.pi/40)) for i in range(1,40)]+[(.54,.90),(.54,.03)]:
   y=-1.65*math.sqrt(max(0,1-(x/1.4)**2-((z-.03)/1.65)**2));p=Vector((x,y,z));p+=displacement(p,kind);p.y-=.007;hem.append(p)
  sewn('Continuous arched doorway binding',hem,.004)
 else:
  def radius(x):
   x=abs(x)
   if x<.22:return .167
   if x<.30:return .167-(x-.22)/.08*.007
   return .16-(x-.30)/.02*.04
  for theta in [i*math.tau/12 for i in range(12)]:
   # Keep actual bearing underside and all carry straps unobstructed.
   if math.cos(theta)<-.6:continue
   for low,high in [(-.29,-.24),(-.18,.18),(.24,.29)]:
    points=[]
    for j in range(13):
     x=low+(high-low)*j/12;r=radius(x);p=Vector((x,r*math.sin(theta),.17+r*math.cos(theta)));p+=displacement(p,kind);p+=Vector((0,.002*math.sin(theta),.002*math.cos(theta)));points.append(p)
    sewn('Quilt longitudinal stitch',points,.0015)
 id=source.replace('_refined','').replace('_authored','')+'_tailored';o=s.join(parts,id)
 assert old_floor=={g.name:[tuple(v.co) for v in o.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)] for g in o.vertex_groups if floor_group(g.name)}
 # Keep old packed surface textures and UV layers, including the bedroll atlas.
 b.export(id,[o],'campground-cloth-shape',view=(5,-7,3.8) if kind!='roll' else (2,-3,2))
 if kind=='roll':
  original=o.data;o.data=original.copy()
  for name in [u.name for u in o.data.uv_layers if u.name!='AuthoredWearAtlas']:o.data.uv_layers.remove(o.data.uv_layers[name])
  file=ROOT/f'assets/runtime/models/{id}.glb';bpy.context.view_layer.objects.active=o;bpy.ops.object.select_all(action='DESELECT');o.select_set(True)
  bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
  disposable=o.data;o.data=original;bpy.data.meshes.remove(disposable);b.LOG[-1]['sha256']=hashlib.sha256(file.read_bytes()).hexdigest();b.LOG[-1]['bytes']=file.stat().st_size
 row=b.LOG[-1];row.update(previous_source=source,kind=kind,sewn_segments=seams,physical_shell_nominal_m=.008 if kind!='roll' else None,source_floor_unchanged=True);records.append(row)

bpy.ops.wm.open_mainfile(filepath=str(scene_source));bpy.context.preferences.filepaths.save_version=0
terrain=bpy.data.objects['level_camp_terrain_amenities'];ground=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons])
def height(x,y):
 p=ground.ray_cast(Vector((x,y,10)),Vector((0,0,-1)),40)[0];assert p is not None,(x,y);return p.z
templates={}
for a in records:
 with bpy.data.libraries.load(str(ROOT/a['editable']),link=False) as (src,dst):dst.objects=[a['id']]
 templates[a['kind']]=dst.objects[0]
instances=[]
for o in list(bpy.context.scene.objects):
 source=o.get('refinement_source','');kind='ridge' if 'tent_ridge' in source else 'dome' if 'tent_dome' in source else 'roll' if 'bedroll_rolled' in source else None
 if kind is None:continue
 t=templates[kind];old_matrix=o.matrix_world.copy();old_pose=(o.location.copy(),o.rotation_euler.copy(),o.scale.copy());old_floor=[]
 if kind!='roll':
  floor_ids={g.index for g in o.vertex_groups if floor_group(g.name)}
  old_floor=[tuple(v.co) for v in o.data.vertices if any(a.group in floor_ids and a.weight>.5 for a in v.groups)]
  # Save the real already-fitted dense floor, discard only the old canopy/frame.
  bm=bmesh.new();bm.from_mesh(o.data);layer=bm.verts.layers.deform.active
  bmesh.ops.delete(bm,geom=[v for v in bm.verts if not any(i in v[layer] for i in floor_ids)],context='VERTS');o.data=o.data.copy();bm.to_mesh(o.data);bm.free()
  insert=bpy.data.objects.new('Tailored canopy fitted to existing floor',t.data.copy());bpy.context.scene.collection.objects.link(insert);insert.location,insert.rotation_euler,insert.scale=old_pose
  bpy.context.view_layer.update()
  for g in t.vertex_groups:insert.vertex_groups.new(name=g.name)
  bm=bmesh.new();bm.from_mesh(insert.data);layer=bm.verts.layers.deform.active;ids={g.index for g in insert.vertex_groups if floor_group(g.name)}
  bmesh.ops.delete(bm,geom=[v for v in bm.verts if any(i in v[layer] for i in ids)],context='VERTS');bm.to_mesh(insert.data);bm.free()
  for v in insert.data.vertices:
   w=insert.matrix_world@v.co;v.co.z+=(height(w.x,w.y)-insert.location.z)/insert.scale.z
  bpy.ops.object.select_all(action='DESELECT');o.select_set(True);insert.select_set(True);bpy.context.view_layer.objects.active=o;bpy.ops.object.join()
  used={a.group for v in o.data.vertices for a in v.groups if a.weight>0}
  for g in list(o.vertex_groups)[::-1]:
   if g.index not in used:o.vertex_groups.remove(g)
  floor_ids={g.index for g in o.vertex_groups if floor_group(g.name)}
  assert old_floor==[tuple(v.co) for v in o.data.vertices if any(a.group in floor_ids and a.weight>.5 for a in v.groups)]
 else:
  o.vertex_groups.clear();o.data=t.data.copy()
  for g in t.vertex_groups:
   if o.vertex_groups.get(g.name) is None:o.vertex_groups.new(name=g.name)
  for v in o.data.vertices:
   w=o.matrix_world@v.co;v.co.z+=(height(w.x,w.y)-height(o.location.x,o.location.y))/o.scale.z
 # Preserve original transform components: decomposing a matrix can wrap +pi
 # into a slightly different -pi rotation and move exact fitted floor vertices.
 o['tailored_source']=t.name;o.location,o.rotation_euler,o.scale=old_pose
 instances.append(dict(name=o.name,kind=kind,source=t.name,groundsheet_vertices_preserved=len(old_floor),site_roll_seams_installed=kind=='roll'))
scene=bpy.context.scene;scene['production_status']='Tailored tent/bedding geometry; runtime gates remain open'
target=OUT/'hollow-creek-tailored.blend';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
r=dict(status='cloth silhouette refinement; independent validation pending',assets=records,instances=instances,scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),protected_sources=protected,limitations=['Native geometry and standalone asset milestone, no runtime map unlock/physics/circuit/AI/LAN acceptance.'])
(OUT/'tailored-fabric.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');print('TAILORED',len(records),'families',len(instances),'fitted instances; original floors/terrain preserved',flush=True)
