#!/usr/bin/env python3
"""Recover Castle sprites' *alpha* from their source-sampled RGB, not the old cutout.

The existing 256x448 runtime PNGs store original-master RGB even at alpha=0:
the prior repair sampled the source concept on the entire canvas, then copied
the damaged legacy cutout alpha. This restores a new edge map by graph-cut,
using the prior mask for location only. No masters or original RGB are edited.

Without --write: diagnostic previews only. With --write: backup-free atomic
repacking after verifying all 16 sprites; all source PNGs remain untouched.
"""
from __future__ import annotations
import argparse
import io
import json
from pathlib import Path
import zipfile
import cv2
import numpy as np
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"assets/runtime/castle-support-directions.zip"
PEOPLE=("elsbeth_vale","rowan_pike","tomas_greaves","sister_mirelle_ash")
VIEWS=("front","side","back","right")


def extract(rgba: np.ndarray):
    rgb=np.ascontiguousarray(rgba[:,:,:3].copy())
    previous=rgba[:,:,3]
    h,w=previous.shape
    old=(previous>90).astype("uint8")
    # The canonical turnaround is on charcoal, NOT on white paper. Estimate
    # the actual background from transparent border pixels in this *view*.
    border=np.zeros((h,w),np.uint8)
    border[:12,:]=1; border[-12:,:]=1
    border[:,:10]=1; border[:,-10:]=1
    samples=rgb[(border>0)&(previous<12)]
    if len(samples)<100:
        samples=rgb[previous<12]
    if len(samples)<100:
        raise ValueError("Missing untouched background RGB")
    background=np.median(samples,axis=0).astype(np.uint8)
    lab=cv2.cvtColor(rgb,cv2.COLOR_RGB2LAB).astype(np.float32)
    bg_lab=cv2.cvtColor(background.reshape((1,1,3)),cv2.COLOR_RGB2LAB)[0,0].astype(np.float32)
    color_delta=np.linalg.norm(lab-bg_lab,axis=2)
    raw_bg_delta=color_delta[(previous<12)&(border>0)]
    cutoff=float(np.clip(np.quantile(raw_bg_delta,.80)+8,15,43))
    candidate=(color_delta>cutoff).astype("uint8")
    grow=cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(25,25))
    near_old=cv2.dilate(old,grow)>0
    candidate &= near_old.astype("uint8")
    expanded=cv2.morphologyEx(np.maximum(old,candidate),
       cv2.MORPH_CLOSE,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(9,9)))
    near=cv2.dilate(expanded,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(17,17)))>0
    sure_fg=(cv2.erode((old>0).astype("uint8"),
       cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(7,7)))>0)&(color_delta>cutoff*.55)
    sure_bg=~cv2.dilate(expanded,cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(35,35))).astype(bool)
    classes=np.full((h,w),cv2.GC_PR_BGD,np.uint8)
    classes[near]=cv2.GC_PR_FGD
    classes[sure_bg]=cv2.GC_BGD
    classes[sure_fg]=cv2.GC_FGD
    if (classes==cv2.GC_FGD).sum()<150 or (classes==cv2.GC_BGD).sum()<150:
        raise ValueError("Unusable segmentation training seeds")
    bgr=cv2.cvtColor(rgb,cv2.COLOR_RGB2BGR)
    bgm=np.zeros((1,65),np.float64)
    fgm=np.zeros((1,65),np.float64)
    cv2.grabCut(bgr,classes,None,bgm,fgm,7,cv2.GC_INIT_WITH_MASK)
    fg=np.isin(classes,(cv2.GC_FGD,cv2.GC_PR_FGD)).astype(np.uint8)
    # Keep the painted body (plus attached straps) but exclude disconnected
    # captions, tiny black labels and stray source-sheet panel lines.
    n, cc, stats, centers=cv2.connectedComponentsWithStats(fg,8)
    if n<=1:raise ValueError("No connected character silhouette")
    large=max(stats[1:,cv2.CC_STAT_AREA])
    keep=np.zeros((h,w),np.uint8)
    for k in range(1,n):
        area=int(stats[k,cv2.CC_STAT_AREA])
        # Body, lower separated boots, and larger source accessories.
        near_existing=(np.count_nonzero((cc==k)&(old>0))>max(10,area*.18))
        if area >= max(85,large*.015) and near_existing:
            keep[cc==k]=1
    keep=cv2.morphologyEx(keep,cv2.MORPH_CLOSE,
         cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(3,3)))
    # Remove only *enclosed* background holes whose pixels aren't genuinely
    # dark/source background. A plain flood fill over the pants would erase them.
    from scipy.ndimage import binary_fill_holes
    filled=binary_fill_holes(keep>0)
    holes=filled & (keep==0)
    keep[holes & (color_delta>cutoff*.7)]=1
    alpha=cv2.GaussianBlur(keep.astype(np.float32)*255,(3,3),.65)
    alpha=np.clip(np.rint(alpha),0,255).astype(np.uint8)
    old_area=int((previous>120).sum())
    new_area=int((alpha>120).sum())
    ratio=new_area/max(1,old_area)
    bbox=cv2.boundingRect((alpha>120).astype("uint8"))
    # Diagnostic pass must produce ALL candidate previews, including large
    # corrections to a badly perforated old alpha. Only reject catastrophic
    # matches here; strict per-character visual approval precedes ZIP writing.
    if not(.48<=ratio<=2.2) or bbox[3]<h*.60 or bbox[2]<w*.15:
        raise ValueError(f"Catastrophic silhouette area/bounds ratio={ratio:.3f} bbox={bbox}")
    output=np.dstack((rgb,alpha))
    return output,{
        "background_rgb":background.tolist(),"cutoff_lab":round(cutoff,2),
        "old_opaque_pixels":old_area,"new_opaque_pixels":new_area,
        "foreground_ratio":round(ratio,3),"new_bbox":list(bbox),
        "old_alpha_zero_new_foreground":int(((previous<32)&(alpha>120)).sum()),
        "old_alpha_hole_fill":int(((previous<120)&(alpha>120)).sum())}


