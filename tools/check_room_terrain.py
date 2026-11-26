"""Independently check occupied-room terrain composites and new solid repairs."""
from pathlib import Path
import collections,json,sys
import bpy
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from check_macroterrain_exports import inspect
from check_roster_decks import read_parts
from review_roster_macroterrain import records,sha
from export_roster_collision import native_faces,face_keys
BASE=ROOT/'development/levels/rebuild/terrain-occupancy-pass'


def main():
    rows=[]
    for contract in sorted((BASE/'assemblies').glob('*/construction.json')):
        r=json.loads(contract.read_text());folder=contract.parent;source=ROOT/r['source_glb'];candidate=folder/'occupied-terrain.glb';old=ROOT/r['source_physics'];native=folder/'occupied-terrain.hcs2';world=ROOT/r['saved_world']
        for p,h in [(source,r['source_glb_sha256']),(old,r['source_physics_sha256']),(candidate,r['glb_sha256']),(native,r['physics_sha256'])]:assert sha(p)==h,p
        a=inspect(source,False);b=inspect(candidate,False);removed=set(a)-set(b);new=set(b)-set(a)
        assert len(removed)==1 and 'terrain' in next(iter(removed)).lower(),(r['id'],'unexpected source removal',removed)
        for name in set(a)&set(b):
            before,after=a[name],b[name];assert before['parts']==after['parts'] and before['images']==after['images'],(r['id'],name,'mesh/UV/image changed')
            assert max(abs(x-y) for p,q in zip(before['matrix'],after['matrix']) for x,y in zip(p,q))<1e-5,(r['id'],name,'source pose changed')
        solids={p['name'] for p in r['floor_repairs']+r['excavation_collars']};assert new==solids|{'Saved editor occupied-room terrain '+r['id']}
        before_parts=read_parts(old);after_parts=read_parts(native);preserved={name:part for name,part in before_parts.items() if not name.startswith('terrain-') and name.split(':',1)[-1] not in removed}
        assert len(preserved)==r['preserved_nonterrain_parts']
        for name,part in preserved.items():assert after_parts[name]==part,(r['id'],name,'nonterrain native changed')
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(candidate));meshes={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
        terrain=meshes['Saved editor occupied-room terrain '+r['id']];ground=native_faces(world/'terrain-physics.hcs2');assert face_keys(records(terrain))==face_keys(ground)
        extras=[]
        for name in solids:
            rs=records(meshes[name]);assert face_keys(rs)==face_keys(after_parts[name][1]) and after_parts[name][0]==1,(r['id'],name,'new solid native mismatch')
            origin=rs[0][0][:3];volume=0;edges=collections.Counter()
            for xyz,flags in rs:
                points=[tuple(xyz[i:i+3]) for i in [0,3,6]]
                va,vb,vc=[[xyz[i+k]-origin[k] for k in range(3)] for i in [0,3,6]]
                volume+=(va[0]*(vb[1]*vc[2]-vb[2]*vc[1])+va[1]*(vb[2]*vc[0]-vb[0]*vc[2])+va[2]*(vb[0]*vc[1]-vb[1]*vc[0]))/6
                for p,q in zip(points,points[1:]+points[:1]):edges[tuple(sorted((tuple(round(c,5) for c in p),tuple(round(c,5) for c in q))))]+=1
            assert volume>1e-8 and all(n==2 for n in edges.values()),(r['id'],name,'open/inverted repair solid');extras.extend(rs)
        assert face_keys(native_faces(native))==face_keys([t for c,ts in preserved.values() for t in ts]+ground+extras),(r['id'],'unexpected native surface')
        author=json.loads((BASE/(r['id'].lower()+'-world.json')).read_text())
        assert all(sha(ROOT/p)==h for p,h in author['source_world_preserved'].items()),'Prior source world changed'
        assert all(sha(ROOT/p)==h for p,h in author['saved_world_files'].items()),'Candidate world changed'
        verification=json.loads((world/'verification.json').read_text());assert verification['disk_reopen_verified'] and verification['punched_body_clear_and_unpunched_body_blocked'] and verification['unpunched_optical_negative_verified']
        rows.append(dict(id=r['id'],preserved_original_meshes=len(a)-1,preserved_native_parts=len(preserved),saved_terrain_triangles=len(ground),new_closed_solids=len(solids),glb_sha256=sha(candidate),physics_sha256=sha(native),saved_world_body_optical_and_reopen=True))
        print('PASS_OCCUPIED_TERRAIN_EXPORT',r['id'],len(a)-1,len(solids),flush=True)
    after=json.loads((BASE/'after-room-terrain.json').read_text());assert len(after['sites'])==25 and all(not r['intrusions'] for r in after['sites']),'Occupied terrain remains'
    for r in after['sites']:assert sha(ROOT/r['source_glb'])==r['source_glb_sha256'],'Room audit is stale'
    out=BASE/'verification';out.mkdir(exist_ok=True);(out/'independent-export.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_TERRAIN_AND_REPAIR_EXPORT_CHECKS',sites=rows,all25_room_audit_clear=True,checker_sha256=sha(Path(__file__)),limitation='Half-metre architectural-envelope sampling and native construction checks; not full wall clearance, whole landscape, physical boundaries, engineering or runtime/network/performance acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
