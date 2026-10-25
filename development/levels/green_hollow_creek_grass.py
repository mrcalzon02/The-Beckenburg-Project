"""Material-only green grass variant of the verified route-graded scene."""
import bpy,json,hashlib,sys,array,struct
import numpy as np
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek';sys.path.insert(0,str(ROOT/'development/levels'))
r=json.loads((OUT/'route-grade.json').read_text(encoding='utf-8'));parent=ROOT/r['scene'];assert hashlib.sha256(parent.read_bytes()).hexdigest()==r['scene_sha256']
bpy.ops.wm.open_mainfile(filepath=str(parent));bpy.context.preferences.filepaths.save_version=0;scene=bpy.context.scene
def digest(o):
 h=hashlib.sha256();h.update(struct.pack('>16d',*(v for row in o.matrix_world for v in row)))
 for data,field,code,n in [(o.data.vertices,'co','f',3),(o.data.loops,'vertex_index','i',1),(o.data.polygons,'material_index','i',1)]:
  a=array.array(code,[0])*(len(data)*n);data.foreach_get(field,a);h.update(a.tobytes())
 for uv in o.data.uv_layers:
  a=array.array('f',[0])*(len(uv.data)*2);uv.data.foreach_get('uv',a);h.update(uv.name.encode());h.update(a.tobytes())
 h.update(bytes(p.use_smooth for p in o.data.polygons));return h.hexdigest()
before={o.name:digest(o) for o in scene.objects if o.type=='MESH'}
grass=bpy.data.images.load(str(ROOT/'development/levels/textures/hollow-creek/grass-green-v3.png'));grass.name='Forest green grass v3 runtime512 derivative';grass.scale(512,512);grass.pack()
old=bpy.data.images['grass-dirty runtime512 derivative'];changed=[]
for mat in bpy.data.materials:
 if mat.use_nodes:
  for node in mat.node_tree.nodes:
   if node.type=='TEX_IMAGE' and node.image==old:node.image=grass;changed.append(mat.name)
for weight in [.25,.5,.75]:
 image=bpy.data.images['Grass tread transition '+str(weight)];a=grass.copy();a.scale(256,256);d=bpy.data.images['trail-dirt runtime512 derivative'].copy();d.scale(256,256)
 image.pixels.foreach_set(np.array(a.pixels[:],dtype=np.float32)*(1-weight)+np.array(d.pixels[:],dtype=np.float32)*weight);image.pack();bpy.data.images.remove(a);bpy.data.images.remove(d)
assets=[];objects=[('level_camp_terrain_amenities','level_camp_ground_green'),('level_camp_boundary_banks','level_camp_banks_green'),('Continuous west forest lakeshore return','level_camp_west_green'),('Continuous east forest lakeshore return','level_camp_east_green')]
for name,id in objects:bpy.data.objects[name]['runtime_geometry_source']=id
assert before=={o.name:digest(o) for o in scene.objects if o.type=='MESH'}
target=OUT/'hollow-creek-green-grass.blend';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
for name,id in objects:
 original=bpy.data.objects[name];o=original.copy();o.data=original.data.copy();o.name=id;scene.collection.objects.link(o)
 active=next(u.name for u in o.data.uv_layers if u.active_render)
 for name_to_remove in [u.name for u in o.data.uv_layers if u.name!=active]:
  o.data.uv_layers.remove(o.data.uv_layers[name_to_remove])
 o.data.uv_layers.active=o.data.uv_layers[active];o.data.calc_loop_triangles();bpy.ops.object.select_all(action='DESELECT');o.select_set(True);bpy.context.view_layer.objects.active=o
 file=ROOT/f'assets/runtime/models/{id}.glb';bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
 editable=ROOT/f'development/levels/blender/{id}.blend';bpy.data.libraries.write(str(editable),{o},compress=True)
 assert len(o.data.loop_triangles)<50000 and file.stat().st_size<4000000
 assets.append(dict(id=id,native_object=name,model=file.relative_to(ROOT).as_posix(),editable=editable.relative_to(ROOT).as_posix(),triangles=len(o.data.loop_triangles),bytes=file.stat().st_size,sha256=hashlib.sha256(file.read_bytes()).hexdigest()));bpy.data.objects.remove(o,do_unlink=True)
from finalize_earthen_bank_sources import materialize
materialize(assets)
assert hashlib.sha256(parent.read_bytes()).hexdigest()==r['scene_sha256']
report=dict(status='material-only green grass variant',parent=r['scene'],parent_sha256=r['scene_sha256'],scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),unchanged_mesh_geometry_uvs_poses=len(before),mesh_digests=before,changed_materials=changed,recomposed_transition_weights=[.25,.5,.75],source_texture='development/levels/textures/hollow-creek/grass-green-v3.png',assets=assets,physics_authority=r['scene'],runtime_mission_unlocked=False)
(OUT/'green-grass.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8');print('GREEN GRASS',len(before),'unchanged meshes;',len(assets),'exports',flush=True)
