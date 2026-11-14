"""Construct the two missing Cinder shaft galleries and rectangular room throats."""
import bpy,math
from pathlib import Path
from planar_structure import plate
from build_roster_floor_closures import difference
from build_roster_foundations import material
from review_roster_macroterrain import sha
RAIL=Path(__file__).resolve().parents[1]/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def construct_galleries(built,concrete):
 def slab(name,poly,y,depth,role,zone):
  for o in plate('WLD-18 '+name,[poly],y,depth,concrete):o['role']=role;o['zone']=zone;built.append(o)
 def annulus(name,inner,outer,top,depth,zone):
  n=64;vs=[]
  for y,radius in [(top,outer),(top,inner),(top-depth,outer),(top-depth,inner)]:
   vs.extend((32+radius*math.cos(2*math.pi*i/n),-(28+radius*math.sin(2*math.pi*i/n)),y) for i in range(n))
  fs=[]
  for i in range(n):
   j=(i+1)%n;fs.extend([(i,j,n+j,n+i),(2*n+i,3*n+i,3*n+j,2*n+j),(i,2*n+i,2*n+j,j),(n+i,n+j,3*n+j,3*n+i)])
  mesh=bpy.data.meshes.new(name);mesh.from_pydata(vs,[],fs);mesh.materials.append(concrete);mesh.update();uv=mesh.uv_layers.new(name='World metre gallery concrete')
  for f in mesh.polygons:
   for li in f.loop_indices:
    p=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(p.x/4,p.y/4)
  o=bpy.data.objects.new('WLD-18 '+name,mesh);bpy.context.collection.objects.link(o);o['role']='closed annular gallery floor or roof';o['zone']=zone;built.append(o)
 previous=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));template=next(o for o in set(bpy.context.scene.objects)-previous if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-previous:bpy.data.objects.remove(o,do_unlink=True)
 for zone,y in [('upperring',-4),('lowring',-8)]:
  annulus(zone+' full continuous gallery floor',8.2,10.24,y,.30,zone);annulus(zone+' enclosed gallery ceiling',8.2,10.24,y+3,.30,zone)
  for i in range(64):
   if i not in [0,1,30,31,32,33,62,63]:
    a,b=2*math.pi*i/64,2*math.pi*(i+1)/64
    foot=[(32+r*math.cos(t),28+r*math.sin(t)) for r,t in [(10,a),(10.24,a),(10.24,b),(10,b)]]
    slab(zone+' curved retaining wall '+str(i),foot,y+3,4.3 if y==-4 else 3.3,'gallery retaining wall',zone)
   a,b=2*math.pi*i/64,2*math.pi*(i+1)/64;p=(32+8.4*math.cos(a),28+8.4*math.sin(a));q=(32+8.4*math.cos(b),28+8.4*math.sin(b));length=math.dist(p,q);ux,uz=(q[0]-p[0])/length,(q[1]-p[1])/length;mesh=template.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*length/2;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[1]+uz*t+ux*off),y+v.co.z)
   o=bpy.data.objects.new('WLD-18 '+zone+' supported inner shaft guard '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='indexed supported annular shaft guard';o['zone']=zone;o['source_asset']='level_blockout_guardrail';o['source_asset_sha256']=sha(RAIL);built.append(o)
  for side,a,c,wa,wc in [('west',21.76,23.8,21.76,22.5),('east',40.2,42.24,41.5,42.24)]:
   slab(zone+' '+side+' doorway throat floor',rect(a,26,c,30),y,.30,'gallery throat structural floor',zone);slab(zone+' '+side+' enclosed throat roof',rect(a,26,c,30),y+3,.30,'gallery throat structural roof',zone)
   for b,d in [(26,26.24),(29.76,30)]:slab(zone+' '+side+' throat return '+str(b),rect(wa,b,wc,d),y+3,4.3 if y==-4 else 3.3,'gallery throat retaining return',zone)

 annulus('continuous outer gallery ring footing',9.85,10.6,-8.3,.45,'lowring')
 for poly in difference(rect(40,42,54,54),rect(50.9,42,53.1,47.2)):slab('shop floor with rectangular escape-stair well',poly,0,.30,'rectangular stair floor or roof','shop')
 # A compact supported U stair, with one rectangular well shared by its two flights.
 cut=rect(43.9,28,52.1,36)
 for top,zone,label in [(-4,'exhibit','upper floor'),(-5,'east','lower room ceiling')]:
  for poly in difference(rect(42,18,56,38),cut):slab('compact stair '+label,poly,top,.30,'rectangular stair floor or roof',zone)
 polys=[rect(8,26,22,38)]
 for cut in [rect(12,28.1,19,29.9)]:polys=[q for p in polys for q in difference(p,cut)]
 for p in polys:slab('west continuous upper floor and rectangular crew-stair well',p,-4,.30,'reconstructed upper floor with rectangular stair wells','west')
 slab('upper escape arrival landing',rect(50.9,38,53.1,41.6),-4,.30,'supported escape arrival slab','exhibit')
 slab('escape arrival bearing on lower plant north wall',rect(50.9,37.76,53.1,38.24),-4.3,.8,'escape arrival structural bearing','exhibit')
 for x in [50.9,52.9]:slab('escape arrival longitudinal beam '+str(x),rect(x,38,x+.2,41.6),-4.3,.4,'escape arrival structural bearing','exhibit')
 steps=[]
 for flight,x,z0,z1,y0,y1 in [(0,51,28,34,-4,-6),(1,45,34,28,-6,-8)]:
  for i in range(12):
   za=z0+(z1-z0)*i/12;zb=z0+(z1-z0)*(i+1)/12;top=y0+(y1-y0)*(i if y1<y0 else i+1)/12
   previous=len(built);slab('compact U stair flight'+str(flight)+' tread'+str(i),rect(x-.9,min(za,zb),x+.9,max(za,zb)),top,top+8.3,'structural-stair-step','exhibit' if flight==0 else 'east')
   for o in built[previous:]:o['link']='route-14';steps.append(o.name)
  # The lower end of the second flight meets the existing full lower-room floor.
 previous=len(built);slab('compact U stair solid middle landing',rect(43.9,34,52.1,36),-6,2.3,'structural-stair-landing','east')
 for o in built[previous:]:o['link']='route-14';steps.append(o.name)
 def guard(label,p,q):
  length=math.hypot(q[0]-p[0],q[2]-p[2]);ux,uz=(q[0]-p[0])/length,(q[2]-p[2])/length;n=math.ceil(length/2)
  for i in range(n):
   mesh=template.copy()
   for v in mesh.vertices:
    t=(v.co.x+1)*length/n/2+i*length/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),p[1]+(q[1]-p[1])*t/length+v.co.z)
   o=bpy.data.objects.new('WLD-18 compact U stair guard '+label+' '+str(i),mesh);bpy.context.collection.objects.link(o);o['role']='indexed supported rectangular stair-well guard';o['zone']='exhibit';o['source_asset']='level_blockout_guardrail';o['source_asset_sha256']=sha(RAIL);built.append(o)
 for label,p,q in [('west rim',(43.76,-4,27.86),(43.76,-4,36.14)),('east rim',(52.24,-4,28.4),(52.24,-4,36.14)),('back rim',(43.76,-4,36.14),(52.24,-4,36.14)),('head west return',(43.76,-4,27.86),(50.1,-4,27.86)),('head east return',(51.9,-4,27.86),(52.24,-4,27.86)),('middle back',(44.1,-6,35.86),(51.9,-6,35.86))]:guard(label,p,q)
 for flight,x,z0,z1,y0,y1 in [(0,51,28,34,-4,-6),(1,45,34,28,-6,-8)]:
  for side in [-.83,.83]:guard('flight'+str(flight)+' side'+str(side),(x+side,y0,z0),(x+side,y1,z1))
 return steps
