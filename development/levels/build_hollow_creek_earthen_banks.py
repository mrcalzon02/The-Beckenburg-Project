"""Replace the wall-like bank and gravel blobs with shaped earth/bedrock.

Only the native boundary-bank mesh changes. Fit its inner toe around existing
facilities/routes, widen outward, preserve source checkpoints and export actual
world-origin geometry. Current world collision integration remains pending.
"""
import bpy,bmesh,json,math,random,hashlib,sys
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path[:0]=[str(ROOT/'development/assets'),str(ROOT/'development/levels')]
import rebuild_support as s
source=OUT/'hollow-creek-siteart-fitted.blend';source_sha=hashlib.sha256(source.read_bytes()).hexdigest();bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene;terrain=bpy.data.objects['level_camp_terrain_amenities'];old=bpy.data.objects['level_camp_boundary_banks']
def tree(o,indices=None):
 o.data.calc_loop_triangles();return BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if indices is None or t.polygon_index in indices],all_triangles=True)
grounds=[tree(terrain)]+[tree(o) for o in scene.objects if o.type=='MESH' and o.name.startswith(('Continuous west forest lakeshore return','Continuous east forest lakeshore return'))]
previous=tree(old,{p.index for p in old.data.polygons if p.material_index==0})
def height(x,y):
 hits=[t.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0] for t in grounds];hits=[h for h in hits if h is not None]
 return max(h.z for h in hits) if hits else None
def prior_top(x,y):
 hit=previous.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0];return hit.z if hit else None
amenities=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));contract=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));boundary=json.loads((OUT/'boundaries.json').read_text(encoding='utf-8'))
anchors=[(e[0],-e[1]) for row in boundary['log_contacts'] for e in row['ends']]
foliage=bpy.data.objects['level_camp_boundary_logfall'];gids={g.index for g in foliage.vertex_groups if 'rooted scrub stem' in g.name};stem={g:[] for g in gids}
for v in foliage.data.vertices:
 for a in v.groups:
  if a.group in stem:stem[a.group].append(foliage.matrix_world@v.co)
for points in stem.values():
 low=min(p.z for p in points);base=[p for p in points if p.z<low+.12];anchors.append((sum(p.x for p in base)/len(base),sum(p.y for p in base)/len(base)))
reserved=[]
for name in [p['name'] for p in amenities['placements']]+['west-pitched-ridge','east-pitched-dome','Camp wardens heron memorial']:
 o=bpy.data.objects[name];p=[o.matrix_world@Vector(c) for c in o.bound_box];reserved.append((min(v.x for v in p)-.30,max(v.x for v in p)+.30,min(v.y for v in p)-.30,max(v.y for v in p)+.30))
routes=[((a[0],-a[2]),(c[0],-c[2]),r['width']/2+.4) for r in contract['routes'] for a,c in zip(r['points'],r['points'][1:])]
def distance(x,y,a,c):
 dx=c[0]-a[0];dy=c[1]-a[1];t=max(0,min(1,((x-a[0])*dx+(y-a[1])*dy)/(dx*dx+dy*dy)));return math.hypot(x-a[0]-t*dx,y-a[1]-t*dy)
