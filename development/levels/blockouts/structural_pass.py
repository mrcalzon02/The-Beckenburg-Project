"""Shared measured structural blocking; no terrain, finished roof or gameplay grant.

All coordinates retain source X/Z and Y elevation. Historical scenes are kept.
"""
import math,json,hashlib
import bpy,bmesh
from mathutils import Vector
from collections import Counter
from mathutils.bvhtree import BVHTree


def saved_editor_terrain(data,collection,material):
    """Import the exact mesh published from persistent editor punched leaves."""
    from pathlib import Path
    root=Path(__file__).resolve().parents[3]
    contract=data['saved_editor_terrain']
    for name,digest in contract['files_sha256'].items():
        assert hashlib.sha256((root/name).read_bytes()).hexdigest()==digest,('Stale saved editor terrain',name)
    points=[];faces=[]
    for row in (root/contract['mesh']).read_text(encoding='utf-8').splitlines():
        values=row.split()
        if not values:continue
        if values[0]=='v':
            x,y,z=map(float,values[1:4]);points.append((x,-z,y))
        elif values[0]=='f':faces.append(tuple(int(v.split('/')[0])-1 for v in values[1:]))
    assert points and faces and all(len(face)==3 for face in faces),'Editor terrain must publish actual triangles'
    mesh=bpy.data.meshes.new('Saved editor terrain');mesh.from_pydata(points,[],faces);mesh.update()
    obj=bpy.data.objects.new('Saved editor terrain '+data['id'],mesh);collection.objects.link(obj);mesh.materials.append(material)
    obj['role']='saved editor terrain / actual punched leaves';obj['source_obj_sha256']=contract['files_sha256'][contract['mesh']]
    obj['terrain_contract']=json.dumps(contract);return obj

def shaped_deck(zone,holes,collection,material,role):
    a,b,c,d=zone['bounds']
    if any(h[0]<=a+.001 and h[1]<=b+.001 and h[2]>=c-.001 and h[3]>=d-.001 for h in holes):return None
    # A top roof is a closed disk; the intermediate maintenance/stair core is
    # not an instruction to punch the same hole through the weather enclosure.
    inner=zone.get('inner_radius',0) or (1.0 if role=='floor' and zone['y']>0 else 0)
    if inner:
        from proposal_landforms import shaped_slab
        obj=shaped_slab(zone,collection,material)
    else:
        bpy.ops.mesh.primitive_cylinder_add(vertices=64,radius=zone['radius'],depth=.16,location=((a+c)/2,-(b+d)/2,zone['y']-.08))
        obj=bpy.context.object
        for current in list(obj.users_collection):current.objects.unlink(obj)
        collection.objects.link(obj);obj.data.materials.append(material)
    obj.name=('Floor ' if role=='floor' else 'Ceiling cap ')+zone['id'];obj['role']=role;obj['zone']=zone['id']
    for index,(x0,z0,x1,z1) in enumerate(holes):
        if x1<=a or x0>=c or z1<=b or z0>=d:continue
        bpy.ops.mesh.primitive_cube_add(size=1,location=((x0+x1)/2,-(z0+z1)/2,zone['y']-.08))
        cutter=bpy.context.object;cutter.dimensions=(x1-x0,z1-z0,.6)
        bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
        bpy.context.view_layer.objects.active=obj
        modifier=obj.modifiers.new('Actual deck opening '+str(index),'BOOLEAN');modifier.operation='DIFFERENCE';modifier.object=cutter
        bpy.ops.object.modifier_apply(modifier=modifier.name);bpy.data.objects.remove(cutter,do_unlink=True)
    # Overlapping projected spiral apertures can leave Boolean sliver edges
    # below export precision. Weld only sub-0.1mm construction noise, then
    # remove collapsed faces; the independent closed-solid check still gates.
    bm=bmesh.new();bm.from_mesh(obj.data)
    bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.0001)
    bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=.0001)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces))
    bm.to_mesh(obj.data);bm.free();obj.data.update()
    return obj

