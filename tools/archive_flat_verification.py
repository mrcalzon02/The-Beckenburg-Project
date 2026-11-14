"""Losslessly archive large raw flat-route tables; preserve decoded byte hashes.
PNGs, models, saved worlds, reports and small diagnostic tables stay unchanged.
"""
from pathlib import Path
import argparse,gzip,hashlib,json
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/flat-circulation-pass';INDEX=BASE/'verification/archive-index.json'
def digest(data):return hashlib.sha256(data).hexdigest()
def main():
 global BASE,INDEX
 p=argparse.ArgumentParser();p.add_argument('--root',type=Path,help='Repository-relative source pass; default remains flat-circulation-pass');p.add_argument('--check',action='store_true');p.add_argument('--restore-to',type=Path);args=p.parse_args()
 if args.root is not None:
  BASE=(ROOT/args.root).resolve();assert BASE.is_relative_to(ROOT/'development/levels/rebuild') and BASE.is_dir();INDEX=BASE/'verification/archive-index.json'
 old=json.loads(INDEX.read_text()) if INDEX.exists() else dict(files=[]);records={r['original_path']:r for r in old['files']}
 if not args.check and args.restore_to is None:
  for path in sorted(BASE.rglob('*.tsv')):
   if path.stat().st_size<2_000_000:continue
   data=path.read_bytes();packed=gzip.compress(data,compresslevel=6,mtime=0);assert gzip.decompress(packed)==data;target=path.with_name(path.name+'.gz');target.write_bytes(packed);key=path.relative_to(ROOT).as_posix();records[key]=dict(original_path=key,archive_path=target.relative_to(ROOT).as_posix(),original_bytes=len(data),original_sha256=digest(data),archive_bytes=len(packed),archive_sha256=digest(packed),encoding='gzip, exact original raw bytes including Java CRLF');path.unlink()
  INDEX.write_bytes((json.dumps(dict(status='LOSSLESS_RAW_VERIFICATION_ARCHIVES_NOT_NEW_TEST_ACCEPTANCE',files=list(records.values()),scope='Large raw TSVs archived after terminal verification. JSON evidence hashes still identify exact decoded original bytes; reconstruct with --restore-to build/flat-evidence-replay. Original absolute payload references remain historical and SHA-bound.'),indent=2)+'\n').encode())
 for r in records.values():
  path=ROOT/r['archive_path'];packed=path.read_bytes();assert digest(packed)==r['archive_sha256'];data=gzip.decompress(packed);assert len(data)==r['original_bytes'] and digest(data)==r['original_sha256']
  if args.restore_to is not None:
   dest=(ROOT/args.restore_to/r['original_path']).resolve();assert dest.is_relative_to(ROOT),dest;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
 print('PASS_RAW_EVIDENCE_ARCHIVES',len(records),'original_bytes',sum(r['original_bytes'] for r in records.values()),'archive_bytes',sum(r['archive_bytes'] for r in records.values()),flush=True)
if __name__=='__main__':main()
