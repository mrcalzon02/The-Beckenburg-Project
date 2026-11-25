"""Source-library furnishing candidates; authored room programs and exact placement.

Blender background. Structure masters and source GLBs are never rewritten.
"""
from pathlib import Path
import argparse,hashlib,json,math,sys
import bpy
from mathutils import Vector,Matrix
from mathutils.bvhtree import BVHTree

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'development/levels/rebuild/furnishings'
CAPTURES=ROOT/'development/levels/rebuild/captures/furnishing-batch'

# Recipes are authored against the staged-source inventory in room-programs.json.
RECIPES={}

def bounds(objects):
    points=[o.matrix_world@v.co for o in objects if o.type=='MESH' for v in o.data.vertices]
    return [min(p[i] for p in points) for i in range(3)]+[max(p[i] for p in points) for i in range(3)]

def rotated_size(asset,yaw):
    size=asset['size_m'];c=abs(math.cos(yaw));s=abs(math.sin(yaw))
    return (c*size[0]+s*size[1],s*size[0]+c*size[1],size[2])

def group_parts(recipe,assets,angle):
    c=math.cos(angle);s=math.sin(angle);result=[]
    for entry in RECIPES[recipe]:
        asset,x,z,yaw=entry[:4];scale=entry[4] if len(entry)>4 else 1
        assert .8<=scale<=1.2,('Author an explicit model-budget redesign outside the bounded uniform rough fit',asset,scale)
        xx=c*x-s*z;zz=s*x+c*z;sx,sz,h=rotated_size(assets[asset],yaw+angle);sx*=scale;sz*=scale;h*=scale
        result.append(dict(asset=asset,x=xx,z=zz,yaw=yaw+angle,scale=scale,rect=[xx-sx/2,zz-sz/2,xx+sx/2,zz+sz/2],height=h))
    return result

def overlaps(a,b,margin=0):
    if isinstance(b,dict):
        poly=b['polygon'];rect=[(a[0]-margin,a[1]-margin),(a[2]+margin,a[1]-margin),(a[2]+margin,a[3]+margin),(a[0]-margin,a[3]+margin)]
        axes=[(1,0),(0,1)]+[(poly[(i+1)%4][1]-poly[i][1],poly[i][0]-poly[(i+1)%4][0]) for i in range(4)]
        for x,z in axes:
            ar=[x*p[0]+z*p[1] for p in rect];br=[x*p[0]+z*p[1] for p in poly]
            if max(ar)<=min(br)+1e-8 or max(br)<=min(ar)+1e-8:return False
        return True
    return a[0]<b[2]+margin and a[2]>b[0]-margin and a[1]<b[3]+margin and a[3]>b[1]-margin

def reservations(zone,data):
    result=[];y=zone['y']
    for link in data['links']:
        if link.get('player_access')=='SEALED_NO_ROUTE':continue
        # Routes can physically cross a room that is not one of their named
        # endpoints. Protect actual coordinates, not just graph labels.
        for p,q in zip(link['points'],link['points'][1:]):
            if min(p[1],q[1])>y+.35 or max(p[1],q[1])<y-.35:continue
            # Authored width already denotes clear pedestrian space. Reserve
            # that full lateral width; do not add another actor radius to it.
            run=math.hypot(q[0]-p[0],q[2]-p[2])
            if run<.001:continue
            dx=(q[0]-p[0])/run;dz=(q[2]-p[2])/run;r=link['width']/2
            corners=[(v[0]+s*(-dz)*r+end*dx*.3,v[2]+s*dx*r+end*dz*.3) for v,end,s in [(p,-1,-1),(q,1,-1),(q,1,1),(p,-1,1)]]
            result.append(dict(polygon=corners))
    for rite in data.get('rituals',[]):
        x,ry,z=rite['point']
        if abs(ry-y)<.3:result.append([x-2.5,z-2.5,x+2.5,z+2.5])
    return result