def deck_holes(elevation,data):
    holes=[]
    for link in data['links']:
        for p,q in zip(link['points'],link['points'][1:]):
            if link['kind']!='stairs' and abs(p[1]-q[1])<.01:continue
            # A person's head reaches an intermediate ceiling before their feet
            # reach the next floor. Reserve the complete authored stair shaft.
            if elevation<=min(p[1],q[1])+.001 or elevation>max(p[1],q[1])+2.2:continue
            half=link['width']/2+.15
            run=math.hypot(q[0]-p[0],q[2]-p[2])
            if run<.01:continue
            nx=-(q[2]-p[2])/run*half;nz=(q[0]-p[0])/run*half
            corners=[(point[0]+sign*nx,point[2]+sign*nz) for point in [p,q] for sign in [-1,1]]
            # Lateral clearance only: extending past a flight end would cut
            # through the landing that must meet the final tread edge.
            holes.append((min(v[0] for v in corners),min(v[1] for v in corners),max(v[0] for v in corners),max(v[1] for v in corners)))
    return holes

def stair_flights(link,collection,material,box,notes):
    flights=[]
    for p,q in zip(link['points'],link['points'][1:]):
        rise=abs(q[1]-p[1]);run=math.hypot(q[0]-p[0],q[2]-p[2])
        if rise<.01:continue
        count=max(1,math.ceil(rise/.18));tread=run/count
        if tread<.24:
            notes.append(dict(link=link['id'],issue='Stair needs an authored longer flight/switchback; no fake vertical steps',rise=rise,run=run,minimum_run=count*.24))
            return False
        flights.append((p,q,rise,run,count,tread))
    if not flights:return False
    for index,(p,q) in enumerate(zip(link['points'],link['points'][1:])):
        if link.get('landings_authored_in_zones'):continue
        run=math.hypot(q[0]-p[0],q[2]-p[2])
        if abs(q[1]-p[1])>=.01 or run<.01:continue
        landing=box(f"{link['id']} landing{index}",(p[0]+q[0])/2,p[1]-.16,(p[2]+q[2])/2,link['width'],.16,run,material,collection,
            role='structural-stair-landing',link=link['id'],clear_width=link['width'])
        landing.rotation_euler.z=math.atan2(q[0]-p[0],q[2]-p[2])
    for flight,(p,q,rise,run,count,tread) in enumerate(flights):
        low,high=(p,q) if p[1]<q[1] else (q,p)
        dx=(high[0]-low[0])/run;dz=(high[2]-low[2])/run
        for index in range(count):
            distance=(index+.5)*tread;y=low[1]+rise*(index+1)/count
            step=box(f"{link['id']} flight{flight} step{index+1}",low[0]+dx*distance,low[1]-.16,low[2]+dz*distance,link['width'],y-low[1]+.16,tread,material,collection,
                role='structural-stair-step',link=link['id'],riser=rise/count,tread=tread,clear_width=link['width'])
            step.rotation_euler.z=math.atan2(dx,dz)
    notes.append(dict(link=link['id'],issue='Measured treads built; capsule/headroom/guards/end landing and runtime integration still require verification',flights=len(flights)))
    return True

def deduplicate_solids(scene):
    seen=set();removed=0
    for obj in list(scene.objects):
        if obj.type!='MESH' or not(obj.name.startswith(('Wall ','Header '))):continue
        key=tuple(round(v,6) for v in (*obj.location,*obj.dimensions,*obj.rotation_euler))
        if key in seen:bpy.data.objects.remove(obj,do_unlink=True);removed+=1
        else:seen.add(key)
    return removed

