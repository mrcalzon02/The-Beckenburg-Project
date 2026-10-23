"""Remaining-proposal rough shell blocking from the existing measured layout authority.
Blender --background --factory-startup --python this_file
No Java, engine, playable-map registry or source layouts are changed.
"""
import bpy,math,json,hashlib,sys
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parent
LAYOUTS=ROOT.parent/'layouts'
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
def slab(zone,holes,c,mat):
    a,b,d,e=zone['bounds'];xs={a,d};zs={b,e}
    for x0,z0,x1,z1 in holes:
        xs.update([max(a,min(d,x0)),max(a,min(d,x1))]);zs.update([max(b,min(e,z0)),max(b,min(e,z1))])
    xs=sorted(xs);zs=sorted(zs)
    for x0,x1 in zip(xs,xs[1:]):
        for z0,z1 in zip(zs,zs[1:]):
            if any(h[0]<(x0+x1)/2<h[2] and h[1]<(z0+z1)/2<h[3] for h in holes):continue
            box('Floor '+zone['id'],(x0+x1)/2,zone['y']-.16,(z0+z1)/2,x1-x0,.16,z1-z0,mat,c,zone=zone['id'],role='floor')
def route_mesh(link,c,mat):
    for i,(p,q) in enumerate(zip(link['points'],link['points'][1:])):
        dx=q[0]-p[0];dz=q[2]-p[2];run=math.hypot(dx,dz);width=link['width']
        if run<.01:
            if abs(q[1]-p[1])>.01:box(link['id']+' vertical core',p[0],min(p[1],q[1]),p[2],width,abs(q[1]-p[1]),width,mat,c,role='UNRESOLVED_VERTICAL_CORE')
            continue
        nx=-dz/run*width/2;nz=dx/run*width/2
        vv=[(x,-z,y+depth) for depth in [-.12,0] for x,y,z in [(p[0]+nx,p[1],p[2]+nz),(p[0]-nx,p[1],p[2]-nz),(q[0]-nx,q[1],q[2]-nz),(q[0]+nx,q[1],q[2]+nz)]]
        mesh=bpy.data.meshes.new(link['id']);mesh.from_pydata(vv,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);mesh.update()
        o=bpy.data.objects.new(link['id']+' support '+str(i),mesh);c.objects.link(o);mesh.materials.append(mat)
        o['role']='rough route support; stair/ramp engineering unverified';o['clear_width']=width
