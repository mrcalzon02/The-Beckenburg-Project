"""Slice generated 4x3 pose sheets into the shipped hunter atlas; preserve source PNGs."""
from pathlib import Path
import hashlib
import io
import json
import zipfile
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "art/hunters/animation-v1"
VIEWS = ("front", "side", "back", "three_quarter")
POSES = ("walk_a", "walk_b", "crouch")


def build():
    frames = []
    manifest = {"format": "hunter-animation-v1", "size": [128, 224],
                "generator": "built-in image_gen", "frames": []}
    target = ROOT / "assets/runtime/hunter-animation.zip"
    preview = Image.new("RGB", (4 * 160, 9 * 248), (35, 39, 46))
    draw = ImageDraw.Draw(preview)
    for hunter_index, hunter in enumerate(("cleric", "observer", "demonologist")):
        path = SOURCE / f"{hunter}.png"
        sheet = Image.open(path).convert("RGBA")
        cells = []
        for row in range(3):
            for col in range(4):
                cell = sheet.crop((col*sheet.width//4, row*sheet.height//3,
                                   (col+1)*sheet.width//4, (row+1)*sheet.height//3))
                # Alpha threshold locates the silhouette; original edge alpha is preserved.
                bounds = cell.getchannel("A").point(lambda a: 255 if a >= 64 else 0).getbbox()
                if bounds is None:
                    raise ValueError(f"Empty cell {hunter}/{row}/{col}")
                cells.append(cell.crop(bounds))
        # Shared horizontal scale across all poses prevents crouch width inflation.
        scale = min(210/max(c.height for c in cells[:8]), 116/max(c.width for c in cells))
        for index, cell in enumerate(cells):
            row, col = divmod(index, 4)
            width = max(1, round(cell.width*scale))
            height = round(cell.height*scale)
            if row == 2:
                # Runtime gives crouch its physical 1.05m height. Normalize vertical
                # sampling here so the character is not shrunk a second time.
                height = 210
            cell = cell.resize((width, height), Image.Resampling.LANCZOS)
            frame = Image.new("RGBA", (128, 224))
            frame.alpha_composite(cell, ((128-width)//2, 220-height))
            name = f"{hunter}/{POSES[row]}/{VIEWS[col]}.png"
            payload = io.BytesIO()
            frame.save(payload, format="PNG")
            data = payload.getvalue()
            frames.append((name, data))
            manifest["frames"].append({"entry": name, "sha256": hashlib.sha256(data).hexdigest(),
                "source": path.relative_to(ROOT).as_posix(),
                "source_sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
            x, y = col*160, (hunter_index*3+row)*248
            # Preview crouch at its actual world-height proportion.
            display = frame if row < 2 else frame.resize((128, 132), Image.Resampling.LANCZOS)
            preview.paste(display, (x+16, y+224-display.height), display)
            draw.text((x+5,y+229), f"{hunter} {POSES[row]}", fill="white")
        # Individual PNGs are convenient for game/art tools, ZIP is the JAR resource.
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name, data in frames:
            entry = zipfile.ZipInfo(name, (2026, 11, 2, 0, 0, 0))
            archive.writestr(entry, data, compress_type=zipfile.ZIP_DEFLATED)
            output = ROOT / "assets/runtime/hunter-animation" / name
            output.parent.mkdir(parents=True, exist_ok=True)
            output.write_bytes(data)
    (ROOT / "assets/runtime/hunter-animation-manifest.json").write_text(json.dumps(manifest, indent=2)+"\n")
    preview.save(SOURCE / "preview.png")
    print(f"Built {len(frames)} transparent hunter frames: {target}")


if __name__ == "__main__":
    build()
