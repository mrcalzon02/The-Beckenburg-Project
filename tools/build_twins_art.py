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
 "front":{"a":(17,280,128,616),"b":(126,280,231,616)},
 "side":{"a":(242,281,316,614),"b":(315,281,384,614)},
 # In the BACK source the pink sister is LEFT, blue sister RIGHT.
 # The former script swapped both identities and failed cross-view continuity.
 "back":{"a":(475,280,555,614),"b":(392,280,480,614)},
 # Three-quarter models are at the RIGHT of the panel. Old x557/x651
 # split their torsos and frequently removed both heads.
 "three_quarter":{"a":(587,281,676,615),"b":(672,281,756,615)}
}
CANVAS=(128,224)
def matte(source, box, facing, member):
    patch=np.asarray(source.crop(box).convert("RGB"))
    h,w=patch.shape[:2]
    seg=np.full((h,w),cv2.GC_PR_BGD,dtype=np.uint8)
    edge=max(3,min(w,h)//24)
    seg[:edge,:]=seg[-edge:,:]=cv2.GC_BGD
    seg[:,:edge]=seg[:,-edge:]=cv2.GC_BGD
    yy,xx=np.ogrid[:h,:w]
    seg[(yy>h*.035)&(yy<h*.97)&(xx>w*.14)&(xx<w*.86)]=cv2.GC_PR_FGD
    # Positive seeds stay inside the actual head, torso and skirt of the
    # individually identified sibling. Center-only seeds lose faces on
    # asymmetric three-quarter and rear poses.
    head_x={
        "front":{"a":.46,"b":.56},
        "side":{"a":.56,"b":.40},
        "back":{"a":.53,"b":.58},
        "three_quarter":{"a":.55,"b":.42},
    }[facing][member]
    torso_x={
        "front":{"a":.46,"b":.50},
        "side":{"a":.50,"b":.50},
        "back":{"a":.53,"b":.49},
        "three_quarter":{"a":.46,"b":.48},
    }[facing][member]
    seg[(yy>h*.12)&(yy<h*.20)&(np.abs(xx/w-head_x)<.055)]=cv2.GC_FGD
    seg[(yy>h*.32)&(yy<h*.54)&(np.abs(xx/w-torso_x)<.10)]=cv2.GC_FGD
    seg[(yy>h*.65)&(yy<h*.72)&(np.abs(xx/w-torso_x)<.08)]=cv2.GC_FGD
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
    # GrabCut can leave a convincing-looking central dress but silently lose
    # an entire head or feet. Check coverage in SOURCE coordinates, before
    # scaling: canvas-level "head pixels" can actually be displaced torso.
    if bb is None or bb[1]>45 or bb[3]<290:
        # Recover disconnected dark hair and shoes against the near-black
        # background of the canonical master, keeping each real silhouette
        # component rather than selecting only the largest surviving skirt.
        candidate=np.uint8((patch.max(axis=2)>25)*255)
        candidate[:edge,:]=candidate[-edge:,:]=0
        candidate[:,:edge]=candidate[:,-edge:]=0
        candidate=cv2.morphologyEx(candidate,cv2.MORPH_CLOSE,
            cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(5,5)))
        total,labels,stats,_=cv2.connectedComponentsWithStats(candidate,8)
        recovered=np.zeros_like(candidate)
        for component in range(1,total):
            if stats[component,cv2.CC_STAT_AREA]>=45:
                recovered[labels==component]=255
        rgba=Image.fromarray(np.dstack((patch,
            cv2.GaussianBlur(recovered,(3,3),.48))),"RGBA")
        bb=rgba.getbbox()
    if bb is None or bb[1]>45 or bb[3]<290:
        raise ValueError(f"Unacceptable head/feet crop {facing}/{member}: {bb}")
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
    manifest={"identity":"twins","version":"two-independent-source-cutouts-v3-full-silhouette",
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
            rgba=matte(board,rectangle,facing,member)
            pixels=np.asarray(rgba)
            count=int(np.count_nonzero(pixels[:,:,3]>=24))
            if not 1200<count<0.76*128*224:raise RuntimeError(f"Bad matte {facing}/{member}: {count}")
            top=int(np.count_nonzero(pixels[6:62,:,3]>=24))
            middle=int(np.count_nonzero(pixels[64:158,:,3]>=24))
            feet=int(np.count_nonzero(pixels[159:220,:,3]>=24))
            # Don't accept merely a severed skirt or an upper-body-only crop.
            if top<100 or middle<420 or feet<70:
                raise RuntimeError(
                    f"Head/body/feet missing from {facing}/{member}: "
                    f"top={top} torso={middle} feet={feet}")
            collage.alpha_composite(rgba,(column*128,row*224))
            contents=io.BytesIO()
            rgba.save(contents,format="PNG")
            filename=f"twins/{member}/{facing}.png"
            binary=contents.getvalue()
            entries.append((filename,binary))
            manifest["directions"][facing][member]={
                "path":filename,"source_crop":rectangle,
                "visible_pixels":count,"head_pixels":top,"torso_pixels":middle,
                "feet_pixels":feet,"sha256":hashlib.sha256(binary).hexdigest()}
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
