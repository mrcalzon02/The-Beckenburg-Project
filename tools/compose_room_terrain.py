"""Replace only terrain in the current furnished/bearing/deck candidate.

Reimport actual published editor OBJ; preserve all non-terrain native records
byte-for-byte. Editable source and exported/native views remain source-only.
"""
from pathlib import Path
import argparse,io,json,struct,sys,math
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import prism,material,route_void
from check_roster_foundations import tree
from build_roster_decks import difference,area
from append_static_glb import append
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'terrain-occupancy-pass';CAP=BASE/'captures/room-terrain-batch'


def split_native(path):
    raw=path.read_bytes();magic,count,total=struct.unpack_from('>III',raw);assert magic==0x48435332;cursor=12;parts=[]
    for i in range(count):
        start=cursor;n=struct.unpack_from('>H',raw,cursor)[0];cursor+=2;name=raw[cursor:cursor+n].decode();cursor+=n;closed,size=struct.unpack_from('>BI',raw,cursor);cursor+=5+37*size
        parts.append((name,size,raw[start:cursor]))
    assert cursor==len(raw) and sum(p[1] for p in parts)==total
    return parts


def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:])
    for site in args.sites:
        author=json.loads((OUT/(site.lower()+'-world.json')).read_text());r=author['source'];source=ROOT/r['source_glb'];assert sha(source)==r['source_glb_sha256'];oldphysics=source.with_suffix('.hcs2');layout=ROOT/r['layout'];data=json.loads(layout.read_text());world=ROOT/author['world']
        assert all(sha(ROOT/p)==h for p,h in author['saved_world_files'].items())
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
        terrain=[o for o in bpy.context.scene.objects if o.type=='MESH' and (o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name)]
        assert len(terrain)==1,(site,'explicit single source terrain required');oldterrain=terrain[0];oldterrainname=oldterrain.name;mat=oldterrain.data.materials[0]
        soil,_=tree([oldterrain]);collars=[];collar_proof=[]
        if site=='WLD-14':
            zone=next(z for z in data['zones'] if z['id']=='tower0');a,b,c,d=zone['bounds'];cx=(a+c)/2;cz=(b+d)/2;radius=zone['radius'];stone=material('Rough curved tower retaining footing',(.35,.33,.29))
            for i in range(64):
                angles=[2*math.pi*i/64,2*math.pi*(i+1)/64]
                polygon=[(cx+r*math.cos(t),cz+r*math.sin(t)) for r,t in [(radius-.1,angles[0]),(radius+.4,angles[0]),(radius+.4,angles[1]),(radius-.1,angles[1])]]
                probes=[(cx+(radius+.4)*math.cos(t),cz+(radius+.4)*math.sin(t)) for t in angles]
                heights=[soil.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)))[0] for x,z in probes]
                if any(h is None for h in heights):continue # Existing adjoining excavation; never invent a soil footing
                top=max(h.z for h in heights)+.1;bottom=min(zone['y']-.4,min(h.z for h in heights)-.4)
                rect=[min(p[0] for p in polygon),min(p[1] for p in polygon),max(p[0] for p in polygon),max(p[1] for p in polygon)]
                if route_void(rect,bottom,top,data):continue
                o=prism('Tower excavation collar '+str(i),polygon,top,[bottom]*4,stone);collars.append(o);collar_proof.append(dict(name=o.name,footprint=polygon,top_y=top,bottom_y=bottom,source_terrain_probes=probes,source_terrain_y=[h.z for h in heights],outer_radius=radius+.4,inner_radius=radius-.1))
        floor_repairs=[]
        if site=='WLD-04':
            zone=next(z for z in data['zones'] if z['id']=='ruin');a,b,c,d=zone['bounds'];e=zone['y'];polygons=[[(a,b),(c,b),(c,d),(a,d)]]
            # Preserve actual sloped flight shafts, not their oversized axis-
            # aligned bounding boxes that cut unrelated corners of this room.
            for link in data['links']:
                for p,q in zip(link['points'],link['points'][1:]):
                    if abs(p[1]-q[1])<.01 or e<=min(p[1],q[1])+.001 or e>max(p[1],q[1])+2.2:continue
                    run=math.hypot(q[0]-p[0],q[2]-p[2]);half=link['width']/2+.15;nx=-(q[2]-p[2])/run*half;nz=(q[0]-p[0])/run*half
                    cutter=[(p[0]+nx,p[2]+nz),(q[0]+nx,q[2]+nz),(q[0]-nx,q[2]-nz),(p[0]-nx,p[2]-nz)]
                    polygons=[r for poly in polygons for r in difference(poly,cutter)]
            for o in list(bpy.context.scene.objects):
                if o.type!='MESH' or not(o.get('role')=='floor' or o.get('role','').startswith('rough route support')):continue
                o.data.calc_loop_triangles()
                for t in o.data.loop_triangles:
                    ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
                    if (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0 or any(abs(p.z-e)>.0001 for p in ps):continue
                    cutter=[(p.x,-p.y) for p in ps];polygons=[r for poly in polygons for r in difference(poly,cutter)]
            floor_mat=next(o.data.materials[0] for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Floor ruin'))
            for poly in polygons:
                if area(poly)<.0001:continue
                # Convex clipping can retain collinear original seam vertices.
                changed=True
                while changed and len(poly)>3:
                    changed=False
                    for i,p in enumerate(poly):
                        x,y=poly[i-1],poly[(i+1)%len(poly)];length=math.dist(x,y)
                        if length and abs((p[0]-x[0])*(y[1]-x[1])-(p[1]-x[1])*(y[0]-x[0]))/length<.00001:poly=poly[:i]+poly[i+1:];changed=True;break
                o=prism('Restored ruin floor '+str(len(collars)),poly,e,[e-.16]*len(poly),floor_mat);o['role']='floor';o['zone']='ruin';collars.append(o);floor_repairs.append(dict(name=o.name,footprint=poly,top_y=e,bottom_y=e-.16,reason='False diagonal-shaft AABB removed this floor; replace only outside actual flight polygons and existing tops'))
        bpy.data.objects.remove(oldterrain,do_unlink=True)
        points=[];faces=[]
        for path in sorted((world/'scene-package/meshes').glob('site*.obj')):
            offset=len(points)
            for row in path.read_text().splitlines():
                vs=row.split()
                if not vs:continue
                if vs[0]=='v':x,y,z=map(float,vs[1:4]);points.append((x,-z,y))
                elif vs[0]=='f':faces.append(tuple(offset+int(v.split('/')[0])-1 for v in vs[1:]))
        mesh=bpy.data.meshes.new('Persistent occupied-room terrain triangles');mesh.from_pydata(points,[],faces);mesh.update();obj=bpy.data.objects.new('Saved editor occupied-room terrain '+site,mesh);bpy.context.scene.collection.objects.link(obj);mesh.materials.append(mat);obj['role']='saved editor terrain / occupied-room excavation candidate'
        uv=mesh.uv_layers.new(name='World metre UV')
        for face in mesh.polygons:
            for li in face.loop_indices:
                v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x/8,v.y/8)
        expected=face_keys(native_faces(world/'terrain-physics.hcs2'));assert face_keys(records(obj))==expected
        folder=OUT/'assemblies'/site.lower();folder.mkdir(parents=True,exist_ok=True);CAP.mkdir(parents=True,exist_ok=True)
        bpy.ops.wm.save_as_mainfile(filepath=str(folder/'occupied-terrain.blend'),compress=True)
        bpy.ops.object.select_all(action='DESELECT')
        for o in [obj]+collars:o.select_set(True)
        addition=ROOT/'build'/('room-terrain-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True)
        retained_resources=append(source,addition,folder/'occupied-terrain.glb',remove_mesh_names=[oldterrainname])
        parts=split_native(oldphysics);kept=[p for p in parts if not p[0].startswith('terrain-') and p[0].split(':',1)[-1]!=oldterrainname]
        assert len(kept)<len(parts),'No source terrain removed'
        ground=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',ground)
        extra=[];collar_triangles=0
        for o in collars:
            triangles=records(o);collar_triangles+=len(triangles);raw=o.name.encode();payload=struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(triangles))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in triangles);extra.append(payload)
        result=struct.pack('>III',magic,len(kept)+count+len(extra),sum(p[1] for p in kept)+total+collar_triangles)+b''.join(p[2] for p in kept)+ground[12:]+b''.join(extra);(folder/'occupied-terrain.hcs2').write_bytes(result)
        assert split_native(folder/'occupied-terrain.hcs2')[:len(kept)]==kept
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'occupied-terrain.glb'))
        current=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Saved editor occupied-room terrain'));assert face_keys(records(current))==expected
        scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Native occupied-room excavation review')
        a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.55;scene.camera=cam;captures=[]
        for name,offset in [('overview',Vector((span*.8,-span*.9,span*.8))),('terrain-only',Vector((span*.7,-span*.9,span*.8)))]:
            if name=='terrain-only':
                for o in bpy.context.scene.objects:
                    if o.type=='MESH' and o!=current:o.hide_render=True
            cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/f'{site.lower()}-{name}.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=sha(path),type='native reimported occupied-room terrain candidate'))
        report=dict(id=site,status='ACTUAL_OCCUPIED_ROOM_TERRAIN_COMPOSITE_REVIEW_OPEN',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=oldphysics.relative_to(ROOT).as_posix(),source_physics_sha256=sha(oldphysics),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),saved_world=author['world'],saved_native_sha256=sha(world/'terrain-physics.hcs2'),preserved_nonterrain_parts=len(kept),replaced_terrain_parts=len(parts)-len(kept),saved_terrain_triangles=total,glb_sha256=sha(folder/'occupied-terrain.glb'),physics_sha256=sha(folder/'occupied-terrain.hcs2'),composer_sha256=sha(Path(__file__)),captures=captures,remaining='Independent non-terrain mesh/UV/pose preservation and complete flat/stair movement comparison; actual terrain/foundation/cave/retaining/roof fit and runtime/network/hardware acceptance remain open.')
        report['retained_glb_resources']=retained_resources;report['excavation_collars']=collar_proof;report['floor_repairs']=floor_repairs
        (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('COMPOSED_OCCUPIED_ROOM_TERRAIN',site,total,len(kept),flush=True)
if __name__=='__main__':main()
