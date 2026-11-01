"""Prove saved editor terrain packages retain their exact bytes under Git EOL filters."""
from pathlib import Path
import hashlib
import subprocess

ROOT=Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(['git',*args],cwd=ROOT)


paths=[name for name in git('ls-files','-z','--','development/levels/rebuild').decode('utf-8').split('\0')
       if '/terrain-world/' in name]
assert paths,'No saved editor terrain package to inspect'
for path in paths:
    stored=git('show','HEAD:'+path)
    assert (ROOT/path).read_bytes()==stored,'Working terrain source differs from committed review: '+path
    for mode in ['true','false','input']:
        checkout=git('-c','core.autocrlf='+mode,'cat-file','--filters','HEAD:'+path)
        assert checkout==stored, f'Git autocrlf={mode} corrupts editor package fingerprint: {path}'
    print(hashlib.sha256(stored).hexdigest(),path)
print(f'PASS exact editor terrain bytes for {len(paths)} files under Windows/Unix/input checkout filters')