def candidate_on_side(request,zone,assets,occupied,support):
    # Staged glTF convention is -Z forward: face most floor fixtures away from
    # their supporting wall. Recipe yaw retains explicitly authored exceptions.
    side=request['side'];angle={'N':math.pi,'E':-math.pi/2,'S':0,'W':math.pi/2}[side]
    angle=request.get('yaw',angle)
    parts=group_parts(request['recipe'],assets,angle);x0,z0,x1,z1=zone['bounds'];inset=.16
    box=[min(p['rect'][0] for p in parts),min(p['rect'][1] for p in parts),max(p['rect'][2] for p in parts),max(p['rect'][3] for p in parts)]
    width=box[2]-box[0];depth=box[3]-box[1]
    for fraction in [request.get('fraction',.25),.15,.85,.5,.35,.65]:
        if side in ['N','S']:
            cx=x0+inset+width/2+(x1-x0-2*inset-width)*fraction-(box[0]+box[2])/2
            cz=z0+inset-box[1] if side=='N' else z1-inset-box[3]
        else:
            cx=x0+inset-box[0] if side=='W' else x1-inset-box[2]
            cz=z0+inset+depth/2+(z1-z0-2*inset-depth)*fraction-(box[1]+box[3])/2
        if request.get('position'):cx,cz=request['position']
        placed=[];valid=True
        for part in parts:
            rect=[part['rect'][0]+cx,part['rect'][1]+cz,part['rect'][2]+cx,part['rect'][3]+cz]
            if rect[0]<x0+inset-1e-5 or rect[2]>x1-inset+1e-5 or rect[1]<z0+inset-1e-5 or rect[3]>z1-inset+1e-5 or part['height']>zone['height']-.2:valid=False;break
            if any(overlaps(rect,r,.12) for r in occupied):valid=False;break
            # Four footprint corner supports, including real stair/floor voids.
            for x,z in [(rect[0]+.03,rect[1]+.03),(rect[2]-.03,rect[1]+.03),(rect[0]+.03,rect[3]-.03),(rect[2]-.03,rect[3]-.03)]:
                hit,normal,index,distance=support.ray_cast(Vector((x,-z,zone['y']+.08)),Vector((0,0,-1)),.15)
                if hit is None or normal.z<.9 or abs(hit.z-zone['y'])>.01:valid=False;break
            if not valid:break
            placed.append(dict(part,x=part['x']+cx,z=part['z']+cz,rect=rect,y=zone['y']))
        if valid:return placed
    return None

def candidate(request,zone,assets,occupied,support):
    for side in [request['side']]+request.get('fallback_sides',[]):
        result=candidate_on_side(dict(request,side=side),zone,assets,occupied,support)
        if result is not None:return result
    return None

def support_tree(objects):
    points=[];faces=[]
    for obj in objects:
        if obj.type!='MESH' or obj.get('role') not in ['floor','structural-stair-landing']:continue
        start=len(points);points.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in p.vertices) for p in obj.data.polygons)
    assert faces,'No authored floor support';return BVHTree.FromPolygons(points,faces)

