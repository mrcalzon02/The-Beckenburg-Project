"""Remaining-proposal rough shell blocking from the existing measured layout authority.
Blender --background --factory-startup --python this_file
No Java, engine, playable-map registry or source layouts are changed.
"""
import bpy,bmesh,math,json,hashlib,sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parent
LAYOUTS=ROOT.parent/'layouts'
sys.path.insert(0,str(ROOT))
WALL=.16
def material(name,color):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    m.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(*color,1)
    m.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.9
    return m
def box(name,x,y,z,w,h,d,mat,collection,**meta):
    bpy.ops.mesh.primitive_cube_add(size=1,location=(x,-z,y+h/2))
    o=bpy.context.object;o.name=name;o.dimensions=(w,d,h)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    for c in list(o.users_collection):c.objects.unlink(o)
    collection.objects.link(o);o.data.materials.append(mat)
    for k,v in meta.items():o[k]=v
    return o
def collection(name):
    c=bpy.data.collections.new(name);bpy.context.scene.collection.children.link(c);return c
def label(text,x,y,z,c,size=.5):
    data=bpy.data.curves.new(text,'FONT');data.body=text;data.size=size;data.align_x='CENTER'
    o=bpy.data.objects.new(text,data);c.objects.link(o);o.location=(x,-z,y);return o
def intervals(lo,hi,cuts):
    current=lo;out=[]
    for a,b in sorted(cuts):
        a=max(lo,a);b=min(hi,b)
        if b<=current:continue
        if a>current:out.append((current,a))
        current=max(current,b)
    if current<hi:out.append((current,hi))
    return out
def wall_crossings(zone,data):
    x0,z0,x1,z1=zone['bounds'];out={s:[] for s in ['N','S','W','E']}
    for link in data['links']:
        if zone['id'] not in [link['a'],link['b']]:continue
        for p,q in zip(link['points'],link['points'][1:]):
            for side,axis,plane,tangent,lo,hi in [('N',2,z0,0,x0,x1),('S',2,z1,0,x0,x1),('W',0,x0,2,z0,z1),('E',0,x1,2,z0,z1)]:
                delta=q[axis]-p[axis]
                if abs(delta)<1e-8:continue
                t=(plane-p[axis])/delta
                if not -.0001<=t<=1.0001:continue
                along=p[tangent]+t*(q[tangent]-p[tangent]);y=p[1]+t*(q[1]-p[1])
                if lo-.001<=along<=hi+.001 and abs(y-zone['y'])<.15:
                    width=min(link['width'],hi-lo-.32)
                    if width>.3:out[side].append((max(lo+.16,min(hi-.16-width,along-width/2)),width,link['id']))
    return out
def slab(zone,holes,c,mat,role='floor',structural=False):
    if zone.get('shape'):
        if structural:
            from structural_pass import shaped_deck
            return shaped_deck(zone,holes,c,mat,role)
        from proposal_landforms import shaped_slab
        obj=shaped_slab(zone,c,mat)
        if role!='floor':obj['role']=role;obj.name='Ceiling cap '+zone['id']
        return obj
    a,b,d,e=zone['bounds']
    if structural:
        holes=[h for h in holes if h[2]>a+.0001 and h[0]<d-.0001 and h[3]>b+.0001 and h[1]<e-.0001]
    xs={a,d};zs={b,e}
    for x0,z0,x1,z1 in holes:
        xs.update([max(a,min(d,x0)),max(a,min(d,x1))]);zs.update([max(b,min(e,z0)),max(b,min(e,z1))])
    xs=sorted(xs);zs=sorted(zs)
    if structural:
        # Nearly coincident flight/cap boundaries can create sub-millimetre
        # cells which collapse in the exported float mesh. Merge construction
        # planes within 0.1mm, preserving the exact outer slab boundaries.
        def planes(values,lo,hi):
            merged=[lo]
            for value in values:
                if value-lo>.0001 and hi-value>.0001 and value-merged[-1]>.0001:merged.append(value)
            return merged+[hi]
        xs=planes(xs,a,d);zs=planes(zs,b,e)
    for x0,x1 in zip(xs,xs[1:]):
        for z0,z1 in zip(zs,zs[1:]):
            if any(h[0]<(x0+x1)/2<h[2] and h[1]<(z0+z1)/2<h[3] for h in holes):continue
            box(('Floor ' if role=='floor' else 'Ceiling cap ')+zone['id'],(x0+x1)/2,zone['y']-.16,(z0+z1)/2,x1-x0,.16,z1-z0,mat,c,zone=zone['id'],role=role)