def preview(old:Image.Image,new:Image.Image,path:Path):
    from PIL import ImageDraw
    w,h=old.size
    canvas=Image.new("RGB",(w*3,h*2),(26,32,39))
    originals=[old,new]
    backgrounds=("#d4c6b0","#2a373e","#b94caa")
    for row,img in enumerate(originals):
        for col,bg in enumerate(backgrounds):
            dest=Image.new("RGBA",(w,h),bg)
            dest.alpha_composite(img)
            canvas.paste(dest.convert("RGB"),(col*w,row*h))
    canvas.thumbnail((1000,850),Image.Resampling.LANCZOS)
    canvas.save(path,quality=88,optimize=True)


def run(out:Path,write:bool):
    out.mkdir(parents=True,exist_ok=True)
    before=ASSETS.read_bytes()
    zipin=zipfile.ZipFile(io.BytesIO(before))
    old={f.filename:zipin.read(f) for f in zipin.infolist() if not f.is_dir()}
    updated={}
    report=[]
    for person in PEOPLE:
        for view in VIEWS:
            name=f"assets/castle_support/{person}/{view}.png"
            image=Image.open(io.BytesIO(old[name])).convert("RGBA")
            if image.size != (256,448):
                raise ValueError(f"Expected original RGB 256x448 for {name}")
            cutout,stats=extract(np.array(image))
            result=Image.fromarray(cutout,"RGBA")
            data=io.BytesIO();result.save(data,"PNG",optimize=True)
            updated[name]=data.getvalue()
            preview(image,result,out/f"{person}-{view}-matte-review.jpg")
            report.append({"identity":person,"view":view,**stats})
            print("MATTE",person,view,stats,flush=True)
    (out/"diagnostics.json").write_text(json.dumps(report,indent=2)+"\n")
    if write:
        dest=ASSETS.with_suffix(".alpha-repaired.tmp")
        try:
            with zipfile.ZipFile(dest,"w",zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
                for path,data in old.items():archive.writestr(path,updated.get(path,data))
            with zipfile.ZipFile(dest) as check:
                assert check.testzip() is None
                assert set(check.namelist())==set(old)
            dest.replace(ASSETS)
        finally:dest.unlink(missing_ok=True)
        print("ALPHA_REPAIRED_ALL_16_FRAMES",flush=True)
    else:
        print("PREVIEW_ONLY_NO_RUNTIME_ASSET_MUTATION",flush=True)


if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--out",type=Path,required=True)
    p.add_argument("--write",action="store_true")
    a=p.parse_args()
    run(a.out,a.write)
