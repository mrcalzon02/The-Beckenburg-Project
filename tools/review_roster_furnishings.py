"""Verify source-bound rough allocations and assemble native inspection sheets."""
from pathlib import Path
import argparse,hashlib,json,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'build/structure-review-python'))
from PIL import Image,ImageDraw
OUT=ROOT/'development/levels/rebuild/furnishings'
CAP=ROOT/'development/levels/rebuild/captures/furnishing-batch'

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    program=json.loads((OUT/'room-programs.json').read_text());assets={a['asset']:a for a in json.loads((OUT/'staged-asset-bounds.json').read_text())['assets']}
    rows=[];poses=[]
    for site,requests in program['sites'].items():
        folder=OUT/site.lower();r=json.loads((folder/'placements.json').read_text());p=json.loads((folder/'physics-contract.json').read_text())
        assert not r['issues'] and r['independently_reimported_instances']==len(r['placements'])
        assert digest(folder/'furnished.blend')==r['native_sha256']
        assert digest(folder/'furnished.glb')==r['furnished_glb_sha256']==p['source_glb_sha256']
        assert digest(folder/'furnished-physics.hcs2')==p['physics_sha256']
        assert digest(ROOT/r['source_layout'])==r['source_layout_sha256']
        assert digest(ROOT/f'development/levels/rebuild/structures/{site.lower()}/rough-shell.glb')==r['structure_glb_sha256']
        assert digest(ROOT/'tools/build_roster_furnishings.py')==r['source_builder_sha256']
        authored=dict(requests=requests,recipes={q['recipe']:program['recipes'][q['recipe']] for q in requests})
        assert hashlib.sha256(json.dumps(authored,sort_keys=True,separators=(',',':')).encode()).hexdigest()==r['authored_program_sha256']
        assert digest(OUT/'staged-asset-bounds.json')==r['staged_asset_bounds_sha256']
        assert len(r['placements'])==sum(len(program['recipes'][q['recipe']]) for q in requests)
        for pose in r['placements']:
            assert digest(ROOT/assets[pose['asset']]['path'])==pose['source_sha256']
            assert pose['triangles']==assets[pose['asset']]['triangles']
            assert abs(pose['bounds'][2]-pose['floor_y'])<.001
            poses.append(pose)
        terrain=json.loads((ROOT/r['source_layout']).read_text()).get('saved_editor_terrain')
        if terrain:
            assert p['saved_editor_terrain_fidelity']['exact_float_triangle_and_flag_match']
            assert digest(ROOT/terrain['native_physics'])==p['saved_editor_terrain_fidelity']['native_physics_sha256']
        room=r['placements'][0]['zone'];images=[CAP/(site.lower()+'-export-overview.png'),CAP/(site.lower()+'-room-'+room+'.png')]
        assert all(f.exists() for f in images)
        rows.append(dict(id=site,instances=len(r['placements']),prop_triangles=sum(q['triangles'] for q in r['placements']),images=[f.relative_to(ROOT).as_posix() for f in images]))
    assert len(rows)==25
    source_roster=json.loads((ROOT/'development/levels/rebuild/stage-register.json').read_text())['sites']
    roster=[dict(id=s['id'],status='INITIAL_ROUGH_ALLOCATIONS_PLACED_FULL_PROGRAM_REMAINS' if s['id'] in program['sites'] else 'RETAIN_MATURE_SOURCE_INDIVIDUAL_MODEL_AUDIT_REQUIRED') for s in source_roster]
    summary=dict(scope=29,generated=25,retained=4,instances=len(poses),source_designs=len({q['asset'] for q in poses}),prop_triangles=sum(q['triangles'] for q in poses),
        uniform_rough_fit_budgets=[dict(id=q['id'],asset=q['asset'],scale=q['uniform_rough_fit_scale'],bounds=q['bounds']) for q in poses if q['uniform_rough_fit_scale']!=1],
        packets=rows,status='INITIAL_SPATIAL_ALLOCATION_NOT_FULL_FURNISHING_OR_RUNTIME_ACCEPTANCE')
    outputs={'verification.json':summary,'roster.json':dict(scope=29,sites=roster)}
    for name,data in outputs.items():
        raw=(json.dumps(data,indent=2)+'\n').encode();path=OUT/name
        if args.check:assert path.read_bytes()==raw,('Stale furnishing review',name)
        else:path.write_bytes(raw)
    if not args.check:
        for first in range(0,len(rows),4):
            sheet=Image.new('RGB',(1920,880),(22,22,24));draw=ImageDraw.Draw(sheet)
            for slot,row in enumerate(rows[first:first+4]):
                for view,path in enumerate(row['images']):
                    img=Image.open(ROOT/path).convert('RGB');img.thumbnail((480,400));x=(slot%2*2+view)*480;y=slot//2*440
                    sheet.paste(img,(x,y+28));draw.text((x+7,y+7),row['id']+(' / actual export' if view==0 else ' / isolated room cutaway'),fill='white')
            sheet.save(CAP/f'rough-allocation-sheet-{first//4+1:02d}.png')
    print('PASS25 source-bound native candidates;',len(poses),'existing rough instances;',summary['source_designs'],'source designs; complete model program and runtime gates remain open')

if __name__=='__main__':main()
