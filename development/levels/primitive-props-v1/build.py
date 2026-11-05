#!/usr/bin/env python3
"""Regenerate primitive prop GLBs from catalog.json. Python stdlib only.

Intentional graybox: static meshes and placeholder colors only; not playable
scene placements or replacements for finished texture-authored props.
"""
from __future__ import annotations
import argparse
import json
import math
from pathlib import Path
import struct
import hashlib
import zlib

HERE = Path(__file__).resolve().parent
PALETTE = {
    "wood": [0.39, 0.29, 0.19, 1.0],
    "stone": [0.46, 0.47, 0.47, 1.0],
    "metal": [0.39, 0.44, 0.50, 1.0],
    "dark": [0.14, 0.17, 0.19, 1.0],
    "industrial": [0.24, 0.32, 0.34, 1.0],
    "canvas": [0.47, 0.45, 0.36, 1.0],
    "accent": [0.67, 0.45, 0.22, 1.0],
}

def triangles(parts):
    groups = {}
    def face(poly, material):
        g = groups.setdefault(material, {"position": [], "normal": [], "indices": []})
        for j in range(1, len(poly)-1):
            a,b,c = poly[0],poly[j],poly[j+1]
            u=[b[i]-a[i] for i in range(3)]
            v=[c[i]-a[i] for i in range(3)]
            n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            mag=math.sqrt(sum(t*t for t in n))
            if mag < 1e-8: raise ValueError("degenerate primitive triangle")
            n=[x/mag for x in n]
            for point in (a,b,c):
                g["position"].extend(point)
                g["normal"].extend(n)
                g["indices"].append(len(g["indices"]))
    for part in parts:
        kind=part["type"];px,py,pz=part["p"];material=part["material"]
        if material not in PALETTE: raise ValueError(material)
        if kind == "box":
            w,h,d=part["size"];yaw=part.get("yaw",0.0)
            sn,cs=math.sin(yaw),math.cos(yaw)
            corners=[]
            for x,y,z in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]:
                x*=w/2;y*=h/2;z*=d/2
                corners.append([px+x*cs-z*sn,py+y,pz+x*sn+z*cs])
            for ids in ((0,3,2,1),(4,5,6,7),(4,7,3,0),(1,2,6,5),(3,7,6,2),(0,1,5,4)):
                face([corners[i] for i in ids],material)
        elif kind == "cylinder":
            seg=part.get("segments",10);r=part["radius"];h=part["height"]
            if not (6<=seg<=24 and r>0 and h>0):raise ValueError("invalid cylinder")
            def at(index,y):
                theta=2*math.pi*(index%seg)/seg
                x,z=r*math.cos(theta),r*math.sin(theta)
                axis=part.get("axis","y")
                if axis=="x":x,y,z=y,-x,z
                elif axis=="z":x,y,z=x,-z,y
                elif axis!="y":raise ValueError(axis)
                return [px+x,py+y,pz+z]
            for i in range(seg):
                lo,ln,hi,hn=at(i,-h/2),at(i+1,-h/2),at(i,h/2),at(i+1,h/2)
                face([lo,hi,hn,ln],material)
                # End centers must be transformed in the same way as the perimeter.
                axis=part.get("axis","y")
                top = [px+h/2,py,pz] if axis=="x" else [px,py,pz+h/2] if axis=="z" else [px,py+h/2,pz]
                bot = [px-h/2,py,pz] if axis=="x" else [px,py,pz-h/2] if axis=="z" else [px,py-h/2,pz]
                face([top,hn,hi],material)
                face([bot,lo,ln],material)
        elif kind == "tri_prism":
            w,h,d=part["size"];xl,xr=px-w/2,px+w/2;low,high=py-h/2,py+h/2;zn,zf=pz-d/2,pz+d/2
            a,b,c=[xl,low,zn],[xr,low,zn],[px,high,zn]
            A,B,C=[xl,low,zf],[xr,low,zf],[px,high,zf]
            for p in ((a,c,b),(A,B,C),(a,A,C,c),(b,c,C,B),(a,b,B,A)):
                face(p,material)
        else: raise ValueError("unexpected primitive: "+kind)
    return groups

def white_png():
    """Tiny shared valid RGB PNG; material baseColorFactor supplies each proxy color."""
    def chunk(kind, payload):
        return (struct.pack(">I",len(payload)) + kind + payload
                + struct.pack(">I",zlib.crc32(kind+payload)&0xffffffff))
    ihdr=struct.pack(">IIBBBBB",2,2,8,2,0,0,0)
    scan=b"".join(b"\\x00"+b"\\xff\\xff\\xff"*2 for _ in range(2))
    return b"\\x89PNG\\r\\n\\x1a\\n"+chunk(b"IHDR",ihdr)+chunk(b"IDAT",zlib.compress(scan,9))+chunk(b"IEND",b"")

