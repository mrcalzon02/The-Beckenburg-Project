#!/usr/bin/env python3
"""Remove verified detached line-alpha fragments from four Twins ghost facings.
Preserve all original RGB, 181 other PNGs, eight directions and source fades.
The runtime remains the original nine-part v3 ZIP, not a substitute texture.
"""
from pathlib import Path
from PIL import Image
import cv2
import numpy as np
import io, zipfile, hashlib, json

ROOT=Path(__file__).resolve().parents[1]
PARTS=ROOT/'assets/runtime/ghost-sprite-parts'
REPORT=ROOT/'development/verification/twins-source-line-alpha-repair.json'
PREVIEW=ROOT/'development/verification/twins-source-line-alpha-repair.png'
SOURCE_SHA='e5a2a80d25e7d003ed3ab109fc42316d8fe745bce199390d92de60f68adfa3cc'
STRIPS={'front_right':(0,18),'right':(0,18),'left':(46,64),'front_left':(46,64)}
W,H=64,112

def repair_png(binary,facing):
    original=Image.open(io.BytesIO(binary))
    if original.mode!='P' or original.size!=(W*4,H*8):
        raise ValueError('Expected real indexed Twins 4x8 sheet: '+facing)
    palette=original.getpalette()
    transparent=original.info.get('transparency')
    ncolors=len(palette)//3
    if not isinstance(transparent,bytes) or len(transparent)!=ncolors or ncolors*2>256:
        raise ValueError('Unexpected original indexed palette')
    source=np.asarray(original.convert('RGBA'))
    indices=np.asarray(original,dtype=np.uint8)
    if np.any(indices>=ncolors):raise ValueError('Unknown source color index')
    removed=np.zeros(indices.shape,dtype=bool)
    strong=0
    components=0
    frames=0
    faded=0
    xmin,xmax=STRIPS[facing]
    for row in range(8):
        for col in range(4):
            y,x=row*H,col*W
            alpha=source[y:y+H,x:x+W,3]
            count,labels,stats,_=cv2.connectedComponentsWithStats(np.uint8(alpha>=24),8)
            if count<2:continue
            principal=max(range(1,count),key=lambda i:int(stats[i,cv2.CC_STAT_AREA]))
            if int(stats[principal,cv2.CC_STAT_AREA])<400:
                faded+=1 # genuine dissipating source, preserve
                continue
            candidate=np.zeros(alpha.shape,dtype=np.uint8)
            for k in range(1,count):
                if k==principal:continue
                xx,yy,ww,hh,area=map(int,stats[k])
                if area>=15 and hh>=9 and ww<=9 and xmin<=xx and xx+ww<=xmax and 35<=yy<=96:
                    candidate[labels==k]=1
                    components+=1
            if not np.any(candidate):continue
            margin=cv2.dilate(candidate,np.ones((3,3),np.uint8),iterations=1)>0
            body=cv2.dilate(np.uint8(labels==principal),np.ones((3,3),np.uint8),iterations=1)>0
            # Remove disconnected subpixel crumbs left in the same source
            # strip, but never touch the principal sisters' body silhouette.
            stray=(labels>0)&(labels!=principal)
            stray[:35,:]=False
            stray[97:,:]=False
            stray[:,:xmin]=False
            stray[:,xmax:]=False
            margin |= stray
            margin &= ~body
            margin[:,:xmin]=False
            margin[:,xmax:]=False
            clear=margin&(alpha>0)
            if not np.any(clear):continue
            removed[y:y+H,x:x+W] |= clear
            strong+=int(np.count_nonzero(clear&(alpha>=24)))
            frames+=1
    # Source atlas verification, not just a computed alpha-only preview.
    if strong<2500 or components<25 or frames<24:
        raise ValueError(f'Expected source line missing in {facing}: {strong}/{components}/{frames}')
    alias=indices.copy()
    alias[removed]=alias[removed]+ncolors
    output=Image.fromarray(alias,'P')
    output.putpalette(palette+palette)
    output.info['transparency']=transparent+bytes(ncolors)
    buf=io.BytesIO()
    output.save(buf,format='PNG',compress_level=9)
    actual=np.asarray(Image.open(io.BytesIO(buf.getvalue())).convert('RGBA'))
    if not np.array_equal(actual[:,:,:3],source[:,:,:3]):
        raise AssertionError('Source RGB was altered')
    if not np.array_equal(actual[:,:,3][~removed],source[:,:,3][~removed]):
        raise AssertionError('Non-artifact alpha was altered')
    if np.any(actual[:,:,3][removed]):
        raise AssertionError('Detached source alpha remains')
    return buf.getvalue(),{'facing':facing,'visible_line_pixels_removed':strong,
        'detached_components_removed':components,'frames_changed':frames,
        'intentionally_faded_frames_preserved':faded,'source_rgb_preserved':True}

