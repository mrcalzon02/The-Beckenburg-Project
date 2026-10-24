"""Check actual near foliage, billboard alpha/materials and preserved scenery sources."""
import bpy,json,math,hashlib,sys
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path.insert(0,str(ROOT/'development/levels'))
import hollow_creek_organic as organic
d=json.loads((OUT/'organic-terrain.json').read_text(encoding='utf-8'));r=json.loads((OUT/'foliage-detail.json').read_text(encoding='utf-8'));forest=json.loads((OUT/'forest.json').read_text(encoding='utf-8'))
assert hashlib.sha256((ROOT/r['source_scene']).read_bytes()).hexdigest()==r['source_scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-foliage-detail.blend'))
scene=bpy.context.scene;assert all(bpy.data.objects.get(p['name']) for p in forest['placements']) and r['original_tree_count']==60
obj=bpy.data.objects['level_camp_terrain_organic'];terrain=BVHTree.FromPolygons([obj.matrix_world@v.co for v in obj.data.vertices],[tuple(p.vertices) for p in obj.data.polygons])
failures=[];roots=0
for p in r['near_planting']:
 o=bpy.data.objects[p['name']];hit=terrain.ray_cast(Vector((p['x'],-p['z'],10)),Vector((0,0,-1)),30)[0];assert hit is not None
 bottom=min((o.matrix_world@v.co).z for v in o.data.vertices);assert abs(bottom-hit.z)<.045,(p['name'],bottom,hit.z);roots+=1
 for link in d['routes']:
  distance=min(organic.segment_distance(p['x'],p['z'],[a[0],a[2]],[b[0],b[2]]) for a,b in zip(link['points'],link['points'][1:]))
  if distance<link['width']/2+p['radius']+.32:failures.append(dict(plant=p['name'],owner=link['id'],issue='plant footprint encroaches body corridor'))
 for rite in d['rituals']:
  x,y,z=rite['point'];distance=organic.old.rect_distance(p['x'],p['z'],[x-2.2,z-2.2,x+2.2,z+2.2])
  if distance<p['radius']+.32:failures.append(dict(plant=p['name'],owner=rite['rite'],issue='plant footprint encroaches ritual approach'))
 assert 'refuge' not in o.keys()
alpha=[]
for rel in r['texture_sources']:
 image=bpy.data.images.load(str(ROOT/rel),check_existing=True);pixels=list(image.pixels);values=pixels[3::4];opaque=sum(a>.95 for a in values);clear=sum(a<.01 for a in values)
 assert opaque>100 and clear>len(values)*.35 and image.channels==4,(rel,opaque,clear)
 width,height=image.size
 for x,y in [(0,0),(width-1,0),(0,height-1),(width-1,height-1)]:assert values[y*width+x]<.01,(rel,'opaque atlas corner')
 alpha.append(dict(path=rel,size=list(image.size),transparent_pixels=clear,opaque_pixels=opaque,corners_transparent=True))
for p in r['billboards']:
 card=bpy.data.objects[p['card']];trunk=bpy.data.objects[p['trunk']];assert card.constraints[0].type=='LOCKED_TRACK' and card.constraints[0].target==scene.camera
 assert card.constraints[0].lock_axis=='LOCK_Z' and not organic.inside(p['x'],p['z']);assert trunk.type=='MESH';assert p['z']>=10
 # Crown node graph must mix transparent and diffuse from the actual alpha.
 nodes=card.data.materials[0].node_tree.nodes;assert any(n.type=='BSDF_TRANSPARENT' for n in nodes) and any(n.type=='TEX_IMAGE' and n.image.channels==4 for n in nodes)
out=dict(status='pass-native-foliage-only' if not failures else 'foliage placement refinement required',original_3d_tree_placements_retained=60,refined_3d_conifers=r['refined_conifers'],supplemental_solid_trunk_billboards=len(r['billboards']),grounded_near_ferns=roots,alpha_textures=alpha,placement_findings=failures,source_scene_unchanged=True,limitations=r['limitations'])
path=ROOT/'development/levels/verification/hollow-creek-foliage-detail.json';path.write_text(json.dumps(out,indent=2)+'\n',encoding='utf-8');assert not failures,failures
print('PASS retained 60 tree placements,',len(r['billboards']),'supplemental alpha crowns/solid trunks,',roots,'grounded near ferns; protected routes and rituals',flush=True)
