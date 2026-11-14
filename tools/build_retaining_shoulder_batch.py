"""Terrain-rooted retaining shoulders beside the current saved circulation cuts."""
from pathlib import Path
import sys,json,math,struct
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
from build_roster_foundations import prism,material,route_void
from append_static_glb import append
from export_roster_collision import face_keys,native_faces
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'retaining-shoulder-pass';SITES=['ACADEMY','WLD-05','WLD-09','WLD-10','WLD-13','WLD-18']
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def tree(objs):
 vs=[];fs=[]
 for o in objs:
  offset=len(vs);vs.extend(o.matrix_world@v.co for v in o.data.vertices);fs.extend(tuple(offset+i for i in p.vertices) for p in o.data.polygons)
 return BVHTree.FromPolygons(vs,fs)
def build(s):
 site=s['id']
 if site=='WLD-18':return s # Visual review rejects isolated stubs; redesign the full bank first.
 folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];assert sha(source)==s['glb_sha256'] and sha(native)==s['native_sha256'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));original=[o for o in bpy.context.scene.objects if o.type=='MESH'];soil=[o for o in original if o.get('role','').startswith('saved editor terrain')];assert len(soil)==1,(site,'saved terrain required');groundtree=tree(soil)
 def ground(x,z):
  hit=groundtree.ray_cast(Vector((x,-z,45)),Vector((0,0,-1)),100)[0];return None if hit is None else hit.z
 mat=material(site+' masonry retaining shoulder',(.33,.31,.27));built=[];proof=[];deferred=[];used=set()
 for link in data['links']:
  if link.get('player_access')=='SEALED_NO_ROUTE':continue
  for index,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
   if abs(a[1]-b[1])>.01:continue
   length=math.hypot(b[0]-a[0],b[2]-a[2]);ux=(b[0]-a[0])/length;uz=(b[2]-a[2])/length;n=max(1,math.ceil(length/2))
   for side in [-1,1]:
    nx=-uz*side;nz=ux*side
    for j in range(n):
     # A shoulder wall stands beyond the clear circulation bed, rather than
     # replacing its footing or narrowing the authored lane.
     d=link['width']/2+1.7;r0=length*j/n;r1=length*(j+1)/n
     ends=[(a[0]+ux*r+nx*d,a[2]+uz*r+nz*d) for r in [r0,r1]]
     stamp=tuple(round(v,2) for p in sorted(ends) for v in p)
     if stamp in used:continue
     inner=[ground(x-nx*.4,z-nz*.4) for x,z in ends];outer=[ground(x+nx*.7,z+nz*.7) for x,z in ends]
     if any(h is None for h in inner+outer):continue
     if max(outer)-a[1]<.15 or max(outer)-a[1]>3.2 or min(o-i for i,o in zip(inner,outer))<.08:continue
     poly=[(x+nx*w,z+nz*w) for (x,z),w in [(ends[0],-.16),(ends[1],-.16),(ends[1],.16),(ends[0],.16)]];ground_y=[ground(x,z) for x,z in poly]
     if any(h is None for h in ground_y):continue
     top=max(outer)+.12
     probes=[]
     for i in range(5):
      for k in range(5):
       u=i/4;v=k/4;x=(1-v)*((1-u)*poly[0][0]+u*poly[1][0])+v*((1-u)*poly[3][0]+u*poly[2][0]);z=(1-v)*((1-u)*poly[0][1]+u*poly[1][1])+v*((1-u)*poly[3][1]+u*poly[2][1]);h=ground(x,z);probes.append([x,z,h])
     if any(q[2] is None for q in probes):continue
     bottoms=[min(q[2] for q in probes)-.4]*4;rect=[min(p[0] for p in poly),min(p[1] for p in poly),max(p[0] for p in poly),max(p[1] for p in poly)]
     conflict=route_void([rect[0]-.25,rect[1]-.25,rect[2]+.25,rect[3]+.25],min(bottoms),top,data)
     rooms=[z['id'] for z in data['zones'] if z['kind'] not in ['outside','staging'] and z['y']<top and z['y']+z['height']>min(bottoms) and rect[0]<z['bounds'][2]+.25 and rect[2]>z['bounds'][0]-.25 and rect[1]<z['bounds'][3]+.25 and rect[3]>z['bounds'][1]-.25]
     if conflict or rooms:deferred.append(dict(link=link['id'],segment=index,reason='RESERVE_ROUTE_OR_BUILDING',route=conflict,rooms=rooms));continue
     # Exclude preexisting structure/props at the actual new wall volume.
     occupied=[]
     for o in original:
      if o in soil:continue
      ps=[o.matrix_world@Vector(p) for p in o.bound_box];xs=[p.x for p in ps];zs=[-p.y for p in ps];ys=[p.z for p in ps]
      if max(xs)>rect[0] and min(xs)<rect[2] and max(zs)>rect[1] and min(zs)<rect[3] and max(ys)>min(bottoms)+.05 and min(ys)<top:occupied.append(o.name)
     if occupied:continue
     used.add(stamp);name='Retaining shoulder '+site+' '+link['id']+' '+str(index)+' '+str(side)+' '+str(j);o=prism(name,poly,top,bottoms,mat);o['role']='terrain-rooted retaining shoulder';o['link']=link['id'];built.append(o);proof.append(dict(name=name,link=link['id'],footprint=poly,top_y=top,bottom_y=bottoms,actual_soil_y=ground_y,cut_inner_y=inner,bank_outer_y=outer,soil_embedding=.4,soil_root_probes=probes))
 if not built:
  print('RETAIN_NO_JUSTIFIED_SHOULDER',site,len(deferred),flush=True);return s
 folder.mkdir(parents=True,exist_ok=True);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'retained-cuts.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 assert built,(site,'No physical construction; do not publish a copy-only batch')
 addition=ROOT/'build'/('retaining-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'retained-cuts.glb');parts=[(o.name,records(o)) for o in built];raw=native.read_bytes();magic,n,t=struct.unpack_from('>III',raw);payload=struct.pack('>III',magic,n+len(parts),t+sum(len(rs) for _,rs in parts))+raw[12:]
 for name,rs in parts:
  encoded=name.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(rs))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in rs)
 target=folder/'retained-cuts.hcs2';target.write_bytes(payload);assert face_keys(native_faces(target))==face_keys(native_faces(native)+[r for _,rs in parts for r in rs]);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'retained-cuts.glb'));meshes={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,rs in parts:
  o=meshes[name];assert face_keys(records(o))==face_keys(rs);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.free()
 root_checks=[]
 for q in proof:
  walltree=tree([meshes[q['name']]]);embedding=[]
  for x,z,h in q['soil_root_probes']:
   # Move boundary probes 0.1% towards centre to avoid ambiguous edge rays.
   cx=sum(p[0] for p in q['footprint'])/4;cz=sum(p[1] for p in q['footprint'])/4;x=x*.999+cx*.001;z=z*.999+cz*.001
   soil_y=ground(x,z);bottom=walltree.ray_cast(Vector((x,-z,-45)),Vector((0,0,1)),100)[0];assert bottom is not None and soil_y is not None
   embedding.append(soil_y-bottom.z)
  assert min(embedding)>.35,(site,q['name'],min(embedding));root_checks.append(dict(name=q['name'],samples=len(embedding),minimum_embedding=min(embedding)))
 meta.update(root_checks=root_checks,status='TERRAIN_ROOTED_RETAINING_SHOULDERS_CANDIDATE_RUNTIME_OPEN',source_glb=s['glb'],source_glb_sha256=sha(source),source_physics=s['native'],source_physics_sha256=sha(native),before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],glb_sha256=sha(folder/'retained-cuts.glb'),physics_sha256=sha(target),retaining_shoulders=proof,deferred_retaining=deferred,retaining_builder_sha256=sha(Path(__file__)),remaining='Coping/drainage/returns and full landscape art, off-landform routes, remaining connectivity and runtime integration are open. Soil/sculpt/topology bytes unchanged; no new terrain hole, map unlock or terrain acceptance.');write(folder/'construction.json',meta);print('RETAINING_CONSTRUCTION',site,len(proof),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=s['layout'],glb=(folder/'retained-cuts.glb').relative_to(ROOT).as_posix(),native=target.relative_to(ROOT).as_posix(),blend=(folder/'retained-cuts.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'stair-end-pass/roster.json').read_text());active=[build(s) if s['id'] in SITES else s for s in roster['active_sources']];write(OUT/'roster.json',dict(active_sources=active,sites=[dict(id=row['id'],decision='RETAINING_SHOULDERS_CONSTRUCTED_NATIVE_CANDIDATE' if any(a['id']==row['id'] and a['construction'].startswith(OUT.relative_to(ROOT).as_posix()) for a in active) else 'REJECT_ISOLATED_STUBS_REDESIGN_BANK' if row['id']=='WLD-18' else 'RETAIN_NO_JUSTIFIED_SHOULDER' if row['id'] in SITES else 'RETAIN_PRIOR_SOURCE') for row in roster['sites']]))
if __name__=='__main__':main()
