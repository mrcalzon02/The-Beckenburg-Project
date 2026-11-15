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
B=ROOT/'development/levels/rebuild';OUT=B/'household-sacred-bay-pass';BASE=B/'architectural-restart-pass/roster.json'
RAIL=B.parent/'primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bb(o):
 p=[o.matrix_world@v.co for v in o.data.vertices]
 return [min(v.x for v in p),min(-v.y for v in p),max(v.x for v in p),max(-v.y for v in p),min(v.z for v in p),max(v.z for v in p)]
# Fixed architectural schedules; these are deliberately independent of paths.
PLANS={
 'FARMHOUSE':dict(
  flights=[('L34',[20,12,26,22],24.5,13.6,18.4,0,3,1.5,17),('L35',[20,12,26,22],21.5,13.6,18.4,-3,0,1.5,17),('L36',[39,16,43,24],41,17.5,22.3,0,3,1.5,17)],
  floors=[([20,12,26,22],-3,[]),([20,12,26,22],0,[[20.55,13.35,22.45,18.4]]),([23,12,26,22],3,[[23.55,13.35,25.45,18.4]]),([23,12,26,22],6,[]),([39,16,43,24],0,[]),([39,16,43,24],3,[[40.05,17.25,41.95,22.3]]),([31,16,39,24],3,[]),([39,16,43,24],6,[])],
  rooms=[([20,12,26,22],-3,0,{'S':[(23,1.05)]}),([20,12,26,22],0,3,{'N':[(23,1.05)],'S':[(23,1.05)],'W':[(19.2,1.05)]}),([23,12,26,22],3,6,{'W':[(21,1.05)]}),([39,16,43,24],0,3,{'N':[(41,1.5)],'W':[(17.5,1.05)]}),([39,16,43,24],3,6,{'W':[(23,1.5)]})]),
 'MANSION':dict(
  flights=[('L58',[20,24,28,36],24,26,31.76,0,3.6,2,20),('L59',[40,2,46,14],41.5,10.4,5.75,-3,0,1.5,17),('L60',[40,2,46,14],44.5,5,10.25,0,3.6,1.5,20),('L61',[40,2,46,14],41.5,11,5,3.6,7.2,1.5,20)],
  floors=[([20,24,28,36],0,[]),([20,24,28,50],3.6,[[22.8,25.75,25.2,31.76]]),([40,2,46,14],-3,[]),([40,2,46,14],0,[[40.55,5.75,42.45,10.65]]),([40,2,46,14],3.6,[[43.55,4.75,45.45,10.25]]),([40,2,46,14],7.2,[[40.55,5,42.45,11.25]]),([40,2,46,14],10.2,[])],
  rooms=[([20,24,28,36],0,3.6,{'W':[(30,1.05)],'S':[(24,2)]}),([40,2,46,14],-3,0,{'S':[(41.5,1.05)]}),([40,2,46,14],0,3.6,{'N':[(43,1.05)],'S':[(41.5,1.05)]}),([40,2,46,14],3.6,7.2,{'S':[(41.5,1.05)]}),([40,2,46,14],7.2,10.2,{'S':[(41.5,1.05)]})]),
 'CHURCH':dict(
  flights=[('L33',[18,26,21,34],19.5,27.6,32.4,0,3,1.5,17),('L34',[10,26,18,34],16.5,27.6,32.4,3,6,1.5,17),('L35',[26,26,34,34],31.5,27.6,32.4,-3,0,1.5,17)],
  floors=[([18,26,26,34],0,[]),([18,26,21,34],3,[[18.55,27.35,20.45,32.4]]),([18,26,21,34],6,[]),([10,26,18,34],3,[]),([10,26,18,34],6,[[15.55,27.35,17.45,32.4]]),([10,26,18,34],9,[]),([26,26,34,34],-3,[]),([26,26,34,34],0,[[30.55,27.35,32.45,32.4]]),([26,26,34,34],3,[])],
  rooms=[([18,26,21,34],3,6,{'W':[(33.1,1.05)],'E':[(33.1,1.05)]}),([10,26,18,34],3,6,{'E':[(33.1,1.05)]}),([10,26,18,34],6,9,{}),([26,26,34,34],-3,0,{'W':[(30,1.05)]}),([26,26,34,34],0,3,{'N':[(28.5,1.05)]})])}

