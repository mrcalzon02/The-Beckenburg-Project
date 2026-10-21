#!/usr/bin/env python3
"""Build TWO distinct Twins actor sprites from each canonical paired turnaround.

Do not upscale the entire pair as one 64x112 actor. Each member gets an
individually matted 128x224 PNG in all four authored facing categories.
Tether belongs to gameplay/VFX, never to the cutout's opaque silhouette.
Source is the canonical full-resolution Twins profile checked into Git.
"""
from __future__ import annotations
import hashlib,io,json,zipfile
from pathlib import Path
import cv2
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"assets/concept-art/v001/03-ghosts/twins-ghost-profile.png"
TARGET=ROOT/"assets/runtime/twins-directions.zip"
MANIFEST=ROOT/"assets/runtime/twins-directions-manifest.json"
PREVIEW=ROOT/"development/verification/twins-two-body-turnarounds.png"

# Full-resolution master sheet 1448x1086. The four columns depict a PAIR,
# each split around the two individually identifiable blue/red members.
# Tight sub-rectangles avoid all captions, closeups, and the other member.
RECTS={
 "front":{"a":(16,280,119,616),"b":(108,280,224,616)},
 "side":{"a":(237,283,311,614),"b":(307,283,383,614)},
 "back":{"a":(390,278,473,611),"b":(467,278,556,611)},
 "three_quarter":{"a":(557,280,663,615),"b":(651,280,754,615)}
}
CANVAS=(128,224)
def matte(source, box):
    patch=np.asarray(source.crop(box).convert("RGB"))
    h,w=patch.shape[:2]
    seg=np.full((h,w),cv2.GC_PR_BGD,dtype=np.uint8)
    edge=max(3,min(w,h)//24)
    seg[:edge,:]=seg[-edge:,:]=cv2.GC_BGD
    seg[:,:edge]=seg[:,-edge:]=cv2.GC_BGD
    yy,xx=np.ogrid[:h,:w]
    seg[(yy>h*.035)&(yy<h*.97)&(xx>w*.14)&(xx<w*.86)]=cv2.GC_PR_FGD
    seg[(yy>h*.18)&(yy<h*.51)&(xx>w*.36)&(xx<w*.63)]=cv2.GC_FGD
    bg=np.zeros((1,65),np.float64);fg=np.zeros((1,65),np.float64)
    cv2.grabCut(cv2.cvtColor(patch,cv2.COLOR_RGB2BGR),seg,None,bg,fg,6,cv2.GC_INIT_WITH_MASK)
    opaque=np.where((seg==cv2.GC_FGD)|(seg==cv2.GC_PR_FGD),255,0).astype(np.uint8)
    count,labels,stats,_=cv2.connectedComponentsWithStats(opaque,8)
    if count<=1:raise ValueError("empty ghost")
    # The figure is one connected silhouette; isolated tether fragments are removed.
    chosen=max(range(1,count),key=lambda i:int(stats[i,cv2.CC_STAT_AREA]))
    opaque=np.where(labels==chosen,255,0).astype(np.uint8)
    opaque=cv2.GaussianBlur(opaque,(3,3),.48)
    rgba=Image.fromarray(np.dstack((patch,opaque)),"RGBA")
    bb=rgba.getbbox()
    if bb is None:raise ValueError("empty alpha")
    rgba=rgba.crop(bb)
    factor=min(116/rgba.width,214/rgba.height)
    nw,nh=max(1,round(rgba.width*factor)),max(1,round(rgba.height*factor))
    dest=Image.new("RGBA",CANVAS,(0,0,0,0))
    dest.alpha_composite(rgba.resize((nw,nh),Image.Resampling.LANCZOS),((128-nw)//2,224-nh))
    return dest

def main():
    original=SOURCE.read_bytes()
    board=Image.open(io.BytesIO(original)).convert("RGB")
    if board.size!=(1448,1086):
        raise RuntimeError(f"Unexpected canonical Twins master dimensions: {board.size}")
    collage=Image.new("RGBA",(4*128,2*224),(24,24,28,255))
    manifest={"identity":"twins","version":"two-independent-source-cutouts-v1",
        "source":SOURCE.relative_to(ROOT).as_posix(),
        "source_sha256":hashlib.sha256(original).hexdigest(),
        "directions":{},"canvas":CANVAS}
    entries=[]
    for column,(facing,members) in enumerate(RECTS.items()):
        manifest["directions"][facing]={}
        for row,member in enumerate(("a","b")):
            rectangle=members[member]
            assert 0<=rectangle[0]<rectangle[2]<=board.width
            assert 0<=rectangle[1]<rectangle[3]<=board.height
            rgba=matte(board,rectangle)
            pixels=np.asarray(rgba)
            count=int(np.count_nonzero(pixels[:,:,3]>=24))
            if not 600<count<0.76*128*224:raise RuntimeError(f"Bad matte {facing}/{member}: {count}")
            collage.alpha_composite(rgba,(column*128,row*224))
            contents=io.BytesIO()
            rgba.save(contents,format="PNG")
            filename=f"twins/{member}/{facing}.png"
            binary=contents.getvalue()
            entries.append((filename,binary))
            manifest["directions"][facing][member]={
                "path":filename,"source_crop":rectangle,
                "visible_pixels":count,"sha256":hashlib.sha256(binary).hexdigest()}
    TARGET.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(TARGET,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for name,data in entries:
            info=zipfile.ZipInfo(name,(2026,10,21,0,0,0))
            info.compress_type=zipfile.ZIP_DEFLATED
            z.writestr(info,data)
    PREVIEW.parent.mkdir(parents=True,exist_ok=True)
    collage.convert("RGB").save(PREVIEW)
    manifest["archive_sha256"]=hashlib.sha256(TARGET.read_bytes()).hexdigest()
    MANIFEST.write_text(json.dumps(manifest,indent=2)+"\n")
    assert len(entries)==8
    print("PASS: 8 independently matted original Twins source views (A and B, four directions)")

if __name__=="__main__":main()
