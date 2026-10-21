"""Daylight inspection views of actual exported runtime geometry; no concept art or mesh substitution."""
import bpy,math,json,sys,collections
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
LINEUP='--lineup' in sys.argv
if LINEUP:
    SRC=ROOT/'build/level-verification/scenes/foliage-variation'
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
    faces=json.loads((SRC/'geometry.json').read_text());groups=collections.defaultdict(list)
    for face in faces:groups[(face['material'],face['color'])].append(face)
    for (mid,color),group in groups.items():
        mat=bpy.data.materials.new('Exact exported foliage '+str(mid));mat.use_nodes=True;shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.9
        image=bpy.data.images.load(str(SRC/f'textures/{mid}.png'),check_existing=True);image.pack()
        tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
        verts=[];uv=[];tri=[]
        for face in group:
            n=len(verts)
            for k in ['a','b','c']:p=face[k];verts.append((p['x'],-p['z'],p['y']));uv.append((face[k+'u'],face[k+'v']))
            tri.append((n,n+1,n+2))
        mesh=bpy.data.meshes.new('Shared-source varied foliage');mesh.from_pydata(verts,[],tri);mesh.update();layer=mesh.uv_layers.new(name='Original runtime UV')
        for poly in mesh.polygons:
            for i in poly.loop_indices:layer.data[i].uv=uv[mesh.loops[i].vertex_index]
        obj=bpy.data.objects.new('Runtime leaf palette '+str(mid),mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(mat)
    scene=bpy.context.scene
    scene.world=bpy.data.worlds.new('Inspection daylight');scene.world.use_nodes=True
    scene['source_of_truth']='FoliageVariationCapture -> SceneRenderer.imported -> FoliageVariation / original single conifer GLB'
    views=[('foliage-one-source-eight-instances',(17,-33,12),(17,0,3.6),True)]
else:
    path=ROOT/'development/levels/blender/blackpine-dressed.blend';bpy.ops.wm.open_mainfile(filepath=str(path));scene=bpy.context.scene
    scene['source_of_truth']='BlackpineLevel.java / BlackpineDressing.java / SiteTerrain.java / SceneryInstances.java / FoliageVariation.java'
    scene['inspection_note']='Daylight renders are art/placement review. Runtime remains the actual night environment.'
    bpy.ops.wm.save_as_mainfile(filepath=str(path),compress=True)
    views=[('blackpine-dressed-daylight-overview',(78,-83,49),(33,-30,1.3),False),
           ('blackpine-dressed-well-and-patio',(35,-31,8),(25,-20,1.0),False),
           ('blackpine-dressed-forest-edge',(64,-39,4.2),(83,-28,3.5),False)]
scene.world.node_tree.nodes['Background'].inputs[0].default_value=(.35,.42,.51,1);scene.world.node_tree.nodes['Background'].inputs[1].default_value=.65
scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1
for obj in scene.objects:
    if obj.type=='LIGHT':obj.data.energy=0
bpy.ops.object.light_add(type='SUN',location=(25,-35,50));sun=bpy.context.object;sun.name='Inspection daylight only';sun.data.energy=2.1;sun.data.color=(1,.93,.81);sun.rotation_euler=(.45,-.5,-.55)
scene.render.engine='CYCLES';scene.cycles.samples=20;scene.cycles.use_denoising=True
scene.render.resolution_x=1440;scene.render.resolution_y=900 if not LINEUP else 600;scene.render.resolution_percentage=100
scene.view_settings.view_transform='Standard';scene.view_settings.look='None';scene.view_settings.exposure=0
for name,eye,target,ortho in views:
    bpy.ops.object.camera_add(location=eye);cam=bpy.context.object;cam.name='Inspection '+name;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.type='ORTHO' if ortho else 'PERSP';cam.data.ortho_scale=41;cam.data.lens=32;scene.camera=cam
    scene.render.image_settings.file_format='PNG';scene.render.filepath=str(ROOT/f'development/levels/previews/{name}.png');bpy.ops.render.render(write_still=True)
if LINEUP:
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'development/levels/blender/foliage-variation-review.blend'),compress=True)
print('REVIEW_COMPLETE',len(views),flush=True)
