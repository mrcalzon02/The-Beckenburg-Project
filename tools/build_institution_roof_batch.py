"""Enclose the measured institutional wings without changing occupied volumes."""
from pathlib import Path
import sys,json
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import build_roster_enclosed_roofs as roof
BASE=ROOT/'development/levels/rebuild';roof.OUT=BASE/'institution-roof-pass'
roof.WINGS={
'ACADEMY':[
 ('west-residential-library',7,9,22,53,7.2,2.5),('north-dormitory',22,4,46,19,7.2,2.5),
 ('east-cloister-spine',42,19,45,55,7.2,.7),('science-upper',45,19,57,29,7.2,2),
 ('east-teaching-stair',45,32,57,55,7.2,2.2),('science-vestibule',45,29,57,32,3.6,.6),
 ('north-cloister',22,19,42,22,3.6,.6),('south-cloister',22,42,42,45,3.6,.6),('reception',22,45,36,53,3.6,1.6),('service-intake',46,9,50,19,3.6,.8)],
'ASYLUM':[
 ('two-storey-ward',10,12,26,29,6.8,2.6),('upper-spine',26,12,29,40,6.8,.65),
 ('main-stair-tower',29,20,35,30,6.8,1.1),('lift-head',22,31,26,36,6.8,.8),
 ('staff-office',29,12,35,20,3.4,1),('day-pavilion',10,40,26,50,3.4,2.2),
 ('admission-spine',26,40,29,50,3.4,.6),('admission',26,50,35,58,3.4,1.3),
 ('interview',35,50,41,54,3.4,.8),('records-and-wc',35,54,44,58,3.4,.8),
 ('clinical-and-dining',29,30,54,50,3.4,2),('clinical-service',35,24,54,26,3.4,.4),
 ('clinical-stores',35,26,45,30,3.4,.7),('staff-rest',35,20,45,24,3.4,.7),
 ('service-link',35,16,54,20,3.4,.7),('quiet-pavilion',45,4,54,16,3.4,1.8),
 ('quiet-wc',45,0,50,4,3.4,.7),('service-stair',54,12,60,24,3.4,1.1),
 ('service-corridor',54,24,57,50,3.4,.6),('refuse',57,40,61,44,3.4,.7),('chapel',10,2,22,10,3.4,2)],
'COLLEGE':[
 ('teaching-and-lecture',6,10,24,54,7.2,2.8),('teaching-stair',24,34,30,46,7.2,1.1),
 ('tutorial',24,46,36,54,7.2,1.8),('teaching-lobby-admin',20,54,36,62,3.6,1.2),
 ('library',28,4,54,20,7.2,3),('teaching-library-bridge',24,12,28,16,7.2,.7),
 ('residential-bridge',50,20,54,34,7.2,.7),('west-residence',42,26,50,66,7.2,1.5),
 ('residence-spine',50,34,54,70,7.2,.7),('east-residence-and-service',54,22,62,70,7.2,1.5),
 ('north-stair',62,30,68,42,7.2,1.1),('south-stair',62,58,68,70,7.2,1.1),
 ('residence-cellar-entry',42,66,50,78,3.6,1.2),('rear-service-hall',50,70,54,78,3.6,.6)]}
def main():
 roster=json.loads((BASE/'retaining-shoulder-pass/roster.json').read_text());sources=[]
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
  p=ROOT/result['construction'];r=json.loads(p.read_text());r['aboveground_indoor_roof_probes']=checks;r['status']='INSTITUTION_WING_ROOF_CONSTRUCTION_CANDIDATE_RUNTIME_OPEN';r['wing_plan_builder']='tools/build_institution_roof_batch.py';r['wing_plan_sha256']=roof.sha(Path(__file__));r['remaining']='Roof-step flashing/drainage, structural join review, complete terrain/room connectivity and actual runtime integration remain open. Roofs do not add accessible search spaces or satisfy availability gates.';roof.write(p,r);sources.append(result)
 decisions=[dict(id=s['id'],decision='INSTITUTION_WING_ROOFS_CONSTRUCTED_NATIVE_CANDIDATE' if s['id'] in roof.WINGS else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']];roof.write(roof.OUT/'roster.json',dict(active_sources=sources,sites=decisions,baseline='development/levels/rebuild/retaining-shoulder-pass/roster.json'));print('INSTITUTION_ROOFS_COMPLETE',list(roof.WINGS),flush=True)
if __name__=='__main__':main()
