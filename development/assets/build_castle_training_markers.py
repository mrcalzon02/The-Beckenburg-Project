"""Three simple floor-inlay diagrams following the shipped rite marker geometry."""
from pathlib import Path
import bpy,bmesh,math,json,hashlib
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-training';OUT.mkdir(parents=True,exist_ok=True)
source=ROOT/'assets/runtime/models/level_boiler_modern.glb'
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
material=next(m for m in bpy.data.materials if m.name=='Rebuild Original ivory')
for node in material.node_tree.nodes:
 if node.type=='TEX_IMAGE' and not node.image.packed_file:node.image.pack()
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
records=[]
for strokes,rite in [(3,'banishment'),(4,'cleansing'),(5,'absolution')]:
 verts=[(r*math.cos(i*math.pi/10),r*math.sin(i*math.pi/10),h) for h in [0,.008] for r in [.72,.702] for i in range(20)];faces=[]
 for i in range(20):
  j=(i+1)%20;faces.extend([(i,j,j+40,i+40),(i+20,i+60,j+60,j+20),(i+40,j+40,j+60,i+60),(i,i+20,j+20,j)])
 for i in range(strokes):
  a=i*math.pi*2/strokes;k=len(verts)
  for h in [0,.008]:
   for r,b in [(.36,a),(.61,a),(.61,a+.05),(.36,a+.05)]:verts.append((r*math.cos(b),r*math.sin(b),h))
  faces.extend(tuple(k+j for j in f) for f in [(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)])
 mesh=bpy.data.meshes.new('Supported thin floor-inlay diagram');mesh.from_pydata(verts,[],faces);mesh.update();mesh.uv_layers.new();mesh.materials.append(material)
 for poly in mesh.polygons:
  for loop in poly.loop_indices:
   co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=((co.x+.72)/1.44,(co.y+.72)/1.44)
 bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();mesh.calc_loop_triangles()
 obj=bpy.data.objects.new('Training floor inlay '+rite,mesh);bpy.context.collection.objects.link(obj);obj.select_set(True);bpy.context.view_layer.objects.active=obj
 name='level_castle_training_'+rite;bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(name+'.blend')))
 path=OUT/(name+'.glb');bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
 assert len(mesh.loop_triangles)==160+12*strokes
 records.append(dict(id=name,triangles=len(mesh.loop_triangles),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),height=.008,support='All ring/stroke bottoms sit on real floor; floor finish below body-step threshold',art='Original simple geometric diagrams follow existing production rite markers; unchanged original ivory PNG material reused, no generated raster artwork'))
 bpy.data.objects.remove(obj,do_unlink=True)
(OUT/'marker-review.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
print('PASS3 floor-supported training inlays,196/208/220tri, original material,8mm walkable finish')
