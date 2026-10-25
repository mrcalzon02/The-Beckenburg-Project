"""Independent exact local geometry and world-pose audit of all scene exports."""
import bpy,json,struct,hashlib,collections
from pathlib import Path
from mathutils import Matrix
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'runtime-scene.json').read_text(encoding='utf-8'));source=ROOT/r['scene'];assert hashlib.sha256(source.read_bytes()).hexdigest()==r['source_sha256'];bpy.ops.wm.open_mainfile(filepath=str(source));deps=bpy.context.evaluated_depsgraph_get();C=Matrix(((1,0,0,0),(0,0,1,0),(0,-1,0,0),(0,0,0,1)));Ci=C.inverted();total=0;checks=[]
boat_override=None
if r.get('boat_override'):
 override=ROOT/r['boat_override']['scene'];assert hashlib.sha256(override.read_bytes()).hexdigest()==r['boat_override']['sha256']
 with bpy.data.libraries.load(str(override),link=False) as (src,dst):dst.objects=['West moored rowing boat']
 boat_override=dst.objects[0]

def canonical(v):
 i=min(range(3),key=lambda i:v[i]);return tuple(v[i:]+v[:i])
for asset in r['assets']:
 data=(ROOT/f"assets/runtime/models/{asset['id']}.glb").read_bytes();assert hashlib.sha256(data).hexdigest()==asset['sha256'] and len(data)<4000000;n=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+n]);base=28+n
 assert len(g['nodes'])==1 and all(k not in g['nodes'][0] for k in ['matrix','translation','rotation','scale'])
 def values(index):
  a=g['accessors'][index];v=g['bufferViews'][a['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[a['componentType']];count={'SCALAR':1,'VEC3':3,'VEC2':2}[a['type']];start=base+v.get('byteOffset',0)+a.get('byteOffset',0)
  return [struct.unpack_from('<'+code*count,data,start+i*v.get('byteStride',count*size)) for i in range(a['count'])]
 actual=collections.Counter();actual_uv=collections.defaultdict(list)
 for mesh in g['meshes']:
  for p in mesh['primitives']:
   assert 'TEXCOORD_0' in p['attributes'];points=values(p['attributes']['POSITION']);indices=[v[0] for v in values(p['indices'])]
   uvs=values(p['attributes']['TEXCOORD_0'])
   for i in range(0,len(indices),3):
    actual[canonical([points[j] for j in indices[i:i+3]])]+=1
    vv=[points[j]+uvs[j] for j in indices[i:i+3]];first=min(range(3),key=lambda k:vv[k][:3]);vv=vv[first:]+vv[:first];actual_uv[tuple(v[:3] for v in vv)].append(tuple(v[3:] for v in vv))
 for image in g['images']:assert image['mimeType']=='image/png' and 'bufferView' in image
 o=boat_override if asset.get('override_scene') else bpy.data.objects[asset['source_object']];o.data.calc_loop_triangles();tri=list(o.data.loop_triangles)[asset['source_triangle_start']:asset['source_triangle_start']+asset['triangles']];expected=collections.Counter(canonical([(o.data.vertices[i].co.x,o.data.vertices[i].co.z,-o.data.vertices[i].co.y) for i in t.vertices]) for t in tri)
 uv=next(u for u in o.data.uv_layers if u.active_render)
 for t in tri:
  vv=[(o.data.vertices[vi].co.x,o.data.vertices[vi].co.z,-o.data.vertices[vi].co.y,uv.data[li].uv.x,1-uv.data[li].uv.y) for vi,li in zip(t.vertices,t.loops)];first=min(range(3),key=lambda k:vv[k][:3]);vv=vv[first:]+vv[:first];key=tuple(v[:3] for v in vv);candidates=actual_uv[key]
  match=next((i for i,c in enumerate(candidates) if max(abs(a-b) for aa,bb in zip(c,[v[3:] for v in vv]) for a,b in zip(aa,bb))<2e-6),None)
  assert match is not None,('Native UV mapping changed',asset['id'],key,candidates,[v[3:] for v in vv]);candidates.pop(match)

 assert expected==actual,('Raw native triangle mismatch',asset['id']);assert sum(actual.values())==asset['triangles']<=24000;total+=asset['triangles'];checks.append(dict(id=asset['id'],triangles=asset['triangles'],sha256=asset['sha256'],exact_raw_local_geometry=True))
for instance in r['instances']:
 o=bpy.data.objects[instance['object']];matrix=C@o.evaluated_get(deps).matrix_world@Ci;assert instance['matrix']==[v for row in matrix for v in row],('Native world pose changed',o.name)
 assert not o.hide_render and not any(m and m.name.startswith('Canonical van material') for m in o.data.materials) and o.name!='Lake water boundary'
report=dict(status='pass-exact-native-scene-export',source_sha256=r['source_sha256'],unique_assets=len(checks),unique_asset_triangles=total,instances=len(r['instances']),exact_source_world_matrices=True,asset_checks=checks,water_and_van_owned_by_separate_modules=True,full_game_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-runtime-scene.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS',len(checks),'exact native local geometry exports;',len(r['instances']),'exact source poses;',total,'unique source triangles',flush=True)
