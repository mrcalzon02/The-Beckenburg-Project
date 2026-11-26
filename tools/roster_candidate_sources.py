"""Exact manifest-based candidate selection shared by native batch auditors."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[1]
def arguments(parser,output=False):
 parser.add_argument('--source-manifest',type=Path)
 parser.add_argument('--candidate-root',type=Path)
 parser.add_argument('--candidate-stem')
 if output:parser.add_argument('--output',type=Path)
def select(args,site):
 assert args.source_manifest and args.candidate_root and args.candidate_stem,'Supply manifest, candidate root and candidate stem together'
 manifest=ROOT/args.source_manifest;rows=json.loads(manifest.read_text())['active_sources'];row=next(r for r in rows if r['id']==site);before=ROOT/row['construction'];old=json.loads(before.read_text());glb=ROOT/row['glb'];native=ROOT/row['native'];assert glb.parent==native.parent==before.parent
 sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
 assert sha(glb)==row['glb_sha256']==old['glb_sha256'] and sha(native)==row['native_sha256']==old['physics_sha256'];assert sha(ROOT/row['layout'])==old['layout_sha256']
 candidate=ROOT/args.candidate_root/site.lower();contract=candidate/'construction.json'
 if contract.exists():
  r=json.loads(contract.read_text());assert ROOT/r['source_glb']==glb and ROOT/r['source_physics']==native;assert r['source_glb_sha256']==sha(glb) and r['source_physics_sha256']==sha(native);assert ROOT/r['before_layout']==ROOT/row['layout'];after=(candidate,args.candidate_stem+'.glb',args.candidate_stem+'.hcs2')
 else:
  r=dict(old,source_glb=glb.relative_to(ROOT).as_posix(),source_glb_sha256=sha(glb),source_physics=native.relative_to(ROOT).as_posix(),source_physics_sha256=sha(native),before_layout=row['layout'],before_layout_sha256=old['layout_sha256'],before_segment_map=old.get('source_segment_map',{}),before_stair_components=old['stair_components']);after=(before.parent,glb.name,native.name)
 return (before.parent,glb.name,native.name),after,r,candidate
