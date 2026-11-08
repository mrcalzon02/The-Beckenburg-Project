"""Read-only native structure batch integrity and review contact sheets.

This checks delivered bytes and records source-specific unfinished work. It does
not claim gameplay, collision, art acceptance or a manual review on its own.
"""
from pathlib import Path
import argparse,hashlib,json,sys

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'build/structure-review-python'))
from PIL import Image,ImageDraw
OUT=ROOT/'development/levels/rebuild/structures'
CAPTURES=ROOT/'development/levels/rebuild/captures/structure-batch'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    roster=json.loads((OUT/'roster.json').read_text(encoding='utf-8'));assert len(roster['sites'])==29
    reports=[]
    for site in roster['sites']:
        if site['status'].startswith('RETAIN_'):continue
        assert site['status']=='NATIVE_STRUCTURE_BLOCKING_RUNTIME_GATES_OPEN',site
        folder=OUT/Path(site['evidence']).parent.name
        report=json.loads((folder/'implementation.json').read_text(encoding='utf-8'));check=report['structural_review']
        assert check['independently_reimported'] and check['checked_closed_solids']>0 and check['measured_depth_parts']>0 and check['exported_cap_rays']>0
        assert hashlib.sha256((folder/'rough-shell.glb').read_bytes()).hexdigest()==check['glb_sha256']
        assert hashlib.sha256((folder/'rough-shell.blend').read_bytes()).hexdigest()==report['blend_sha256']
        for name,digest in report['preserved_sources'].items():assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,(site['id'],name)
        for name,digest in report['generator_sha256'].items():assert hashlib.sha256((ROOT/name).read_bytes()).hexdigest()==digest,(site['id'],'stale generator',name)
        if 'curved_stair_revision' in report:
            revision=report['curved_stair_revision']
            assert hashlib.sha256((ROOT/'tools/refine_roster_curved_stairs.py').read_bytes()).hexdigest()==revision['builder_sha256'],(site['id'],'stale curved stair revision')
            assert len(revision['components'])==revision['treads'] and revision['treads']>0
        assert len(check['exported_fixed_boundary_checks'])==check['fixed_infill_routes']
        source=json.loads((ROOT/report['authoritative_input']).read_text(encoding='utf-8'))
        unresolved=[item for item in report['issues'] if 'rough mass' in item.get('issue','') or 'needs an authored' in item.get('issue','')]
        reports.append(dict(id=site['id'],folder=folder.name,triangles=check['triangles'],closed_solids=check['checked_closed_solids'],
            depth_parts=check['measured_depth_parts'],cap_rays=check['exported_cap_rays'],stair_treads=check['stair_treads'],fixed_routes=check['fixed_infill_routes'],
            open_search_spaces=source.get('search_envelope',{}).get('accessible_spaces'),
            basement_zones=[zone['id'] for zone in source['zones'] if zone['y']<0 and zone['kind'] not in ['outside','staging']],
            unresolved_elevation_support=unresolved,stage='NATIVE_BLOCKING_NOT_RUNTIME_ACCEPTED'))
        if not args.check:
            destination=CAPTURES/folder.name;destination.mkdir(parents=True,exist_ok=True)
            for stem in ['structure-export-closed','structure-export-cutaway']:(destination/(stem+'.png')).write_bytes((folder/(stem+'.png')).read_bytes())
    assert len(reports)==25
    summary=dict(scope=29,generated=25,mature_native_retained=4,manual_visual_review='REQUIRED; generated sheets are not acceptance',
        packets=reports,next_batch='Runtime/native coordinate bridge and actual swept routes; terrain holes and source-specific models/services; retain <=40 accessible spaces')
    path=OUT/'verification.json';raw=(json.dumps(summary,indent=2,ensure_ascii=False)+'\n').encode('utf-8')
    if args.check:assert path.read_bytes()==raw,'Stale structure review'
    else:
        path.write_bytes(raw)
        for page in range(0,len(reports),4):
            sheet=Image.new('RGB',(1920,768),(22,22,24));draw=ImageDraw.Draw(sheet)
            for slot,report in enumerate(reports[page:page+4]):
                for view,stem in enumerate(['structure-export-closed','structure-export-cutaway']):
                    image=Image.open(CAPTURES/report['folder']/(stem+'.png')).convert('RGB');image.thumbnail((480,360))
                    x=(slot%2*2+view)*480;y=slot//2*384
                    sheet.paste(image,(x,y+24));draw.text((x+8,y+5),report['id']+(' / caps closed' if view==0 else ' / cap cutaway'),fill='white')
            sheet.save(CAPTURES/f'roster-structure-sheet-{page//4+1:02d}.png')
    print('PASS25 native exports /29roster decisions; exact originals/generators/closed solids/depth/cap rays and fixed boundary counts. Runtime gates open.')

if __name__=='__main__':main()