def main():
    files=[PARTS/('ghost-sprite-expansion-v3.zip.part%02d'%i) for i in range(9)]
    if any(not p.is_file() for p in files):raise ValueError('Missing v3 source part')
    parts=[p.read_bytes() for p in files]
    old=b''.join(parts)
    sha=hashlib.sha256(old).hexdigest()
    if REPORT.is_file():
        prior=json.loads(REPORT.read_text(encoding='utf-8'))
        if prior.get('repaired_archive_sha256')==sha:
            print('PASS already patched original Twins archive')
            return
    if sha!=SOURCE_SHA:raise ValueError('Unrecognized original v3 archive: '+sha)
    with zipfile.ZipFile(io.BytesIO(old)) as archive:
        entries={name:archive.read(name) for name in archive.namelist()}
        infos=list(archive.infolist())
    if len(entries)<180:raise ValueError('Missing other source ghost identities')
    patches={}
    reports=[]
    collage=Image.new('RGBA',(4*128,224),(80,80,80,255))
    for col,facing in enumerate(STRIPS):
        key=f'frames/twins/{facing}.png'
        if key not in entries:raise ValueError('Missing Twins '+facing)
        patches[key],summary=repair_png(entries[key],facing)
        reports.append(summary)
        for row,payload in [(0,entries[key]),(112,patches[key])]:
            image=Image.open(io.BytesIO(payload)).convert('RGBA').crop((0,0,W,H))
            collage.alpha_composite(image,(col*128,row))
    PREVIEW.parent.mkdir(parents=True,exist_ok=True)
    collage.save(PREVIEW)
    stream=io.BytesIO()
    with zipfile.ZipFile(stream,'w') as archive:
        for info in infos:
            archive.writestr(info,patches.get(info.filename,entries[info.filename]))
    new=stream.getvalue()
    with zipfile.ZipFile(io.BytesIO(new)) as archive:
        if len(archive.namelist())!=len(entries):
            raise AssertionError('Original ghost catalog changed')
        for name,payload in entries.items():
            if name not in patches and archive.read(name)!=payload:
                raise AssertionError('Other original sprite altered: '+name)
    # The prior Possessive fix distributed the ZIP evenly across all nine
    # parts. Re-split after recompression: their original byte offsets are
    # not meaningful; ordered concatenation is the only runtime contract.
    old_cap=len(parts[0])
    if any(len(x)!=old_cap for x in parts[:-1]):
        raise ValueError('Original nine-part boundary layout invalid')
    split=(len(new)+8)//9
    if split>786432:
        raise ValueError('Repaired ZIP exceeds GitHub-safe nine-part budget')
    chunks=[new[i*split:(i+1)*split] for i in range(9)]
    if not all(chunks) or b''.join(chunks)!=new:
        raise AssertionError('Rebalanced nine-part archive does not roundtrip')
    for p,data in zip(files,chunks):
        p.write_bytes(data)
    record={'original_archive_sha256':sha,
            'repaired_archive_sha256':hashlib.sha256(new).hexdigest(),
            'repaired_pngs':sorted(patches),
            'unchanged_png_entries':len(entries)-len(patches),
            'original_rgb_preserved':True,
            'in_game_visual_approval':'pending new four-angle DEV renderer screenshots',
            'repairs':reports}
    REPORT.write_text(json.dumps(record,indent=2)+'\n',encoding='utf-8')
    print('PASS source Twins repair, %d line pixels removed; %d other PNGs identical'
          %(sum(x['visible_line_pixels_removed'] for x in reports),len(entries)-len(patches)))

if __name__=='__main__':main()