def glb(model):
    groups=triangles(model["parts"])
    raw=bytearray();views=[];accessors=[];prims=[];materials=[]
    def accessor(data,target,component,shape,count,lo=None,hi=None):
        while len(raw)%4:raw.append(0)
        offset=len(raw);raw.extend(data)
        idx=len(views);views.append({"buffer":0,"byteOffset":offset,"byteLength":len(data),"target":target})
        a={"bufferView":idx,"componentType":component,"count":count,"type":shape}
        if lo is not None:a["min"]=lo
        if hi is not None:a["max"]=hi
        accessors.append(a)
        return len(accessors)-1
    total=0
    for name,group in groups.items():
        pos,norm,inds=group["position"],group["normal"],group["indices"]
        count=len(pos)//3;total+=len(inds)//3
        if count>65535:raise ValueError("too many vertices: "+model["id"])
        material=len(materials)
        materials.append({"name":"blockout_"+name,
            "pbrMetallicRoughness":{"baseColorFactor":PALETTE[name],"baseColorTexture":{"index":0},"metallicFactor":0,"roughnessFactor":1},
            "doubleSided":False})
        axes=[pos[i::3] for i in range(3)]
        a=accessor(struct.pack("<%sf"%len(pos),*pos),34962,5126,"VEC3",count,
                   [min(a) for a in axes],[max(a) for a in axes])
        b=accessor(struct.pack("<%sf"%len(norm),*norm),34962,5126,"VEC3",count)
        uv=accessor(struct.pack("<%sf"%(count*2),*([0.5,0.5]*count)),34962,5126,"VEC2",count)
        c=accessor(struct.pack("<%sH"%len(inds),*inds),34963,5123,"SCALAR",len(inds))
        prims.append({"attributes":{"POSITION":a,"NORMAL":b,"TEXCOORD_0":uv},"indices":c,"material":material,"mode":4})
    if total>3500:raise ValueError("triangle budget exceeded "+model["id"])
    while len(raw)%4:raw.append(0)
    png=white_png()
    png_index=len(views)
    views.append({"buffer":0,"byteOffset":len(raw),"byteLength":len(png)})
    raw.extend(png)
    while len(raw)%4:raw.append(0)
    doc={"asset":{"version":"2.0","generator":"Beckenburg primitive-props-v1"},
         "scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0,"name":model["id"]}],
         "meshes":[{"name":model["label"],"primitives":prims}],
         "buffers":[{"byteLength":len(raw)}],"bufferViews":views,"accessors":accessors,
         "images":[{"bufferView":png_index,"mimeType":"image/png"}],
         "textures":[{"source":0}],"samplers":[],
         "materials":materials,"extras":{"stage":"graybox_only","modelId":model["id"],"meters":True,"runtimeApproved":False}}
    js=json.dumps(doc,separators=(",",":"),ensure_ascii=True).encode("ascii")
    js+=b" "*((-len(js))%4)
    return (struct.pack("<III",0x46546c67,2,12+8+len(js)+8+len(raw))
            +struct.pack("<II",len(js),0x4e4f534a)+js
            +struct.pack("<II",len(raw),0x004e4942)+raw),total

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--check",action="store_true",help="validate committed outputs byte-for-byte")
    args=parser.parse_args()
    catalog=json.loads((HERE/"catalog.json").read_text(encoding="utf-8"))
    assert catalog["schema"]=="beckenburg.primitive-props/1"
    assert len(catalog["sites"])==29 and len(catalog["models"])==30
    outputs=HERE/"models"
    if not args.check:outputs.mkdir(exist_ok=True)
    records=[]
    for model in catalog["models"]:
        blob,tri=glb(model)
        path=outputs/(model["id"]+".glb")
        if args.check:
            if not path.is_file() or path.read_bytes()!=blob:
                raise SystemExit("MISSING OR STALE: "+str(path))
        else:path.write_bytes(blob)
        magic,version,total=struct.unpack_from("<III",blob,0)
        assert magic==0x46546c67 and version==2 and total==len(blob)
        records.append({"id":model["id"],"triangles":tri,"bytes":len(blob),"sha256":hashlib.sha256(blob).hexdigest()})
    if not args.check:
        (HERE/"verification.json").write_text(json.dumps({"status":"mesh-format-and-catalog-only",
            "runtime_verified":False,"models":records},indent=2)+"\n")
    print("PRIMITIVES",len(records),"TRIANGLES",sum(x["triangles"] for x in records),
          "MODE", "check" if args.check else "build")
if __name__=="__main__":main()
