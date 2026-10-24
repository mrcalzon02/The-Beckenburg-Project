"""Refresh the model-worklist catalog without regenerating any authored worklist.
Supports original 'zones' and newer 'room_by_room' packets. Annexes stay separate.
"""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parent;LEVELS=ROOT.parent
def main():
    rows=[];expansions=[]
    paths=sorted((LEVELS/'layouts').glob('*/layout.json'))+sorted((LEVELS/'layout-expansions').glob('*/layout.json'))
    for path in paths:
        d=json.loads(path.read_text(encoding='utf-8'));p=ROOT/(path.parent.name+'.json')
        record=json.loads(p.read_text(encoding='utf-8'))
        assert record['id']==d['id'] and record['title']==d['title'],p
        assert record['source_layout_sha256']==hashlib.sha256(path.read_bytes()).hexdigest(),p
        zones=record.get('zones',record.get('room_by_room',[]));assert len(zones)==len(d['zones']),p
        row=dict(id=d['id'],title=d['title'],folder=path.parent.name,rooms=len(zones),map_specific_candidates=len(record['map_specific_models']))
        (expansions if path.is_relative_to(LEVELS/'layout-expansions') else rows).append(row)
    (ROOT/'catalog.json').write_text(json.dumps(dict(maps=rows,expansions=expansions,existing_map_geometry_untouched=True,authored_worklists_unchanged=True),indent=2)+'\n',encoding='utf-8')
    print('CATALOG_CHECKED',len(rows),'base maps;',len(expansions),'additive expansions; authored worklists preserved.')
if __name__=='__main__':main()
