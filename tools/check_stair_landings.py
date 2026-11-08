"""Independent preservation, closed-solid, native and editable-source checks."""
from pathlib import Path
import collections,json,sys
import bpy,bmesh
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from check_macroterrain_exports import inspect
from check_roster_decks import read_parts
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from append_static_glb import read
BASE=ROOT/'development/levels/rebuild/stair-landing-pass'

def main():
    rows=[]
    for contract in sorted(BASE.glob('*/construction.json')):
        r=json.loads(contract.read_text());folder=contract.parent;source=ROOT/r['source_glb'];target=folder/'supported-stairs.glb';native=folder/'supported-stairs.hcs2'
        for p,h in [(source,r['source_glb_sha256']),(ROOT/r['source_physics'],r['source_physics_sha256']),(target,r['glb_sha256']),(native,r['physics_sha256']),(ROOT/r['layout'],r['layout_sha256'])]:assert sha(p)==h,p
        old=read_parts(ROOT/r['source_physics']);new=read_parts(native);removed=set(r['removed_native_parts']);added=set(new)-(set(old)-removed)
        assert set(old)-set(new)==removed
        for n in set(old)-removed:assert old[n]==new[n],(r['id'],n,'native source changed')
        assert len(added)==r['new_parts']
        a=inspect(source,False);b=inspect(target,False);removed_meshes=set(r['removed_source_meshes']);assert set(a)-set(b)==removed_meshes
        assert set(b)-set(a)==added
        for n in set(a)-removed_meshes:assert a[n]==b[n],(r['id'],n,'mesh/UV/image/pivot changed')
        doc,blob=read(source);after,after_blob=read(target);assert after_blob[:len(blob)]==blob
        for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert after.get(key,[])[:len(doc.get(key,[]))]==doc.get(key,[]),(r['id'],key)
        for i,node in enumerate(doc['nodes']):
            expected=dict(node)
            if node.get('name') in removed_meshes:expected.pop('mesh',None)
            assert after['nodes'][i]==expected,(r['id'],i,'unexpected original node change')
        objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};triangles=0
        for n in added:
            obj=objects[n];ts=records(obj);assert face_keys(ts)==face_keys(new[n][1]) and new[n][0]==1
            # GLB splits vertices at UV/material seams; close geometric edges.
            bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8)
            assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(r['id'],n,'open or inverted')
            bm.free();triangles+=len(ts)
        scene=folder/'supported-stairs.blend';bpy.ops.wm.open_mainfile(filepath=str(scene));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
        for n in added:assert face_keys(records(objects[n]))==face_keys(new[n][1]),(r['id'],n,'editable/native mismatch')
        images={n.image for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image}
        assert all(i.size[0]>0 and i.size[1]>0 and i.packed_file is not None for i in images)
        rows.append(dict(id=r['id'],retained_source_meshes=len(a)-len(removed_meshes),replaced_owned_steps=len(removed_meshes),closed_new_parts=len(added),new_triangles=triangles,packed_images=len(images),blend_sha256=sha(scene),glb_sha256=sha(target),physics_sha256=sha(native)))
        print('PASS_STAIR_SOURCE',r['id'],len(added),triangles,flush=True)
    (BASE/'verification/independent-sources.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_SOURCE_SOLID_NATIVE_EDITABLE_CHECKS',sites=rows,checker_sha256=sha(Path(__file__)),limitations='No full map, guard/support engineering, runtime/network or hardware acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
