"""Measure current overall ground/envelope readiness before terrain/blockout revision."""
from pathlib import Path
import argparse,hashlib,json,struct
ROOT=Path(__file__).resolve().parents[1]
def ground(raw):
    magic,parts,total=struct.unpack_from('>III',raw);assert magic==0x48435332
    pos=12;result=[];count=0
    for _ in range(parts):
        length=struct.unpack_from('>H',raw,pos)[0];pos+=2;name=raw[pos:pos+length].decode();pos+=length;_,n=struct.unpack_from('>BI',raw,pos);pos+=5
        ys=[]
        for _ in range(n):
            *v,flags=struct.unpack_from('>9fB',raw,pos);pos+=37;count+=1
            if 'terrain' in name.lower() and ('Saved editor' in name or 'Authored ' in name):ys.extend(v[1::3])
        if ys:result.append(dict(component=name,triangles=n,min_y=min(ys),max_y=max(ys)))
    assert pos==len(raw) and count==total
    return result
def main():
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);args=p.parse_args();rows=[]
    roster=json.loads((ROOT/'development/levels/rebuild/stage-register.json').read_text())['sites']
    for site in roster:
        if site['id'] in ['CASTLE','HOUSE','WLD-01','WLD-02']:
            rows.append(dict(id=site['id'],status='RETAIN_MATURE_NATIVE_INDIVIDUAL_TERRAIN_AUDIT_REQUIRED'));continue
        folder=ROOT/f'development/levels/rebuild/structures/{site["folder"]}';report=json.loads((folder/'implementation.json').read_text());layout_path=ROOT/report['authoritative_input'];layout=json.loads(layout_path.read_text());contract=json.loads((folder/'physics-contract.json').read_text());raw=(folder/'structure-physics.hcs2').read_bytes();assert hashlib.sha256(raw).hexdigest()==contract['physics_sha256'];terrain=ground(raw)
        state='SAVED_PUNCHED_SOURCE_MACRO_REVIEW_OPEN' if (layout.get('current_saved_editor_terrain') or layout.get('saved_editor_terrain')) else 'PROPOSAL_MACRO_REVIEW_OPEN' if terrain else 'CONTINUOUS_TERRAIN_SOURCE_ABSENT'
        rows.append(dict(id=site['id'],status=state,layout=layout_path.relative_to(ROOT).as_posix(),layout_sha256=hashlib.sha256(layout_path.read_bytes()).hexdigest(),physics_sha256=contract['physics_sha256'],site_bounds=layout['site_bounds'],ground_parts=terrain,terrain_triangles=sum(r['triangles'] for r in terrain),occupied_floor_levels=sorted({z['y'] for z in layout['zones']}),exterior_floor_reservations=[dict(id=z['id'],y=z['y'],bounds=z['bounds']) for z in layout['zones'] if z['kind'] in ['outside','staging']],next_action='Settle broad landform/height layers, drainage and foundation envelope first; review terrain/building relationships and move blockouts or stair bays where required. Then engineer paths, retaining edges and terrain entrances. No acceptance from route strips.'))
    result=dict(scope=29,owner_priority='Overall terrain layers and elevation relationships before further local connector fixes; coherent blockout reshuffles authorized',source_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),sites=rows,limitation='Ground presence/range only, not terrain acceptance, geographic coherence or coverage under every location. Four mature sources retained; canonical van/edges/foundations and all acceptance gates remain separate.')
    args.output.mkdir(parents=True,exist_ok=True);(args.output/'terrain-envelopes.json').write_bytes((json.dumps(result,indent=2)+'\n').encode())
    lines=['# Overall terrain and elevation pass','','Owner priority: settle overall terrain layers, then revise the blockouts to fit. Existing paths and orange elevation links are evidence to review, not fixed terrain constraints.','','| Site | Current measured ground state | Occupied floor levels |','| --- | --- | --- |']
    for r in rows:lines.append('| '+r['id']+' | '+r['status']+' | '+(', '.join(map(str,r.get('occupied_floor_levels',[]))) or 'Retained individual audit')+' |')
    lines+=['','Set broad ground forms/strata, slopes, drainage, retaining edges and foundation heights. Inspect the full site; adjust building groups, wings and stair bays where those relationships are awkward. Preserve source models, stable room IDs, the40-space budget and shared coordinate contracts. Use saved editor punches for actual basement entrances. No map is accepted by this matrix.','']
    (args.output/'README.md').write_bytes(('\n'.join(lines)).encode())
    print({s:sum(r['status']==s for r in rows) for s in sorted({r['status'] for r in rows})})
if __name__=='__main__':main()
