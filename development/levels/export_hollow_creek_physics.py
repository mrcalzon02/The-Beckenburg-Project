"""Export real saved native surfaces/solids plus authored movement-only barriers.
No native source, model, pose or terrain is modified. Runtime integration remains
locked until the separate gameplay/render/circuit acceptance gates are complete.
"""
import bpy,json,hashlib,struct,math,collections
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';authority=json.loads((OUT/'grounded-berm.json').read_text(encoding='utf-8'))
if (OUT/'route-grade.json').exists():
 graded=json.loads((OUT/'route-grade.json').read_text(encoding='utf-8'));assert graded['parent_sha256']==authority['scene_sha256'],'Regenerate local grading from current bank authority';authority=graded
source=ROOT/authority['scene'];before=hashlib.sha256(source.read_bytes()).hexdigest();assert before==authority['scene_sha256'],'Stale native authority';bpy.ops.wm.open_mainfile(filepath=str(source));scene=bpy.context.scene
a=json.loads((OUT/'amenities.json').read_text(encoding='utf-8'));r=json.loads((OUT/'refined-models.json').read_text(encoding='utf-8'));d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));early=json.loads((OUT/'tents.json').read_text(encoding='utf-8'))
selected={p['name'] for p in r['replaced_instances'] if 'shrub' not in p['clean_source']}
selected.update(['level_camp_terrain_amenities','dock-foundation','creek-crossing-foundation'])
selected.update(p['native_object'] for p in early['placements'] if p['asset']=='level_trace_bedroll')
selected.update(o.name for o in scene.objects if o.type=='MESH' and (o.name.startswith('level_camp_boundary_') or o.name.startswith('Toilet supported entry ramp ')))
selected.discard('level_camp_boundary_logfall') # Soft canopy/branches are not opaque body boxes.
selected.update(o.name for o in scene.objects if o.type=='MESH' and o.name.startswith('Grounded lake stone '))
assert not selected.intersection(b['id'] for b in a['blockers']),'Invisible movement barriers must never become optical triangles'
assert all(not bpy.data.objects[n].hide_render for n in selected),'Hidden helper geometry selected as visible solid'
records=[];counts=[];components=[]
def floor_group(name):return any(word in name.lower() for word in ['groundsheet','physical floor','slab closed solid','service floor','raised cubicle floor','recent sleeping mat'])
for name in sorted(selected):
 o=bpy.data.objects[name];o.data.calc_loop_triangles();groups={g.index for g in o.vertex_groups if floor_group(g.name)};floor_verts={v.index for v in o.data.vertices if any(g.group in groups for g in v.groups)};n=0;walk=0;parent=list(range(len(o.data.vertices)));parts=collections.defaultdict(list)
 def find(i):
  while parent[i]!=i:parent[i]=parent[parent[i]];i=parent[i]
  return i
 def union(a,c):
  a=find(a);c=find(c)
  if a!=c:parent[c]=a
 for t in o.data.loop_triangles:union(t.vertices[0],t.vertices[1]);union(t.vertices[1],t.vertices[2])
 all_floor=name in ['level_camp_terrain_amenities','dock-foundation','creek-crossing-foundation'] or name.startswith('Toilet supported entry ramp ') or name.endswith('laid-bedding')
 for t in o.data.loop_triangles:
  points=[o.matrix_world@o.data.vertices[i].co for i in t.vertices];normal=(points[1]-points[0]).cross(points[2]-points[0]);length=normal.length
  if length<1e-10:continue
  support=(all_floor or all(i in floor_verts for i in t.vertices)) and normal.z/length>.20
  # Small scattered pebbles remain visible optically without becoming trip walls.
  body=not(name.startswith('Grounded lake stone ') and o.dimensions.z<.22)
  flags=2+(4 if body else 0)+(1 if support else 0);coords=[q for p in points for q in (p.x,p.z,-p.y)];assert all(math.isfinite(v) and abs(v)<256 for v in coords);records.append((coords,flags));parts[find(t.vertices[0])].append((tuple(t.vertices),coords,flags));n+=1;walk+=support
 for number,part in enumerate(parts.values()):
  edges=collections.Counter();orientation=collections.Counter();volume=0
  for ids,coords,flags in part:
   for u,v in zip(ids,ids[1:]+ids[:1]):key=tuple(sorted((u,v)));edges[key]+=1;orientation[key]+=1 if u<v else -1
   aa,bb,cc=[Vector(coords[i:i+3]) for i in [0,3,6]];volume+=aa.dot(bb.cross(cc))/6
  closed=bool(edges) and all(v==2 and orientation[k]==0 for k,v in edges.items()) and abs(volume)>1e-10 and all(flags&4 for ids,coords,flags in part)
  components.append(dict(name=name+'#'+str(number),closed=closed,triangles=[(coords,flags) for ids,coords,flags in part]))
 counts.append(dict(object=name,triangles=n,support_triangles=walk))
target=ROOT/'assets/runtime/sites/hollow-creek-physics.bin';target.parent.mkdir(exist_ok=True)
with target.open('wb') as f:
 f.write(struct.pack('>III',0x48435332,len(components),len(records)))
 for component in components:
  name=component['name'].encode('utf-8');assert len(name)<=256;f.write(struct.pack('>H',len(name)));f.write(name);f.write(struct.pack('>BI',int(component['closed']),len(component['triangles'])))
  for coords,flags in component['triangles']:f.write(struct.pack('>9fB',*coords,flags))
