"""Measure and view existing GLBs without changing source pivots, UVs or files."""
from pathlib import Path
import argparse,hashlib,json,math,sys
import bpy
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
ASSETS=['level_student_desk','chair_mp_5','table_large_2','dinner_table_1','sofa_1','armchair_1',
        'bed_5','bed_1','level_trace_camp_bed','shelf_mp_4','level_lived_shelf','level_lockers',
        'wooden_crate_1','level_work_repair_bench','level_workbench','level_castle_four_poster_bed',
        'level_kitchen_counter','level_laundry_washer','level_lived_pantry','level_hay_bale',
        'level_work_crew_footlocker','level_lived_linen_press','kenney_detail_bench','wardrobe_mp_1']

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--runtime-sample',action='store_true')
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    if args.runtime_sample:
        records=[dict(id=name,path=f'assets/runtime/models/{name}.glb',maps=[],status='EXISTING_RUNTIME_SAMPLE') for name in ASSETS]
    else:
        inventory=json.loads((ROOT/'development/assets/asset-library-index.json').read_text())
        records=[r for r in inventory['model_files'] if r['collection'] in ['existing_map_specific_roughs','existing_map_specific_refinements']]
    bpy.ops.wm.read_factory_settings(use_empty=True);rows=[];groups=[]
    for record in records:
        name=record['id'];path=ROOT/record['path'];before=set(bpy.context.scene.objects)
        bpy.ops.import_scene.gltf(filepath=str(path));objects=set(bpy.context.scene.objects)-before
        meshes=[o for o in objects if o.type=='MESH'];points=[o.matrix_world@v.co for o in meshes for v in o.data.vertices]
        low=[min(p[i] for p in points) for i in range(3)];high=[max(p[i] for p in points) for i in range(3)]
        for o in meshes:o.data.calc_loop_triangles()
        triangles=sum(len(o.data.loop_triangles) for o in meshes)
        rows.append(dict(asset=name,path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
            blender_min=low,blender_max=high,size_m=[high[i]-low[i] for i in range(3)],triangles=triangles,
            meshes=len(meshes),materials=len({m for o in meshes for m in o.data.materials if m}),
            uv_meshes=sum(bool(o.data.uv_layers) for o in meshes),images=len({node.image.name for o in meshes for m in o.data.materials if m and m.use_nodes for node in m.node_tree.nodes if node.type=='TEX_IMAGE' and node.image}),
            maps=record.get('maps',[]),source_stage=record.get('status'),mount=record.get('mount',record.get('authoring_hints',{}).get('mount')),
            status='SOURCE_LIBRARY_MEASURED_NOT_PLACED_OR_ACCEPTED'))
        anchor=bpy.data.objects.new(name+' source pivot anchor',None);bpy.context.collection.objects.link(anchor)
        for o in objects:
            if o.parent not in objects:o.parent=anchor
        groups.append((anchor,low,high))
    # Inspection is a labelled selection; the measured catalog retains every
    # staged source. Keep large architectural studies off this prop-size sheet.
    visible=set(range(len(rows))) if args.runtime_sample else {i for i,r in enumerate(rows) if r['mount']=='floor' and max(r['size_m'])<4}
    visible=set(sorted(visible)[:24]);spacing=max(max(rows[i]['size_m'][:2]) for i in visible)+1.5
    slot=0
    for i,(anchor,low,high) in enumerate(groups):
        if i not in visible:
            for child in anchor.children_recursive:child.hide_render=True
            continue
        x=(slot%6)*spacing;y=(slot//6)*spacing;slot+=1
        anchor.location=(x-(low[0]+high[0])/2,y-(low[1]+high[1])/2,-low[2])
        text=bpy.data.curves.new('label','FONT');text.body=rows[i]['asset'];text.size=.23;text.align_x='CENTER'
        obj=bpy.data.objects.new('Source label',text);bpy.context.collection.objects.link(obj);obj.location=(x,y-spacing*.46,.01)
    centre=Vector((spacing*2.5,spacing*1.5,0));span=spacing*6
    bpy.ops.object.camera_add(location=centre+Vector((span*.35,-span*.55,span*.9)));cam=bpy.context.object
    cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.6;bpy.context.scene.camera=cam
    bpy.ops.object.light_add(type='SUN',location=(0,0,15));bpy.context.object.rotation_euler=(.4,-.35,-.5);bpy.context.object.data.energy=2
    scene=bpy.context.scene;scene.world=bpy.data.worlds.new('Neutral library');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.55
    scene.render.engine='CYCLES';scene.cycles.samples=8;scene.render.resolution_x=2400;scene.render.resolution_y=1700;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
    output=ROOT/'development/levels/rebuild/furnishings';output.mkdir(parents=True,exist_ok=True)
    capture=ROOT/('development/levels/rebuild/captures/furnishing-batch/library-pilot.png' if args.runtime_sample else 'development/levels/rebuild/captures/furnishing-batch/staged-source-inspection.png');capture.parent.mkdir(parents=True,exist_ok=True)
    scene.render.filepath=str(capture);bpy.ops.render.render(write_still=True)
    (output/('runtime-sample-assets.json' if args.runtime_sample else 'staged-asset-bounds.json')).write_bytes((json.dumps(dict(scope='Selected runtime sample' if args.runtime_sample else 'All existing staged map roughs and refinements; named chat artifact import remains pending',assets=rows,inspection=capture.relative_to(ROOT).as_posix()),indent=2)+'\n').encode())
    print('MEASURED_SOURCE_LIBRARY',len(rows),sum(r['triangles'] for r in rows),'triangles',flush=True)

if __name__=='__main__':main()
