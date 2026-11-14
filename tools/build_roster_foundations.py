"""Terrain-bearing construction across all25remaining native candidates.

Actual source terrain rays determine embedded bottoms. Existing source meshes,
models and collision parts are preserved. No phantom ground or body bypass.
Blender background only; construction candidates, not gameplay acceptance.
"""
from pathlib import Path
import argparse,collections,hashlib,io,json,math,struct,sys
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'terrain-fit-pass';CAP=BASE/'captures/terrain-fit-batch'

def material(name,color):
    m=bpy.data.materials.new(name);m.use_nodes=True;m.diffuse_color=(*color,1);m.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=(*color,1);m.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.95;return m

def prism(name,polygon,top,bottoms,mat):
    vertices=[(x,-z,y) for heights in [bottoms,[top]*len(polygon)] for (x,z),y in zip(polygon,heights)];n=len(polygon)
    faces=[tuple(range(n)),tuple(range(n,2*n))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(vertices,[],faces);mesh.update();bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert bm.calc_volume(signed=True)>0;bm.to_mesh(mesh);bm.free()
    obj=bpy.data.objects.new(name,mesh);bpy.context.scene.collection.objects.link(obj);mesh.materials.append(mat);obj['role']='terrain-bearing foundation construction'
    uv=mesh.uv_layers.new(name='Metre structural UV')
    for poly in mesh.polygons:
        for li in poly.loop_indices:
            v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x+v.y,v.z)
    return obj

def overlap(a,b):return a[0]<b[2]-1e-6 and a[2]>b[0]+1e-6 and a[1]<b[3]-1e-6 and a[3]>b[1]+1e-6

def route_void(rect,bottom,top,data):
    for link in data['links']:
        if link.get('player_access')=='SEALED_NO_ROUTE':continue
        for p,q in zip(link['points'],link['points'][1:]):
            if min(p[1],q[1])>=top+.01 or max(p[1],q[1])+1.8<=bottom:continue
            r=link['width']/2;box=[min(p[0],q[0])-r,min(p[2],q[2])-r,max(p[0],q[0])+r,max(p[2],q[2])+r]
            if overlap(rect,box):return link['id']
    return None