def build(row):
 site=row['id'];plan=PLANS[site];out=OUT/site.lower();out.mkdir(parents=True,exist_ok=True)
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
 if site=='FARMHOUSE':
  by['L34']['points']=[[23,0,17],[23,0,12.8],[24.5,0,12.8],[24.5,0,13.6],[24.5,3,18.4],[24.5,3,20]]
  by['L35']['points']=[[23,-3,17],[23,-3,12.8],[21.5,-3,12.8],[21.5,-3,13.6],[21.5,0,18.4],[21.5,0,20],[23,0,20],[23,0,17]]
  by['L36']['points']=[[39.5,0,17.5],[39.5,0,16.8],[41,0,16.8],[41,0,17.5],[41,3,22.3],[41,3,23]]
  by['L08']['points']=[[16,0,18],[18.5,0,18],[18.5,0,19.2],[20,0,19.2],[23,0,19.2],[23,0,17]]
  by['L13']['points']=[[21.5,3,19],[22,3,21],[23,3,21],[24.5,3,21],[24.5,3,20]]
  # Grounds-to-barn circulation belongs OUTSIDE the house. Retire the old
  # through-wall spur rather than punching an exit beside a rising flight.
  by['L22']['points']=by['L22']['points'][1:]
  by['L22']['points'][0]=[28.5,0,16.5]
  for o in list(built):
   if o.get('role')=='retained clipped plate outside reserved bay' and 'L22 support 0' in o.name:retire(o)
  # The loft door is on its complete south landing, clear of the flight side.
  for l in data['links']:
   if l['id'] not in selected and 'loft-landing' in [l['a'],l['b']]:
    if l['b']=='loft-landing':l['points']=[[35,3,20],[35,3,23],[39,3,23],[41,3,23]]
    else:l['points']=[[41,3,23],[39,3,23],[35,3,23],[35,3,20]]
  anchors={'upper-stair':[24.5,3,20],'loft-landing':[41,3,23],'grounds':[28.5,0,16.5]}
 elif site=='MANSION':
  by['L58']['points']=[[22.5,0,30],[22.5,0,25],[24,0,25],[24,0,26],[24,3.6,31.76],[24,3.6,37]]
  by['L59']['points']=[[43,-3,8],[43,-3,11.8],[41.5,-3,11.8],[41.5,-3,10.4],[41.5,0,5.75],[41.5,0,4.2],[43,0,4.2],[43,0,8]]
  by['L60']['points']=[[43,0,8],[43,0,4.2],[44.5,0,4.2],[44.5,0,5],[44.5,3.6,10.25],[44.5,3.6,11.5],[43,3.6,11.5],[43,3.6,8]]
  by['L61']['points']=[[43,3.6,8],[43,3.6,11.9],[41.5,3.6,11.9],[41.5,3.6,11],[41.5,7.2,5],[41.5,7.2,3.8],[43,7.2,3.8],[43,7.2,8]]
  by['L54']['points']=[[43,-3,8],[43,-3,12.5],[41.5,-3,12.5],[41.5,-3,14],[41.5,-3,21]]
  anchors={}
 else:
  by['L33']['points']=[[22,0,30],[22,0,26.8],[19.5,0,26.8],[19.5,0,27.6],[19.5,3,32.4],[19.5,3,33.1]]
  by['L34']['points']=[[14,3,30],[14,3,26.8],[16.5,3,26.8],[16.5,3,27.6],[16.5,6,32.4],[16.5,6,33.1],[14,6,33.1],[14,6,30]]
  by['L35']['points']=[[30,-3,30],[30,-3,26.8],[31.5,-3,26.8],[31.5,-3,27.6],[31.5,0,32.4],[31.5,0,33.1],[29,0,33.1],[29,0,30],[30,0,30]]
  by['L19']['points']=[[19.5,3,33.1],[21,3,33.1],[23.5,3,33.1],[23.5,3,30]]
  by['L20']['points']=[[19.5,3,33.1],[18,3,33.1],[14,3,33.1],[14,3,30]]
  by['L21']['points']=[[23.5,3,30],[23.5,3,32.7],[26,3,32.7],[27.5,3,32.7],[27.5,3,30]]
  by['L22']['points']=[[27.5,3,30],[27.5,3,28],[29,3,28],[31.5,3,28]]
  by['L23']['points']=[[27.5,3,30],[27.5,3,32],[29,3,32],[31.5,3,32]]
  anchors={'choir-stair':[19.5,3,33.1]}
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
 meta.update(status='ARCHITECTURAL_BAY_REPLACEMENT_NATIVE_REVIEW_OPEN',source_glb=row['glb'],source_glb_sha256=sha(ROOT/row['glb']),source_physics=row['native'],source_physics_sha256=sha(ROOT/row['native']),before_layout=row['layout'],before_layout_sha256=sha(ROOT/row['layout']),before_segment_map=meta.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=meta['stair_components'],stair_components=[n for n in meta['stair_components'] if n.split(':',1)[-1] not in removed]+steps,interface_added_flights=[],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(out/'authored-bays.glb'),physics_sha256=sha(out/'authored-bays.hcs2'),blend_sha256=sha(out/'authored-bays.blend'),removed_source_meshes=sorted(removed),new_interface_parts=[o.name for o in built],architectural_plan=plan,doorways=[p for p in meta.get('doorways',[]) if not replaced_portal(p)]+portals,bay_builder='tools/build_household_sacred_bays.py',bay_builder_sha256=sha(Path(__file__)),retained_native_records=len(kept),source_terrain='Exact original saved editor owner and holes retained; no excavations')
 meta['removed_coplanar_route_solids']=pad_proof;meta['wall_caps_seated_under_floors']=wall_caps;write(out/'construction.json',meta);print('BUILT_ARCHITECTURAL_BAYS',site,len(removed),'retired',len(built),'closed additions',len(pad_proof),'coplanar pads removed',len(wall_caps),'wall caps seated',flush=True)
 return dict(id=site,construction=(out/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(out/'authored-bays.glb').relative_to(ROOT).as_posix(),native=(out/'authored-bays.hcs2').relative_to(ROOT).as_posix(),blend=(out/'authored-bays.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads(BASE.read_text());changes={site:build(next(s for s in r['active_sources'] if s['id']==site)) for site in PLANS};r['active_sources']=[changes.get(s['id'],s) for s in r['active_sources']]
 for s in r['sites']:
  if s['id'] in PLANS:s['decision']='ARCHITECTURAL_BAYS_REBUILT_NATIVE_REVIEW_OPEN'
 r['baseline']=BASE.relative_to(ROOT).as_posix();write(OUT/'roster.json',r)
if __name__=='__main__':main()