def context_infill(data,sealed_links,collection,material,box):
    byid={zone['id']:zone for zone in data['zones']}
    for link in sealed_links:
        a,b=(byid[link[key]] for key in ['a','b'])
        if a['kind']!='outside' or b['kind']!='outside':continue
        ax0,az0,ax1,az1=a['bounds'];bx0,bz0,bx1,bz1=b['bounds'];y=max(a['y'],b['y'])
        for along,plane,low,high in [
            (True,az1,max(ax0,bx0),min(ax1,bx1)) if abs(az1-bz0)<.01 else (True,az0,max(ax0,bx0),min(ax1,bx1)) if abs(az0-bz1)<.01 else (True,0,0,0),
            (False,ax1,max(az0,bz0),min(az1,bz1)) if abs(ax1-bx0)<.01 else (False,ax0,max(az0,bz0),min(az1,bz1)) if abs(ax0-bx1)<.01 else (False,0,0,0)]:
            if high-low<.01:continue
            box('Permanent context closure '+link['id'],(low+high)/2 if along else plane,y,plane if along else (low+high)/2,
                high-low if along else .16,2.6,.16 if along else high-low,material,collection,
                role='fixed-context-infill',link=link['id'],purpose='Full shared exterior-sector mouth; terrain/enclosure return integration still required')

