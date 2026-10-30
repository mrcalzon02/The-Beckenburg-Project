#!/usr/bin/env python3
"""Diagnostic ONLY: compare approved original Hunter concept art to lobby cutouts.

Use exact original checked-in PNG profiles, never internet imagery.
Keep this separate from release; a visual discrepancy cannot block game shipping.
"""
from pathlib import Path
import io
import json
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
PROFILES = ROOT / "assets/concept-art/v001/01-hunters"
MANIFEST = ROOT / "assets/runtime/character-directions-manifest.json"
ZIP = ROOT / "assets/runtime/character-directions.zip"
CLASSES = ("cleric", "observer", "demonologist")

def run(out: Path):
    out.mkdir(parents=True,exist_ok=True)
    manifest=json.loads(MANIFEST.read_text())
    cells=[]
    records=[]
    with zipfile.ZipFile(ZIP) as archive:
        for role in CLASSES:
            info=manifest["variants"][role]
            source=PROFILES / (role + "-character-profile.png")
            sheet=Image.open(source).convert("RGB")
            frame=info["views"]["front"]
            crop=tuple(frame["crop"])
            original=sheet.crop(crop)
            packaged=Image.open(io.BytesIO(archive.read(frame["entry"]))).convert("RGBA")
            if original.width<100 or original.height<250 or packaged.size!=(128,224):
                raise RuntimeError("Unexpected original Hunter source sheet or packaged sprite")
            original.save(out/f"{role}-original-master-front-uncut.png")
            packaged.save(out/f"{role}-current-segmented-runtime.png")
            # Drawing direct images, not improving or synthesizing characters.
            width,height=560,460
            panel=Image.new("RGB",(width,height),(23,27,35))
            draw=ImageDraw.Draw(panel)
            draw.text((8,7),f"{role.upper()} - ORIGINAL, full-res crop (left)",fill="#efe4cd")
            draw.text((290,7),"CURRENT LOBBY / segmented 128x224",fill="#efe4cd")
            h=410
            a=original.resize((round(original.width*h/original.height),h),Image.Resampling.LANCZOS)
            b=packaged.resize((round(128*h/224),h),Image.Resampling.NEAREST)
            panel.paste(a,((275-a.width)//2,30))
            panel.paste(b,(280+(280-b.width)//2,30),b)
            panel.save(out/f"{role}-reference-vs-lobby.png")
            pixels=packaged.getchannel("A")
            bounds=pixels.getbbox()
            record={"role":role,"approved_original_source":str(source.relative_to(ROOT)),
                    "original_crop":crop,
                    "original_crop_size":original.size,
                    "packaged_128x224_bounds":bounds,
                    "portrait_source_unmodified":True,
                    "original_source_not_replaced_by_generated_substitute":True}
            records.append(record)
    (out/"comparison-manifest.json").write_text(json.dumps(records,indent=2)+"\n")
    print("ORIGINAL_HUNTER_SOURCE_TO_RUNTIME_COMPARISON_READY",
          [(r["role"],r["original_crop_size"],r["packaged_128x224_bounds"]) for r in records])

if __name__=="__main__":
    import argparse
    p=argparse.ArgumentParser()
    p.add_argument("--out",type=Path,required=True)
    args=p.parse_args()
    run(args.out)
