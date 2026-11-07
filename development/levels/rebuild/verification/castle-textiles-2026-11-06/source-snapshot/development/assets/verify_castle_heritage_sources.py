"""Freshly reopen delivered editable scenes and require connected packed images."""
from pathlib import Path
import bpy,hashlib,json
OUT=Path(__file__).with_name('castle-museum');records=[]
for name in ['tapestry','restoration_painting','library_rug']:
 path=OUT/(name+'.blend');digest=hashlib.sha256(path.read_bytes()).hexdigest()
 bpy.ops.wm.open_mainfile(filepath=str(path))
 images=[]
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH':continue
  for material in obj.data.materials:
   for node in material.node_tree.nodes:
    if node.type!='TEX_IMAGE':continue
    image=node.image
    assert image and image.packed_file,(name,node.name,'missing packed payload')
    pixel=list(image.pixels[:4]) # fresh .blend images load lazily
    assert len(pixel)==4 and image.has_data and min(image.size)>0,(name,node.name,'packed image failed to decode')
    images.append(dict(name=image.name,size=list(image.size),packed=True))
 assert images and hashlib.sha256(path.read_bytes()).hexdigest()==digest
 records.append(dict(source=path.name,sha256=digest,images=images))
(OUT/'packed-heritage-sources.json').write_text(json.dumps(dict(status='PASS',criterion='Fresh Blender reopen; every mesh-connected image packed, loaded and nonempty; source bytes unchanged',sources=records),indent=2)+'\n',encoding='utf-8')
print('PASS freshly reopened3 editable heritage sources; all connected texture images packed and loaded; original scene bytes unchanged')
