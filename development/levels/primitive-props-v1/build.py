#!/usr/bin/env python3
"""Regenerate primitive prop GLBs from catalog.json. Python stdlib only.

Intentional graybox: static meshes and material-factor placeholder colors,
with spatially mapped UV0 and 32px material-specific procedural albedo study textures.
Not playable scene placements or replacements for finished props.
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
    # New relic-study colors are material factors, not final authored textures.
    "bronze": [0.45, 0.30, 0.15, 1.0],
    "gilt": [0.70, 0.52, 0.20, 1.0],
    "silver": [0.60, 0.65, 0.66, 1.0],
    "ivory": [0.74, 0.70, 0.60, 1.0],
    "ceramic": [0.45, 0.54, 0.52, 1.0],
    "leather": [0.25, 0.14, 0.10, 1.0],
    "redcloth": [0.38, 0.11, 0.15, 1.0],
    "glass": [0.29, 0.47, 0.50, 1.0],
    "patina": [0.21, 0.40, 0.34, 1.0],
    "wax": [0.76, 0.70, 0.49, 1.0],
}

def triangles(parts):
    groups = {}
    def face(poly, material):
        g = groups.setdefault(material, {"position": [], "normal": [], "uv": [], "indices": []})
        for j in range(1, len(poly)-1):
            a,b,c = poly[0],poly[j],poly[j+1]
            u=[b[i]-a[i] for i in range(3)]
            v=[c[i]-a[i] for i in range(3)]
            n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
            mag=math.sqrt(sum(t*t for t in n))
            if mag < 1e-8: raise ValueError("degenerate primitive triangle")
            n=[x/mag for x in n]
            # Project each physical triangle to its dominant normal plane.
            # This yields nonzero area in UV0 for box/cylinder/prism faces,
            # unlike the old constant 0.5,0.5 UV on every vertex. Source
            # world coordinates remain metres; sample at 1 UV per 1.25 m.
            dominant=max(range(3),key=lambda axis:abs(n[axis]))
            axes=((1,2),(0,2),(0,1))[dominant]
            for point in (a,b,c):
                g["position"].extend(point)
                g["normal"].extend(n)
                g["uv"].extend((point[axes[0]]/1.25,point[axes[1]]/1.25))
                g["indices"].append(len(g["indices"]))
    for part in parts:
        kind=part["type"];px,py,pz=part["p"];material=part["material"]
        if material not in PALETTE: raise ValueError(material)
        if kind == "box":
            w,h,d=part["size"];yaw=part.get("yaw",0.0)
            # Roll rotates local XY first; yaw then rotates around world Y.
            # This is essential for upright fan/radial hardware. Yaw-only
            # would turn every blade sideways instead of around the hub.
            roll=part.get("roll",0.0)
            if not all(math.isfinite(a) for a in (roll,yaw)):
                raise ValueError("nonfinite part angle")
            sr,cr=math.sin(roll),math.cos(roll)
            sn,cs=math.sin(yaw),math.cos(yaw)
            corners=[]
            for x,y,z in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]:
                x*=w/2;y*=h/2;z*=d/2
                xr,yr=x*cr-y*sr,x*sr+y*cr
                corners.append([px+xr*cs-z*sn,py+yr,pz+xr*sn+z*cs])
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
                yaw=part.get("yaw",0)
                c,si=math.cos(yaw),math.sin(yaw)
                return [px+x*c-z*si,py+y,pz+x*si+z*c]
            for i in range(seg):
                lo,ln,hi,hn=at(i,-h/2),at(i+1,-h/2),at(i,h/2),at(i+1,h/2)
                face([lo,hi,hn,ln],material)
                # End centers must be transformed in the same way as the perimeter.
                axis=part.get("axis","y")
                yaw=part.get("yaw",0);co,si=math.cos(yaw),math.sin(yaw)
                if axis=="x":
                    top=[px+(h/2)*co,py,pz+(h/2)*si]
                    bot=[px-(h/2)*co,py,pz-(h/2)*si]
                elif axis=="z":
                    top=[px-(h/2)*si,py,pz+(h/2)*co]
                    bot=[px+(h/2)*si,py,pz-(h/2)*co]
                else:
                    top=[px,py+h/2,pz]
                    bot=[px,py-h/2,pz]
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

def material_png(name):
    """Portable deterministic 32x32 RGB texture for authored BLOCKOUT review.

    Low-frequency wear + material-specific micro-patterns use grayscale to
    keep the source PBR baseColorFactor as the authority for actual color.
    These are properly mapped texture studies, NOT replacement art atlases.
    """
    size=32
    seed=sum((i+1)*ord(c) for i,c in enumerate(name))
    rows=[]
    for y in range(size):
        row=bytearray((0,))  # PNG filter NONE, one scanline
        for x in range(size):
            h=(seed*151 + x*1097 + y*1543 + x*y*173)
            h=(h ^ (h>>9) ^ (h<<5)) & 0xffffffff
            fine=(h%17)-8
            if name in ("wood","leather"):
                # Long wood/leather fibers and darker end-grain streaks.
                pattern=7*math.sin(2*math.pi*x/8 + math.sin(y*.18)) + 4*math.sin(2*math.pi*y/32)
            elif name in ("stone","ceramic","ivory"):
                # Mineral speckle and occasional fine mortar/porosity line.
                pattern=5*math.sin(2*math.pi*(x+y)/16) - (14 if (x+2*y+seed)%23==0 else 0)
            elif name in ("metal","industrial","bronze","silver","gilt","patina"):
                # Brushed metal grain with uneven aged patches.
                pattern=5*math.sin(2*math.pi*y/4) - (11 if (x*7+y+seed)%29==0 else 0)
            elif name in ("canvas","redcloth"):
                # Low-contrast weave in both warp and weft directions.
                pattern=8*(1 if x%4==0 else 0) + 8*(1 if y%4==0 else 0) - 6
            elif name in ("glass",):
                # Still OPAQUE in a graybox: just pale scuffs.
                pattern=5*math.sin(2*math.pi*(x-y)/32)
            else:
                pattern=4*math.sin(2*math.pi*(x+y)/16)
            value=max(172,min(250,int(231+fine+pattern)))
            row.extend((value,value,value))
        rows.append(bytes(row))
    raw=b"".join(rows)
    def chunk(kind,data):
        return (struct.pack(">I",len(data))+kind+data+
                struct.pack(">I",zlib.crc32(kind+data)&0xffffffff))
    ihdr=struct.pack(">IIBBBBB",size,size,8,2,0,0,0)
    return (b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",ihdr)+
            chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b""))


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
            "pbrMetallicRoughness":{"baseColorFactor":PALETTE[name],"baseColorTexture":{"index":material},"metallicFactor":0,"roughnessFactor":1},
            "doubleSided":False})
        axes=[pos[i::3] for i in range(3)]
        a=accessor(struct.pack("<%sf"%len(pos),*pos),34962,5126,"VEC3",count,
                   [min(a) for a in axes],[max(a) for a in axes])
        b=accessor(struct.pack("<%sf"%len(norm),*norm),34962,5126,"VEC3",count)
        assert len(group["uv"])==count*2
        uv=accessor(struct.pack("<%sf"%(count*2),*group["uv"]),34962,5126,"VEC2",count)
        c=accessor(struct.pack("<%sH"%len(inds),*inds),34963,5123,"SCALAR",len(inds))
        prims.append({"attributes":{"POSITION":a,"NORMAL":b,"TEXCOORD_0":uv},"indices":c,"material":material,"mode":4})
    if total>3500:raise ValueError("triangle budget exceeded "+model["id"])
    # Separate tiny valid PNG per material. Actual game EditorGlbReader
    # already supports each glTF material's own embedded texture.
    images=[];textures=[]
    for index,name in enumerate(groups):
        while len(raw)%4:raw.append(0)
        png=material_png(name)
        view_index=len(views)
        views.append({"buffer":0,"byteOffset":len(raw),"byteLength":len(png)})
        raw.extend(png)
        images.append({"bufferView":view_index,"mimeType":"image/png"})
        textures.append({"source":index,"sampler":0})
    while len(raw)%4:raw.append(0)
    doc={"asset":{"version":"2.0","generator":"Beckenburg primitive-props-v1"},
         "scene":0,"scenes":[{"nodes":[0]}],"nodes":[{"mesh":0,"name":model["id"]}],
         "meshes":[{"name":model["label"],"primitives":prims}],
         "buffers":[{"byteLength":len(raw)}],"bufferViews":views,"accessors":accessors,
         "images":images,
         "textures":textures,"samplers":[{"magFilter":9728,"minFilter":9728,"wrapS":10497,"wrapT":10497}],
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
    assert len(catalog["sites"])==29 and len(catalog["models"])>=41
    ids=[model["id"] for model in catalog["models"]]
    assert len(ids)==len(set(ids))
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
