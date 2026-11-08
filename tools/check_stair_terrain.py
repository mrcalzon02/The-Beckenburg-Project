"""Independent saved-world, resource, editable and exact native terrain checks."""
from pathlib import Path
import json,sys
import bpy,bmesh
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from check_macroterrain_exports import inspect
from compose_room_terrain import split_native
from append_static_glb import read
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from check_roster_decks import read_parts
BASE=ROOT/'development/levels/rebuild/stair-terrain-pass'
def main():
    rows=[];(BASE/'verification').mkdir(exist_ok=True)
    for p in sorted((BASE/'assemblies').glob('*/construction.json')):
        r=json.loads(p.read_text());folder=p.parent;world=ROOT/r['world'];author=json.loads((BASE/(r['id'].lower()+'-world.json')).read_text());source=ROOT/r['source_glb'];target=folder/'benched-terrain.glb';native=folder/'benched-terrain.hcs2';old=ROOT/r['source_physics']
        for path,digest in [(source,r['source_glb_sha256']),(old,r['source_physics_sha256']),(target,r['glb_sha256']),(native,r['physics_sha256']),(ROOT/r['layout'],r['layout_sha256'])]:assert sha(path)==digest,path
        assert all(sha(ROOT/name)==h for name,h in r['world_files_sha256'].items())
        assert all(sha(ROOT/name)==h for name,h in author['source_world_files_preserved'].items())
        assert sha(world/'tile_0_0.png')==author['source_height_sha256'] and author['grading_limit']=='LOWER_ONLY'
        thresholds={p['name'] for p in r.get('threshold_repairs',[])};repairs=thresholds|{p['name'] for p in r.get('bearing_extensions',[])}
        a=inspect(source,False);b=inspect(target,False);removed=set(r['removed_source_meshes']);assert set(a)-set(b)==removed and set(b)-set(a)=={r['terrain_mesh']}|repairs
        for name in set(a)-removed:assert a[name]==b[name],(r['id'],name,'protected mesh/UV/image/pivot changed')
        doc,blob=read(source);after,after_blob=read(target);assert after_blob[:len(blob)]==blob
        for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert after.get(key,[])[:len(doc.get(key,[]))]==doc.get(key,[]),(r['id'],key)
        for i,node in enumerate(doc['nodes']):
            expected=dict(node)
            if node.get('name') in removed:expected.pop('mesh',None)
            assert after['nodes'][i]==expected
        before_parts=split_native(old);after_parts=split_native(native);kept=[part for part in before_parts if part[0] not in set(r['removed_native_parts'])];assert after_parts[:len(kept)]==kept and len(kept)==r['preserved_nonterrain_parts']
        prior_ground=ROOT/author['source_world']/'terrain-physics.hcs2'
        assert [part for part in before_parts if part[0] in set(r['removed_native_parts'])]==split_native(prior_ground),'Prior saved world does not match source composite'
        world_parts=split_native(world/'terrain-physics.hcs2');assert after_parts[len(kept):len(kept)+len(world_parts)]==world_parts
        def vertices(path):
            return {(v[i],v[i+2]):v[i+1] for v,flags in native_faces(path) for i in [0,3,6]}
        old_vertices=vertices(prior_ground);new_vertices=vertices(world/'terrain-physics.hcs2');shared=set(old_vertices)&set(new_vertices);assert all(new_vertices[k]<=old_vertices[k]+1e-6 for k in shared),'Lower-only terrain raised a saved terrain vertex'
        parsed=read_parts(native);ground=native_faces(world/'terrain-physics.hcs2');objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};assert face_keys(records(objects[r['terrain_mesh']]))==face_keys(ground)
        for repair in repairs:
            obj=objects[repair];assert parsed[repair][0]==1 and face_keys(records(obj))==face_keys(parsed[repair][1]);bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.free();heights=[(obj.matrix_world@v.co).z for v in obj.data.vertices]
            if repair in thresholds:assert abs(max(heights)-min(heights)-.16)<.0001
        scene=folder/'benched-terrain.blend';bpy.ops.wm.open_mainfile(filepath=str(scene));obj=bpy.data.objects[r['terrain_mesh']];assert face_keys(records(obj))==face_keys(ground)
        for repair in repairs:assert face_keys(records(bpy.data.objects[repair]))==face_keys(parsed[repair][1])
        images={n.image for mat in bpy.data.materials if mat.use_nodes for n in mat.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert all(i.size[0]>0 and i.size[1]>0 and i.packed_file for i in images)
        active=json.loads((ROOT/r['layout']).read_text())['current_saved_editor_terrain'];assert Path(ROOT/active['scene_package']).is_file();assert active['files_sha256']==r['world_files_sha256']
        rows.append(dict(id=r['id'],preserved_nonterrain_meshes=len(a)-1,preserved_nonterrain_native_parts=len(kept),actual_shared_terrain_triangles=len(ground),common_saved_vertices=len(shared),lowered_saved_vertices=sum(new_vertices[k]<old_vertices[k]-1e-6 for k in shared),raised_saved_vertices=0,source_height_unchanged=True,prior_world_unchanged=True,packed_images=len(images),blend_sha256=sha(scene),glb_sha256=sha(target),physics_sha256=sha(native)));print('PASS_SAVED_EDITOR_STAIR_TERRAIN',r['id'],len(ground),len(kept),flush=True)
    (BASE/'verification/independent-sources.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_SAVED_WORLD_SOURCE_NATIVE_EDITABLE_CHECKS',sites=rows,checker_sha256=sha(Path(__file__)),limitations='No terrain aesthetic/retaining/cave/physical edge/van arrival or actual runtime/hardware acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
