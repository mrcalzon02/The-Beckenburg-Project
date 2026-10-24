"""Independent native validation for breadth-first terrain foundations.
This certifies only explicitly measured foundation/export properties, not gameplay.
"""
import bpy,json,sys,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/terrain-foundations'
def rd(x,z,r):return math.hypot(max(r[0]-x,0,x-r[2]),max(r[1]-z,0,z-r[3]))
def tree(obj):return BVHTree.FromPolygons([obj.matrix_world@v.co for v in obj.data.vertices],[tuple(p.vertices) for p in obj.data.polygons],all_triangles=False)
reports=[]
keys=sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [p.name for p in OUT.iterdir() if (p/'contract.json').exists()]
for key in keys:
 folder=OUT/key;d=json.loads((folder/'contract.json').read_text(encoding='utf-8'));a=d['asset']
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));obj=bpy.data.objects[a['id']];obj.data.calc_loop_triangles()
 assert len(obj.data.loop_triangles)==a['triangles'] and 0<a['triangles']<50000
 assert obj.data.uv_layers and all(im.packed_file for im in bpy.data.images if im.source=='FILE')
 assert hashlib.sha256((ROOT/f"assets/runtime/models/{a['id']}.glb").read_bytes()).hexdigest()==a['sha256']
 for tri in obj.data.loop_triangles:
  assert tri.normal.z>0,'inverted terrain surface'
 t=tree(obj);x0,z0,x1,z1=d['bounds'];nx,nz=d['nx'],d['nz'];heights=d['heights'];count=0
 # Probe deterministic interior cells against exact baked diagonal heights.
 for j in range(0,nz-1,3):
  for i in range(0,nx-1,3):
   x,z=x0+i+.5,z0+j+.5
   if any(rd(x,z,p['bounds'])<1e-7 for p in d['pads']):continue
   expected=(heights[j*nx+i]+heights[(j+1)*nx+i+1])/2
   hit,normal,_,_=t.ray_cast(Vector((x,-z,max(heights)+20)),Vector((0,0,-1)))
   assert hit is not None and abs(hit.z-expected)<.0001,(key,x,z,hit,expected)
   assert normal.z>0;count+=1
 assert count>50
 assert max(heights)-min(heights)>.25,'site accidentally flat'
 def signature(o):
  return hashlib.sha256(repr((tuple(tuple(row) for row in o.matrix_world),[tuple(v.co) for v in o.data.vertices],[tuple(p.vertices) for p in o.data.polygons])).encode()).hexdigest()
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/f'development/levels/blockouts/{key}/rough-shell.blend'))
 original_parts={o.name:signature(o) for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Floor ')}
 bpy.ops.wm.open_mainfile(filepath=str(folder/'site-terrain.blend'));scene=bpy.context.scene
 for name,sha in original_parts.items():assert signature(bpy.data.objects[name])==sha,(key,name,'original floor mesh/transform changed')
 native=bpy.data.objects[a['id']];assert (native.location-Vector((0,0,0))).length<1e-6
 ground=tree(native);floors=[]
 for name in d['retained_native_floors']:
  o=bpy.data.objects[name];world=[o.matrix_world@v.co for v in o.data.vertices];zid=o.get('zone',name[6:].split('.')[0])
  expected=next(q['y'] for q in d['source_floor_layers'] if q['id']==zid)
  assert abs(max(v.z for v in world)-expected)<1e-5,(key,zid,'source floor elevation changed')
  floors.append(dict(id=zid,y=expected))
 for p in d['piles']:
  o=bpy.data.objects[p['id']];world=[o.matrix_world@v.co for v in o.data.vertices]
  assert abs(min(v.z for v in world)-p['bottom'])<1e-5 and abs(max(v.z for v in world)-p['top'])<1e-5
  hit=ground.ray_cast(Vector((p['x'],-p['z'],max(heights)+20)),Vector((0,0,-1)))[0]
  assert hit is not None and p['bottom']<hit.z-.05,(key,p,'footing floats over baked terrain')
 for path,sha in d['protected_sources'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==sha,path
 # Sample source level-link support against the real ground/floor triangles.
 # This is diagnostic: unresolved crossings/grades remain explicit next work.
 support_trees=[ground]+[tree(bpy.data.objects[name]) for name in d['retained_native_floors']]
 support_samples=0;route_findings=[]
 for link in d['routes']:
  if link['kind']!='level':continue
  for start,end in zip(link['points'],link['points'][1:]):
   dx,dz=end[0]-start[0],end[2]-start[2];run=math.hypot(dx,dz)
   if run<.01:continue
   n=max(1,math.ceil(run))
   for step in range(n+1):
    fraction=step/n;expected=start[1]*(1-fraction)+end[1]*fraction
    for offset in [-link['width']/2,0,link['width']/2]:
     x=start[0]+dx*fraction-dz/run*offset;z=start[2]+dz*fraction+dx/run*offset
     hits=[t.ray_cast(Vector((x,-z,expected+.35)),Vector((0,0,-1)),1.5) for t in support_trees]
     hits=[(p,norm) for p,norm,index,dist in hits if p is not None and norm.z>.5]
     support_samples+=1
     if not hits:
      route_findings.append(dict(link=link['id'],x=round(x,3),z=round(z,3),issue='no native standing support near planned height'));continue
     pos,norm=max(hits,key=lambda q:q[0].z)
     if abs(pos.z-expected)>.25 or math.hypot(norm.x,norm.y)/norm.z>.25:
      route_findings.append(dict(link=link['id'],x=round(x,3),z=round(z,3),issue='grade/height needs refinement',actual_y=round(pos.z,3),planned_y=round(expected,3)))
 # Quantify remaining route engineering work instead of silently accepting ramps.
 transition=[]
 for l in d['routes']:
  if l['kind']!='level':
   run=sum(math.hypot(c[0]-b[0],c[2]-b[2]) for b,c in zip(l['points'],l['points'][1:]));rise=sum(abs(c[1]-b[1]) for b,c in zip(l['points'],l['points'][1:]))
   transition.append(dict(id=l['id'],run=round(run,3),rise=round(rise,3),status='engineering/headroom/guard acceptance pending'))
 report=dict(key=key,title=d['title'],status='pass-native-foundation-properties-only',triangles=a['triangles'],ground_triangle_samples=count,height_range=d['height_range'],preserved_floor_layers=floors,pile_footings_checked=len(d['piles']),source_files_unchanged=True,original_floor_parts_unchanged=len(original_parts),diagnostic_route_samples=support_samples,route_refinement_findings=len(route_findings),route_finding_examples=route_findings[:20],route_findings=route_findings,remaining_transitions=transition,scene_sha256=hashlib.sha256((folder/'site-terrain.blend').read_bytes()).hexdigest(),limitations=d['limits'])
 (folder/'verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');reports.append(report)
 print('PASS native terrain',key,count,'baked samples;',len(floors),'source floor layers;',len(d['piles']),'buried footings',flush=True)
print('PASS',len(reports),'native foundation property checks; runtime/full navigation acceptance remains separate',flush=True)