def build(site,plan,assets):
    folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True)
    structure=ROOT/f'development/levels/rebuild/structures/{site.lower()}'
    report=json.loads((structure/'implementation.json').read_text(encoding='utf-8'))
    layout=ROOT/report['authoritative_input'];data=json.loads(layout.read_text(encoding='utf-8'))
    before=hashlib.sha256((structure/'rough-shell.glb').read_bytes()).hexdigest()
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(structure/'rough-shell.glb'))
    structural=list(bpy.context.scene.objects);structural_mesh_count=sum(o.type=='MESH' for o in structural);support=support_tree(structural)
    cache=bpy.data.collections.new('SOURCE LIBRARY / hidden immutable pivot copies');bpy.context.scene.collection.children.link(cache);cache.hide_render=True;cache.hide_viewport=True
    live=bpy.data.collections.new('AUTHORED FUNCTIONAL FURNISHINGS');bpy.context.scene.collection.children.link(live)
    library={};placed=[];issues=[];occupied={};zones={z['id']:z for z in data['zones']}
    for number,request in enumerate(plan):
        zone=zones[request['zone']]
        assert zone.get('gameplay_eligible',True) and zone.get('player_access') not in ['SEALED_CONTEXT','SEALED_NO_ROUTE'],('Furnishing outside accessible envelope',site,zone['id'])
        keep=occupied.setdefault(zone['id'],reservations(zone,data))
        parts=candidate(request,zone,assets,keep,support)
        if parts is None:issues.append(dict(request=request,issue='No supported position clear of authored routes, doors, rituals and prior furniture'));continue
        for part_index,part in enumerate(parts):
            info=assets[part['asset']];source=ROOT/info['path'];assert hashlib.sha256(source.read_bytes()).hexdigest()==info['sha256']
            if part['asset'] not in library:
                old=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(source));originals=set(bpy.context.scene.objects)-old
                for obj in originals:
                    for coll in list(obj.users_collection):coll.objects.unlink(obj)
                    cache.objects.link(obj)
                    obj.select_set(False)
                library[part['asset']]=originals
            originals=library[part['asset']];identity=f'{site}:{zone["id"]}:{number}:{part_index}'
            anchor=bpy.data.objects.new(identity,None);live.objects.link(anchor)
            lo=info['blender_min'];hi=info['blender_max'];centre=Vector(((lo[0]+hi[0])/2,(lo[1]+hi[1])/2,lo[2]))
            anchor.matrix_world=Matrix.Translation(Vector((part['x'],-part['z'],part['y'])))@Matrix.Rotation(-part['yaw'],4,'Z')@Matrix.Scale(part['scale'],4)@Matrix.Translation(-centre)
            copies={o:o.copy() for o in originals}
            for original,copy in copies.items():
                live.objects.link(copy);copy.parent=copies.get(original.parent,anchor);copy.matrix_local=original.matrix_local.copy()
                copy['placement_id']=identity;copy['asset']=part['asset'];copy['zone']=zone['id'];copy['role']='placed source-library furnishing'
            bpy.context.view_layer.update();actual=bounds(copies.values())
            assert abs(actual[2]-part['y'])<.001,(identity,'floor contact',actual[2],part['y'])
            expected=[part['rect'][0],-part['rect'][3],part['y'],part['rect'][2],-part['rect'][1],part['y']+part['height']]
            assert max(abs(a-b) for a,b in zip(actual,expected))<.001,(identity,'pivot placement changed bounds',actual,expected)
            placed.append(dict(id=identity,zone=zone['id'],purpose=request['purpose'],recipe=request['recipe'],asset=part['asset'],source_sha256=info['sha256'],bounds=actual,rect=part['rect'],floor_y=part['y'],yaw=part['yaw'],uniform_rough_fit_scale=part['scale'],triangles=info['triangles']))
            keep.append(part['rect'])
    scene=bpy.context.scene
    bpy.ops.wm.save_as_mainfile(filepath=str(folder/'furnished.blend'),compress=True)
    bpy.ops.object.select_all(action='DESELECT')
    for obj in structural+list(live.objects):obj.select_set(True)
    bpy.ops.export_scene.gltf(filepath=str(folder/'furnished.glb'),export_format='GLB',export_extras=True,use_selection=True)
    manifest=dict(site=site,status='SOURCE_LIBRARY_NATIVE_CANDIDATE_NOT_RUNTIME_ACCEPTED' if not issues else 'REVISE_UNPLACED_REQUIREMENTS',
        source_layout=layout.relative_to(ROOT).as_posix(),source_layout_sha256=hashlib.sha256(layout.read_bytes()).hexdigest(),structure_glb_sha256=before,
        furnished_glb_sha256=hashlib.sha256((folder/'furnished.glb').read_bytes()).hexdigest(),native_sha256=hashlib.sha256((folder/'furnished.blend').read_bytes()).hexdigest(),requests=len(plan),placements=placed,issues=issues)
    program=dict(requests=plan,recipes={r['recipe']:RECIPES[r['recipe']] for r in plan})
    manifest['authored_program_sha256']=hashlib.sha256(json.dumps(program,sort_keys=True,separators=(',',':')).encode()).hexdigest()
    manifest['staged_asset_bounds_sha256']=hashlib.sha256((OUT/'staged-asset-bounds.json').read_bytes()).hexdigest()
    # Independently reimport delivered meshes and verify every transformed instance.
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'furnished.glb'))
    imported=list(bpy.context.scene.objects);groups={}
    for obj in imported:
        if obj.type=='MESH' and obj.get('placement_id'):groups.setdefault(obj['placement_id'],[]).append(obj)
    for row in placed:
        assert row['id'] in groups,(site,'Lost export instance',row['id'])
        assert max(abs(a-b) for a,b in zip(bounds(groups[row['id']]),row['bounds']))<.001
        assert all(o.data.uv_layers for o in groups[row['id']]),(row['id'],'Lost source UV')
    assert len([o for o in imported if o.type=='MESH' and not o.get('placement_id')])==structural_mesh_count,'Hidden source-cache meshes leaked into export'
    assert hashlib.sha256((structure/'rough-shell.glb').read_bytes()).hexdigest()==before
    manifest['independently_reimported_instances']=len(groups)
    # Actual export cutaway, coloured by its preserved source materials/textures.
    for obj in imported:
        if obj.get('role')=='ceiling-cap':obj.hide_render=True
    x0,z0,x1,z1=data['site_bounds'];span=max(x1-x0,z1-z0);centre=Vector(((x0+x1)/2,-(z0+z1)/2,2))
    bpy.ops.object.camera_add(location=centre+Vector((span*.65,-span*.8,span*1.2)));cam=bpy.context.object;cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5
    scene=bpy.context.scene;scene.camera=cam;scene.world=bpy.data.worlds.new('Furnishing review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.7
    bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.4,-.4,-.5);bpy.context.object.data.energy=2
    scene.render.engine='CYCLES';scene.cycles.samples=4;scene.render.resolution_x=1200;scene.render.resolution_y=1000;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
    CAPTURES.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(CAPTURES/(site.lower()+'-export-overview.png'));bpy.ops.render.render(write_still=True)
    if placed:
        target=placed[0]['zone'];zone=zones[target];a,b,c,d=zone['bounds'];span=max(c-a,d-b);centre=Vector(((a+c)/2,-(b+d)/2,zone['y']+.7))
        for obj in imported:
            if obj.type!='MESH':continue
            obj.hide_render=obj.get('zone')!=target or obj.get('role')=='ceiling-cap' or obj.name.startswith(('Wall '+target+' S','Wall '+target+' E'))
        cam.location=centre+Vector((span*.65,-span*.85,span*1.1));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=span*1.6
        scene.render.filepath=str(CAPTURES/(site.lower()+'-room-'+target+'.png'));bpy.ops.render.render(write_still=True)
    manifest['source_builder_sha256']=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    (folder/'placements.json').write_bytes((json.dumps(manifest,indent=2)+'\n').encode())
    print('FURNISHED_CANDIDATE',site,len(placed),'source instances;',len(issues),'unplaced requirements',flush=True)
    return manifest

def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:])
    assets={r['asset']:r for r in json.loads((OUT/'staged-asset-bounds.json').read_text())['assets']}
    program=json.loads((OUT/'room-programs.json').read_text());RECIPES.update(program['recipes']);plan=program['sites'];results=[]
    for site in args.sites:results.append(build(site,plan[site],assets))
    if any(r['issues'] for r in results):raise SystemExit('Furnishing positions require source-specific repair; all evidence retained')

if __name__=='__main__':main()
