"""Replace terrain only with the actual saved editor layer/punch export."""
from pathlib import Path
import json,struct,sys,math
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_roster_floor_closures import difference,clean_polygon
from build_roster_decks import area
from planar_structure import plate
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-terrain-pass'
def main(sites=('WLD-03','WLD-04','WLD-12')):
    for site in sites:
        w=json.loads((OUT/(site.lower()+'-world.json')).read_text());source=ROOT/w['source_glb'];native=ROOT/w['source_physics'];world=ROOT/w['world'];prior=json.loads(source.with_name('construction.json').read_text());assert sha(source)==w['source_glb_sha256'] and sha(native)==w['source_physics_sha256'];assert all(sha(ROOT/name)==h for name,h in w['world_files_sha256'].items());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role','').startswith('saved editor terrain')];assert len(old)==1,(site,'terrain mesh identity');old=old[0];name=old.name;materials=list(old.data.materials);bpy.data.objects.remove(old,do_unlink=True);points=[];faces=[];chunks=sorted((world/'scene-package/meshes').glob('site*.obj'))
        for chunk in chunks:
            offset=len(points)
            for row in chunk.read_text().splitlines():
                v=row.split()
                if v and v[0]=='v':x,y,z=map(float,v[1:]);points.append((x,-z,y))
                elif v and v[0]=='f':faces.append(tuple(offset+int(x.split('/')[0])-1 for x in v[1:]))
        mesh=bpy.data.meshes.new('Saved editor bench/punch triangles');mesh.from_pydata(points,[],faces);mesh.update();obj=bpy.data.objects.new('Saved editor stair bench terrain '+site,mesh);bpy.context.scene.collection.objects.link(obj);obj['role']='saved editor terrain / actual stair-bed layers and basement punches'
        for mat in materials:mesh.materials.append(mat)
        uv=mesh.uv_layers.new(name='World metre UV')
        for p in mesh.polygons:
            if len(materials)>2:p.material_index=1 if p.normal.z<.93 else 2 if sum(mesh.vertices[i].co.z for i in p.vertices)/len(p.vertices)<-.9 else 0
            for li in p.loop_indices:
                v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x/8,v.y/8)
        assert face_keys(records(obj))==face_keys(native_faces(world/'terrain-physics.hcs2')),'Saved OBJ/render/native mismatch'
        repairs=[];repair_proof=[]
        if site=='WLD-04':
            # Real160mm threshold replaces the old accidental soil footing.
            polygons=[[(39.5,21),(40.5,21),(40.5,23),(39.5,23)]]
            for other in list(bpy.context.scene.objects):
                role=other.get('role','')
                if other.type!='MESH' or not(role in ['floor','structural-stair-step','structural-stair-landing'] or role.startswith('rough route support')):continue
                other.data.calc_loop_triangles()
                for t in other.data.loop_triangles:
                    ps=[other.matrix_world@other.data.vertices[i].co for i in t.vertices]
                    if any(abs(v.z-6)>.0001 for v in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
                    cut=[(round(v.x,5),round(-v.y,5)) for v in ps];polygons=[r for poly in polygons for r in difference(poly,cut)]
            polygons=[clean_polygon(p) for p in polygons if area(p)>1e-8];mat=next(o.data.materials[0] for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='floor' and o.get('zone')=='ruin');repairs=plate('Authored ruin threshold bearing plate',polygons,6,.16,mat) if polygons else []
            for repair in repairs:repair['role']='floor';repair['zone']='ruin';repair['link']='L07';repair_proof.append(dict(name=repair.name,top_y=6,bottom_y=5.84,threshold=[40,6,22],clear_width=2))
        extensions=[];extension_proof=[];soil_points=[];soil_faces=[]
        for xyz,flags in native_faces(world/'terrain-physics.hcs2'):
            start=len(soil_points);soil_points.extend(tuple(xyz[i:i+3]) for i in [0,3,6]);soil_faces.append((start,start+1,start+2))
        soil=BVHTree.FromPolygons(soil_points,soil_faces)
        prior_points=[];prior_faces=[]
        for xyz,flags in native_faces(ROOT/w['source_world']/'terrain-physics.hcs2'):
            start=len(prior_points);prior_points.extend(tuple(xyz[i:i+3]) for i in [0,3,6]);prior_faces.append((start,start+1,start+2))
        prior_soil=BVHTree.FromPolygons(prior_points,prior_faces)
        bearings=json.loads((BASE/'terrain-fit-pass'/site.lower()/'construction.json').read_text())['construction']
        for bearing in bearings:
            if bearing.get('kind')!='skirt':continue
            probes=bearing['terrain_probe_points'];old_bottom=bearing['bottom_y'];hits=[soil.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0] for x,z in probes]
            if any(hit is None for hit in hits):continue # Never invent a soil footing in a punched void.
            prior_hits=[prior_soil.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0] for x,z in probes]
            if not any(prior_hits[i] is not None and prior_hits[i].y>=old_bottom[min(i,len(old_bottom)-1)]-.02 and hit.y<old_bottom[min(i,len(old_bottom)-1)]-.02 for i,hit in enumerate(hits)):continue
            original=bpy.data.objects[bearing['name']];poly=bearing['footprint'];bottom=[]
            for i,(x,z) in enumerate(poly):
                nearest=min(range(len(probes)),key=lambda k:math.dist((x,z),probes[k]));bottom.append(min(old_bottom[i]-.05,hits[nearest].y-.35))
            n=len(poly);coords=[(x,-z,y) for levels in [bottom,old_bottom] for (x,z),y in zip(poly,levels)];faces=[tuple(range(n)),tuple(reversed(range(n,2*n)))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)];mesh=bpy.data.meshes.new('Soil-bearing skirt extension');mesh.from_pydata(coords,[],faces);mesh.update();bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.to_mesh(mesh);bm.free();extension=bpy.data.objects.new('Grounded skirt extension '+bearing['name'],mesh);bpy.context.scene.collection.objects.link(extension);mesh.materials.append(original.data.materials[0]);extension['role']='structural soil-bearing extension';extension['source_bearing']=bearing['name'];uv=mesh.uv_layers.new(name='Metre foundation UV')
            for face in mesh.polygons:
                for li in face.loop_indices:
                    v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x+v.y,v.z) if abs(face.normal.z)<.5 else (v.x,v.y)
            extensions.append(extension);extension_proof.append(dict(name=extension.name,source_bearing=bearing['name'],footprint=poly,top_y=old_bottom,bottom_y=bottom,terrain_probe_points=probes,terrain_y=[h.y for h in hits],embedding_metres=.35))
        folder=OUT/'assemblies'/site.lower();folder.mkdir(parents=True,exist_ok=True);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'benched-terrain.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT');obj.select_set(True);
        for repair in repairs+extensions:repair.select_set(True)
        addition=ROOT/'build'/('bench-terrain-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);retained=append(source,addition,folder/'benched-terrain.glb',remove_mesh_names=[name]);oldparts=split_native(native);removed=[n for n,count,raw in oldparts if n.startswith('terrain-') or n.split(':',1)[-1]==name];kept=[p for p in oldparts if p[0] not in removed];assert removed
        ground=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',ground);extra=[(o.name,records(o)) for o in repairs+extensions];payload=struct.pack('>III',magic,len(kept)+count+len(extra),sum(n for name,n,raw in kept)+total+sum(len(ts) for name,ts in extra))+b''.join(raw for name,n,raw in kept)+ground[12:]
        for part,ts in extra:
            encoded=part.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
        (folder/'benched-terrain.hcs2').write_bytes(payload)
        layout=folder/'authored-layout.json';data=json.loads((ROOT/w['layout']).read_text());data['current_saved_editor_terrain']=dict(world=w['world'],scene_package=(world/'scene-package/scene.bin').relative_to(ROOT).as_posix(),native_physics=(world/'terrain-physics.hcs2').relative_to(ROOT).as_posix(),mesh_chunks=[p.relative_to(ROOT).as_posix() for p in chunks],files_sha256=w['world_files_sha256'],source_height_unchanged=True);layout.write_bytes((json.dumps(data,indent=2)+'\n').encode())
        report=dict(id=site,status='ACTUAL_SAVED_STAIR_TERRAIN_COMPOSITE_CANDIDATE_NOT_ACCEPTED',source_glb=w['source_glb'],source_glb_sha256=sha(source),source_physics=w['source_physics'],source_physics_sha256=sha(native),before_layout=w['layout'],before_layout_sha256=w['layout_sha256'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),before_segment_map=prior.get('source_segment_map',{}),source_segment_map=prior.get('source_segment_map',{}),before_stair_components=prior['stair_components'],stair_components=prior['stair_components'],world=w['world'],world_files_sha256=w['world_files_sha256'],removed_source_meshes=[name],removed_native_parts=removed,terrain_mesh=obj.name,terrain_triangles=total,threshold_repairs=repair_proof,bearing_extensions=extension_proof,preserved_nonterrain_parts=len(kept),retained_glb_resources=retained,glb_sha256=sha(folder/'benched-terrain.glb'),physics_sha256=sha(folder/'benched-terrain.hcs2'),composer_sha256=sha(Path(__file__)),remaining='Independent saved editor/source/export/native/regression and landscape/retaining/cave/water/van arrival acceptance. No runtime unlock.')
        (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('COMPOSED_SAVED_STAIR_TERRAIN',site,total,len(kept),flush=True)
if __name__=='__main__':main()