def construct(data,terrain,policy,include_route_piles=True):
    points=[];faces=[]
    for obj in terrain:
        offset=len(points);points.extend(obj.matrix_world@v.co for v in obj.data.vertices);faces.extend(tuple(offset+i for i in p.vertices) for p in obj.data.polygons)
    tree=BVHTree.FromPolygons(points,faces)
    def ground(x,z):
        hit=tree.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)))[0];return None if hit is None else hit.z
    points=[];faces=[];owners=[]
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH' or obj in terrain or 'water' in obj.name.lower():continue
        offset=len(points);points.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        for poly in obj.data.polygons:faces.append(tuple(offset+i for i in poly.vertices));owners.append(obj.name)
    loads=BVHTree.FromPolygons(points,faces)
    def bearing(x,z,elevation):
        hit,normal,index,distance=loads.ray_cast(Vector((x,-z,elevation-.35)),Vector((0,0,1)),.30)
        return None if hit is None or normal.z>-.5 else (hit.z,owners[index])
    masonry=material('Terrain-bearing masonry / rough construction',(.34,.32,.28));wood=material('Timber pile and bearing beam / rough construction',(.27,.20,.13));built=[];findings=[];proof=[]
    zones=[]
    for zone in data['zones']:
        if zone['y']<0 or zone.get('shape'):continue
        if zone['kind'] in ['outside','staging'] and zone['id'] not in policy['supported_decks']:continue
        if zone['id'] in policy['exclude_zones']:continue
        a=zone['bounds'];area=(a[2]-a[0])*(a[3]-a[1]);lower=False
        for other in data['zones']:
            if other['y']<0 or other['y']>=zone['y']-.1:continue
            b=other['bounds'];common=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]))
            if common>area*.25:lower=True;break
        if not lower:zones.append(zone)
    pile_zones=[z for z in zones if z['id'] in policy['pile_zones'] or policy.get('all_piles')]
    skirt_zones=[z for z in zones if z not in pile_zones]
    # Union exact authored rectangles per elevation, exposing only perimeter
    # edges. Internal room boundaries never become foundation walls.
    for elevation in sorted({z['y'] for z in skirt_zones}):
        selected=[z for z in skirt_zones if z['y']==elevation];xs=sorted({x for z in selected for x in z['bounds'][::2]});zs=sorted({x for z in selected for x in z['bounds'][1::2]})
        filled=set()
        for iz,(z0,z1) in enumerate(zip(zs,zs[1:])):
            for ix,(x0,x1) in enumerate(zip(xs,xs[1:])):
                if any(z['bounds'][0]<(x0+x1)/2<z['bounds'][2] and z['bounds'][1]<(z0+z1)/2<z['bounds'][3] for z in selected):filled.add((ix,iz))
        edges=[]
        for ix,iz in sorted(filled):
            x0,x1=xs[ix:ix+2];z0,z1=zs[iz:iz+2]
            for neighbor,p,q,normal in [((ix,iz-1),(x0,z0),(x1,z0),(0,-1)),((ix,iz+1),(x0,z1),(x1,z1),(0,1)),((ix-1,iz),(x0,z0),(x0,z1),(-1,0)),((ix+1,iz),(x1,z0),(x1,z1),(1,0))]:
                if neighbor not in filled:edges.append((p,q,normal))
        for p,q,(nx,nz) in edges:
            n=max(1,math.ceil(math.dist(p,q)/2))
            for i in range(n):
                ends=[(p[0]+(q[0]-p[0])*t/n,p[1]+(q[1]-p[1])*t/n) for t in [i,i+1]]
                heights=[ground(x+nx*.35,z+nz*.35) for x,z in ends];top=elevation-.16
                bearing_point=[(ends[0][0]+ends[1][0])/2-nx*.04,(ends[0][1]+ends[1][1])/2-nz*.04]
                receiver=bearing(*bearing_point,elevation)
                reason='NO_NATIVE_GROUND' if any(h is None for h in heights) else 'EARTH_OVERTOPS_WALKING_FLOOR' if max(heights)>elevation+.01 else 'REQUIRES_DEEP_RETAINING_OR_ROCK_VOLUME' if top-min(heights)>3 else None
                if receiver is None:reason='NO_ACTUAL_DECK_UNDERSIDE'
                elif not receiver[1].startswith('Floor '):reason='NO_STRUCTURAL_FLOOR_BEARING'
                else:top=receiver[0]
                if reason is None and min(heights)>=top-.01:continue
                polygon=[(x+nx*side,z+nz*side) for (x,z),side in [(ends[0],-.1),(ends[1],-.1),(ends[1],.35),(ends[0],.35)]]
                rect=[min(v[0] for v in polygon),min(v[1] for v in polygon),max(v[0] for v in polygon),max(v[1] for v in polygon)]
                low=min(h for h in heights if h is not None)-.35 if any(h is not None for h in heights) else top
                passage=route_void(rect,low,top,data)
                if passage:reason='RESERVE_LOW_ROUTE_'+passage
                if reason:findings.append(dict(type=reason,edge=ends,elevation=elevation));continue
                bottom=[heights[0]-.35,heights[1]-.35,heights[1]-.35,heights[0]-.35];name=f'Bearing skirt {len(built):04d}'
                built.append(prism(name,polygon,top,bottom,masonry));proof.append(dict(name=name,kind='skirt',top_y=top,terrain_probe_points=[[x+nx*.35,z+nz*.35] for x,z in ends],ground_y=heights,bottom_y=bottom,footprint=polygon,supports=receiver[1],bearing_point=bearing_point))
    # Coastal/marsh decks use piles, never solid earth-filled room pads.
    used=set()
    pile_requests=[]
    for zone in pile_zones:
        x0,z0,x1,z1=zone['bounds'];top=zone['y']-.16
        for x,z in [(x0+.4,z0+.4),(x1-.4,z0+.4),(x1-.4,z1-.4),(x0+.4,z1-.4)]:
            pile_requests.append((zone,x,z))
    pile_ids={z['id'] for z in pile_zones}
    for link in data['links']:
        if not include_route_piles or link.get('player_access')=='SEALED_NO_ROUTE' or not pile_ids.intersection([link['a'],link['b']]):continue
        for p,q in zip(link['points'],link['points'][1:]):
            if abs(p[1]-q[1])>.01 or p[1]<0:continue
            n=max(1,math.ceil(math.dist(p,q)/4))
            for i in range(n+1):
                x=p[0]+(q[0]-p[0])*i/n;z=p[2]+(q[2]-p[2])*i/n
                pile_requests.append((dict(id='route-'+link['id'],bounds=[x-.5,z-.5,x+.5,z+.5],y=p[1]),x,z))
    for zone,x,z in pile_requests:
            x0,z0,x1,z1=zone['bounds'];receiver=bearing(x,z,zone['y'])
            if receiver is None:findings.append(dict(type='NO_ACTUAL_DECK_UNDERSIDE',zone=zone['id'],point=[x,z]));continue
            top=receiver[0]
            chosen=None
            for px,pz in [(x,z),(x0-.35 if x<(x0+x1)/2 else x1+.35,z0-.35 if z<(z0+z1)/2 else z1+.35)]:
                h=ground(px,pz)
                if h is not None and h<top-.15 and top-h<9:chosen=(px,pz,h);break
            if chosen is None:findings.append(dict(type='PILE_NATIVE_GROUND_MISSING_OR_OVERTOPPED',zone=zone['id'],point=[x,z]));continue
            px,pz,h=chosen;stamp=(round(px,3),round(pz,3),top)
            if stamp in used:continue
            used.add(stamp);half=.14;poly=[(px-half,pz-half),(px+half,pz-half),(px+half,pz+half),(px-half,pz+half)]
            corner_heights=[ground(x,z) for x,z in poly]
            if any(v is None for v in corner_heights) or max(corner_heights)>=top-.1:
                findings.append(dict(type='PILE_FOOTPRINT_NOT_FULLY_SUPPORTED',zone=zone['id'],point=[px,pz]));continue
            bottom=min(corner_heights)-.4
            rect=[px-half,pz-half,px+half,pz+half];passage=route_void(rect,h-.4,top,data)
            if passage:findings.append(dict(type='RESERVE_LOW_ROUTE_'+passage,zone=zone['id'],point=[px,pz]));continue
            name=f'Bearing pile {len(built):04d}';built.append(prism(name,poly,top,[bottom]*4,wood));proof.append(dict(name=name,kind='pile',zone=zone['id'],top_y=top,terrain_probe_points=poly,ground_y=corner_heights,bottom_y=[bottom]*4,footprint=poly,supports=receiver[1],bearing_point=[x,z]))
            # A relocated pile must physically meet the supported deck through
            # an explicit short bearing beam, not hover beyond its corner.
            if abs(px-x)+abs(pz-z)>.01:
                beam=[(min(px,x)-half,min(pz,z)-half),(max(px,x)+half,min(pz,z)-half),(max(px,x)+half,max(pz,z)+half),(min(px,x)-half,max(pz,z)+half)]
                beamname=f'Pile bearing beam {len(built):04d}';built.append(prism(beamname,beam,top,[top-.2]*4,wood));proof.append(dict(name=beamname,kind='connected-bearing-beam',top_y=top,bottom_y=[top-.2]*4,footprint=beam,supported_by=name,supports=receiver[1],bearing_point=[x,z]))
    return built,findings,proof

