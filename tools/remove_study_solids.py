"""Remove study-derived solids; consolidate the Cannery's real ground floor."""
from pathlib import Path
import bpy,bmesh,json,sys,struct,io,copy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from review_roster_macroterrain import records,sha
from compose_room_terrain import split_native
from append_static_glb import append
from export_roster_collision import face_keys
from build_roster_foundations import prism
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'study-solid-removal-pass'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def empty_glb(p):
 j=json.dumps(dict(asset=dict(version='2.0'),scene=0,scenes=[dict(nodes=[])],nodes=[],buffers=[dict(byteLength=0)])).encode();j+=b' '*((-len(j))%4);p.write_bytes(struct.pack('<III',0x46546c67,2,28+len(j))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',0,0x004e4942))
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=[];built=[]
 for o in old:
  role=o.get('role','');vs=[o.matrix_world@v.co for v in o.data.vertices]
  if role.startswith('rough route support') or role in ['structural-rooted-circulation-bed','structural-arrival-end-bearing']:removed.append(o.name)
  elif site=='WLD-13' and ((role=='floor' and o.get('zone')=='processing') or role=='structural-stair-landing' and max(v.z for v in vs)<.001):removed.append(o.name)
 if site=='WLD-13':
  mat=next(o.data.materials[0] for o in old if o.get('role')=='floor' and o.get('zone')=='processing');zone=next(z for z in data['zones'] if z['id']=='processing');a,b,c,d=zone['bounds'];floor=prism('Cannery continuous processing ground floor',[(a,b),(c,b),(c,d),(a,d)],0,[-.16]*4,mat);floor['role']='floor';floor['zone']='processing';built.append(floor)
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 assert not any(o.type=='MESH' and o.get('role','').startswith('rough route support') for o in bpy.context.scene.objects)
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'architecture-without-guides.blend'),compress=True);addition=ROOT/'build'/('study-removal-'+site+'.glb');bpy.ops.object.select_all(action='DESELECT')
 if built:
  for o in built:o.select_set(True)
  bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True)
 else:empty_glb(addition)
 append(source,addition,folder/'architecture-without-guides.glb',remove_mesh_names=removed);keep=[p for p in split_native(native) if p[0].split(':',1)[-1] not in removed];parts=[(o.name,records(o)) for o in built];stream=io.BytesIO();stream.write(struct.pack('>III',0x48435332,len(keep)+len(parts),sum(p[1] for p in keep)+sum(len(ts) for _,ts in parts)))
 for _,_,raw in keep:stream.write(raw)
 for name,ts in parts:
  raw=name.encode();stream.write(struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:stream.write(struct.pack('>9fB',*xyz,flags))
 targetnative=folder/'architecture-without-guides.hcs2';targetnative.write_bytes(stream.getvalue());removed_native=[name for name,_,_ in split_native(native) if name.split(':',1)[-1] in removed];stairs=[n for n in meta['stair_components'] if n not in removed_native]
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'architecture-without-guides.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};assert all(n not in objs for n in removed)
 for n,ts in parts:
  assert face_keys(records(objs[n]))==face_keys(ts);bm=bmesh.new();bm.from_mesh(objs[n].data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.free()
 if site=='WLD-13':
  # Exactly one unbroken ground floor, with depth; no route ribbons or coplanar landing faces.
  floor=objs['Cannery continuous processing ground floor'];assert len(floor.data.polygons)>0
  from mathutils.bvhtree import BVHTree
  tree=BVHTree.FromPolygons([floor.matrix_world@v.co for v in floor.data.vertices],[tuple(f.vertices) for f in floor.data.polygons]);probes=0
  for x in range(25,48):
   for z in range(27,56):
    hit=tree.ray_cast(Vector((x,-z,.05)),Vector((0,0,-1)),.1)[0];assert hit is not None and abs(hit.z)<.00001;probes+=1
  assert probes==667
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Architecture correction');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/study-solid-removal-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='STUDY_SOLIDS_REMOVED_ARCHITECTURAL_REMEDIATION_NOT_ACCEPTED',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],stair_components=stairs,glb_sha256=sha(folder/'architecture-without-guides.glb'),physics_sha256=sha(targetnative),blend_sha256=sha(folder/'architecture-without-guides.blend'),study_solid_removal=removed,removed_native_parts=removed_native,correction_builder='tools/remove_study_solids.py',correction_builder_sha256=sha(Path(__file__)),remaining='Removal deliberately exposes incomplete terrain, thresholds and earlier route-cut wall/floor damage. Do not refill with path extrusions or claim maps accepted. Deliberate authored site structures remain subject to architectural review.',captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native architectural correction cutaway, roofs hidden for inspection, not gameplay')]);write(folder/'construction.json',meta);print('STUDY_SOLIDS_REMOVED',site,len(removed),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=s['layout'],glb=(folder/'architecture-without-guides.glb').relative_to(ROOT).as_posix(),native=targetnative.relative_to(ROOT).as_posix(),blend=(folder/'architecture-without-guides.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'circulation-construction-pass/roster.json').read_text());sites={'WLD-06','WLD-08','WLD-13'};rows=[build(s) if s['id'] in sites else s for s in roster['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(id=s['id'],decision='STUDY_SOLIDS_REMOVED_REMEDIATION_OPEN' if s['id'] in sites else s['decision']) for s in roster['sites']],baseline='development/levels/rebuild/circulation-construction-pass/roster.json'));print('STUDY_REMOVAL_COMPLETE',flush=True)
if __name__=='__main__':main()
