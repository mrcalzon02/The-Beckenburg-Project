"""Index committed development captures without copying or altering originals.
Run before each map/model checkpoint; --check is the CI stale-index gate.
"""
from pathlib import Path
import argparse, hashlib, html, json, struct, subprocess
from urllib.parse import quote
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
paths=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z','development'],cwd=ROOT).decode('utf-8').split('\0')
items=[]
for path in sorted(set(paths)):
    if not path.lower().endswith('.png'):continue
    if not any(t in path for t in ['/previews/','/captures/','/screenshots/','/blockouts/']):continue
    p=ROOT/path
    if not p.exists():continue
    data=p.read_bytes()
    if data[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Invalid PNG: '+path)
    w,h=struct.unpack('>II',data[16:24])
    kind='Development review'
    if '/blockouts/' in path:kind='Rough blockout'
    elif '-blender-' in path:kind='Native scene overview'
    elif 'studio' in p.stem:kind='Model inspection'
    elif any(t in path for t in ['/runtime/','-runtime/','/blackpine/','/mill-lighting/','/mill-expansion/','/van/','/optical-evidence/']):kind='Runtime capture'
    elif p.stem.startswith('level_') or '/model-production/' in path or '/equipment-catalog/' in path:kind='Model inspection'
    title=p.stem.replace('_',' ').replace('-',' ').strip().capitalize()
    items.append(dict(path=path,url=quote(path,safe='/'),title=title,kind=kind,width=w,height=h,bytes=len(data),sha256=hashlib.sha256(data).hexdigest()))
manifest=dict(version=1,title='Beckenburg development gallery',count=len(items),images=items)
encoded=json.dumps(manifest,indent=2,ensure_ascii=False)+'\n'
template=(ROOT/'development/gallery/template.html').read_text(encoding='utf-8')
page=template.replace('GALLERY_DATA',json.dumps(manifest,ensure_ascii=False).replace('<','\\u003c'))
outputs={ROOT/'development/gallery/index.json':encoded,ROOT/'beckenburg-development-gallery.html':page}
for p,text in outputs.items():
    if args.check:
        if not p.exists() or p.read_text(encoding='utf-8')!=text:raise SystemExit('Stale gallery: run python tools/build_development_gallery.py')
    else:p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(text.encode('utf-8'))
print('PASS development gallery:',len(items),'original PNGs, valid dimensions and SHA-256; index '+('current' if args.check else 'generated'))
