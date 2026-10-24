"""Validate the desquareified saved terrain and grounded reused forest geometry."""
import bpy,json,math,hashlib,sys
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path.insert(0,str(ROOT/'development/levels'))
import hollow_creek_organic as organic
d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));assets=json.loads((OUT/'organic-assets.json').read_text(encoding='utf-8'));forest=json.loads((OUT/'forest.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-forest.blend'))
def tree(objects):
 vv=[];ff=[]
 for o in objects:
  o.data.calc_loop_triangles();base=len(vv);vv.extend(o.matrix_world@v.co for v in o.data.vertices);ff.extend(tuple(base+i for i in f.vertices) for f in o.data.loop_triangles)
 return BVHTree.FromPolygons(vv,ff,all_triangles=True)
terrain=bpy.data.objects['level_camp_terrain_organic'];tt=tree([terrain]);floors=[tt]+[tree([bpy.data.objects[name]]) for name in ['level_camp_pavilion','dock-foundation','creek-crossing-foundation']]
samples=0;flags=[];max_grade=0;max_error=0
for link in d['routes']:
 for a,b in zip(link['points'],link['points'][1:]):
  dx=b[0]-a[0];dz=b[2]-a[2];length=math.hypot(dx,dz);count=max(1,math.ceil(length/.25))
  for i in range(count+1):
   t=i/count;y=a[1]+(b[1]-a[1])*t
   for band in range(9):
    off=(band/8*2-1)*(link['width']/2+.22);x=a[0]+dx*t-dz/(length or 1)*off;z=a[2]+dz*t+dx/(length or 1)*off
    hits=[b.ray_cast(Vector((x,-z,y+.25)),Vector((0,0,-1)),.8) for b in floors];hits=[(p,n) for p,n,_,_ in hits if p is not None and n.z>.65];samples+=1
    if not hits:flags.append(dict(link=link['id'],point=[x,y,z],issue='missing native support'));continue
    top=max(p.z for p,n in hits);p,n=max([v for v in hits if top-v[0].z<.0001],key=lambda v:v[1].z);error=abs(p.z-y);grade=math.hypot(n.x,n.y)/n.z;max_grade=max(grade,max_grade);max_error=max(error,max_error)
    if error>.20 or grade>.20:flags.append(dict(link=link['id'],point=[x,y,z],issue='grade/support corridor',grade=grade,error=error))
terrain.data.calc_loop_triangles();assert len(terrain.data.loop_triangles)<50000
assert assets['rectangular_landscape_pads']==0 and d['render_floor_cutout']==[25,35,39,49]
assert not any(o.name.startswith('Pavilion apron ') for o in bpy.context.scene.objects)
# The narrow arrival neck now excludes formerly empty side margins as real mesh.
cropped=[]
for x,z in [(5,77),(57,76),(12,74),(55,72)]:
 hit=tt.ray_cast(Vector((x,-z,20)),Vector((0,0,-1)),40)[0];assert hit is None,('loose rectangular terrain margin survived',x,z);cropped.append([x,z])
van=[o for o in bpy.context.scene.objects if o.get('canonical_van')];assert van
van_triangles=sum(len(o.data.polygons) for o in van);assert van_triangles==assets['canonical_van_triangles']==4968
for rel,h in assets['protected_sources'].items():assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,rel
ground_objects=[o for o in bpy.context.scene.objects if o.name.startswith('Non-playable ') and o.type=='MESH'];assert len(ground_objects)==2;scenery=tree(ground_objects)
root_checks=0
for place in forest['placements']:
 obj=bpy.data.objects[place['name']];hit=scenery.ray_cast(Vector((place['x'],-place['z'],40)),Vector((0,0,-1)),100)[0];assert hit is not None
 bottom=min((obj.matrix_world@v.co).z for v in obj.data.vertices);assert -.13<bottom-hit.z<-.03,(place['name'],bottom,hit.z)
 assert not organic.inside(place['x'],place['z']);assert 'refuge' not in obj.keys();root_checks+=1
for rel,h in forest['source_hashes'].items():assert hashlib.sha256((ROOT/rel).read_bytes()).hexdigest()==h,rel
out=dict(status='pass-native-organic-and-scenery' if not flags else 'organic terrain grade refinement required',terrain_triangles=len(terrain.data.loop_triangles),supported_route_samples=samples,max_route_error=max_error,max_route_grade=max_grade,terrain_findings=flags,old_rectangular_margins_removed=cropped,rectangular_landscape_pads=0,physical_pavilion_slab_retained=True,canonical_van_triangles=van_triangles,grounded_off_map_trees=root_checks,scenery_aprons=len(ground_objects),source_hashes_unchanged=True,limitations=['Native static geometry checks only; runtime map/collision/evidence/LAN/performance remain pending.','The lake shoreline interfaces and water entry/containment remain open.','Scene still contains placeholder dock/bridge and unbuilt facilities; no completed-map claim.'])
(ROOT/'development/levels/verification/hollow-creek-organic.json').write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8')
print(out['status'],samples,'route samples;',len(flags),'grade/support findings; max grade',max_grade,';',root_checks,'grounded trees; canonical van',van_triangles,flush=True)
assert not flags,('Native route refinement required; findings saved',len(flags))