def free(x,y):return not any(a<x<b and c<y<d for a,b,c,d in reserved) and all(distance(x,y,a,c)>w for a,c,w in routes)
def smooth(t):t=max(0,min(1,t));return t*t*(3-2*t)
def catmull(a,b,c,d,t):return .5*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t)
soil_materials=list(terrain.data.materials);rock_image=bpy.data.images.load(str(ROOT/'development/levels/textures/hollow-creek/bedrock-v1.png'));rock_image.name='Solid weathered bedrock runtime512 derivative';rock_image.scale(512,512);rock_image.pack()
rock_material=bpy.data.materials.new('Solid fractured woodland bedrock; no gravel aggregate');rock_material.use_nodes=True;shader=rock_material.node_tree.nodes['Principled BSDF'];shader.inputs['Roughness'].default_value=.94;tex=rock_material.node_tree.nodes.new('ShaderNodeTexImage');tex.image=rock_image;rock_material.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
parts=[];stations=[];outcrops=[];rng=random.Random(251025)
for path in contract['boundary_paths']:
 controls=[Vector((x,-z)) for x,z in path['points']];samples=[controls[0]]
 for i in range(len(controls)-1):
  a=controls[max(0,i-1)];b=controls[i];c=controls[i+1];d=controls[min(len(controls)-1,i+2)];steps=math.ceil((c-b).length/.40)
  for j in range(1,steps+1):
   p=catmull(a,b,c,d,j/steps);samples.append(Vector(tuple(max(min(b[k],c[k]),min(max(b[k],c[k]),p[k])) for k in range(2))))
 arc=[0]
 for a,c in zip(samples,samples[1:]):arc.append(arc[-1]+(c-a).length)
 vv=[];ff=[];uvs=[];wear=[];rows=[];row_count=15
 grng=random.Random(path['id']+' irregular drainage');gullies=[];cursor=grng.uniform(6,10)
 while cursor<arc[-1]-3:gullies.append((cursor,grng.uniform(.5,1.25)));cursor+=grng.uniform(6,13)
 for i,p in enumerate(samples):
  tangent=(samples[min(len(samples)-1,i+2)]-samples[max(0,i-2)]).normalized();normal=Vector((tangent.y,-tangent.x));x,y=p
  # Path coordinates are native X/-Z; this is the original outward normal.
  desired=3.05+.55*math.sin(arc[i]*.17)+.25*math.sin(arc[i]*.57+1)
  if -y>74:desired=1.0
  inside=1.0
  for step in range(1,25):
   candidate=1+(desired-1)*step/24;q=p-normal*candidate
   if not free(q.x,q.y):break
   inside=candidate
  outside=3.8+.7*math.sin(arc[i]*.13+1.2)+.4*math.sin(arc[i]*.41)
  while outside>1.6 and any(height(*(p+normal*(outside*f))) is None for f in [.25,.5,.75,1]):outside=max(1.6,outside-.2)
  base=height(x,y);assert base is not None
  previous_peak=prior_top(x,y);peak=previous_peak if previous_peak is not None and previous_peak>base+.2 else base+1.8
  da=min((math.hypot(x-a,y-b) for a,b in anchors),default=10);variation=(.42*math.sin(arc[i]*.25)+.20*math.sin(arc[i]*.73+2))*smooth((da-.7)/1.5)
  peak+=variation;peak=base+(peak-base)*(.65+.35*smooth(min(arc[i],arc[-1]-arc[i])/3))
  peak=max(peak,base+(1.30 if -y<74 else .55)*(.65+.35*smooth(min(arc[i],arc[-1]-arc[i])/3)))
  profile=[];profile_wear=[]
  for f in [-1,-.96,-.84,-.65,-.40,-.15,0,.17,.37,.60,.82,1]:
   off=f*(inside if f<0 else outside);q=p+normal*off;g=height(*q);assert g is not None,(path['id'],i,tuple(q))
   bell=math.sin((f+1)*math.pi/2)**1.55 if f<0 else math.cos(f*math.pi/2)**1.8
   z=g+(peak-g)*bell-.06*(1-bell)
   # Small erosional scallops on shoulders, with no repeated flat plateau.
   z+=.075*math.sin(arc[i]*1.17+f*4)*math.sin(math.pi*abs(f))**2
   scar=max((math.exp(-((arc[i]-center-.35*math.sin(f*4))/width)**2) for center,width in gullies),default=0)*math.sin(math.pi*abs(f))**2 if f<0 else 0
   z-=.10*scar;profile.append(Vector((q.x,q.y,z)));profile_wear.append(scar)
  for f in [outside,0,-inside]:
   q=p+normal*f;profile.append(Vector((q.x,q.y,height(*q)-.25)))
  assert len(profile)==row_count
  distances=[0]
  for a,c in zip(profile,profile[1:]):distances.append(distances[-1]+(c-a).length)
  start=len(vv);vv.extend(profile);wear.extend(profile_wear+[1,1,1])
  foot=profile[0];uvs.extend([Vector((foot.x/2,foot.y/2))+normal*d/2 for d in distances])
  if i:
   for j in range(row_count):a=start-row_count+j;b=start-row_count+(j+1)%row_count;c=start+(j+1)%row_count;d=start+j;ff.extend([(a,b,c),(a,c,d)])
  rows.append(dict(center=[x,y],normal=list(normal),inside_m=inside,outside_m=outside,peak_z=peak,ground_z=base,arc=arc[i],profile=[list(v) for v in profile]))
 ff.extend([tuple(range(row_count-1,-1,-1)),tuple(range(len(vv)-row_count,len(vv)))])
 o=s.mesh('Earthen bank '+path['id'],vv,ff,soil_materials[0]);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.triangulate(bm,faces=list(bm.faces));bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(o.data);bm.free()
 for material in soil_materials[1:]:o.data.materials.append(material)
 for poly in o.data.polygons:
  poly.use_smooth=True;value=sum(wear[j] for j in poly.vertices)/len(poly.vertices);poly.material_index=1 if value>.75 else 5 if value>.55 else 4 if value>.32 else 3 if value>.10 else 0
 for li in o.data.loops:o.data.uv_layers.active.data[li.index].uv=uvs[li.vertex_index]
 o['role']='Rounded variable-width terrain-supported earth bank; movement solid candidate';parts.append(o)
 local_soil=tree(o)
 # Sparse fractured bedrock ledges, with backs and bases buried in the bank.
 for i,row in enumerate(rows):
  if row['arc']<5 or row['arc']>arc[-1]-5 or i%29 or -row['center'][1]>74 or row['inside_m']<1.3:continue
  p=Vector(row['center']);n=Vector(row['normal']);t=Vector((-n.y,n.x));off=-row['inside_m']*.30;q=p+n*off
  g=height(*q);f=off/row['inside_m'];bell=math.sin((f+1)*math.pi/2)**1.55;z=g+(row['peak_z']-g)*bell
  length=rng.uniform(1.8,3.2);depth=min(rng.uniform(.8,1.25),row['inside_m']*.65);rise=rng.uniform(.35,.65)
  # Staggered broken wedges: no four-corner rectangular front or level lid.
  template=[(-.58,-.38,-.63),(-.03,-.68,-.48),(.50,-.22,-.60),(-.42,.55,-.75),(.40,.45,-.67),(-.42,.12,.12),(.05,.02,rise),(.48,.10,.02),(.25,.58,-.04),(-.05,-.48,.10)];noise=[rng.uniform(-.09,.09) for _ in template]
  for attempt in range(7):
   points=[]
   for (along,across,dz),jitter in zip(template,noise):
    v=q+t*(along*length)+n*(across*depth);points.append((v.x,v.y,z+dz+jitter))
   if all(local_soil.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0] is not None for x,y,z in points):break
   length*=.8;depth*=.8
  else:continue
  mesh=bpy.data.meshes.new('Fractured bedrock ledge');mesh.from_pydata(points,[],[]);rock=bpy.data.objects.new('Bedrock outcrop '+path['id']+' '+str(i),mesh);scene.collection.objects.link(rock)
  bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.convex_hull(bm,input=list(bm.verts));bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();rock.data.materials.append(rock_material);s.uv_project(rock)
  bpy.context.view_layer.objects.active=rock;bevel=rock.modifiers.new('Small worn fracture arrises','BEVEL');bevel.width=.012;bevel.segments=1;bpy.ops.object.modifier_apply(modifier=bevel.name)
  for poly in rock.data.polygons:
   axis=max(range(3),key=lambda k:abs(poly.normal[k]));axes=[k for k in range(3) if k!=axis]
   for li in poly.loop_indices:
    v=rock.data.vertices[rock.data.loops[li].vertex_index].co;rock.data.uv_layers.active.data[li].uv=(v[axes[0]]/2,v[axes[1]]/2)
  parts.append(rock);outcrops.append(dict(name=rock.name,center=[q.x,q.y,z],length_m=length,depth_m=depth))
 stations.append(dict(id=path['id'],rows=rows))
