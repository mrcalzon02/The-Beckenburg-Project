#!/usr/bin/env python3
"""Extract four ACTUAL approved master-sheet turnarounds; no synthetic animation poses.

Standalone offline asset-authoring step. Production inputs are the immutable PNG
masters already checked into assets/concept-art. Every ZIP entry represents
exactly one human-authored master-sheet view with an alpha segmentation mask.
No color variants, mirrors, animated loops, or substitute character identities.
"""
from __future__ import annotations
import hashlib
import io
import json
from pathlib import Path
import zipfile
import cv2
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
# Pixel rectangles were audited on the 1448x1086 Hunter and 1536x1024 Elder
# master boards. Order on every sheet: front, side, back, three-quarter.
SOURCES={
 "cleric":("assets/concept-art/v001/01-hunters/cleric-character-profile.png",
    [(24,300,218,727),(227,300,379,727),(385,300,572,727),(581,300,747,727)]),
 "observer":("assets/concept-art/v001/01-hunters/observer-character-profile.png",
    [(24,294,206,685),(215,294,370,685),(383,294,554,685),(574,294,735,685)]),
 "demonologist":("assets/concept-art/v001/01-hunters/demonologist-character-profile.png",
    [(23,291,208,690),(215,291,371,690),(380,291,544,690),(550,291,734,690)]),
 "elder_blade_demon":("assets/concept-art/v001/03-ghosts/elder-blade-demon-master-concept.png",
    [(18,263,240,638),(243,263,407,638),(411,263,611,638),(614,263,865,638)]),
 "elder_plague_demon":("assets/concept-art/v001/03-ghosts/elder-plague-demon-master-concept.png",
    [(16,235,258,581),(260,235,432,581),(435,235,641,581),(645,235,848,581)]),
 "elder_nothic":("assets/concept-art/v001/03-ghosts/elder-nothic-master-concept.png",
    [(16,186,283,515),(286,186,475,515),(478,186,705,515),(710,186,931,515)]),
}
DIRECTIONS=("front","side","back","three_quarter")
DEST=ROOT/"assets/runtime/character-directions.zip"
REPORT=ROOT/"assets/runtime/character-directions-manifest.json"
CONTACT=ROOT/"development/verification/character-directions-preview.png"

def extract(board: Image.Image, rect: tuple[int,int,int,int]) -> Image.Image:
    crop=np.asarray(board.crop(rect).convert("RGB"))
    height,width=crop.shape[:2]
    # Dark source plate: segment character rather than shipping black matte panels.
    mask=np.full((height,width),cv2.GC_PR_BGD,dtype=np.uint8)
    edge=max(7,width//28)
    mask[:edge,:]=mask[-edge:,:]=cv2.GC_BGD
    mask[:,:edge]=mask[:,-edge:]=cv2.GC_BGD
    yy,xx=np.ogrid[:height,:width]
    mask[(xx>width*.13)&(xx<width*.87)&(yy>height*.035)&(yy<height*.975)]=cv2.GC_PR_FGD
    # Solid torso/head cores lock the coherent source silhouette.
    mask[(xx>width*.42)&(xx<width*.58)&(yy>height*.24)&(yy<height*.58)]=cv2.GC_FGD
    mask[(xx>width*.46)&(xx<width*.54)&(yy>height*.15)&(yy<height*.21)]=cv2.GC_FGD
    # Dark pants can otherwise be incorrectly classified as black background.
    mask[(xx>width*.38)&(xx<width*.44)&(yy>height*.65)&(yy<height*.86)]=cv2.GC_FGD
    mask[(xx>width*.56)&(xx<width*.62)&(yy>height*.65)&(yy<height*.86)]=cv2.GC_FGD
    bg=np.zeros((1,65),np.float64)
    fg=np.zeros((1,65),np.float64)
    cv2.grabCut(cv2.cvtColor(crop,cv2.COLOR_RGB2BGR),mask,None,bg,fg,5,cv2.GC_INIT_WITH_MASK)
    alpha=np.where((mask==cv2.GC_FGD)|(mask==cv2.GC_PR_FGD),255,0).astype(np.uint8)
    count,components,stats,_=cv2.connectedComponentsWithStats(alpha,8)
    if count<=1: raise ValueError("Source panel has no foreground")
    largest=max(range(1,count),key=lambda i:int(stats[i,cv2.CC_STAT_AREA]))
    alpha=np.where(components==largest,255,0).astype(np.uint8)
    alpha=cv2.GaussianBlur(alpha,(3,3),.55)
    # Keep source pose registration: all four views share a fixed 128x224 canvas.
    content=Image.fromarray(np.dstack((crop,alpha)),"RGBA")
    extent=content.getbbox()
    if extent is None: raise ValueError("Empty direction after matte removal")
    content=content.crop(extent)
    target=Image.new("RGBA",(128,224),(0,0,0,0))
    scale=min(115/content.width,215/content.height)
    nw=max(1,round(content.width*scale))
    nh=max(1,round(content.height*scale))
    resized=content.resize((nw,nh),Image.Resampling.LANCZOS)
    target.alpha_composite(resized,((128-nw)//2,224-nh))
    assert 128==target.width and 224==target.height
    return target

def main():
    output={}
    review=Image.new("RGBA",(128*4,224*6),(28,28,30,255))
    compressed=[]
    for row,(identity,(path,rects)) in enumerate(SOURCES.items()):
        data=(ROOT/path).read_bytes()
        board=Image.open(io.BytesIO(data)).convert("RGB")
        assert len(rects)==4
        assert all(0<=a<c<=board.width and 0<=b<d<=board.height for a,b,c,d in rects)
        item={"source":path,"source_sha256":hashlib.sha256(data).hexdigest(),"views":{}}
        for col,(direction,bounds) in enumerate(zip(DIRECTIONS,rects)):
            sprite=extract(board,bounds)
            px=np.asarray(sprite)
            visible=int(np.count_nonzero(px[:,:,3]>=24))
            if visible<850 or visible>128*224*.86:
                raise ValueError(f"Rejected {identity}/{direction} visibility {visible}")
            png=io.BytesIO()
            sprite.save(png,format="PNG",optimize=False)
            name=f"assets/{identity}/{direction}.png"
            contents=png.getvalue()
            compressed.append((name,contents))
            item["views"][direction]={"entry":name,"bytes":len(contents),
                                      "sha256":hashlib.sha256(contents).hexdigest(),
                                      "visible_pixels":visible,"crop":list(bounds)}
            review.alpha_composite(sprite,(col*128,row*224))
        output[identity]=item
    DEST.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(DEST,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        for name,data in sorted(compressed):
            spec=zipfile.ZipInfo(name,date_time=(2026,10,20,0,0,0))
            spec.compress_type=zipfile.ZIP_DEFLATED
            archive.writestr(spec,data,compress_type=zipfile.ZIP_DEFLATED,compresslevel=9)
    CONTACT.parent.mkdir(parents=True,exist_ok=True)
    review.convert("RGB").save(CONTACT)
    manifest={"format":"approved-master-turnaround-only-v1","directions":list(DIRECTIONS),
              "size":[128,224],"variants":output,"archive_sha256":hashlib.sha256(DEST.read_bytes()).hexdigest()}
    REPORT.write_text(json.dumps(manifest,indent=2)+"\n",encoding="utf-8")
    assert len(compressed)==24
    print(f"PASS: {len(compressed)} master-derived static directions for {len(SOURCES)} named appearances, ZIP {DEST.stat().st_size} bytes")

if __name__=="__main__":
    main()
