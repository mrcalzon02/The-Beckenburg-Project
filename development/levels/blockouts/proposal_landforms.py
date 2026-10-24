"""Terrain-first proposal massing. Blender coordinates: X, -source Z, source Y.
Only selected new layouts carrying a terrain contract use these functions.
No terrain mass is navigation acceptance; water is opaque review geometry.
"""
import bpy,math,json
from mathutils import Vector

def mesh(name,vertices,faces,c,mat,role):
 m=bpy.data.meshes.new(name);m.from_pydata(vertices,[],faces);m.update()
 o=bpy.data.objects.new(name,m);c.objects.link(o);m.materials.append(mat);o['role']=role
 # Planar metre UVs, no procedural black/gray stripe material.
 uv=m.uv_layers.new(name='Metre plan UV')
 for poly in m.polygons:
  for li in poly.loop_indices:
   v=m.vertices[m.loops[li].vertex_index].co;uv.data[li].uv=(v.x/8,v.y/8)
 return o

def shaped_slab(z,c,mat):
 a,b,d,e=z['bounds'];cx=(a+d)/2;cz=(b+e)/2;r=z['radius'];inner=z.get('inner_radius',0);n=64;vv=[]
 # Circular tower landings retain a central one-metre maintenance/core void.
 inner=inner or (1.0 if z.get('shape')=='circle' and z['y']>0 else 0)
 for y in [z['y']-.16,z['y']]:
  for rad in [inner,r]:
   for i in range(n):
    t=2*math.pi*i/n;vv.append((cx+rad*math.cos(t),-(cz+rad*math.sin(t)),y))
 ff=[]
 for i in range(n):
  j=(i+1)%n
  ff += [(2*n+i,3*n+i,3*n+j,2*n+j),(i,j,n+j,n+i),(n+i,n+j,3*n+j,3*n+i),(i,2*n+i,2*n+j,j)]
 o=mesh('Floor '+z['id'],vv,ff,c,mat,'160mm shaped floor; actual ring void retained');o['zone']=z['id']
 # Source X/-Z reverses winding: enforce upward top normals.
 if o.data.polygons[0].normal.z<0:
  for p in o.data.polygons:p.flip()
 return o

def shaped_wall(z,c,mat,data):
 a,b,d,e=z['bounds'];cx=(a+d)/2;cz=(b+e)/2;r=z['radius'];n=64
 # Only real external passage openings break the round shell; internal spiral stays inside.
 angles=[]
 for link in data['links']:
  if z['id'] not in [link['a'],link['b']]:continue
  other=link['b'] if link['a']==z['id'] else link['a']
  if other.startswith('tower'):continue
  q=link['points'][-1] if link['a']==z['id'] else link['points'][0]
  angles.append(math.atan2(q[2]-cz,q[0]-cx))
 vv=[];ff=[]
 for i in range(n):
  t=2*math.pi*(i+.5)/n
  if any(abs(math.atan2(math.sin(t-v),math.cos(t-v)))<.22 for v in angles):continue
  k=len(vv)
  for y in [z['y'],z['y']+z['height']]:
   for rad in [r-.08,r+.08]:
    for j in [i,i+1]:
     t=2*math.pi*j/n;vv.append((cx+rad*math.cos(t),-(cz+rad*math.sin(t)),y))
  ff.extend(tuple(k+j for j in f) for f in [(0,1,3,2),(4,6,7,5),(0,4,5,1),(2,3,7,6),(0,2,6,4),(1,5,7,3)])
 mesh('Round shell '+z['id'],vv,ff,c,mat,'rough tower shell; real passage opening reservation')

def keep_slab(z,data):
 if z['kind']!='outside':return True
 return bool(z.get('shape')) or z['id'] in ('porch','aft','fore','side','portpier','quay','westwalk','eastwalk','northwalk')

def inside(x,z,poly):
 hit=False;j=len(poly)-1
 for i in range(len(poly)):
  a,b=poly[i];c,d=poly[j]
  if (b>z)!=(d>z) and x<(c-a)*(z-b)/(d-b)+a:hit=not hit
  j=i
 return hit

def distance_rect(x,z,b):return math.hypot(max(b[0]-x,0,x-b[2]),max(b[1]-z,0,z-b[3]))
def segment(x,z,p,q):
 dx=q[0]-p[0];dz=q[2]-p[2];t=max(0,min(1,((x-p[0])*dx+(z-p[2])*dz)/(dx*dx+dz*dz or 1)))
 return math.hypot(x-p[0]-dx*t,z-p[2]-dz*t),p[1]+t*(q[1]-p[1])