bpy.data.objects.remove(old,do_unlink=True);bank=s.join(parts,'level_camp_boundary_banks');bank['role']='Reshaped earthen boundary and embedded solid-bedrock outcrops';bank['runtime_geometry_source']='level_camp_boundary_banks_grounded'
bank.data.calc_loop_triangles();assert len(bank.data.loop_triangles)<50000
soil_groups={g.index for g in bank.vertex_groups if g.name.startswith('Earthen bank ')};soil_vertices={v.index for v in bank.data.vertices if any(a.group in soil_groups and a.weight>.5 for a in v.groups)}
bank_tree=BVHTree.FromPolygons([v.co for v in bank.data.vertices],[tuple(t.vertices) for t in bank.data.loop_triangles if all(i in soil_vertices for i in t.vertices)],all_triangles=True)
def bank_top(x,y):
 hit=bank_tree.ray_cast(Vector((x,y,40)),Vector((0,0,-1)),100)[0];assert hit is not None,(x,y);return hit.z
# Refit whole log/plant assemblies rigidly; branches stay attached. A changed
# earth surface must never leave the old crest dressing floating above it.
foliage.data=foliage.data.copy();group_vertices={g.index:[] for g in foliage.vertex_groups}
for v in foliage.data.vertices:
 for a in v.groups:
  if a.weight>.5:group_vertices[a.group].append(v.index)
