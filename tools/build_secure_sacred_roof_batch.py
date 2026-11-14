"""Prison cellhouse and sacred/cloister roofs on current measured sources."""
from pathlib import Path
import sys,json
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import build_roster_enclosed_roofs as roof
BASE=ROOT/'development/levels/rebuild';roof.OUT=BASE/'secure-sacred-roof-pass'
roof.LONG_AXIS_RIDGES=True;roof.FACADE_CLOSURES=True
roof.HIP_WINGS={('CHURCH','belfry'),('WLD-10','bell-tower')}
roof.WINGS={
'PRISON':[('west-cellhouse',10,22,22,58,6.8,2.4),('east-cellhouse',46,22,58,58,6.8,2.4),('north-cross',10,16,58,22,3,1.1),('food-and-medical',10,4,46,16,3,2.4),('isolation',58,16,70,34,3,2.2),('chapel',58,34,70,52,4,2.5),('intake-and-visitors',0,58,26,68,3,1.8),('sally-port',26,58,42,68,4,2),('guard-and-staff',42,58,58,68,3,1.8),('plant-stair',28,22,32,34,3,.8)],
'CHURCH':[('nave-and-choir',18,12,26,34,6,3),('vestibule',18,34,26,39,3,1.5),('chancel',18,6,26,12,5,2.2),('side-chapel',26,14,34,26,4,2.2),('vestry',26,6,34,14,3,1.8),('music-and-crypt-entry',26,26,34,34,6,1.8),('belfry',10,26,18,34,9,3.5),('confessional-and-consultation',10,18,18,26,3,1.8)],
'WLD-10':[('gatehouse',24,56,40,62,3,1.3),('west-cloister',8,22,14,50,3,1.1),('east-cloister',44,22,50,50,3,1.1),('north-cloister',8,16,50,22,3,1.1),('south-cloister',8,50,50,56,3,1.1),('archive',8,2,28,16,3,3),('vestry',28,2,44,16,3,2.6),('refectory',50,30,68,50,3,3.2),('dormitory',-8,22,8,50,3,3),('bell-tower',50,16,60,30,9,3.6)]}
def main():
 roster=json.loads((BASE/'institution-roof-pass/roster.json').read_text());sources=[]
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
  p=ROOT/result['construction'];r=json.loads(p.read_text());r['aboveground_indoor_roof_probes']=checks;r['status']='SECURE_SACRED_WING_ROOF_CONSTRUCTION_CANDIDATE_RUNTIME_OPEN';r['wing_plan_builder']='tools/build_secure_sacred_roof_batch.py';r['wing_plan_sha256']=roof.sha(Path(__file__));r['remaining']='Roof-step flashing/drainage, structural join review, complete terrain/room connectivity and actual runtime integration remain open. Roofs do not add accessible search spaces or satisfy availability gates.';roof.write(p,r);sources.append(result)
 decisions=[dict(id=s['id'],decision='SECURE_SACRED_WING_ROOFS_CONSTRUCTED_NATIVE_CANDIDATE' if s['id'] in roof.WINGS else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']];roof.write(roof.OUT/'roster.json',dict(active_sources=sources,sites=decisions,baseline='development/levels/rebuild/institution-roof-pass/roster.json'));print('SECURE_SACRED_ROOFS_COMPLETE',list(roof.WINGS),flush=True)
if __name__=='__main__':main()
