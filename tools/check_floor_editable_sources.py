"""Reopen editable floor sources and verify packed images and native parity."""
from pathlib import Path
import bpy,json,sys,hashlib
sys.path.insert(0,'tools');from check_roster_decks import read_parts;from review_roster_macroterrain import records,sha;from export_roster_collision import face_keys
base=Path('development/levels/rebuild/floor-closure-pass');rows=[]
for p in sorted(base.glob('*/construction.json')):
 r=json.loads(p.read_text());scene=p.parent/'closed-floors.blend';bpy.ops.wm.open_mainfile(filepath=str(scene.resolve()));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};native=read_parts(p.parent/'closed-floors.hcs2')
 for plate in r['plates']:assert face_keys(records(objects[plate['name']]))==face_keys(native[plate['name']][1]),(r['id'],plate['name'],'editable/native mismatch')
 images={n.image for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image}
 assert all(i.size[0]>0 and i.size[1]>0 and i.packed_file is not None for i in images),(r['id'],'unpacked/unloaded source image')
 rows.append(dict(id=r['id'],blend_sha256=sha(scene),new_plates_match_native=True,packed_loaded_material_images=len(images)));print('PASS_EDITABLE_FLOOR_SOURCE',r['id'],len(images),flush=True)
(base/'verification/editable-sources.json').write_bytes((json.dumps(dict(status='PASS_REOPENED_EDITABLE_SOURCES',sites=rows,checker_sha256=sha(Path(__file__))),indent=2)+'\n').encode())