assert target.stat().st_size<32_000_000 and len(records)<500_000 and len(components)<16384
sha=hashlib.sha256(target.read_bytes()).hexdigest()
walking_vertices=[];walking_faces=[]
for coords,flags in records:
 if not flags&1:continue
 points=[Vector((coords[i],-coords[i+2],coords[i+1])) for i in [0,3,6]];normal=(points[1]-points[0]).cross(points[2]-points[0])
 if normal.z<=0 or math.hypot(normal.x,normal.y)/normal.z>.20+1e-5:continue
 start=len(walking_vertices);walking_vertices.extend(points);walking_faces.append((start,start+1,start+2))
walking=BVHTree.FromPolygons(walking_vertices,walking_faces,all_triangles=True);zone_spawns=[]
# Generate source constants directly from the approved native/layout contracts.
def j(value):return json.dumps(value,ensure_ascii=True)
def nums(values):return ','.join(repr(float(v)) for v in values)
zones=[z for z in d['zones'] if z['id']!='stage'];assert len(zones)==12
links=d['routes'];names={z['id']:i for i,z in enumerate(zones)};names['stage']=13
lines=['package org.beckenburg;','import java.util.List;','import org.beckenburg.LevelDefinition.*;','/** Generated HCS2 contract; actual gameplay/renderer integration remains gated. */','final class HollowCreekContract {',f' static final String SHA256="{sha}";',f' static final String SOURCE_SHA256="{before}";',' static final String RESOURCE="/org/beckenburg/sites/hollow-creek-physics.bin";',' record Zone(String key,String name,double x0,double z0,double x1,double z1,double x,double y,double z){}',' static List<Zone> zones(){return List.of(']
for i,z in enumerate(zones):
 if z['id']=='store':name='Camp wardens memorial clearing';point=[5,53]
 elif z['id']=='camper':name='Beach sanitary clearing';point=[13,20]
 else:
  name={'ranger':'Ranger clearing','bath':'Washhouse clearing','pavilion':'Hollow Creek pavilion'}.get(z['id'],z['name']);point=next((l['points'][-1][::2] for l in links if l['b']==z['id']),[(z['bounds'][0]+z['bounds'][2])/2,(z['bounds'][1]+z['bounds'][3])/2])
 floor=walking.ray_cast(Vector((point[0],-point[1],12)),Vector((0,0,-1)),30)[0];assert floor is not None,('No actual walking anchor',z['id'])
 zone_spawns.append(dict(key=z['id'],name=name,x=point[0],y=floor.z,z=point[1]))
 lines.append('  new Zone('+j(z['id'])+','+j(name)+','+nums(z['bounds']+[point[0],floor.z,point[1]])+')'+(',' if i<len(zones)-1 else ');}'))
lines.append(' static List<Collider> blockers(){return List.of(')
for i,b in enumerate(a['blockers']):
 x=(b['a'][0]+b['b'][0])/2;z=(b['a'][1]+b['b'][1])/2;length=math.dist(b['a'],b['b']);yaw=math.atan2(b['b'][1]-b['a'][1],b['b'][0]-b['a'][0]);values=[x,b['bottom'],z,(length+b['width'])/2,b['width']/2,b['top']-b['bottom'],yaw];lines.append('  new Collider('+nums(values)+')'+(',' if i<len(a['blockers'])-1 else ');}'))
lines+=[' record Link(int a,int b,double width,double[][] points){}',' static List<Link> links(){return List.of(']
for i,l in enumerate(links):lines.append('  new Link('+str(names[l['a']])+','+str(names[l['b']])+','+str(float(l['width']))+',new double[][]{'+','.join('{'+nums(p)+'}' for p in l['points'])+'})'+(',' if i<len(links)-1 else ');}'))
lines+=[' static double[][] refuges(){return new double[][]{'+','.join('{'+nums([p['x'],p['y'],p['z']])+'}' for p in a['proposed_refuges'])+'};}','}']
(ROOT/'src/main/java/org/beckenburg/HollowCreekContract.java').write_text('\n'.join(lines)+'\n',encoding='utf-8')
report=dict(status='actual native HCS2 export; runtime checks pending',source=source.relative_to(ROOT).as_posix(),source_sha256=before,binary=target.relative_to(ROOT).as_posix(),binary_sha256=sha,triangles=len(records),components=len(components),closed_body_components=sum(c['closed'] for c in components),bytes=target.stat().st_size,objects=counts,zone_spawns=zone_spawns,player_only_blockers=len(a['blockers']),invisible_barriers_occlude_light=False,flags=dict(walking_support=1,optical_solid=2,body_solid=4),limitations=['No mission unlock or renderer/circuit/AI/host acceptance from this export alone.','Canonical van support/interactions stay owned by VanStaging; raised placement integration remains open.','Soft foliage alpha and distant scenery optics remain renderer integration gates.'])
(ROOT/'development/levels/verification/hollow-creek-physics-export.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');assert hashlib.sha256(source.read_bytes()).hexdigest()==before;print('EXPORTED',len(records),'actual triangles;',target.stat().st_size,'bytes;',len(a['blockers']),'movement-only blockers; source unchanged',flush=True)
