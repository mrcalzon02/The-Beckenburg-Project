#!/usr/bin/env python3
"""Read the original shipped Hollow Creek billboard GLBs and PNGs, not proxies."""
from pathlib import Path
import hashlib,io,json,struct
from PIL import Image

root=Path(__file__).resolve().parents[1]
out=root/'build'/'hollow-creek-billboard-inspection'
out.mkdir(parents=True,exist_ok=True)
models=sorted((root/'assets/runtime/models').glob('level_hc_native_*.glb'))
report=[]
for source in models:
    binary=source.read_bytes()
    if binary[:4]!=b'glTF':
        raise ValueError('Not original glTF '+source.name)
    n=struct.unpack_from('<I',binary,12)[0]
    meta=json.loads(binary[20:20+n].decode())
    off=20+n
    bin_size,kind=struct.unpack_from('<II',binary,off)
    if kind!=0x004e4942: raise ValueError('Missing BIN '+source.name)
    binpart=binary[off+8:off+8+bin_size]
    tris=sum(int(meta['accessors'][p['indices']]['count'])//3
        for mesh in meta['meshes'] for p in mesh['primitives'])
    if tris!=2: continue
    entry={
        'name':source.stem,
        'sha256':hashlib.sha256(binary).hexdigest(),
        'triangles':tris,
        'material_alpha_modes':[],
        'image_alpha_stats':[],
    }
    for i,material in enumerate(meta['materials']):
        mode=material.get('alphaMode','OPAQUE')
        cutoff=material.get('alphaCutoff',0.5)
        factor=material.get('pbrMetallicRoughness',{}).get('baseColorFactor',[1,1,1,1])
        entry['material_alpha_modes'].append({'name':material.get('name'),'mode':mode,'cutoff':cutoff,'factor':factor})
    for k,tex in enumerate(meta.get('images',[])):
        view=meta['bufferViews'][tex['bufferView']]
        raw=binpart[view.get('byteOffset',0):view.get('byteOffset',0)+view['byteLength']]
        image=Image.open(io.BytesIO(raw)).convert('RGBA')
        alpha=image.getchannel('A')
        histogram=alpha.histogram()
        corners=[image.getpixel(p) for p in ((0,0),(image.width-1,0),(0,image.height-1),(image.width-1,image.height-1))]
        entry['image_alpha_stats'].append({
            'size':image.size,'sha256':hashlib.sha256(raw).hexdigest(),
            'alpha_zero':histogram[0],'alpha_partial':sum(histogram[1:255]),
            'alpha_opaque':histogram[255],'corners':corners,
            'colour_at_0_0':image.getpixel((0,0)),
        })
        if source.stem in {'level_hc_native_284_0','level_hc_native_300_0','level_hc_native_350_0'}:
            image.save(out/f'{source.stem}_source_texture_{k}.png')
    report.append(entry)
(out/'source-tree-billboard-audit.json').write_text(json.dumps(report,indent=2)+'\n')
if len(report)<50: raise ValueError(f'Expected 50+ original 2-triangle source billboards, saw {len(report)}')
mode_counts={}
for row in report:
    for m in row['material_alpha_modes']:
        mode_counts[m['mode']]=mode_counts.get(m['mode'],0)+1
print('PASS original source inspection: tree billboards=',len(report),'glTF alpha modes=',mode_counts)
print('SAMPLE',json.dumps(report[:3]))