def build_landform(data):
 from build_blockouts import material,collection
 spec=data['terrain'];kind=spec['profile'];c=collection('TERRAIN FIRST / authored landform and reservations')
 ground=material('Massing soil and weathered rock',(.28,.32,.23));water=material('Opaque water review placeholder',(.10,.24,.30));concrete=material('Inert liner and quay',(.30,.33,.34))
 a,b,d,e=data['site_bounds'];step=2;nx=math.ceil((d-a)/step);nz=math.ceil((e-b)/step)
 pads=[z for z in data['zones'] if z['y']>=0 and (z['kind'] not in ('outside',) or z['id'] in ('yard',)) and not z['id'].startswith('tower')]
 if kind=='battery':pads=[z for z in pads if z['y']>=4 or z['id']=='stage']
 if kind=='quay':pads=[z for z in pads if z['id']=='stage']
 segs=[(p,q,l['width']) for l in data['links'] if not (l['a'].startswith('tower') or l['b'].startswith('tower')) for p,q in zip(l['points'],l['points'][1:]) if min(p[1],q[1])>=0]
 def height(x,z):
  if kind=='headland':h=.5+.8*math.sin(z*.07)+.5*math.sin(x*.16);h-=max(0,abs(x-33)-22)*.9+max(0,4-z)*.5
  elif kind=='quay':return -5+.25*math.sin(x*.2) if z<60 else min(0,(z-64)*.5)
  elif kind=='battery':h=max(0,min(5,(78-z)*.13))+.7*math.sin(x*.13)*math.sin(z*.1)
  elif kind=='silo':h=.6+.6*math.sin(x*.16)+.35*math.cos(z*.12)
  elif kind=='roadside':h=.2+.7*math.cos(x*.12)+.3*math.sin(z*.13)-.6*math.exp(-((x-54)/3)**2)
  else:h=.3+.5*math.sin(x*.1)*math.cos(z*.09)
  nearest=min(pads,key=lambda v:distance_rect(x,z,v['bounds']))
  dist=distance_rect(x,z,nearest['bounds']);weight=max(0,min(1,1-dist/4));weight=weight*weight*(3-2*weight)
  h=h*(1-weight)+(nearest['y']-.18)*weight
  best=None
  for p,q,w in segs:
   dist,y=segment(x,z,p,q)
   if best is None or dist<best[0]:best=(dist,y,w)
  if best and best[0]<best[2]/2+1.4:
   dist,y,w=best;t=max(0,min(1,(dist-w/2)/1.4));t=t*t*(3-2*t);h=(y-.1)*(1-t)+h*t
  return h
 vv=[]
 for iz in range(nz+1):
  z=b+(e-b)*iz/nz
  for ix in range(nx+1):
   x=a+(d-a)*ix/nx
   # Organic external edge, all authored reservations stay safely inside the site.
   if ix==0:x+=.6+.5*math.sin(z*.2)
   if ix==nx:x-=.6+.5*math.cos(z*.17)
   vv.append((x,-z,height(x,z)))
 ff=[]
 shaft=spec.get('shaft')
 for iz in range(nz):
  for ix in range(nx):
   k=iz*(nx+1)+ix;face=(k,k+1,k+nx+2,k+nx+1);x=sum(vv[v][0] for v in face)/4;z=-sum(vv[v][1] for v in face)/4
   if shaft and math.hypot(x-shaft['center'][0],z-shaft['center'][1])<shaft['radius']+1.2:continue
   ff.extend([(face[0],face[2],face[1]),(face[0],face[3],face[2])])
 o=mesh('Authored '+kind+' terrain',vv,ff,c,ground,'coarse landform; foundation and incised route grading reservations');o['terrain_contract']=json.dumps(spec);o['acceptance']='Massing only; full-width normals, swept traversal and runtime integration pending'
 if 'water_y' in spec:
  y=spec['water_y'];wx=a-8;ex=d+8;wz=b-8;ez=e+8 if kind=='headland' else 61
  vv2=[];ff2=[];n=50;m=40
  for j in range(m+1):
   for i in range(n+1):vv2.append((wx+(ex-wx)*i/n,-(wz+(ez-wz)*j/m),y))
  poly=spec.get('hull_outline')
  for j in range(m):
   for i in range(n):
    k=j*(n+1)+i;f=(k,k+1,k+n+2,k+n+1)
    # Exclude any cell touching the ship outline, so this placeholder cannot fill the hold.
    if poly and (any(inside(vv2[v][0],-vv2[v][1],poly) for v in f) or inside(sum(vv2[v][0] for v in f)/4,-sum(vv2[v][1] for v in f)/4,poly)):continue
    ff2.append(tuple(reversed(f)))
  mesh('WATER PLACEHOLDER / layers and transparency pending',vv2,ff2,c,water,'opaque review only; not runtime water')
 if kind=='quay':
  poly=spec['hull_outline'];vv3=[(x,-z,y) for y in [-4.6,.05] for x,z in poly];n=len(poly)
  ff3=[tuple(reversed(range(n)))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
  mesh('Pointed retired vessel hull / empty interior',vv3,ff3,c,concrete,'static hull mass; watertight engineering and deck stairs pending')
 if shaft:
  cx,cz=shaft['center'];rad=shaft['radius'];n=96;vv4=[(cx+rad*math.cos(i*2*math.pi/n),-(cz+rad*math.sin(i*2*math.pi/n)),y) for y in [shaft['bottom'],0] for i in range(n)]
  ff4=[tuple(range(n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
  mesh('EMPTY SHAFT / guard engineering pending',vv4,ff4,c,concrete,'inert empty shaft liner; no operational equipment')