def inspect_export(folder,data,sealed_links=()):
    """Independent reimport of the delivered GLB; keep native .blend untouched."""
    raw=(folder/'rough-shell.glb').read_bytes()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(folder/'rough-shell.glb'))
    objects=[obj for obj in bpy.context.scene.objects if obj.type=='MESH']
    triangles=0;caps=0;floors=0;stairs=0;invalid=[];closed=0;depths=[];rays=0;route_solids=0
    for obj in objects:
        mesh=obj.data;mesh.calc_loop_triangles();triangles+=len(mesh.loop_triangles)
        if any(not math.isfinite(v) for vertex in mesh.vertices for v in vertex.co):invalid.append(obj.name)
        caps+=obj.get('role')=='ceiling-cap';floors+=obj.get('role')=='floor';stairs+=obj.get('role')=='structural-stair-step'
        role=obj.get('role','')
        if role in ['floor','ceiling-cap','structural-stair-step','structural-stair-landing','rough shell wall','fixed-context-infill','rough route support; stair/ramp engineering unverified'] or obj.name.startswith('Header '):
            edges=Counter()
            for tri in mesh.loop_triangles:
                points=[tuple(round(v,5) for v in mesh.vertices[index].co) for index in tri.vertices]
                for a,b in zip(points,points[1:]+points[:1]):edges[tuple(sorted((a,b)))]+=1
            assert all(count==2 for count in edges.values()),(data['id'],obj.name,'structural solid not closed')
            closed+=1
        if role=='rough route support; stair/ramp engineering unverified':
            bm=bmesh.new();bm.from_mesh(mesh);volume=bm.calc_volume(signed=True);bm.free()
            assert volume>0,(data['id'],obj.name,'exported route support winding points inward')
            route_solids+=1
        if role in ['floor','ceiling-cap']:
            points=[obj.matrix_world@v.co for v in mesh.vertices]
            depth=max(p.z for p in points)-min(p.z for p in points);assert abs(depth-.16)<.0001,(obj.name,depth)
            depths.append(depth)
        if role=='ceiling-cap':
            # Independent exported mesh ray, from above through its centre.
            # Whole-map sky blocking still requires the actual engine audit.
            centre=sum((v.co for v in mesh.vertices),Vector())/len(mesh.vertices)
            top=max(mesh.loop_triangles,key=lambda tri:sum((obj.matrix_world@mesh.vertices[i].co).z for i in tri.vertices))
            world=sum((obj.matrix_world@mesh.vertices[i].co for i in top.vertices),Vector())/3;inverse=obj.matrix_world.inverted()
            hit,point,normal,index=obj.ray_cast(inverse@(world+Vector((0,0,10))),inverse.to_3x3()@Vector((0,0,-1)))
            assert hit,(obj.name,'exported ceiling cannot block a vertical ray');rays+=1
    assert not invalid,invalid
    assert floors>0 and caps>0,(data['id'],'missing depth/ceiling geometry')
    assert rays>0,(data['id'],'no exported ceiling rays actually executed')
    vertices=[];faces=[]
    for obj in objects:
        if obj.get('role') not in ['floor','ceiling-cap','rough shell wall','rough opening header','fixed-context-infill']:continue
        start=len(vertices);vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        faces.extend(tuple(start+i for i in poly.vertices) for poly in obj.data.polygons)
    barrier_tree=BVHTree.FromPolygons(vertices,faces)
    verified_seals=[]
    from build_blockouts import wall_crossings
    for link in sealed_links:
        candidates=[]
        for zone in data['zones']:
            if zone['id'] not in [link['a'],link['b']]:continue
            a,b,c,d=zone['bounds']
            for side,cuts in wall_crossings(zone,dict(links=[link])).items():
                for start,width,_ in cuts:
                    x=start+width/2 if side in ['N','S'] else a if side=='W' else c
                    z=b if side=='N' else d if side=='S' else start+width/2
                    normal=Vector((0,1,0)) if side in ['N','S'] else Vector((1,0,0))
                    candidates.append((Vector((x,-z,zone['y']+1.2)),normal))
        if not candidates and link['kind']!='level':
            p=max(link['points'],key=lambda p:p[1]);candidates.append((Vector((p[0],-p[2],p[1])),Vector((0,0,1))))
        assert candidates,(data['id'],link['id'],'sealed route has no authored physical boundary to verify')
        assert all(barrier_tree.ray_cast(point+normal*.35,-normal,.7)[0] is not None for point,normal in candidates),(data['id'],link['id'],'context boundary remains open')
        verified_seals.append(link['id'])
    scene=bpy.context.scene
    zones=[z for z in data['zones'] if z['kind'] not in ['outside','staging']]
    x0=min(z['bounds'][0] for z in zones);x1=max(z['bounds'][2] for z in zones)
    z0=min(z['bounds'][1] for z in zones);z1=max(z['bounds'][3] for z in zones)
    top=max(z['y']+z['height'] for z in zones);span=max(x1-x0,z1-z0)
    centre=Vector(((x0+x1)/2,-(z0+z1)/2,top*.35))
    bpy.ops.object.camera_add(location=centre+Vector((span*.85,-span*.95,span*1.25)))
    camera=bpy.context.object;camera.rotation_euler=(centre-camera.location).to_track_quat('-Z','Y').to_euler()
    camera.data.type='ORTHO';camera.data.ortho_scale=span*1.7;scene.camera=camera
    scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.light='STUDIO';scene.display.shading.color_type='MATERIAL'
    scene.render.resolution_x=960;scene.render.resolution_y=720;scene.render.resolution_percentage=100
    for cutaway in [False,True]:
        for obj in objects:obj.hide_render=cutaway and obj.get('role')=='ceiling-cap'
        scene.render.filepath=str(folder/('structure-export-cutaway.png' if cutaway else 'structure-export-closed.png'))
        bpy.ops.render.render(write_still=True)
    return dict(stage='STRUCTURE_BLOCKING_NOT_RUNTIME_ACCEPTED',glb_sha256=hashlib.sha256(raw).hexdigest(),
        independently_reimported=True,meshes=len(objects),triangles=triangles,floor_parts=floors,ceiling_cap_parts=caps,stair_treads=stairs,
        checked_closed_solids=closed,measured_depth_parts=len(depths),exported_cap_rays=rays,route_support_solids=route_solids,
        fixed_infill_routes=len(sealed_links),exported_fixed_boundary_checks=verified_seals,
        wall_depth_m=.16,floor_depth_m=.16,terrain='Exterior zone pads omitted; real saved terrain/hole work remains',
        roof='Closed ceiling/deck caps only; authored exterior roof form/weathering remains',
        remaining=['Capsule/optical/body/object and roof/skylight rays in actual engine','All remaining unresolved elevation routes and landings/guards','Original source asset fit and supported furnishings','Source-matched terrain holes, services, van and LAN integration'])