def route_mesh(link,c,mat,structural_data=None):
    """Editor-only study line. A route is never architecture or collision."""
    curve=bpy.data.curves.new(link['id']+' navigation study','CURVE');curve.dimensions='3D'
    line=curve.splines.new('POLY');line.points.add(len(link['points'])-1)
    for point,(x,y,z) in zip(line.points,link['points']):point.co=(x,-z,y,1)
    obj=bpy.data.objects.new(link['id']+' nonphysical study',curve);c.objects.link(obj)
    obj.hide_render=True;obj['role']='nonphysical navigation study guide';obj['link']=link['id']
    obj['clear_width']=link['width'];obj['export_as_architecture']=False

def build(path,output_root=None,structural=False):
    data=json.loads(path.read_text(encoding='utf-8'));folder=(output_root or ROOT)/path.parent.name;folder.mkdir(parents=True,exist_ok=True)
    sealed_links=[]
    if structural:
        sealed_links=[link for link in data['links'] if link.get('player_access')=='SEALED_NO_ROUTE']
        data=dict(data,links=[link for link in data['links'] if link not in sealed_links])
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
    scene=bpy.context.scene;scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1
    wall=material('Neutral shell',(.52,.58,.61));floor=material('Interior floors',(.30,.38,.42));outside=material('Exterior terrain reservation',(.30,.40,.28));route=material('Cyan supported route reservation',(.12,.46,.55));vertical=material('Amber stair and grade reservation',(.70,.37,.10));ritual=material('Clear ritual reservation',(.52,.24,.39));van=material('Shared van envelope',(.48,.60,.57))
    if 'terrain' in data:
        from proposal_landforms import build_landform
        build_landform(data)
        if structural and (data.get('current_saved_editor_terrain') or data.get('saved_editor_terrain')):
            from structural_pass import saved_editor_terrain
            old=[o for o in scene.objects if o.type=='MESH' and o.name.startswith('Authored ') and 'terrain' in o.name]
            assert len(old)==1,'Expected one predecessor proposal terrain'
            terrain_material=old[0].data.materials[0];terrain_collection=old[0].users_collection[0]
            bpy.data.objects.remove(old[0],do_unlink=True)
            saved_editor_terrain(data,terrain_collection,terrain_material)
    notes=[];openings=[];byid={z['id']:z for z in data['zones']}
    for zone in data['zones']:
        c=collection(zone['id']+' | '+zone['name']+' | Y'+str(zone['y']))
        holes=[]
        for link in data['links']:
            for p,q in zip(link['points'],link['points'][1:]):
                if abs(p[1]-q[1])<.01:continue
                high=max([p,q],key=lambda v:v[1])
                if abs(high[1]-zone['y'])<.01 and zone['id'] in [link['a'],link['b']]:
                    w=link['width']+.4;holes.append((high[0]-w/2,high[2]-w/2,high[0]+w/2,high[2]+w/2))
        if structural:
            from structural_pass import deck_holes
            holes=deck_holes(zone['y'],data)
        from proposal_landforms import keep_slab, shaped_wall
        exterior=zone['kind'] in ['outside','staging']
        if (not(structural and exterior) or zone.get('constructed_surface')) and ('terrain' not in data or keep_slab(zone,data) or zone.get('constructed_surface')):
            slab(zone,holes,c,outside if zone['kind'] in ['outside','staging'] else floor,structural=structural)
        if structural and not exterior:
            # A real upper floor supplies its underside. Only cap the remainder;
            # never place a second coplanar ceiling over the same upper deck.
            top=zone['y']+zone['height']
            above=[other['bounds'] for other in data['zones'] if (other['kind'] not in ['outside','staging'] or other.get('constructed_surface')) and abs(other['y']-top)<.01]
            slab(dict(zone,y=top),deck_holes(top,data)+above,c,floor,role='ceiling-cap',structural=True)
        a,b,d,e=zone['bounds'];h=zone['height']
        if zone.get('shape')=='circle':
            shaped_wall(zone,c,wall,data)
        if zone['kind'] not in ['outside','staging'] and not zone.get('shape'):
            cuts=wall_crossings(zone,data)
            for side,lo,hi,plane,along in [('N',a,d,b,True),('S',a,d,e,True),('W',b,e,a,False),('E',b,e,d,False)]:
                spans=[(x,x+w) for x,w,_ in cuts[side]]
                for start,end in intervals(lo,hi,spans):
                    o=box('Wall '+zone['id']+' '+side,(start+end)/2 if along else plane,zone['y'],plane if along else (start+end)/2,end-start if along else WALL,h,WALL if along else end-start,wall,c,role='rough shell wall',zone=zone['id'])
                for start,w,linkid in cuts[side]:
                    head=min(2.45,h-.1)
                    box('Header '+zone['id']+' '+side,(start+w/2) if along else plane,zone['y']+head,plane if along else (start+w/2),w if along else WALL,h-head,WALL if along else w,wall,c,role='rough opening header')
                    openings.append(dict(zone=zone['id'],side=side,link=linkid,width=w,height=head))
        label(zone['id']+' '+zone['name'],(a+d)/2,zone['y']+.025,(b+e)/2,c,size=max(.25,min(.7,(d-a)/18)))
        if holes:notes.append(dict(zone=zone['id'],issue='Deck openings reserved; final stair headroom, guards and landings required',openings=holes))
    if structural and sealed_links:
        from structural_pass import context_infill
        context_infill(data,sealed_links,collection('PERMANENT CONTEXT INFILL / no open route'),wall,box)
    routes=collection('ROUTE SUPPORTS / cyan level; amber elevation reservations')
    for link in data['links']:
        if structural and link['kind']=='stairs':
            from structural_pass import stair_flights
            if stair_flights(link,routes,vertical,box,notes):continue
        route_mesh(link,routes,route if link['kind']=='level' else vertical,data if structural else None)
        if link['kind']!='level':
            run=sum(math.hypot(q[0]-p[0],q[2]-p[2]) for p,q in zip(link['points'],link['points'][1:]));rise=max(p[1] for p in link['points'])-min(p[1] for p in link['points'])
            notes.append(dict(link=link['id'],issue='Elevation support is a rough mass, not an accepted stair or path',rise=rise,plan_run=run,requires_switchbacks=run<rise*1.6,source_note=link.get('construction',link.get('note',''))))
    marks=collection('RITUAL RESERVATIONS / keep clear; no gameplay grants')
    for r in data['rituals']:
        x,y,z=r['point'];box(r['rite'],x,y+.012,z,4.4,.025,4.4,ritual,marks,role='ritual clear-area reservation')
    staging=data['staging'];x,y,z=staging['point'];v=box('VAN ENVELOPE / use canonical module during integration',x,y+.5,z,1.784,2.125,4.256,van,marks,role='placeholder envelope; not replacement van');v.rotation_euler.z=-staging['yaw']
    scene['layout_id']=data['id'];scene['status']='ROUGH SHELL BLOCKING ONLY / no gameplay or collision acceptance';scene['source_sha256']=hashlib.sha256(path.read_bytes()).hexdigest();scene['units']='metres; source X/Z plan and Y elevation converted to Blender Z up'
    scene['next_step']='Review openings and connectors against concepts, resolve stairs and terrain, then native collision/navigation integration.'
    if structural:
        from structural_pass import deduplicate_solids
        removed=deduplicate_solids(scene)
        scene['structure_pass']='closed-depth-v1 / intermediate native construction; runtime acceptance pending'
        scene['exact_duplicate_solids_removed']=removed
    if 'terrain' in data:
        for obj in scene.objects:
            if obj.type!='MESH' or obj.data.uv_layers:continue
            uv=obj.data.uv_layers.new(name='Metre plan UV')
            for poly in obj.data.polygons:
                for li in poly.loop_indices:
                    v=obj.data.vertices[obj.data.loops[li].vertex_index].co
                    uv.data[li].uv=(v.x/8,v.y/8)
    # Native scenes retain all measured elevations. Floors and named shells are separately editable.
    blend=folder/'rough-shell.blend';bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    if structural:
        # Native planning labels remain editable in Blender; they must not turn
        # into tens of thousands of irrelevant triangles in structural exports.
        bpy.ops.object.select_all(action='DESELECT')
        for obj in scene.objects:obj.select_set(obj.type=='MESH' and obj.get('role') not in ['ritual clear-area reservation','placeholder envelope; not replacement van'])
    bpy.ops.export_scene.gltf(filepath=str(folder/'rough-shell.glb'),export_format='GLB',export_extras=True,use_selection=structural)
    a,b,d,e=data['site_bounds'];height=max(z['y']+z['height'] for z in data['zones']);center=Vector(((a+d)/2,-(b+e)/2,height*.35));span=max(d-a,e-b)
    bpy.ops.object.camera_add(location=center+Vector((span*.85,-span*.95,span*1.25)));cam=bpy.context.object;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;scene.camera=cam
    scene.world=bpy.data.worlds.new('Blockout neutral review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.7
    bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.45,-.35,-.6);bpy.context.object.data.energy=2
    scene.render.engine='CYCLES';scene.cycles.samples=4;scene.cycles.use_denoising=True;scene.render.resolution_x=960;scene.render.resolution_y=720;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
    if structural:scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL'
    scene.render.image_settings.file_format='PNG';scene.render.filepath=str(folder/'preview.png');bpy.ops.render.render(write_still=True)
    mesh_count=sum(o.type=='MESH' for o in scene.objects)
    bpy.ops.wm.open_mainfile(filepath=str(blend));assert bpy.context.scene['source_sha256']==hashlib.sha256(path.read_bytes()).hexdigest();assert sum(o.type=='MESH' for o in bpy.context.scene.objects)==mesh_count
    report=dict(id=data['id'],title=data['title'],source=str(path.relative_to(ROOT.parent)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),zones=len(data['zones']),routes=len(data['links']),shell_openings=openings,meshes=mesh_count,issues=notes,source_refinement=data.get('refine',[]),native_reopened=True,blend_sha256=hashlib.sha256(blend.read_bytes()).hexdigest(),status='rough-shell-blocked; runtime integration pending')
    (folder/'implementation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    if structural:
        from structural_pass import inspect_export
        report['structural_review']=inspect_export(folder,data,sealed_links)
        (folder/'implementation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    (folder/'NEXT.md').write_text('# '+data['title']+' â€” next implementation steps\n\nRough shell blocking only. Source layout coordinates and elevations are retained.\n\n1. Review actual apertures, floor openings and connector support against the approved concept.\n2. Engineer every amber elevation link: real flights or paths, landings, headroom, guards and terrain.\n3. Resolve overlapping shell partitions and route crossings before building navigation collision.\n4. Integrate the canonical van, authored room volumes, circuits, acoustics and protocol IDs.\n5. Furnish, dress and test human/AI hunts and rituals; retain the map lock until accepted.\n\nSource-specific concerns:\n\n'+'\n'.join('- '+str(n) for n in data.get('refine',[]))+'\n\nSee implementation.json for every connector and floor-opening reservation.\n')
    print('BLOCKED',data['id'],len(data['zones']),'zones',len(openings),'openings',flush=True)
    return report
def main():
    import argparse
    ap=argparse.ArgumentParser(description='Build only explicitly selected new rough packets; existing files are protected.')
    ap.add_argument('layouts',nargs='+',help='Paths to selected layout.json files')
    ap.add_argument('--replace',action='store_true',help='Explicitly rebuild only selected outputs')
    args=ap.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    paths=[Path(p).resolve() for p in args.layouts]
    for path in paths:
        assert path.is_relative_to(ROOT.parent) and path.name=='layout.json',path
        if (ROOT/path.parent.name/'rough-shell.blend').exists() and not args.replace:
            raise RuntimeError('Existing blockout protected; select a new output or explicitly use --replace: '+str(path))
    reports=[build(p) for p in paths]
    out=ROOT/'selected-manifest.json'
    out.write_text(json.dumps(dict(status='selected rough proposals only',maps=reports,limitations=['Native massing only; no collision/navigation acceptance or playable unlock.','Original historic manifest and unselected maps are untouched.']),indent=2)+'\n',encoding='utf-8')
    print('SELECTED_BLOCKOUTS_COMPLETE',len(reports),flush=True)

if __name__=='__main__':main()
