"""Replace stair assemblies and their architectural floor/wall openings.

Room footprints, elevations, lanes and door schedules are the construction inputs.
Movement guides are assigned only after those building elements are constructed.
Original library assets and unselected native records are retained losslessly.
"""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_roster_foundations import material,prism
from build_roster_floor_closures import difference
from build_circulation_construction import native_write
from build_architectural_doorways import frame_template
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from build_deliberate_entrances import write
from build_roster_decks import area
B=ROOT/'development/levels/rebuild';OUT=B/'service-ground-bay-pass';BASE=B/'household-sacred-bay-pass/roster.json'
RAIL=B.parent/'primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bb(o):
 p=[o.matrix_world@v.co for v in o.data.vertices]
 return [min(v.x for v in p),min(-v.y for v in p),max(v.x for v in p),max(-v.y for v in p),min(v.z for v in p),max(v.z for v in p)]
# Fixed architectural schedules; these are deliberately independent of paths.
PLANS={'COLLEGE':dict(
 flights=[('L81',[28,4,34,16],31,6,11.76,0,3.6,1.5,20)],
 floors=[([28,4,34,16],0,[]),([28,4,34,16],3.6,[[30.05,5.75,31.95,11.76]]),([28,4,34,16],7.2,[])],
 rooms=[([28,4,34,16],0,3.6,{'S':[(31,1.4)]}),([28,4,34,16],3.6,7.2,{'S':[(31,1.4)],'W':[(14,1.4)]})])}
PILOT='--pilot' in sys.argv
if not PILOT:
 p=PLANS['COLLEGE']
 p['flights'] += [('L80',[24,34,30,46],28.5,36.6,42.36,0,3.6,1.5,20),('L84',[24,34,30,46],25.2,42,37.35,-3,0,1.4,17)]
 p['floors'] += [([24,34,30,46],-3,[]),([24,34,30,46],0,[[24.3,37.35,26.1,42.25]]),([24,34,30,46],3.6,[[27.55,36.35,29.45,42.36]]),([24,34,30,46],7.2,[])]
 p['rooms'] += [([24,34,30,46],-3,0,{'W':[(44,1.4)],'E':[(40,1.4)]}),([24,34,30,46],0,3.6,{'W':[(44,1.4)]}),([24,34,30,46],3.6,7.2,{'W':[(44,1.4)]})]

