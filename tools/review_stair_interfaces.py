"""Reimport the actual construction exports, verify preserved records and inspect.
Six scoped native views; no editor UI, gameplay or finished-map acceptance.
"""
from pathlib import Path
import sys,json,collections,datetime
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import read
from compose_room_terrain import split_native
from check_macroterrain_exports import inspect
from export_roster_collision import face_keys
from review_roster_macroterrain import records,sha
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-interface-construction-pass'
def main():
 rows=[];captures=[];cap=BASE/'captures/stair-interface-construction-batch'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S%fZ');cap.mkdir(parents=True)
 for site in ['ACADEMY','WLD-09','WLD-04']:
  folder=OUT/site.lower();contract=folder/'construction.json';r=json.loads(contract.read_text());source=ROOT/r['source_glb'];glb=folder/'joined-stairs.glb';native=folder/'joined-stairs.hcs2'
  assert sha(source)==r['source_glb_sha256'] and sha(ROOT/r['source_physics'])==r['source_physics_sha256'];assert sha(glb)==r['glb_sha256'] and sha(native)==r['physics_sha256'] and sha(ROOT/r['layout'])==r['layout_sha256']
  old={n:raw for n,_,raw in split_native(ROOT/r['source_physics'])};new={n:raw for n,_,raw in split_native(native)};removed=set(r['removed_native_parts']);assert set(old)-set(new)==removed
  for name in set(old)-removed:assert old[name]==new[name],(site,name,'Changed retained native record')
  a,blob=read(source);b,afterblob=read(glb);assert afterblob[:len(blob)]==blob
  for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert b.get(key,[])[:len(a.get(key,[]))]==a.get(key,[])
  for i,n in enumerate(a['nodes']):
   expected=dict(n)
   if n.get('name') in r['removed_source_meshes']:expected.pop('mesh',None)
   assert b['nodes'][i]==expected,(site,i,'Changed retained node/pivot/material')
  before=inspect(source,False);after=inspect(glb,False);removedmesh=set(r['removed_source_meshes']);assert set(before)-set(after)==removedmesh
  for name in set(before)-removedmesh:assert before[name]==after[name],(site,name,'Changed mesh/UV/image/pivot')
  objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};added=set(after)-set(before);assert added==set(r['new_interface_parts']);new_native={n:raw for n,_,raw in split_native(native) if n not in old};triangles=0
  from check_roster_decks import read_parts
  parts=read_parts(native)
  for name in added:
   o=objs[name];assert face_keys(records(o))==face_keys(parts[name][1]);assert parts[name][0]==1
   bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,name,'Open or inverted export');bm.free();triangles+=len(records(o))
  data=json.loads((ROOT/r['layout']).read_text());old_data=json.loads((ROOT/r['before_layout']).read_text());assert data['zones']==old_data['zones'];assert sum(z.get('player_access')!='SEALED_CONTEXT' for z in data['zones'])<=40
  bpy.ops.wm.open_mainfile(filepath=str(folder/'joined-stairs.blend'));blendobjs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
  for name in added:assert face_keys(records(blendobjs[name]))==face_keys(parts[name][1])
  images={n.image for mat in bpy.data.materials if mat.use_nodes for n in mat.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert all(i.packed_file and i.size[0]>0 for i in images)
  rows.append(dict(id=site,retained_native_records=len(old)-len(removed),retained_source_meshes=len(before)-len(removedmesh),replaced_meshes=len(removedmesh),new_closed_parts=len(added),new_triangles=triangles,packed_images=len(images),glb_sha256=sha(glb),native_sha256=sha(native),blend_sha256=sha(folder/'joined-stairs.blend'),contract_sha256=sha(contract)))
  # Capture actual reimport, not the source scene. A section is explicitly labeled.
  inspect(glb,False);scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.light='STUDIO';scene.display.shading.show_cavity=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Native interface review');scene.world.color=(.05,.05,.05);scene.view_settings.view_transform='Standard';bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam
  for view in ['construction','interface']:
   hidden=[]
   for o in scene.objects:
    if o.type!='MESH':continue
    o.hide_render=o.get('role') in ['enclosed pitched roof','ceiling-cap']
    if view=='interface' and site=='WLD-09':o.hide_render=o.get('role','').startswith('saved editor terrain') or min((o.matrix_world@Vector(v)).z for v in o.bound_box)>-5.5
    if o.hide_render:hidden.append(o.name)
   target=Vector((42,-47,1.8)) if site=='ACADEMY' else Vector((49,-39,-3)) if site=='WLD-09' else Vector((29,-6,12))
   if view=='construction':cam.data.type='ORTHO';cam.data.ortho_scale=52 if site!='WLD-04' else 15;cam.location=target+Vector((30,-38,38))
   elif site=='ACADEMY':cam.data.type='ORTHO';cam.data.ortho_scale=18;target=Vector((52,-50,1.8));cam.location=target+Vector((-14,-10,13))
   elif site=='WLD-09':cam.data.type='PERSP';cam.data.lens=24;cam.location=Vector((59,-29,-8.38));target=Vector((54,-29,-8.38))
   else:cam.data.type='ORTHO';cam.data.ortho_scale=10;cam.location=target+Vector((8,-10,6))
   cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.clip_start=.02;cam.data.clip_end=400;path=cap/(site.lower()+'-'+view+'.png');scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(site=site,view=view,path=path.relative_to(ROOT).as_posix(),sha256=sha(path),glb_sha256=sha(glb),hidden=hidden,type='Native exported structure review; pitched roofs/caps hidden, Dam interface additionally hides terrain/geometry above-5.5m; not runtime/arrival/whole-map acceptance'))
  print('VERIFIED_CONSTRUCTION_EXPORT',site,len(added),'closed additions',flush=True)
 write(OUT/'verification/source-and-structure-preservation.json',dict(status='PASS_SCOPED_EXPORTED_NATIVE_EDITABLE_CONSTRUCTION',sites=rows,captures=captures,checker=Path(__file__).relative_to(ROOT).as_posix(),checker_sha256=sha(Path(__file__))))
if __name__=='__main__':main()