def build(path):
    data=json.loads(path.read_text());folder=ROOT/path.parent.name;folder.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
    scene=bpy.context.scene;scene.unit_settings.system='METRIC';scene.unit_settings.scale_length=1
    wall=material('Neutral shell',(.52,.58,.61));floor=material('Interior floors',(.30,.38,.42));outside=material('Exterior terrain reservation',(.30,.40,.28));route=material('Cyan supported route reservation',(.12,.46,.55));vertical=material('Amber stair and grade reservation',(.70,.37,.10));ritual=material('Clear ritual reservation',(.52,.24,.39));van=material('Shared van envelope',(.48,.60,.57))
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
        slab(zone,holes,c,outside if zone['kind'] in ['outside','staging'] else floor)
        a,b,d,e=zone['bounds'];h=zone['height']
        if zone['kind'] not in ['outside','staging']:
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
    routes=collection('ROUTE SUPPORTS / cyan level; amber elevation reservations')
    for link in data['links']:
        route_mesh(link,routes,route if link['kind']=='level' else vertical)
        if link['kind']!='level':
            run=sum(math.hypot(q[0]-p[0],q[2]-p[2]) for p,q in zip(link['points'],link['points'][1:]));rise=max(p[1] for p in link['points'])-min(p[1] for p in link['points'])
            notes.append(dict(link=link['id'],issue='Elevation support is a rough mass, not an accepted stair or path',rise=rise,plan_run=run,requires_switchbacks=run<rise*1.6,source_note=link.get('construction',link.get('note',''))))
    marks=collection('RITUAL RESERVATIONS / keep clear; no gameplay grants')
    for r in data['rituals']:
        x,y,z=r['point'];box(r['rite'],x,y+.012,z,4.4,.025,4.4,ritual,marks,role='ritual clear-area reservation')
    staging=data['staging'];x,y,z=staging['point'];v=box('VAN ENVELOPE / use canonical module during integration',x,y+.5,z,1.784,2.125,4.256,van,marks,role='placeholder envelope; not replacement van');v.rotation_euler.z=-staging['yaw']
    scene['layout_id']=data['id'];scene['status']='ROUGH SHELL BLOCKING ONLY / no gameplay or collision acceptance';scene['source_sha256']=hashlib.sha256(path.read_bytes()).hexdigest();scene['units']='metres; source X/Z plan and Y elevation converted to Blender Z up'
    scene['next_step']='Review openings and connectors against concepts, resolve stairs and terrain, then native collision/navigation integration.'
    # Native scenes retain all measured elevations. Floors and named shells are separately editable.
    blend=folder/'rough-shell.blend';bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.export_scene.gltf(filepath=str(folder/'rough-shell.glb'),export_format='GLB',export_extras=True)
    a,b,d,e=data['site_bounds'];height=max(z['y']+z['height'] for z in data['zones']);center=Vector(((a+d)/2,-(b+e)/2,height*.35));span=max(d-a,e-b)
    bpy.ops.object.camera_add(location=center+Vector((span*.85,-span*.95,span*1.25)));cam=bpy.context.object;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.5;scene.camera=cam
    scene.world=bpy.data.worlds.new('Blockout neutral review');scene.world.use_nodes=True;scene.world.node_tree.nodes['Background'].inputs[1].default_value=.7
    bpy.ops.object.light_add(type='SUN');bpy.context.object.rotation_euler=(.45,-.35,-.6);bpy.context.object.data.energy=2
    scene.render.engine='CYCLES';scene.cycles.samples=4;scene.cycles.use_denoising=True;scene.render.resolution_x=960;scene.render.resolution_y=720;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard'
    scene.render.image_settings.file_format='PNG';scene.render.filepath=str(folder/'preview.png');bpy.ops.render.render(write_still=True)
    mesh_count=sum(o.type=='MESH' for o in scene.objects)
    bpy.ops.wm.open_mainfile(filepath=str(blend));assert bpy.context.scene['source_sha256']==hashlib.sha256(path.read_bytes()).hexdigest();assert sum(o.type=='MESH' for o in bpy.context.scene.objects)==mesh_count
    report=dict(id=data['id'],title=data['title'],source=str(path.relative_to(ROOT.parent)),source_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),zones=len(data['zones']),routes=len(data['links']),shell_openings=openings,meshes=mesh_count,issues=notes,source_refinement=data.get('refine',[]),native_reopened=True,blend_sha256=hashlib.sha256(blend.read_bytes()).hexdigest(),status='rough-shell-blocked; runtime integration pending')
    (folder/'implementation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    (folder/'NEXT.md').write_text('# '+data['title']+' — next implementation steps\n\nRough shell blocking only. Source layout coordinates and elevations are retained.\n\n1. Review actual apertures, floor openings and connector support against the approved concept.\n2. Engineer every amber elevation link: real flights or paths, landings, headroom, guards and terrain.\n3. Resolve overlapping shell partitions and route crossings before building navigation collision.\n4. Integrate the canonical van, authored room volumes, circuits, acoustics and protocol IDs.\n5. Furnish, dress and test human/AI hunts and rituals; retain the map lock until accepted.\n\nSource-specific concerns:\n\n'+'\n'.join('- '+str(n) for n in data.get('refine',[]))+'\n\nSee implementation.json for every connector and floor-opening reservation.\n')
    print('BLOCKED',data['id'],len(data['zones']),'zones',len(openings),'openings',flush=True)
    return report
EXISTING={'HOUSE','FARMHOUSE','SCHOOL','CHURCH','WLD-01','CASTLE'}
reports=[build(p) for p in sorted(LAYOUTS.glob('*/layout.json')) if json.loads(p.read_text())['id'] not in EXISTING]
(ROOT/'manifest.json').write_text(json.dumps(dict(status='rough-shell-blocking',maps=reports,limitations=['Planning coordinates retained; these are not playable or collision-accepted levels.','Openings, support strips and deck voids are rough reservations.','Overlapping partitions, vertical links, headroom and connector crossings require per-map engineering.','Existing runtime maps and concurrent engine optimization are untouched.']),indent=2)+'\n',encoding='utf-8')
assert len(reports)==17
print('ALL_ROSTER_BLOCKOUTS_COMPLETE',len(reports),flush=True)