def build(row):
 site=row['id'];plan=PLANS[site];out=OUT/site.lower();out=out/'pilot' if PILOT else out;out.mkdir(parents=True,exist_ok=True)
 data=json.loads((ROOT/row['layout']).read_text());before=copy.deepcopy(data);meta=json.loads((ROOT/row['construction']).read_text())
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(ROOT/row['glb']))
 old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];steps=[];portals=[];replaced_faces=set();clipped=[]
 floor_mat=next(o.data.materials[0] for o in old if o.get('role')=='floor' and o.data.materials)
 wall_mat=next(o.data.materials[0] for o in old if o.get('role')=='rough shell wall' and o.data.materials)
 stair_mat=next(o.data.materials[0] for o in old if o.get('role')=='structural-stair-step' and o.data.materials)
 timber=material(site+' authored stair bearing',(.26,.25,.21))
 selected={f[0] for f in plan['flights']}
 def slab(name,polys,y,depth,role,mt=None):
  made=plate(site+' bay '+name,polys,y,depth,mt or floor_mat) or []
  for o in made:o['role']=role;built.append(o)
  return made
 def box(name,a,b,c,d,lo,hi,role='architectural bearing',mt=None):return slab(name,[rect(a,b,c,d)],hi,hi-lo,role,mt or timber)
 def retire(o):
  if o in old:removed.add(o.name)
  elif o in built:built.remove(o);bpy.data.objects.remove(o,do_unlink=True)
 for o in old:
  role=o.get('role','');link=o.get('link');name=o.name
  if role=='placed source-library furnishing':continue
  if link in selected and any(s in role for s in ['stair','landing','guard','stringer','waist','support']):removed.add(name)
  if any(name.startswith(l+' support') for l in selected):removed.add(name)
 # Remove flat old ownership within each architectural slab, including obsolete
 # route pads and copied ceiling cuts. Preserve structural remnants outside it.
 def replace_plate(foot,y,holes):
  for o in list(old)+list(built):
   if o.name in removed or o.get('role','')=='placed source-library furnishing':continue
   role=o.get('role','');a,b,c,d,lo,hi=bb(o)
   if not any(k in role for k in ['floor','cap','landing','route support','retained clipped plate']):continue
   if abs(hi-y)>.025 or hi-lo>.65 or c<=foot[0]+1e-5 or a>=foot[2]-1e-5 or d<=foot[1]+1e-5 or b>=foot[3]-1e-5:continue
   outside=[];mt=o.data.materials[0] if o.data.materials else floor_mat
   for p in o.data.polygons:
    vs=[o.matrix_world@o.data.vertices[i].co for i in p.vertices]
    if not all(abs(v.z-hi)<.001 for v in vs):continue
    poly=[(v.x,-v.y) for v in vs];outside.extend(difference(poly,rect(*foot)))
   original_name=o.name;retire(o)
   if outside:slab('retained outside '+original_name,outside,hi,hi-lo,'retained clipped plate outside reserved bay',mt)
  polys=[rect(*foot)]
  for h in holes:polys=[q for p in polys for q in difference(p,rect(*h))]
  slab('complete plate '+str((foot,y)),polys,y,.30,'authored complete bay floor or enclosure')
 for foot,y,holes in plan['floors']:replace_plate(foot,y,holes)
 if site=='FARMHOUSE':
  for z in [16.35,23.65]:box('barn loft wall-bearing joist '+str(z),31,z-.10,43,z+.10,2.35,2.7)
 # Architectural faces replace complete selected wall intervals. The only
 # booleans here clip OLD wall remnants outside those intervals; no swept cuts.
 def replace_face(axis,fixed,start,end,lo,hi,doors):
  key=(axis,fixed,start,end,lo,hi)
  if key in replaced_faces:return
  replaced_faces.add(key)
  for o in list(old)+[x for x in built if x.get('role')=='retained clipped old wall']:
   if o.name in removed:continue
   role=o.get('role','')
   if not ('wall' in role or 'header' in role):continue
   a,b,c,d,l,h=bb(o);normal=(a,c) if axis=='x' else (b,d);along=(b,d) if axis=='x' else (a,c)
   if normal[1]-normal[0]>.7 or abs(sum(normal)/2-fixed)>.25 or along[1]<=start+1e-4 or along[0]>=end-1e-4 or h<=lo+1e-4 or l>=hi-1e-4:continue
   fully=along[0]>=start-1e-4 and along[1]<=end+1e-4 and l>=lo-1e-4 and h<=hi+1e-4
   if not fully:
    n=o.copy();n.data=o.data.copy();n.name=site+' retained outside wall '+str(len(clipped));bpy.context.collection.objects.link(n)
    bpy.ops.mesh.primitive_cube_add(size=1);cut=bpy.context.object
    cut.location=(fixed,-(start+end)/2,(lo+hi)/2) if axis=='x' else ((start+end)/2,-fixed,(lo+hi)/2)
    cut.scale=(1.4,end-start,hi-lo) if axis=='x' else (end-start,1.4,hi-lo)
    bpy.context.view_layer.objects.active=n;mod=n.modifiers.new('Exact reserved architectural face interval','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=cut;bpy.ops.object.modifier_apply(modifier=mod.name);bpy.data.objects.remove(cut,do_unlink=True)
    if n.data.polygons:n['role']='retained clipped old wall';built.append(n);clipped.append(n.name)
    else:bpy.data.objects.remove(n,do_unlink=True)
   retire(o)
  def part(u,v,bottom,top,label):
   if v-u<1e-5 or top-bottom<1e-5:return
   poly=rect(fixed-.12,u,fixed+.12,v) if axis=='x' else rect(u,fixed-.12,v,fixed+.12)
   slab('wall '+str(key)+' '+label,[poly],top,top-bottom,'complete reserved bay wall',wall_mat)
  cursor=start
  for i,(center,width) in enumerate(sorted(doors)):
   outer=.62*width;head=lo+2.1*2.275/2.125;left,right=center-outer,center+outer
   assert start<left<right<end and head<hi,(site,key,center)
   part(cursor,left,lo,hi,'pier'+str(i));part(left,right,head,hi,'lintel'+str(i));cursor=right
   frame=frame_template();frame.name=site+' bay indexed door '+str(len(portals));depth=.24
   for v in frame.data.vertices:
    p=frame.matrix_world@v.co;u=p.x*width;n=p.y*depth/.18;y=lo+p.z*2.1/2.125
    v.co=(fixed+n,-center-u,y) if axis=='x' else (center+u,-fixed+n,y)
   frame.matrix_world.identity();frame['role']='fitted indexed bay doorway';frame['source_asset']='level_blockout_doorway';built.append(frame)
   portals.append(dict(id=frame.name,axis=axis,boundary=fixed,along=center,sill_y=lo,clear_width=width,clear_height=2.1,frame_span=[fixed-.12,fixed+.12],frame_depth=.24))
  part(cursor,end,lo,hi,'end')
 for foot,lo,hi,doors in plan['rooms']:
  a,b,c,d=foot
  for side,axis,fixed,start,end in [('N','z',b,a,c),('S','z',d,a,c),('W','x',a,b,d),('E','x',c,b,d)]:replace_face(axis,fixed,start,end,lo,hi,doors.get(side,[]))
 # Adjacent choir/service faces are part of the same normal upper-floor
 # approach. Replace their obsolete stepped doorway fragments as whole faces.
 if site=='CHURCH':
  replace_face('x',26,26,34,3,6,[(32.7,1.05)])
  replace_face('x',29,26,34,3,6,[(28,1.05),(32,1.05)])
 # Reuse indexed railing geometry and materials, preserving its master.
 objects=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));rail=next(o for o in set(bpy.context.scene.objects)-objects if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-objects:bpy.data.objects.remove(o,do_unlink=True)
 def guard(name,p,q):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/run,(q[2]-p[2])/run;n=math.ceil(run/2)
  for i in range(n):
   mesh=rail.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/run+v.co.z)
   o=bpy.data.objects.new(site+' bay '+name+' guard '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='supported indexed bay guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 for link,foot,x,z0,z1,y0,y1,width,n in plan['flights']:
  assert (y1-y0)/n<=.181 and abs(z1-z0)/n>=.25
  profile=[(z0,y0-.42),(z1,y1-.42),(z1,y1-.20),(z0,y0-.20)];vs=[(xx,-z,y) for xx in [x-width/2,x+width/2] for z,y in profile];fs=[(3,2,1,0),(4,5,6,7)]+[(i,(i+1)%4,(i+1)%4+4,i+4) for i in range(4)]
  mesh=bpy.data.meshes.new(link+' closed waist');mesh.from_pydata(vs,[],fs);mesh.materials.append(stair_mat);mesh.update();o=bpy.data.objects.new(site+' bay '+link+' continuous waist',mesh);bpy.context.collection.objects.link(o);o['role']='bounded architectural stair waist';o['link']=link;built.append(o)
  for i in range(n):
   za,zb=z0+(z1-z0)*i/n,z0+(z1-z0)*(i+1)/n;poly=rect(x-width/2,min(za,zb),x+width/2,max(za,zb));bottom=[y0+(y1-y0)*(z-z0)/(z1-z0)-.20 for _,z in poly]
   o=prism(site+' bay '+link+' tread '+str(i+1),poly,y0+(y1-y0)*(i+1)/n,bottom,stair_mat);o['role']='structural-stair-step';o['link']=link;built.append(o);steps.append(o.name)
  for side in [-1,1]:guard(link+' flight side '+str(side),[x+side*width/2,y0,z0],[x+side*width/2,y1,z1])
  # Both ends bear on full room slabs. Rim girders run back to real bay walls.
  lowz,highz=min(z0,z1),max(z0,z1)
  for xx in [x-width/2-.12,x+width/2+.12]:box(link+' rim bearing '+str(xx),xx-.07,lowz-.25,xx+.07,highz+.25,y1-.65,y1-.30)
  for z in [lowz-.20,highz+.20]:box(link+' end wall-bearing girder '+str(z),foot[0],z-.075,foot[2],z+.075,y1-.65,y1-.30)
  # Guard the receiving floor aperture; leave only its actual stair-head open.
  hole=next(h for f,y,hs in plan['floors'] if abs(y-y1)<.01 for h in hs if h[0]<x<h[2] and h[1]<=lowz+.01 and h[3]>=highz-.01)
  a,b,c,d=hole
  for xx in [a-.06,c+.06]:guard(link+' floor rim '+str(xx),[xx,y1,b],[xx,y1,d])
  closed=b if z1>z0 else d;guard(link+' closed floor well end',[a-.06,y1,closed],[c+.06,y1,closed])
 # Now place movement inside the built rooms and approach their real doors.
 by={l['id']:l for l in data['links']}
 by['L81']['points']=[[29.4,0,10],[29.4,0,5.1],[31,0,5.1],[31,0,6],[31,3.6,11.76],[31,3.6,13]]
 by['L19']['points']=[[31,0,18],[31,0,16],[31,0,14.5],[29.4,0,14.5],[29.4,0,10]]
 by['L51']['points']=[[31,3.6,13],[31,3.6,16],[31,3.6,18]]
 by['L87']['points']=[[26,3.6,14],[28,3.6,14],[29.4,3.6,14],[31,3.6,14],[31,3.6,13]]
 anchors={'library-stair':[29.4,0,10],'upper-library-stair':[31,3.6,13]}
 if not PILOT:
  by['L80']['points']=[[26.95,0,44],[26.95,0,35.2],[28.5,0,35.2],[28.5,0,36.6],[28.5,3.6,42.36],[28.5,3.6,44],[26.8,3.6,44]]
  by['L84']['points']=[[27,-3,40],[27,-3,44],[25.2,-3,44],[25.2,-3,42],[25.2,0,37.35],[25.2,0,35.2],[26.95,0,35.2],[26.95,0,44]]
  by['L11']['points']=[[22,0,32],[22,0,44],[24,0,44],[26.95,0,44]]
  by['L47']['points']=[[22,3.6,32],[22,3.6,44],[24,3.6,44],[26.8,3.6,44]]
  by['L68']['points']=[[22,-3,32],[22,-3,44],[24,-3,44],[27,-3,44],[27,-3,40]]
  by['L69']['points']=[[27,-3,40],[30,-3,40],[38,-3,40]]
  anchors.update({'teaching-stair':[26.95,0,44],'upper-teaching-stair':[26.8,3.6,44],'west-cellar-stair':[27,-3,40]})
 for link in selected:
  by[link]['width']=1.4 if link=='L84' else 1.5
  by[link]['construction']='Explicit architectural floor datums, bay walls, rectangular slab opening, continuous waist, wall-bearing rim girders, supported indexed guards and level end landings. Guidance follows the constructed building.'
 for z in data['zones']:
  if z['id'] in anchors:z['label_point']=anchors[z['id']]
 # Reuse the exact floor-ownership method from resolve_flat_floor_ownership:
 # remove coplanar study solids only when real floors own their complete area
 # and depth. Exterior ground is never inferred from a room reservation.
 pool=[o for o in old if o.name not in removed]+list(built);owners={};pad_proof=[]
 def upward_polys(o):
  o.data.calc_loop_triangles();out=[]
  for tri in o.data.loop_triangles:
   p=[o.matrix_world@o.data.vertices[i].co for i in tri.vertices]
   if max(v.z for v in p)-min(v.z for v in p)<.00001 and (p[1]-p[0]).cross(p[2]-p[0]).z>0:out.append((p[0].z,[(v.x,-v.y) for v in p]))
  return out
 for o in pool:
  role=o.get('role','');isfloor=role in ['floor','authored complete bay floor or enclosure'] or role=='retained clipped plate outside reserved bay' and 'support ' not in o.name
  if isfloor:
   bottom=bb(o)[4]
   for y,poly in upward_polys(o):owners.setdefault(round(y,4),[]).append((o.name,poly,bottom))
 for o in list(pool):
  role=o.get('role','');ispad=role.startswith('rough route support') or role=='retained clipped plate outside reserved bay' and 'support ' in o.name
  if not ispad:continue
  if not any(c>f[0]+1e-4 and a<f[2]-1e-4 and d>f[1]+1e-4 and b<f[3]-1e-4 for f,_,_ in plan['floors'] for a,b,c,d,lo,hi in [bb(o)]):continue
  a,b,c,d,lo,hi=bb(o)
  if hi-lo>.3:continue
  polys=[p for y,p in upward_polys(o) if abs(y-hi)<.00001];remaining=list(polys);used=[]
  if not remaining:continue
  for name,poly,bottom in owners.get(round(hi,4),[]):
   if bottom>lo+.00001:continue
   old_area=sum(area(p) for p in remaining);remaining=[q for p in remaining for q in difference(p,poly)]
   if old_area-sum(area(p) for p in remaining)>1e-9:used.append(name)
   if not remaining:break
  unowned=sum(area(p) for p in remaining)
  if unowned<1e-9:
   pad_proof.append(dict(name=o.name,actual_floor_owners=sorted(set(used)),top=hi,bottom=lo,unowned_top_area=unowned));retire(o)
 # The old lower-storey walls/headers ended at the UPPER walking face,
 # showing through it as coplanar stripes. Seat those tops inside the slab.
 # 0.12m setback still embeds them in retained 0.16m neighbour slabs as well
 # as the new 0.30m slabs; it never opens a gap under an adjacent old floor.
 wall_caps=[]
 for o in [x for x in old if x.name not in removed]+list(built):
  role=o.get('role','')
  if not ('wall' in role or 'header' in role):continue
  a,b,c,d,lo,hi=bb(o)
  if hi-lo<.20:continue
  if not any(abs(hi-y)<.025 and c>f[0]+1e-4 and a<f[2]-1e-4 and d>f[1]+1e-4 and b<f[3]-1e-4 for f,y,_ in plan['floors']):continue
  name=o.name
  if o in old:
   n=o.copy();n.data=o.data.copy();n.name=site+' seated wall cap '+str(len(wall_caps));bpy.context.collection.objects.link(n);retire(o);o=n;built.append(o)
  points=[o.matrix_world@v.co for v in o.data.vertices]
  for v,p in zip(o.data.vertices,points):
   if abs(p.z-hi)<.00001:p.z-=.12
   v.co=p
  o.matrix_world.identity();o['role']=role;o['wall_cap_seated']=True;wall_caps.append(dict(original=name,result=o.name,old_top=hi,new_top=hi-.12))
 mapping=copy.deepcopy(meta.get('source_segment_map',{}))
 for link in selected:
  prev=next(l for l in before['links'] if l['id']==link);i=next(i for i,(a,b) in enumerate(zip(prev['points'],prev['points'][1:])) if abs(a[1]-b[1])>.01);identity=mapping.get(link,{}).get(str(i),i)
  revised=by[link]['points'];j=next(i for i,(a,b) in enumerate(zip(revised,revised[1:])) if abs(a[1]-b[1])>.01);mapping[link]={str(j):identity}
 # Filter old doorway metadata at replaced faces, then attach actual new frames.
 def replaced_portal(p):return any(p.get('axis')==axis and abs(p.get('boundary',1e6)-fixed)<.3 and start<=p.get('along',1e6)<=end and lo-.01<=p.get('sill_y',1e6)<hi-.01 for axis,fixed,start,end,lo,hi in replaced_faces)
 data['authored_doorways']=[p for p in data.get('authored_doorways',[]) if not replaced_portal(p)]+portals
 assert sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones'])<=40
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6)
  if o.get('role')=='retained clipped old wall':
   bmesh.ops.dissolve_limit(bm,angle_limit=.000001,use_dissolve_boundaries=False,verts=list(bm.verts),edges=list(bm.edges));bmesh.ops.triangulate(bm,faces=list(bm.faces));bmesh.ops.dissolve_degenerate(bm,dist=.000001,edges=list(bm.edges))
  bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name);bm.to_mesh(o.data);bm.free()
 layout=out/'authored-layout.json';write(layout,data);bpy.ops.wm.save_as_mainfile(filepath=str(out/'authored-bays.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build/household-sacred-addition.glb';bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True)
 append(ROOT/row['glb'],addition,out/'authored-bays.glb',remove_mesh_names=sorted(removed));native_write(ROOT/row['native'],out/'authored-bays.hcs2',removed,built)
 original=split_native(ROOT/row['native']);after=split_native(out/'authored-bays.hcs2');kept=[p for p in original if p[0].split(':',1)[-1] not in removed];assert after[:len(kept)]==kept
 write(out/'quarantine.json',dict(original_source=row,original_source_sha256=sha(ROOT/row['glb']),removed_meshes=sorted(removed),reason='Replace generated stairs, overlapping slabs/pads and cut wall fragments with deliberate architectural bays; original source retained'))
 meta.update(status='ARCHITECTURAL_BAY_REPLACEMENT_NATIVE_REVIEW_OPEN',source_glb=row['glb'],source_glb_sha256=sha(ROOT/row['glb']),source_physics=row['native'],source_physics_sha256=sha(ROOT/row['native']),before_layout=row['layout'],before_layout_sha256=sha(ROOT/row['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=meta['stair_components'],stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed]+steps,interface_added_flights=[],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(out/'authored-bays.glb'),physics_sha256=sha(out/'authored-bays.hcs2'),blend_sha256=sha(out/'authored-bays.blend'),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],architectural_plan=plan,doorways=[p for p in meta.get('doorways',[]) if not replaced_portal(p)]+portals,bay_builder='tools/build_college_service_bays.py',bay_builder_sha256=sha(Path(__file__)),retained_native_records=len(kept),source_terrain='Exact original saved editor owner and holes retained; no excavations')
 meta['removed_coplanar_route_solids']=pad_proof;meta['wall_caps_seated_under_floors']=wall_caps;write(out/'construction.json',meta);print('BUILT_ARCHITECTURAL_BAYS',site,len(removed),'retired',len(built),'closed additions',len(pad_proof),'coplanar pads removed',len(wall_caps),'wall caps seated',flush=True)
 return dict(id=site,construction=(out/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(out/'authored-bays.glb').relative_to(ROOT).as_posix(),native=(out/'authored-bays.hcs2').relative_to(ROOT).as_posix(),blend=(out/'authored-bays.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def review(row):
 from append_static_glb import read
 from export_roster_collision import face_keys
 from check_roster_decks import read_parts
 out=(ROOT/row['construction']).parent;m=json.loads((out/'construction.json').read_text());glb=ROOT/row['glb'];native=ROOT/row['native']
 a,blob=read(ROOT/m['source_glb']);b,after=read(glb);assert after[:len(blob)]==blob
 for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
 removed=set(m['removed_source_meshes'])
 for i,n in enumerate(a['nodes']):
  expected=dict(n)
  if n.get('name') in removed:expected.pop('mesh',None)
  assert b['nodes'][i]==expected,('Changed source node',i)
 old=split_native(ROOT/m['source_physics']);new=split_native(native);keep=[v for v in old if v[0].split(':',1)[-1] not in removed];assert new[:len(keep)]==keep
 assert [v for v in old if 'terrain' in v[0].lower()]==[v for v in new if 'terrain' in v[0].lower()]
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};parts=read_parts(native);treads=0
 for name in m['new_interface_parts']:
  o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]),name
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.free()
  if o.get('role')=='structural-stair-step':
   f=next(f for f in m['architectural_plan']['flights'] if f[0]==o['link']);_,foot,x,z0,z1,y0,y1,width,n=f;p=[o.matrix_world@v.co for v in o.data.vertices];assert all(foot[0]-.001<=v.x<=foot[2]+.001 and foot[1]-.001<=-v.y<=foot[3]+.001 for v in p);assert abs(max(v.x for v in p)-min(v.x for v in p)-width)<2e-5;treads+=1
 assert treads==sum(f[-1] for f in m['architectural_plan']['flights'])
 C=B/'captures/service-ground-bay-batch';C.mkdir(parents=True,exist_ok=True);sc=bpy.context.scene;sc.render.engine='BLENDER_WORKBENCH';sc.display.shading.color_type='MATERIAL';sc.display.shading.show_cavity=True;sc.render.resolution_x=1400;sc.render.resolution_y=1050;sc.render.resolution_percentage=100;sc.view_settings.view_transform='Standard';sc.world=bpy.data.worlds.new('College architectural review');sc.world.color=(.08,.08,.08);bpy.ops.object.camera_add();cam=bpy.context.object;sc.camera=cam;captures=[]
 views=[('library-cutaway',(31,-10,2),(44,-26,23),20,'ORTHO',3.4),('library-foot-standing',(31,-8,1.8),(31,-5.1,1.62),24,'PERSP',None),('library-head-standing',(31,-16,4.7),(31,-12.8,5.22),24,'PERSP',None)]
 if not PILOT:views += [('teaching-cutaway',(27,-40,0),(42,-58,23),23,'ORTHO',3.4),('teaching-cellar-cutaway',(27,-40,-1),(42,-58,20),23,'ORTHO',-.4),('teaching-foot-standing',(28.5,-39,1.8),(28.5,-35.2,1.62),24,'PERSP',None),('teaching-head-standing',(24,-44,4.8),(28.5,-44,5.22),24,'PERSP',None),('cellar-foot-standing',(25.2,-39,-1.5),(25.2,-44,-1.38),24,'PERSP',None)]
 for name,target,pos,scale,kind,cut in views:
  hidden=[]
  for o in objs.values():
   o.hide_render=False
   if cut is not None:
    p=[o.matrix_world@v.co for v in o.data.vertices];lo=min(v.z for v in p);role=o.get('role','');o.hide_render='terrain' in role or 'roof' in role or lo>cut
    if role=='complete reserved bay wall':
     a,bb,c,d,l,h=globals()['bb'](o)
     if (c-a<.3 and a>29) or (d-bb<.3 and bb>15):o.hide_render=True
   if name=='teaching-cellar-cutaway' and ('L80' in o.name or o.get('link')=='L80'):o.hide_render=True
   if o.hide_render:hidden.append(o.name)
  target=Vector(target);cam.location=Vector(pos);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type=kind;cam.data.ortho_scale=scale;cam.data.lens=scale;cam.data.clip_start=.02;cam.data.clip_end=250;path=C/('college-'+('pilot-' if PILOT else '')+name+'.png');sc.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(site='COLLEGE',path=path.relative_to(ROOT).as_posix(),sha256=sha(path),glb_sha256=sha(glb),view=name,hidden=hidden,type='Native source export; standing views hide nothing; cutaway hides named terrain/overhead/faces; not runtime'))
 write(out/'captures.json',captures);write(out/'export-verification.json',dict(closed_positive_new_solids=len(m['new_interface_parts']),constant_width_treads=treads,retained_native_records=len(keep),retained_source_buffer_asset_arrays_nodes_exact=True,terrain_saved_holes_unchanged=True,export_native_triangles_match=True,runtime_acceptance=False))
 print('COLLEGE_EXPORT_REVIEW_PASS',len(m['new_interface_parts']),treads,flush=True)

def diagram(row):
 if PILOT:return
 # Building floor ownership is the source of this drawing; arrows are descriptive.
 out=(ROOT/row['construction']).parent;plan=PLANS['COLLEGE'];elements=[]
 def text(x,y,t,size=16):elements.append(f'<text x="{x}" y="{y}" font-size="{size}" fill="#e5e7eb">{t}</text>')
 def r(x,y,w,h,fill,stroke='#8795a7'):elements.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
 text(30,35,'COLLEGE | architectural stair bays and circulation',24);text(30,60,'Metres. Thick slabs, normal doorways and independent rectangular stair openings precede movement.',15)
 panels=[('Library ground', [28,4,34,16],0,35),('Library upper',[28,4,34,16],3.6,220),('Teaching cellar',[24,34,30,46],-3,405),('Teaching ground',[24,34,30,46],0,590),('Teaching upper',[24,34,30,46],3.6,775)]
 scale=18
 for title,foot,level,left in panels:
  top=110;text(left,95,title,16);text(left,350,f'Floor {level:+.1f} m',14);text(left,370,'6.0 x 12.0 m reserved bay',12);r(left,top,6*scale,12*scale,'#364352')
  holes=next(h for f,y,h in plan['floors'] if f==foot and abs(y-level)<.001)
  for h in holes:r(left+(h[0]-foot[0])*scale,top+(h[1]-foot[1])*scale,(h[2]-h[0])*scale,(h[3]-h[1])*scale,'#0b101a','#f5a854')
  for link,flightfoot,x,z0,z1,y0,y1,width,n in plan['flights']:
   if flightfoot!=foot or level<y0 or level>y1:continue
   minz=min(z0,z1);r(left+(x-width/2-foot[0])*scale,top+(minz-foot[1])*scale,width*scale,abs(z1-z0)*scale,'#9b793e','#d5b471')
   for i in range(1,n):
    z=z0+(z1-z0)*i/n;elements.append(f'<path d="M{left+(x-width/2-foot[0])*scale},{top+(z-foot[1])*scale} h{width*scale}" stroke="#ead1a0" stroke-width=".7"/>')
   text(left+4,top+12,link,10)
  room=next(doors for f,lo,hi,doors in plan['rooms'] if f==foot and lo==level)
  for side,doors in room.items():
   for center,width in doors:
    if side=='S':xx=left+(center-width/2-foot[0])*scale;yy=top+12*scale;dx=width*scale;dy=0
    elif side=='W':xx=left;yy=top+(center-width/2-foot[1])*scale;dx=0;dy=width*scale
    elif side=='E':xx=left+6*scale;yy=top+(center-width/2-foot[1])*scale;dx=0;dy=width*scale
    else:xx=left+(center-width/2-foot[0])*scale;yy=top;dx=width*scale;dy=0
    elements.append(f'<path d="M{xx},{yy} l{dx},{dy}" stroke="#6ee7b7" stroke-width="7"/>')
 text(30,420,'Teaching bay longitudinal sections',19)
 # The cellar lane and upper lane are side by side, each in the same reserved building footprint.
 for label,x0,y0,y1,z0,z1,left in [('Cellar lane x25.20',25.2,-3,0,42,37.35,30),('Upper lane x28.50',28.5,0,3.6,36.6,42.36,480)]:
  top=470;scl=24;text(left,448,label,15)
  for level in [-3,0,3.6,7.2]:
   yy=top+(7.2-level)*scl;holes=next(hs for f,y,hs in plan['floors'] if f==[24,34,30,46] and abs(y-level)<.001);cuts=sorted((h[1],h[3]) for h in holes if h[0]<x0<h[2]);cursor=34
   for zlo,zhi in cuts:
    if zlo>cursor:r(left+(cursor-34)*scl,yy,(zlo-cursor)*scl,7,'#59697c')
    cursor=zhi
   if cursor<46:r(left+(cursor-34)*scl,yy,(46-cursor)*scl,7,'#59697c')
   text(left+12*scl+9,yy+6,f'{level:+.1f} m',12)
  zz=min(z0,z1);px1=left+(z0-34)*scl;px2=left+(z1-34)*scl;py1=top+(7.2-y0)*scl;py2=top+(7.2-y1)*scl;elements.append(f'<path d="M{px1},{py1} L{px2},{py2}" stroke="#e0b16b" stroke-width="7"/>')
  text(left,740,'Continuous supported waist; clear upper opening',12)
 text(30,790,'Circulation follows the constructed doorways and landings',19)
 text(30,820,'Library: issue desk -> south level door -> west bypass -> L81 -> upper landing -> issue or west bridge door',14)
 text(30,848,'Teaching: west hall -> south level door (z44) -> central bypass -> L80 -> upper south landing -> west hall',14)
 text(30,876,'Cellar: west tunnel -> south west door (z44) -> low landing -> L84 -> north landing -> central bypass',14)
 text(30,910,'Orange: reserved void / stair. Green: level door. Roof enclosures retained; no terrain excavation or route-carved opening.',12)
 svg='<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="940" viewBox="0 0 1000 940"><rect width="1000" height="940" fill="#151c27"/><g font-family="Segoe UI,Arial">'+''.join(elements)+'</g></svg>'
 (out/'architectural-plan-section-flow.svg').write_text(svg,encoding='utf-8')

def main():
 r=json.loads(BASE.read_text());row=build(next(s for s in r['active_sources'] if s['id']=='COLLEGE'));out=ROOT/row['construction'];write(out.parent/'row.json',row);review(row);diagram(row)
if __name__=='__main__':main()
