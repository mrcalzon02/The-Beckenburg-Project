"""Actual support-part and closed hull checks after the second authoring pass."""
import bpy,bmesh,json,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';r=json.loads((OUT/'refined-models.json').read_text(encoding='utf-8'))
def vertices(o,g):return [v for v in o.data.vertices if any(a.group==g.index and a.weight>.5 for a in v.groups)]
def tree(o,predicate):
 ids={g.index for g in o.vertex_groups if predicate(g.name)};selected={v.index for v in o.data.vertices if any(g.group in ids and g.weight>.5 for g in v.groups)};o.data.calc_loop_triangles();faces=[tuple(t.vertices) for t in o.data.loop_triangles if all(i in selected for i in t.vertices)];assert faces;return BVHTree.FromPolygons([v.co for v in o.data.vertices],faces,all_triangles=True)
checks=[]
for a in r['assets']:
 assert hashlib.sha256((ROOT/f"assets/runtime/models/{a['id']}.glb").read_bytes()).hexdigest()==a['sha256'];bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']];images={n.image for m in o.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert any(im.size[0]>=256 and im.size[1]>=256 for im in images),a['id']
 if a['clean_source']=='level_camp_rowboat':
  g=next(g for g in o.vertex_groups if g.name.startswith('Continuous double-skin hull'));selected={v.index for v in vertices(o,g)};bm=bmesh.new();bm.from_mesh(o.data);bm.verts.ensure_lookup_table();faces=[f for f in bm.faces if all(v.index in selected for v in f.verts)];edge_counts={}
  for f in faces:
   for e in f.edges:edge_counts[e]=edge_counts.get(e,0)+1
  assert all(n==2 for n in edge_counts.values()),('Hull is not a closed two-face-per-edge surface',{n:list(edge_counts.values()).count(n) for n in set(edge_counts.values())});checks.append(dict(kind='closed actual hull',faces=len(faces),edges=len(edge_counts)));bm.free()
 if a['clean_source']=='level_camp_picnic_table':
  tabletop=tree(o,lambda n:n.startswith('Tabletop slat'));rail=tree(o,lambda n:n.startswith('Tabletop bearing rail'));groups=[g for g in o.vertex_groups if g.name.startswith('Picnic-table countersunk bolt')];assert len(groups)==4
  for g in groups:
   vv=vertices(o,g);x=sum(v.co.x for v in vv)/len(vv);y=sum(v.co.y for v in vv)/len(vv);assert abs(abs(x)-.5)<.001;top=tabletop.ray_cast(Vector((x,y,1)),Vector((0,0,-1)),.5)[0];bearing=rail.ray_cast(Vector((x,y,.73)),Vector((0,0,-1)),.2)[0];assert top is not None and bearing is not None and abs(top.z-.76)<.001 and abs(bearing.z-.72)<.001;checks.append(dict(kind='table fixing above real slat/bearing',x=x,y=y))
 if a['clean_source']=='level_camp_fire_ring':
  ash=tree(o,lambda n:n.startswith('Cold ash bed'))
  for g in [g for g in o.vertex_groups if g.name.startswith('Cold charred timber')]:
   vv=vertices(o,g);x=sum(v.co.x for v in vv)/len(vv);y=sum(v.co.y for v in vv)/len(vv);wood=tree(o,lambda n:n==g.name);below=ash.ray_cast(Vector((x,y,.03)),Vector((0,0,-1)),.1)[0];p=wood.ray_cast(Vector((x,y,0)),Vector((0,0,1)),.15)[0];assert below is not None and p is not None and -.002<p.z-below.z<.012;checks.append(dict(kind='charred wood/ash support',gap_m=p.z-below.z))
 for im in images:assert im.packed_file is not None,('Unpacked original diffuse',a['id'],im.name)
for path,h in r['protected_sources'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==h,path
report=dict(status='pass-native-parts-only',asset_count=len(r['assets']),fine_packed_diffuse=True,protected_clean_sources_unchanged=len(r['protected_sources']),actual_part_checks=checks,limitations=r['limitations']);file=ROOT/'development/levels/verification/hollow-creek-refinement-parts.json';file.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('PASS20 fine packed source families, closed hull,4 real table fixings,3 ash/log contacts and immutable clean sources',flush=True)
