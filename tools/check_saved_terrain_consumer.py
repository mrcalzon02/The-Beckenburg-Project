"""Import actual current chunked and legacy terrain through the shared producer."""
from pathlib import Path
import json,sys
import bpy
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from structural_pass import saved_editor_terrain
from export_roster_collision import native_faces,face_keys
from review_roster_macroterrain import records,sha
BASE=ROOT/'development/levels/rebuild/stair-terrain-pass';rows=[]
paths=sorted((BASE/'assemblies').glob('*/authored-layout.json'))+[ROOT/'development/levels/rebuild/wld-18/rough-layout.json']
for path in paths:
    data=json.loads(path.read_text());current=data.get('current_saved_editor_terrain');contract=current or data['saved_editor_terrain']
    if current:data['saved_editor_terrain']={'mesh':'deliberately-stale-legacy-control.obj','files_sha256':{}}
    bpy.ops.wm.read_factory_settings(use_empty=True);mat=bpy.data.materials.new('Native contract control');obj=saved_editor_terrain(data,bpy.context.scene.collection,mat);expected=native_faces(ROOT/contract['native_physics']);assert face_keys(records(obj))==face_keys(expected),(path,'shared producer did not import exact saved native terrain')
    rows.append(dict(layout=path.relative_to(ROOT).as_posix(),current_contract_preferred=bool(current),legacy_control=not bool(current),triangles=len(expected),mesh_chunks=len(contract.get('mesh_chunks') or [contract['mesh']]),layout_sha256=sha(path)));print('PASS_SHARED_SAVED_TERRAIN_CONSUMER',data['id'],len(expected),bool(current),flush=True)
(BASE/'verification/terrain-contract-consumer.json').write_bytes((json.dumps(dict(status='PASS_CURRENT_CHUNKED_AND_LEGACY_SAVED_TERRAIN_IMPORT',cases=rows,producer_sha256=sha(ROOT/'development/levels/blockouts/structural_pass.py'),checker_sha256=sha(Path(__file__)),scope='Actual persisted current terrain imported despite deliberately stale legacy control, exact native triangles/flags, and unchanged real legacy world control. No runtime unlock.'),indent=2)+'\n').encode())