def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:]);policies=json.loads((OUT/'support-policies.json').read_text())['sites'];roster={r['id']:r for r in json.loads((BASE/'terrain-overall-pass/terrain-envelopes.json').read_text())['sites']};CAP.mkdir(parents=True,exist_ok=True)
    for key in args.sites:
        key=key.upper();site=key.lower();candidate=BASE/'terrain-overall-pass/assemblies'/site/'terrain-assembly.glb';source=candidate if candidate.exists() else BASE/'furnishings'/site/'furnished.glb';physics=source.with_suffix('.hcs2') if candidate.exists() else source.with_name('furnished-physics.hcs2');layout=ROOT/roster[key]['layout'];data=json.loads(layout.read_text());out=OUT/site;out.mkdir(parents=True,exist_ok=True)
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));terrain=[o for o in bpy.context.scene.objects if o.type=='MESH' and (o.get('role','').startswith('saved editor terrain') or (o.name.startswith('Authored ') and 'terrain' in o.name))];assert terrain,key
        terrain_keys={o.name for o in terrain};objects,findings,proof=construct(data,terrain,policies[key]);bpy.ops.wm.save_as_mainfile(filepath=str(out/'supported-terrain.blend'),compress=True);bpy.ops.export_scene.gltf(filepath=str(out/'supported-terrain.glb'),export_format='GLB',export_extras=True)
        parts=[]
        for obj in objects:parts.append((obj.name,records(obj)))
        old=physics.read_bytes();magic,count,total=struct.unpack_from('>III',old);assert magic==0x48435332;stream=io.BytesIO();stream.write(struct.pack('>III',magic,count+len(parts),total+sum(len(r) for _,r in parts)));stream.write(old[12:])
        for name,triangles in parts:
            raw=name.encode();stream.write(struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(triangles)))
            for xyz,flags in triangles:stream.write(struct.pack('>9fB',*xyz,flags))
        native=stream.getvalue();(out/'supported-terrain.hcs2').write_bytes(native);assert native[12:12+len(old)-12]==old[12:]
        # Reimport the actual export before capturing. Verify all new closed
        # construction surfaces against the exact appended native triangles.
        from export_roster_collision import face_keys
        expected={name:face_keys(r) for name,r in parts};bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(out/'supported-terrain.glb'))
        for obj in bpy.context.scene.objects:
            if obj.name in expected:assert face_keys(records(obj))==expected.pop(obj.name)
        assert not expected
        scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Supported terrain native review')
        a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.55;scene.camera=cam;captures=[]
        for name,offset in [('overview',Vector((span*.8,-span*.9,span*.8))),('support-side',Vector((span*.8,-span*.8,span*.30)))]:
            cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/f'{site}-{name}.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=sha(path),type='native exported terrain/support construction candidate'))
        report=dict(id=key,status='NATIVE_BEARING_CONSTRUCTION_CANDIDATE_REVIEW_OPEN',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=physics.relative_to(ROOT).as_posix(),source_physics_sha256=sha(physics),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),new_parts=len(parts),new_triangles=sum(len(r) for _,r in parts),original_physics_parts_byte_preserved=True,render_native_new_parts_exact=True,construction=proof,open_findings=findings,captures=captures,glb_sha256=sha(out/'supported-terrain.glb'),physics_sha256=sha(out/'supported-terrain.hcs2'),generator_sha256=sha(Path(__file__)),limitations='No master replacement or runtime unlock. Coarse foundation/pile construction only; actual native route regression, whole-site terrain fit, remaining unsupported spans/retaining/caves/water/guards and canonical van arrival require separate evidence.')
        (out/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('SUPPORTED_TERRAIN',key,len(parts),'parts',len(findings),'open contacts',flush=True)
if __name__=='__main__':main()
