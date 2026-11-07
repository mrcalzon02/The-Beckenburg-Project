"""Refit retained keeper study as one carved, supported low-count limestone solid.
Run Blender --factory-startup --background --python this_file after texture generation.
Original study/source/export remains preserved and unapproved.
"""
from pathlib import Path
import bpy,bmesh,json,hashlib,math
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/keeper-finish';PRE.mkdir(exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(OUT/'founder_statue.blend'))
for o in list(bpy.data.objects):
    if o.type!='MESH' or 'inscription' in o.name.lower():bpy.data.objects.remove(o,do_unlink=True)
for o in list(bpy.context.scene.objects):
    if o.type=='MESH':
        bpy.context.view_layer.objects.active=o
        for m in list(o.modifiers):bpy.ops.object.modifier_apply(modifier=m.name)
# Bridge the retained study's robe/plinth gap in actual stone, before union.
bpy.ops.mesh.primitive_cylinder_add(vertices=12,radius=.235,depth=.22,location=(0,0,.40))
bpy.ops.object.select_all(action='SELECT');bpy.context.view_layer.objects.active=bpy.context.object
bpy.ops.object.join();o=bpy.context.object;o.name='Keeper / continuous limestone carving'
bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
o.data.remesh_voxel_size=.009;bpy.ops.object.voxel_remesh()
m=o.modifiers.new('Quiet tool transitions','SMOOTH');m.factor=.3;m.iterations=2;bpy.ops.object.modifier_apply(modifier=m.name)
o.data.calc_loop_triangles();m=o.modifiers.new('Sharp low-count carving','DECIMATE');m.ratio=1080/len(o.data.loop_triangles);bpy.ops.object.modifier_apply(modifier=m.name)
m=o.modifiers.new('Explicit runtime triangles','TRIANGULATE');bpy.ops.object.modifier_apply(modifier=m.name)
# Restore authoritative floor contact following voxel reconstruction.
bottom=min(v.co.z for v in o.data.vertices)
for v in o.data.vertices:v.co.z-=bottom
image=bpy.data.images.load(str(OUT/'textures/keeper-imagegen-master.png'));image.scale(512,512);image.filepath_raw=str(OUT/'textures/keeper-runtime.png');image.file_format='PNG';image.save();image.pack()
mat=bpy.data.materials.new('Keeper / original generated limestone face and codex');mat.use_nodes=True
shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.92
tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
o.data.materials.clear();o.data.materials.append(mat)
for layer in list(o.data.uv_layers):o.data.uv_layers.remove(layer)
uv=o.data.uv_layers.new(name='Physical limestone / face / codex')
for p in o.data.polygons:
    p.material_index=0
    c=p.center;n=p.normal
    face=c.z>1.61 and c.z<1.98 and c.y<-.12 and n.y<-.25 and abs(c.x)<.145
    codex=1.27<c.z<1.36 and c.y<-.17 and abs(c.x)<.215 and n.z>.6
    for i in p.loop_indices:
        v=o.data.vertices[o.data.loops[i].vertex_index].co
        if face:u=.52+.46*max(0,min(1,(v.x+.145)/.29));w=.52+.46*max(0,min(1,(v.z-1.61)/.37))
        elif codex:u=.52+.46*max(0,min(1,(v.x+.215)/.43));w=.02+.46*max(0,min(1,(v.y+.42)/.26))
        else:
            # Planar projection per dominant surface with physical-scale grain.
            axis=max(range(3),key=lambda j:abs(n[j]));a,b=((v.y,v.z) if axis==0 else (v.x,v.z) if axis==1 else (v.x,v.y))
            u=.02+.46*((a+.8)/1.6);w=.02+.96*(b/2.2 if axis!=2 else (b+.8)/1.6)
            u=max(.01,min(.49,u));w=max(.01,min(.99,w))
        uv.data[i].uv=(u,w)
o.data.update();o.data.calc_loop_triangles()
assert len(o.data.loop_triangles)<=1100
source=OUT/'keeper_finished.blend';export=OUT/'exports/level_castle_keeper_statue.glb'
bpy.ops.outliner.orphans_purge(do_recursive=True)
bpy.context.preferences.filepaths.save_version=0
bpy.ops.wm.save_as_mainfile(filepath=str(source));bpy.ops.export_scene.gltf(filepath=str(export),export_format='GLB',use_selection=True,export_yup=True)
# Render the actual exported mesh, not the source construction scene.
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(export))
objects=[x for x in bpy.context.scene.objects if x.type=='MESH'];assert len(objects)==1
mesh=objects[0].data.copy();bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(mesh);bm.free()
links=[set() for _ in mesh.vertices]
for e in mesh.edges:a,b=e.vertices;links[a].add(b);links[b].add(a)
left=set(range(len(links)));groups=[]
while left:
    q=[left.pop()];group=set(q)
    while q:
        for j in links[q.pop()]:
            if j in left:left.remove(j);group.add(j);q.append(j)
    groups.append(group)
assert len(groups)==1,'Detached carving pieces'
assert abs(min(v.co.z for v in mesh.vertices))<1e-5
bm=bmesh.new();bm.from_mesh(mesh);nonmanifold=sum(not e.is_manifold for e in bm.edges);bm.free();assert nonmanifold==0,'Open carving joints'
mesh.calc_loop_triangles()
report=dict(status='PASS',triangles=len(mesh.loop_triangles),connected_components=len(groups),nonmanifold_edges=nonmanifold,
    source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),export_sha256=hashlib.sha256(export.read_bytes()).hexdigest(),
    original_study='founder_statue.blend retained unchanged; carved inscription geometry omitted',
    reference='https://www.metmuseum.org/art/collection/search/471046',
    support='Single continuous watertight carved solid; native plinth sits on floor. No detached hands, face, robe, codex or floating ornament.')
(OUT/'keeper-finish-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=12;scene.render.resolution_x=800;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Keeper review');scene.world.color=(.25,.25,.25)
for pos,power in [((1,-3,4),400),((-2,-1,2),200)]:
    bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=2.5;scene.camera=cam
for name,pos in [('front',(0,-4,1.4)),('quarter',(2,-4,1.9)),('side',(4,-.5,1.4)),('rear',(0,4,1.4))]:
    cam.location=pos;cam.rotation_euler=(Vector((0,0,1.03))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
for o in objects:
    for m in o.data.materials:
        for l in list(m.node_tree.links):
            if l.to_socket.name=='Base Color':m.node_tree.links.remove(l)
        m.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value=(.45,.45,.45,1)
cam.location=(2,-4,1.9);cam.rotation_euler=(Vector((0,0,1.03))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/'clay.png');bpy.ops.render.render(write_still=True)
bpy.ops.wm.open_mainfile(filepath=str(source))
for obj in bpy.context.scene.objects:
    if obj.type!='MESH':continue
    for material in obj.data.materials:
        for node in material.node_tree.nodes:
            if node.type=='TEX_IMAGE':
                im=node.image;assert im.packed_file and im.packed_file.size>0 and len(im.pixels)==512*512*4
print('PASS keeper',report)
