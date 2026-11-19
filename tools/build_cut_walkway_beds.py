"""Root missing pavement strips in actual shallow terrain after a cut.
Never fills a real stair shaft, deep/punched void or closed context volume.
"""
from pathlib import Path
import math,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from build_stair_landings import rect
from build_roster_floor_closures import difference,clean_polygon
from build_roster_decks import area
from build_roster_foundations import prism
from structural_pass import deck_openings
from review_roster_macroterrain import records
from build_stair_bays import intersection
def build(site,data,program,ground,terrain_faces):
 soil_polys=[];soil_triangles=[];soil_bins={}
 for source_index,(xyz,flags) in enumerate(terrain_faces):
  poly=[(xyz[i],xyz[i+2]) for i in [0,3,6]]
  if area(poly)<1e-8:continue
  index=len(soil_polys);soil_polys.append(poly);soil_triangles.append(source_index)
  for x in range(math.floor(min(p[0] for p in poly)/2),math.floor(max(p[0] for p in poly)/2)+1):
   for z in range(math.floor(min(p[1] for p in poly)/2),math.floor(max(p[1] for p in poly)/2)+1):soil_bins.setdefault((x,z),set()).add(index)
 links={l['id']:l for l in data['links']};objects=[o for o in bpy.context.scene.objects if o.type=='MESH'];material=next(o.data.materials[0] for o in objects if o.get('role','').startswith('rough route support') and o.data.materials);bins={};surfaces=[]
 def insert(poly):
  key=len(surfaces);surfaces.append(poly)
  for x in range(math.floor(min(p[0] for p in poly)/2),math.floor(max(p[0] for p in poly)/2)+1):
   for z in range(math.floor(min(p[1] for p in poly)/2),math.floor(max(p[1] for p in poly)/2)+1):bins.setdefault((x,z),set()).add(key)
 for obj in objects:
  for xyz,flags in records(obj):
   if flags&1 and max(abs(xyz[i+1]) for i in [0,3,6])<1e-5:insert([(xyz[i],xyz[i+2]) for i in [0,3,6]])
 cuts=[o['polygon'] for o in deck_openings(0,data)]
 for zone in data['zones']:
  if zone.get('player_access')!='SEALED_CONTEXT' or zone['y']>0 or zone['y']+zone['height']<=0:continue
  a,b,c,d=zone['bounds'];cuts.append([(a-.1,b-.1),(c+.1,b-.1),(c+.1,d+.1),(a-.1,d+.1)])
 requests=[]
 for entry in program:
  a,b=entry['points'];assert abs(a[1])<.01 and abs(b[1])<.01;link=links[entry['link']];run=math.hypot(b[0]-a[0],b[2]-a[2]);n=max(1,math.ceil(run/2))
  for i in range(n):
   p=[a[j]+(b[j]-a[j])*i/n for j in range(3)];q=[a[j]+(b[j]-a[j])*(i+1)/n for j in range(3)];requests.append((link['id'],rect(p,q,link['width']/2)))
 for key in sorted({e['link'] for e in program}):
  link=links[key];radius=link['width']/2
  for i,p in enumerate(link['points'][1:-1],1):
   if abs(p[1])>.01 or abs(link['points'][i-1][1])>.01 or abs(link['points'][i+1][1])>.01:continue
   requests.append((key,[(p[0]+radius*math.cos(2*math.pi*i/16),p[2]+radius*math.sin(2*math.pi*i/16)) for i in range(16)]))
 made=[];proof=[];deferred=[]
 for link,footprint in requests:
  polys=[footprint]
  for cut in cuts:polys=[q for p in polys for q in difference(p,cut)]
  candidates=set()
  for x in range(math.floor(min(p[0] for p in footprint)/2),math.floor(max(p[0] for p in footprint)/2)+1):
   for z in range(math.floor(min(p[1] for p in footprint)/2),math.floor(max(p[1] for p in footprint)/2)+1):candidates.update(bins.get((x,z),()))
  for index in sorted(candidates):polys=[q for p in polys for q in difference(p,surfaces[index])]
  soil_candidates=set()
  for x in range(math.floor(min(p[0] for p in footprint)/2),math.floor(max(p[0] for p in footprint)/2)+1):
   for z in range(math.floor(min(p[1] for p in footprint)/2),math.floor(max(p[1] for p in footprint)/2)+1):soil_candidates.update(soil_bins.get((x,z),()))
  clipped=[(intersection(p,soil_polys[i]),i) for p in polys for i in sorted(soil_candidates)];polys=[(p,i) for p,i in clipped if len(p)>2 and area(p)>1e-7]
  for poly,receiver in polys:
   poly=clean_polygon(poly)
   if len(poly)<3 or area(poly)<1e-7:continue
   span=max(math.dist(a,b) for a in poly for b in poly)
   if 2*area(poly)/span<.0002:continue
   probes=list(poly)+[((a[0]+b[0])/2,(a[1]+b[1])/2) for a,b in zip(poly,poly[1:]+poly[:1])]+[(sum(p[0] for p in poly)/len(poly),sum(p[1] for p in poly)/len(poly))];triangle_index=soil_triangles[receiver];xyz=terrain_faces[triangle_index][0];a,b,c=[xyz[i:i+3] for i in [0,3,6]];dx,dz=b[0]-a[0],b[2]-a[2];ex,ez=c[0]-a[0],c[2]-a[2];den=dx*ez-dz*ex;heights=[]
   for x,z in probes:
    u=((x-a[0])*ez-(z-a[2])*ex)/den;v=(dx*(z-a[2])-dz*(x-a[0]))/den;assert min(u,v)>=-1e-7 and u+v<=1+1e-7,'Pavement receiver is outside its exact native soil triangle';heights.append(a[1]+u*(b[1]-a[1])+v*(c[1]-a[1]))
   if any(h<-.6 or h>-.005 for h in heights):deferred.append(dict(link=link,footprint=poly,terrain_probe_points=probes,terrain_y=heights,receiver_native_triangle_index=triangle_index,reason='No complete shallow below-walking-plane soil receiver; requires a real bridge/deck/terrain design, not blind fill'));continue
   bottom=min(-.16,min(heights)-.15);name='Rooted cut walkway '+site+' '+str(len(made));obj=prism(name,poly,0,[bottom]*len(poly),material);obj['role']='structural-rooted-circulation-bed';obj['link']=link;made.append(obj);proof.append(dict(name=name,link=link,footprint=poly,top_y=0,bottom_y=bottom,terrain_probe_points=probes,terrain_y=heights,receiver_native_triangle_index=triangle_index,receiver_native_triangle_xyz=list(xyz),embedding_metres=.15,excluded_actual_shafts=True,excluded_sealed_context=True));insert(poly)
 return made,proof,deferred
