"""Measure actual terrain inside architectural room volumes at every elevation."""
from pathlib import Path
import bpy,json,sys,math,hashlib,argparse
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';rows=[]
p=argparse.ArgumentParser();p.add_argument('--after',action='store_true');args=p.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
env={r['id']:r for r in json.loads((BASE/'terrain-overall-pass/terrain-envelopes.json').read_text())['sites']}
for site in json.loads((BASE/'terrain-fit-pass/support-policies.json').read_text())['sites']:
 source=BASE/'deck-fit-pass'/site.lower()/'deck-terrain.glb'
 if not source.exists():source=BASE/'terrain-fit-pass'/site.lower()/'supported-terrain.glb'
 if args.after:
  candidate=BASE/'terrain-occupancy-pass/assemblies'/site.lower()/'occupied-terrain.glb'
  if candidate.exists():source=candidate
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));points=[];faces=[]
 for o in bpy.context.scene.objects:
  if o.type!='MESH' or not(o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name):continue
  off=len(points);points.extend(o.matrix_world@v.co for v in o.data.vertices);faces.extend(tuple(off+i for i in p.vertices) for p in o.data.polygons)
 tree=BVHTree.FromPolygons(points,faces);layout=ROOT/env[site]['layout'];data=json.loads(layout.read_text());zones=[]
 for z in data['zones']:
  if z['kind'] in ['outside','staging']:continue
  a,b,c,d=z['bounds'];hits=[];count=0
  for ix in range(math.ceil((c-a)/.5)):
   for iz in range(math.ceil((d-b)/.5)):
    x=a+(ix+.5)*.5;y=b+(iz+.5)*.5
    if x>=c or y>=d:continue
    if z.get('shape')=='circle' and math.hypot(x-(a+c)/2,y-(b+d)/2)>z['radius']-.2:continue
    hit=tree.ray_cast(Vector((x,-y,40)),Vector((0,0,-1)))[0];count+=1
    if hit is not None and z['y']+.01<hit.z<z['y']+z['height']:hits.append([x,hit.z,y])
  if hits:zones.append(dict(id=z['id'],bounds=z['bounds'],floor_y=z['y'],height=z['height'],samples=count,occupied_terrain_samples=len(hits),height_range=[min(p[1] for p in hits),max(p[1] for p in hits)],examples=hits[:5]))
 rows.append(dict(id=site,source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=hashlib.sha256(layout.read_bytes()).hexdigest(),intrusions=zones))
 print('ROOM_TERRAIN',site,[(z['id'],z['occupied_terrain_samples']) for z in zones],flush=True)
out=BASE/'terrain-occupancy-pass';out.mkdir(exist_ok=True);(out/('after-room-terrain.json' if args.after else 'baseline-room-terrain.json')).write_bytes((json.dumps(dict(status='MEASURED_ROOM_VOLUME_INTRUSIONS_NOT_MAP_ACCEPTANCE',sites=rows,scope='Actual source terrain triangles sampled every0.5m over non-exterior architectural room envelopes, including above-world-zero floors. No room pad heights inferred.'),indent=2)+'\n').encode())
