"""Export the actual structural GLBs to the existing HCS2 runtime format.

Blender background only. No editor UI, native source rewrite or playable unlock.
"""
from pathlib import Path
import argparse,collections,hashlib,io,json,math,os,struct,sys
import bpy

ROOT=Path(__file__).resolve().parents[1]


def native_faces(path):
    raw=path.read_bytes();magic,parts,count=struct.unpack_from('>III',raw);assert magic==0x48435332
    cursor=12;records=[]
    for _ in range(parts):
        length=struct.unpack_from('>H',raw,cursor)[0];cursor+=2+length
        closed,size=struct.unpack_from('>BI',raw,cursor);cursor+=5
        for _ in range(size):
            *xyz,flags=struct.unpack_from('>9fB',raw,cursor);cursor+=37;records.append((xyz,flags))
    assert cursor==len(raw) and len(records)==count
    return records


def face_keys(records):
    return collections.Counter((tuple(sorted(tuple(xyz[n:n+3]) for n in [0,3,6])),flags) for xyz,flags in records)

def export(folder,furnished=False):
    source=folder/('furnished.glb' if furnished else 'rough-shell.glb');before=hashlib.sha256(source.read_bytes()).hexdigest()
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
    parts=[];poses=[];stairs=[];dropped=0;terrain_faces=[]
    for number,obj in enumerate(obj for obj in bpy.context.scene.objects if obj.type=='MESH'):
        obj.data.calc_loop_triangles();role=obj.get('role','');name=f'{number}:{obj.name}'
        stair=role in ['structural-stair-step','structural-stair-landing']
        if stair:stairs.append(name)
        records=[];edges=collections.Counter();worldpoints=[]
        for triangle in obj.data.loop_triangles:
            points=[obj.matrix_world@obj.data.vertices[i].co for i in triangle.vertices]
            normal=(points[1]-points[0]).cross(points[2]-points[0])
            if normal.length_squared<1e-20:dropped+=1;continue
            water='water' in role.lower() or obj.name.lower().startswith('water')
            flags=2 if water else 6
            if not water and normal.z>0 and math.hypot(normal.x,normal.y)/normal.z<=.20+1e-5:flags|=1
            xyz=[value for point in points for value in (point.x,point.z,-point.y)]
            records.append((xyz,flags));worldpoints.extend(points)
            keys=[tuple(round(c,5) for c in point) for point in points]
            for a,b in zip(keys,keys[1:]+keys[:1]):edges[tuple(sorted((a,b)))]+=1
        if not records:continue
        if role.startswith('saved editor terrain'):terrain_faces.extend(records)
        closed=all(n==2 for n in edges.values()) and all(flags&4 for xyz,flags in records)
        parts.append((name,closed,records))
        if role=='structural-stair-step':
            poses.append(dict(link=obj['link'],component=name,x=sum(p.x for p in worldpoints)/len(worldpoints),
                y=max(p.z for p in worldpoints),z=-sum(p.y for p in worldpoints)/len(worldpoints)))
    target=folder/('furnished-physics.hcs2' if furnished else 'structure-physics.hcs2')
    with io.BytesIO() as stream:
        stream.write(struct.pack('>III',0x48435332,len(parts),sum(len(records) for name,closed,records in parts)))
        for name,closed,records in parts:
            raw=name.encode('utf-8');assert len(raw)<=256
            stream.write(struct.pack('>H',len(raw))+raw+struct.pack('>BI',int(closed),len(records)))
            for xyz,flags in records:stream.write(struct.pack('>9fB',*xyz,flags))
        payload=stream.getvalue()
    assert hashlib.sha256(source.read_bytes()).hexdigest()==before
    contract=dict(source_glb_sha256=before,physics_sha256=hashlib.sha256(payload).hexdigest(),
        exporter_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),stair_components=stairs,
        tread_centres=poses,dropped_degenerate_triangles=dropped,scope='Candidate native collision; gameplay/ownership/terrain/network gates remain open')
    layout=ROOT/f'development/levels/rebuild/{folder.name}/rough-layout.json'
    if layout.exists():
        authority=json.loads(layout.read_text(encoding='utf-8')).get('saved_editor_terrain')
        if authority:
            source_terrain=ROOT/authority['native_physics'];expected=native_faces(source_terrain)
            assert face_keys(expected)==face_keys(terrain_faces),('GLB terrain differs from saved editor physical triangles/flags',folder.name)
            contract['saved_editor_terrain_fidelity']=dict(exact_float_triangle_and_flag_match=True,triangles=len(expected),
                native_physics_sha256=hashlib.sha256(source_terrain.read_bytes()).hexdigest())
    # Complete all geometry/fidelity assertions before replacing either prior
    # authoritative artifact. Failed terrain checks must leave the old pair intact.
    metadata=folder/'physics-contract.json';staged=target.with_suffix('.hcs2.tmp');staged_metadata=metadata.with_suffix('.json.tmp')
    staged.write_bytes(payload);staged_metadata.write_bytes((json.dumps(contract,indent=2)+'\n').encode('utf-8'))
    os.replace(staged,target);os.replace(staged_metadata,metadata)
    print('EXPORTED_HCS2',folder.name,len(parts),len(poses),'actual tread poses',flush=True)

def main():
    parser=argparse.ArgumentParser();selection=parser.add_mutually_exclusive_group(required=True)
    selection.add_argument('--sites',nargs='+');selection.add_argument('--remaining',action='store_true')
    parser.add_argument('--furnished',action='store_true')
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    if args.remaining:
        roster=json.loads((ROOT/'development/levels/rebuild/structures/roster.json').read_text(encoding='utf-8'))
        folders=[(ROOT/row['evidence']).parent for row in roster['sites'] if row['status']=='NATIVE_STRUCTURE_BLOCKING_RUNTIME_GATES_OPEN']
    else:folders=[ROOT/'development/levels/rebuild/structures'/name.lower() for name in args.sites]
    if args.furnished:folders=[ROOT/'development/levels/rebuild/furnishings'/p.name for p in folders]
    for folder in folders:export(folder,args.furnished)

if __name__=='__main__':main()
