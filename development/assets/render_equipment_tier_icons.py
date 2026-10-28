"""Blender background run: one approved runtime model, one close three-quarter icon.

blender --background --python development/assets/render_equipment_tier_icons.py -- --limit 3
The code-side runtime model catalog is authoritative, including original source aliases.
"""
import bpy, sys, re, json, math, argparse
import numpy as np
from pathlib import Path
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view

ap=argparse.ArgumentParser();ap.add_argument('--limit',type=int);ap.add_argument('--only');ap.add_argument('--force',action='store_true')
args=ap.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'assets/runtime/ui/item-tier-icons';OUT.mkdir(parents=True,exist_ok=True)
catalog=(ROOT/'src/main/java/org/beckenburg/EquipmentModelCatalog.java').read_text()
stages=re.findall(r'new Stage\("([A-Z0-9_]+)", EquipmentTiers.Tier\.(I|II|III), "([a-z0-9_]+)"',catalog)
if args.only:stages=[s for s in stages if s[0]==args.only.upper()]
if args.limit:stages=stages[:args.limit]
manifest=[]
for index,(item,tier,model) in enumerate(stages):
    number={'I':1,'II':2,'III':3}[tier];name=f'{item.lower()}_t{number}.png';target=OUT/name
    path=ROOT/'assets/runtime/models'/f'{model}.glb'
    if not path.exists():raise RuntimeError(f'Missing approved runtime model: {path}')
    if not target.exists() or args.force:
        bpy.ops.wm.read_factory_settings(use_empty=True)
        scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=24
        scene.cycles.use_denoising=True;scene.render.resolution_x=512;scene.render.resolution_y=512;scene.render.resolution_percentage=100
        scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA';scene.render.film_transparent=True
        bpy.ops.import_scene.gltf(filepath=str(path))
        meshes=[o for o in scene.objects if o.type=='MESH']
        if not meshes:raise RuntimeError(f'Empty item: {model}')
        corners=[o.matrix_world@Vector(v) for o in meshes for v in o.bound_box]
        low=Vector([min(p[a] for p in corners) for a in range(3)]);high=Vector([max(p[a] for p in corners) for a in range(3)])
        center=(low+high)/2;scale=2/max(high-low)
        # Normalize world coordinates without replacing source pivots, UVs or materials.
        transforms={o:o.matrix_world.copy() for o in meshes}
        for o in meshes:
            o.parent=None;o.matrix_world=transforms[o]
            o.location=(o.location-center)*scale;o.scale*=scale
        bpy.context.view_layer.update()
        corners=[o.matrix_world@Vector(v) for o in meshes for v in o.bound_box]
        camera_data=bpy.data.cameras.new('Item camera');camera=bpy.data.objects.new('Item camera',camera_data);scene.collection.objects.link(camera);scene.camera=camera
        direction=Vector((4,-6,3.2)).normalized();camera.location=direction*8
        camera.rotation_euler=(-camera.location).to_track_quat('-Z','Y').to_euler();camera_data.type='ORTHO';camera_data.ortho_scale=3
        bpy.context.view_layer.update()
        projected=[world_to_camera_view(scene,camera,p) for p in corners]
        span=max(max(p.x for p in projected)-min(p.x for p in projected),max(p.y for p in projected)-min(p.y for p in projected))
        camera_data.ortho_scale*=span/.84
        # Centre the projected object, which is more reliable than a family contact sheet crop.
        projected=[world_to_camera_view(scene,camera,p) for p in corners]
        cx=(min(p.x for p in projected)+max(p.x for p in projected))/2;cy=(min(p.y for p in projected)+max(p.y for p in projected))/2
        camera.location+=camera.rotation_euler.to_matrix()@Vector(((cx-.5)*camera_data.ortho_scale,(cy-.5)*camera_data.ortho_scale,0))
        def area(name,position,power,size):
            data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size
            light=bpy.data.objects.new(name,data);scene.collection.objects.link(light);light.location=position
            light.rotation_euler=(-light.location).to_track_quat('-Z','Y').to_euler()
        area('Key',(3,-4,5),550,4);area('Fill',(-4,-2,2),350,4);area('Rim',(1,4,4),650,3)
        world=bpy.data.worlds.new('Neutral studio');scene.world=world;world.use_nodes=True;world.node_tree.nodes['Background'].inputs[0].default_value=(.3,.34,.4,1);world.node_tree.nodes['Background'].inputs[1].default_value=.35
        scene.view_settings.view_transform='AgX';scene.render.filepath=str(target)
        bpy.ops.render.render(write_still=True)
        # Authored bounding boxes can contain unused space. Reframe the camera
        # using the actual rendered alpha, then render again; never crop a sheet.
        for refinement in range(2):
            rendered=bpy.data.images.load(str(target),check_existing=False)
            pixels=np.array(rendered.pixels[:],dtype=np.float32).reshape(512,512,4)
            yy,xx=np.where(pixels[:,:,3]>.01);bpy.data.images.remove(rendered)
            if not len(xx):raise RuntimeError(f'Invisible item: {model}')
            fill=max(xx.max()-xx.min()+1,yy.max()-yy.min()+1)/512
            if .81<=fill<=.89:break
            cx=(xx.max()+xx.min()+1)/1024;cy=(yy.max()+yy.min()+1)/1024
            camera.location+=camera.rotation_euler.to_matrix()@Vector(((cx-.5)*camera_data.ortho_scale,(cy-.5)*camera_data.ortho_scale,0))
            camera_data.ortho_scale*=fill/.84
            bpy.ops.render.render(write_still=True)
    manifest.append({'id':item,'tier':number,'model':model,'file':name,'renderer':'Blender Cycles','pixels':512,'target_fill':.84})
    print(f'ICON {index+1}/{len(stages)} {name}',flush=True)
(OUT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print(f'Completed {len(manifest)} single-item tier icons',flush=True)
