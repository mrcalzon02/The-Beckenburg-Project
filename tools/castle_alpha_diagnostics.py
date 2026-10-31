#!/usr/bin/env python3
"""Small, reviewable PREVIEWS of the original four Castle master sheets.

Only previews are committed; do not touch masters or runtime sprite assets.
"""
from pathlib import Path
import io
import zipfile
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
MASTERS={
 "elsbeth_vale":"elsbeth-vale",
 "rowan_pike":"rowan-pike",
 "tomas_greaves":"tomas-greaves",
 "sister_mirelle_ash":"sister-mirelle-ash",
}
ARCHIVE=ROOT/"assets/runtime/castle-support-directions.zip"

def main():
    dest=ROOT/"development/quality/castle-resident-art/source-diagnostic/previews"
    dest.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(ARCHIVE) as source:
        for identity,master in MASTERS.items():
            image=Image.open(ROOT/"assets/concept-art/v001/04-npcs"/(master+"-master-concept.png")).convert("RGB")
            print("MASTER",identity,image.size,flush=True)
            image.thumbnail((800,630),Image.Resampling.LANCZOS)
            image.save(dest/(master+"-master-overview.jpg"),quality=75,optimize=True)
            reference=Image.open(io.BytesIO(source.read(
                f"assets/castle_support/{identity}/front.png"))).convert("RGBA")
            ref=reference.resize((256,448),Image.Resampling.LANCZOS)
            canvas=Image.new("RGBA",(768,448),"#d6c8b8")
            for x,bg in [(0,"#e0dfd4"),(256,"#20272b"),(512,"#e34ccc")]:
                canvas.paste(Image.new("RGBA",(256,448),bg),(x,0))
                canvas.alpha_composite(ref,(x,0))
            canvas.convert("RGB").save(dest/(master+"-current-front-matte.png"),optimize=True)
    print("MASTER_PREVIEWS_READY",len(MASTERS)*2)

if __name__=="__main__":main()
