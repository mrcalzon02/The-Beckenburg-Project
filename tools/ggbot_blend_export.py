"""Bake GGBot's August 2023 Blender models, UVs and image materials together.
Blender -b --factory-startup --python tools/ggbot_blend_export.py -- source.zip models/
"""
import bpy
from pathlib import Path
import hashlib
import json
import struct
import sys
import tempfile
import zipfile

HASH="DB67B0B0FBAA02454A5D000DC9EC1CC53F360E8F41AB44F3FB1C7E8F71E699E4"
MODELS={"Car 01/":"ggbot_car01","Car 05/":"ggbot_car05",
        "Car 08/":"ggbot_car08_van_shell"}

def convert(archive,root,folder,name,outdir):
    members=[s for s in archive.namelist() if s.startswith(folder) and
             not s.endswith("/") and s.lower().endswith((".blend",".png"))]
    blends=[s for s in members if s.lower().endswith(".blend") and
            "shadow" not in s.lower()]
    if not blends:
        raise RuntimeError("Original Blender scene absent: "+folder)
    for s in members:
        path=Path(s)
        if ".." in path.parts:
            raise RuntimeError("Unsafe source path")
        dest=root/path
        dest.parent.mkdir(parents=True,exist_ok=True)
        dest.write_bytes(archive.read(s))
    blend=root/max(blends,key=lambda s:len(archive.read(s)))
    bpy.ops.wm.open_mainfile(filepath=str(blend),load_ui=False)
    meshes=[obj for obj in bpy.context.scene.objects
            if obj.type=="MESH" and not obj.hide_render]
    if not meshes:
        raise RuntimeError("Blender scene has no visible vehicle meshes: "+str(blend))
    atlas={Path(s).name.lower():root/Path(s) for s in members
           if s.lower().endswith(".png")}
    material_count=0
    for ob in meshes:
        if not ob.data.uv_layers:
            raise RuntimeError("Original mesh has no UV: "+ob.name)
        for slot in ob.material_slots:
            mat=slot.material
            if not mat or not mat.use_nodes:
                raise RuntimeError("No Blender source image material: "+ob.name)
            nodes=[n for n in mat.node_tree.nodes
                   if n.type=="TEX_IMAGE" and n.image is not None]
            if not nodes:
                raise RuntimeError("No Blender texture node: "+mat.name)
            for node in nodes:
                img=node.image
                key=Path(bpy.path.abspath(img.filepath)).name.lower()
                if key not in atlas:
                    key=img.name.lower()
                if key in atlas:
                    img.filepath=str(atlas[key])
                    img.source="FILE"
                    img.reload()
                elif not img.packed_file:
                    raise RuntimeError("Unresolved original texture: "+img.name)
                if img.size[0]!=128 or img.size[1]!=128:
                    raise RuntimeError("Not the source 128x128 atlas: "+img.name)
                material_count+=1
    bpy.ops.object.select_all(action="DESELECT")
    for obj in meshes:
        obj.hide_set(False)
        obj.select_set(True)
    bpy.context.view_layer.objects.active=meshes[0]
    output=outdir/(name+".glb")
    bpy.ops.export_scene.gltf(filepath=str(output),export_format="GLB",
        use_selection=True,export_texcoords=True,
        export_materials="EXPORT",export_image_format="PNG",
        export_apply=True,export_cameras=False,export_lights=False)
    data=output.read_bytes()
    if not (128<len(data)<4_000_000) or data[:4]!=b"glTF":
        raise RuntimeError("Invalid Blender-exported GLB")
    header=json.loads(data[20:20+struct.unpack_from("<I",data,12)[0]])
    if "Blender" not in header.get("asset",{}).get("generator",""):
        raise RuntimeError("Export did not originate in Blender")
    if not header.get("images") or not header.get("meshes"):
        raise RuntimeError("Missing real source images or vehicle geometry")
    for mesh in header["meshes"]:
        for part in mesh["primitives"]:
            if "TEXCOORD_0" not in part.get("attributes",{}):
                raise RuntimeError("Original UVs absent after conversion")
            mat=header["materials"][part["material"]]
            if "baseColorTexture" not in mat.get("pbrMetallicRoughness",{}):
                raise RuntimeError("Original Blender paint missing")
    print("GGBOT BLEND VERIFIED",blend.name,"->",output.name,
          "mesh_objects",len(meshes),"image_nodes",material_count,
          "size",len(data))

def main():
    if "--" not in sys.argv or len(sys.argv[sys.argv.index("--")+1:])!=2:
        raise RuntimeError("Expected ZIP path and model directory after --")
    archive_path,out=map(Path,sys.argv[sys.argv.index("--")+1:])
    if hashlib.sha256(archive_path.read_bytes()).hexdigest().upper()!=HASH:
        raise RuntimeError("GGBot source archive hash mismatch")
    out.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(archive_path) as archive:
        with tempfile.TemporaryDirectory(prefix="ggbot-original-blend-") as tmp:
            for folder,name in MODELS.items():
                convert(archive,Path(tmp),folder,name,out)

if __name__=="__main__":
    main()
