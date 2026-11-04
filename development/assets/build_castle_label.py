"""86-triangle textured exhibit-label stand. Original museum conversion fitting."""
from pathlib import Path
import bpy,math,json,hashlib,bmesh,importlib.util
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'development/assets/castle-museum'
bpy.ops.wm.read_factory_settings(use_empty=True)

def image_material(name,image):
    mat=bpy.data.materials.new(name);mat.use_nodes=True
    node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Closest'
    mat.node_tree.links.new(node.outputs['Color'],mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
    mat.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.72
    return mat
image=bpy.data.images.load(str(OUT/'textures/armour-exhibit-label.png'));image.pack()
label=image_material('Original accession label / bronze and engraved text',image)
metal_image=bpy.data.images.new('Label stand / worn black iron',64,64);metal_image.pixels=[c for y in range(64) for x in range(64) for c in [.10+.008*math.sin(x*.3),.105+.008*math.sin(x*.3),.11+.008*math.sin(x*.3),1]];metal_image.pack()
metal=image_material('Label stand / blackened steel',metal_image)

def box(name,location,dimensions,angle=0):
    bpy.ops.mesh.primitive_cube_add(size=1,location=location);o=bpy.context.object;o.name=name;o.dimensions=dimensions
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True);o.rotation_euler.x=angle;o.data.materials.append(metal)
    return o
box('Weighted stand foot',(0,.035,.018),(.34,.28,.036))
box('Supporting iron stem',(0,.035,.5155),(.034,.034,.959))
angle=math.radians(-25);centre=Vector((0,0,.96))
back=box('Inclined label backing',centre,(.54,.024,.29),angle)
def transform(x,y,z):
    return centre+Vector((x,y*math.cos(angle)-z*math.sin(angle),y*math.sin(angle)+z*math.cos(angle)))
for x in [-.26,.26]:box('Narrow label side frame',transform(x,-.018,0),(.02,.018,.29),angle)
for z in [-.135,.135]:box('Narrow label edge frame',transform(0,-.018,z),(.50,.018,.02),angle)
points=[transform(-.25,-.0125,-.125),transform(.25,-.0125,-.125),transform(.25,-.0125,.125),transform(-.25,-.0125,.125)]
mesh=bpy.data.meshes.new('Textured caption face');mesh.from_pydata(points,[],[(0,1,2,3)]);mesh.uv_layers.new()
obj=bpy.data.objects.new('Readable original museum caption',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(label)
for loop,uv in zip(mesh.polygons[0].loop_indices,[(0,0),(1,0),(1,1),(0,1)]):mesh.uv_layers.active.data[loop].uv=uv
bpy.ops.object.select_all(action='SELECT');bpy.context.view_layer.objects.active=back;bpy.ops.object.join();obj=bpy.context.object
bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free()
obj.data.calc_loop_triangles();assert len(obj.data.loop_triangles)<=100
spec=importlib.util.spec_from_file_location('contacts',Path(__file__).with_name('castle_armour_lowpoly.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
m.audit_contacts(obj.data,'label-source')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'armour-label-stand.blend'))
path=OUT/'exports/level_castle_armour_label.glb'
bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');copy=obj.data.copy()
bm=bmesh.new();bm.from_mesh(copy);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(copy);bm.free();copy.calc_loop_triangles()
report=m.audit_contacts(copy,'label-export',hashlib.sha256(path.read_bytes()).hexdigest())
(OUT/'label-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS label stand:',len(copy.loop_triangles),'triangles; actual source/export supports connected; original readable texture')
