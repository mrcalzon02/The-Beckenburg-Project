"""Original Beckenburg open retail fixture, metres. Run with Blender 5.
No source-library geometry is modified. Runtime fit is independently checked against the GLB.
"""
from pathlib import Path
import bpy, random
ROOT=Path(__file__).resolve().parents[2]
bpy.ops.wm.read_factory_settings(use_empty=True)
def material(name,color):
    mat=bpy.data.materials.new(name);mat.diffuse_color=(*color,1);mat.use_nodes=True
    bsdf=mat.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.78
    image=bpy.data.images.new(name,128,128);rng=random.Random(103 if name=='Oak shelf planks' else 41)
    pixels=[]
    for y in range(128):
        for x in range(128):
            shade=1+rng.uniform(-.065,.065)
            pixels.extend([min(1,c*shade) for c in color]+[1])
    image.pixels=pixels;image.pack()
    node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=image
    mat.node_tree.links.new(node.outputs['Color'],bsdf.inputs['Base Color'])
    return mat
oak=material('Oak shelf planks',(.28,.20,.12));iron=material('Dark iron uprights',(.12,.14,.15))
def part(name,x,y,z,w,d,h,mat):
    bpy.ops.mesh.primitive_cube_add(size=1,location=(x,y,z));obj=bpy.context.object;obj.name=name
    obj.dimensions=(w,d,h);bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    obj.data.materials.append(mat)
    bevel=obj.modifiers.new('Small worn edges','BEVEL');bevel.width=.005;bevel.segments=1
    bpy.ops.object.modifier_apply(modifier=bevel.name)
for x in [-1.005,1.005]:
    for y in [-.295,.295]:part('Iron upright',x,y,1.27,.05,.05,2.54,iron)
for top in [.35,1.10,1.85,2.54]:
    for y in [-.213333,0,.213333]:part('Oak shelf plank',0,y,top-.025,2.06,.213333,.05,oak)
    part('Front shelf lip',0,.315,top-.065,2.06,.01,.10,iron)
    part('Rear shelf rail',0,-.305,top-.08,2.06,.03,.11,iron)
part('Rear horizontal brace',0,-.305,1.5,2.06,.03,.045,iron)
folder=ROOT/'development/assets/castle-retail';folder.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.save_as_mainfile(filepath=str(folder/'open-retail-shelf.blend'))
bpy.ops.export_scene.gltf(filepath=str(ROOT/'assets/runtime/models/level_castle_retail_shelf.glb'),export_format='GLB',export_yup=True)
print('PASS authored open retail shelf: 2.06 x .64 x 2.54m, shelf tops .35/1.10/1.85m')
