from pathlib import Path
import subprocess,hashlib,json
names=['original','before-finish','rebuilt-v1'];manifest=json.loads(Path('development/levels/rebuild/native-upgrade-baselines/source-manifest.json').read_text(encoding='utf-8'));proof={}
paths=['assets/runtime/sites/castle-baseline-'+n+'.properties' for n in names]+['development/levels/rebuild/native-upgrade-baselines/'+n+'.properties' for n in names]
subprocess.run(['git','-c','core.autocrlf=true','checkout-index','--prefix=build/castle-after-eol-control/','--',*paths],check=True)
for n in names:
 p='assets/runtime/sites/castle-baseline-'+n+'.properties';before=Path('build/castle-before-eol-control',p).read_bytes();after=Path('build/castle-after-eol-control',p).read_bytes();expected=manifest[n]['sha256'];old=hashlib.sha256(before).hexdigest();new=hashlib.sha256(after).hexdigest()
 assert old!=expected and b'\r\n' in before,(n,'old checkout did not reproduce CRLF failure')
 assert new==expected and b'\r' not in after,(n,'corrected bytes mismatch')
 assert Path('build/castle-after-eol-control/development/levels/rebuild/native-upgrade-baselines',n+'.properties').read_bytes()==after
 proof[n]=dict(before_sha256=old,expected_sha256=expected,after_sha256=new,before_crlf=before.count(b'\r\n'),after_crlf=after.count(b'\r\n'))
Path('build/castle-checkout-control.json').write_bytes((json.dumps(dict(status='PASS actual core.autocrlf=true index checkouts before/after byte preservation',baselines=proof),indent=2)+'\n').encode('utf-8'))
print('PASS all3 actual Windows-style checkouts: reproduced broken CRLF digests; exact frozen bytes preserved after pinning; source copies identical')