normals=[Vector(n.vector) for n in foliage.data.corner_normals];rotations={};refits=[];new_contacts=[]
for record in boundary['log_contacts']:
 name=record['name'];members=[g for g in foliage.vertex_groups if g.name==name or g.name.startswith(name+' branch ')];indices={i for g in members for i in group_vertices[g.index]};assert indices,('Missing log assembly',name)
 a,c=[Vector((e[0],-e[1],e[2]+.25)) for e in record['ends']];axis=c-a;length=axis.length;direction=Vector((axis.x,axis.y)).normalized();unit=axis.normalized();body=group_vertices[foliage.vertex_groups[name].index]
 ends=[[foliage.data.vertices[i].co.copy() for i in body if ((foliage.data.vertices[i].co-a).dot(unit)/length<.15 if end==0 else (foliage.data.vertices[i].co-a).dot(unit)/length>.85)] for end in [0,1]];assert all(ends)
 def bearing(angle):
  target=Vector((direction.x*math.cos(angle),direction.y*math.cos(angle),math.sin(angle)));rotation=axis.rotation_difference(target);gaps=[]
  for points in ends:
   candidates=[Vector((a.x,a.y,0))+rotation@(p-a) for p in points];gaps.append(min(p.z-bank_top(p.x,p.y) for p in candidates))
  return gaps,rotation
 lo=-.8;hi=.8
 for _ in range(30):
  angle=(lo+hi)/2;gaps,rotation=bearing(angle);error=gaps[1]-gaps[0]
  if error<0:lo=angle
  else:hi=angle
 angle=(lo+hi)/2;gaps,rotation=bearing(angle);new_a=Vector((a.x,a.y,-.04-sum(gaps)/2));new_c=new_a+rotation@axis
 for i in indices:
  foliage.data.vertices[i].co=new_a+rotation@(foliage.data.vertices[i].co-a);rotations[i]=rotation
 refits.append(dict(kind='rigid-log',name=name,groups=[g.name for g in members],old_axis=[list(a),list(c)],new_axis=[list(new_a),list(new_c)],quaternion=list(rotation),vertices=len(indices)))
 new_contacts.append(dict(name=name,group=name,axis=[list(new_a),list(new_c)]))
plants=[]
for g in foliage.vertex_groups:
 if 'rooted scrub stem' not in g.name:continue
 vv=[foliage.data.vertices[i].co for i in group_vertices[g.index]];low=min(v.z for v in vv);base=[v for v in vv if v.z<low+.12];p=sum(base,Vector())/len(base);plants.append(dict(group=g,base=p,indices=set(group_vertices[g.index]),groups=[g.name]))
for g in foliage.vertex_groups:
 if not g.name.startswith(('Scrub branch contact','Supported boundary scrub crown')):continue
 indices=group_vertices[g.index]
 if not indices:continue
 center=sum((foliage.data.vertices[i].co for i in indices),Vector())/len(indices);plant=min(plants,key=lambda p:(Vector((center.x,center.y))-Vector((p['base'].x,p['base'].y))).length);plant['indices'].update(indices);plant['groups'].append(g.name)
for plant in plants:
 p=plant['base'];delta=bank_top(p.x,p.y)-p.z-.08
 for i in plant['indices']:foliage.data.vertices[i].co.z+=delta
 refits.append(dict(kind='rigid-rooted-scrub',name=plant['group'].name,groups=plant['groups'],translation=[0,0,delta],base_before=list(p),vertices=len(plant['indices'])))
for loop in foliage.data.loops:
 if loop.vertex_index in rotations:normals[loop.index]=rotations[loop.vertex_index]@normals[loop.index]
foliage.data.normals_split_custom_set([tuple(n) for n in normals]);foliage['runtime_geometry_source']='level_camp_boundary_logfall_grounded'
target=OUT/'hollow-creek-grounded-berm.blend';scene['production_status']='Rounded earth bank and bedrock refinement; native support/clearance and runtime gates remain';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
assets=[]
bank_triangle_count=len(bank.data.loop_triangles)
for original,id in [(bank,'level_camp_boundary_banks_grounded'),(foliage,'level_camp_boundary_logfall_grounded')]:
 asset=original.copy();asset.data=original.data.copy();asset.name=id
 editable=ROOT/f'development/levels/blender/{id}.blend';bpy.data.libraries.write(str(editable),{asset},compress=True);scene.collection.objects.link(asset);bpy.ops.object.select_all(action='DESELECT');asset.select_set(True);bpy.context.view_layer.objects.active=asset
 file=ROOT/f'assets/runtime/models/{id}.glb';bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True,export_extras=True);asset.data.calc_loop_triangles();assert len(asset.data.loop_triangles)<50000 and file.stat().st_size<4_000_000
 assets.append(dict(id=id,editable=editable.relative_to(ROOT).as_posix(),model=file.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(file.read_bytes()).hexdigest(),triangles=len(asset.data.loop_triangles),bytes=file.stat().st_size))
 bpy.data.objects.remove(asset,do_unlink=True)
from finalize_earthen_bank_sources import materialize
materialize(assets)
assert hashlib.sha256(source.read_bytes()).hexdigest()==source_sha
report=dict(status='constructed; independent support/clearance/source checks required',source=source.relative_to(ROOT).as_posix(),source_sha256=source_sha,scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),user_authorized_geometry_reshape=True,stations=stations,outcrops=outcrops,crest_refits=refits,crest_log_contacts=new_contacts,assets=assets,runtime_map_acceptance=False)
(OUT/'grounded-berm.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('EARTHEN BANK',bank_triangle_count,'triangles;',len(outcrops),'embedded bedrock ledges',flush=True)
