"""Enclose the working harbor and research buildings, preserving open yards."""
from pathlib import Path
import sys,json
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import build_roster_enclosed_roofs as roof
BASE=ROOT/'development/levels/rebuild';roof.OUT=BASE/'harbor-research-roof-pass'
roof.LONG_AXIS_RIDGES=True;roof.FACADE_CLOSURES=True;roof.HIP_WINGS=set()
roof.WINGS={
'WLD-06':[('west-cottage',4,42,20,54,3,2.2),('east-cottage',44,42,60,54,3,2.2),('boathouse',4,10,20,28,3,3),('net-store',44,10,56,24,3,2.4),('fish-house',24,10,40,24,3,2.6)],
'WLD-08':[('generator-building',2,8,20,26,3,3),('laboratory',2,26,20,44,3,3),('stores',2,44,20,56,3,2),('radio-records',20,12,44,26,3,2.6),('dormitory-and-mess',44,26,62,56,3,3),('remote-shelter',62,8,70,20,3,1.6)],
'WLD-13':[('processing-hall',24,26,48,56,6,4),('cold-store',48,26,64,44,3,2.8),('boiler-and-wash',8,26,24,56,3,3),('office-and-stores',24,14,48,26,3,2.2),('crew-mess-and-bunks',-8,26,8,62,3,3)]}
def main():
 roster=json.loads((BASE/'secure-sacred-roof-pass/roster.json').read_text());sources=[]
 for s in roster['active_sources']:
  if s['id'] not in roof.WINGS:sources.append(s);continue
  result=roof.build(s)
  # Check all authored above-ground indoor/context room footprints, not only listed roof wings.
  import bpy
  from mathutils import Vector
  from mathutils.bvhtree import BVHTree
  vs=[];fs=[]
  for obj in bpy.context.scene.objects:
   if obj.type!='MESH' or obj.get('role')!='enclosed pitched roof':continue
   offset=len(vs);vs.extend(obj.matrix_world@v.co for v in obj.data.vertices);fs.extend(tuple(offset+i for i in f.vertices) for f in obj.data.polygons)
  cover=BVHTree.FromPolygons(vs,fs);checks=0;layout=json.loads((ROOT/s['layout']).read_text())
  for z in layout['zones']:
   if z['kind'] in ['outside','staging'] or z['y']<0:continue
   a,b,c,d=z['bounds']
   for u in [.05,.5,.95]:
    for v in [.05,.5,.95]:
     x=a+(c-a)*u;zz=b+(d-b)*v;hit=cover.ray_cast(Vector((x,-zz,z['y']+z['height']-.12)),Vector((0,0,1)),50)[0];assert hit is not None,(s['id'],z['id'],x,zz,'missing authored roof');checks+=1
  p=ROOT/result['construction'];r=json.loads(p.read_text());r['aboveground_indoor_roof_probes']=checks;r['status']='HARBOR_RESEARCH_WING_ROOF_CONSTRUCTION_CANDIDATE_RUNTIME_OPEN';r['wing_plan_builder']='tools/build_harbor_research_roof_batch.py';r['wing_plan_sha256']=roof.sha(Path(__file__));r['remaining']='Roof-step flashing/drainage, structural join review, complete terrain/room connectivity and actual runtime integration remain open. Roofs do not add accessible search spaces or satisfy availability gates.';roof.write(p,r);sources.append(result)
 decisions=[dict(id=s['id'],decision='HARBOR_RESEARCH_WING_ROOFS_CONSTRUCTED_NATIVE_CANDIDATE' if s['id'] in roof.WINGS else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']];roof.write(roof.OUT/'roster.json',dict(active_sources=sources,sites=decisions,baseline='development/levels/rebuild/secure-sacred-roof-pass/roster.json'));print('HARBOR_RESEARCH_ROOFS_COMPLETE',list(roof.WINGS),flush=True)
if __name__=='__main__':main()
