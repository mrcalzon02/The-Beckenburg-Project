#!/usr/bin/env python3
"""Make the physical Beckenburg County castle contract board as an indexed, textured glTF2 GLB.

The board is static 3D geometry, not a camera-facing sprite or a full-screen prop.
Artwork comes exclusively from the approved unlabeled county map JPEG.
Runtime needs only the GLB; this script needs Pillow at authoring time.
"""
from __future__ import annotations
import argparse
import io
import json
import math
import random
import struct
from pathlib import Path
from PIL import Image, ImageDraw

# Native meters. 1.0 = production placement scale; origin is the grounded centre.
MAP_LEFT, MAP_RIGHT = -1.075, 1.075
MAP_BOTTOM, MAP_TOP = 0.740, 2.352
MAP_Z = 0.106


def procedural_surface(kind: str) -> bytes:
    rng = random.Random(103 if kind == 'oak' else 37)
    im = Image.new('RGB', (96, 96)); p = im.load()
    for y in range(96):
        for x in range(96):
            n = rng.randint(-10, 10)
            if kind == 'oak':
                vein = 13*math.sin((y + 2*math.sin(x/13))*.23) + 7*math.sin((y + x*.1)*.75)
                wear = -12 if (x%39) < 3 else 0
                p[x, y] = tuple(max(0, min(255, round(c+n+vein+wear))) for c in (86, 57, 35))
            else:
                rust = 10*math.sin((x+y*3)*.05)
                p[x, y] = tuple(max(0, min(255, round(c+n*.5+rust))) for c in (90, 79, 65))
    out=io.BytesIO();im.save(out,format='PNG',optimize=True);return out.getvalue()


class Mesh:
    def __init__(self):
        self.position=[[], [], []]; self.uv=[[], [], []];self.index=[[], [], []]
    def tri(self, material, a, b, c, uvs):
        for vertex,uv in zip((a,b,c),uvs):
            self.position[material].append(vertex)
            self.uv[material].append(uv)
            self.index[material].append(len(self.index[material]))
    def quad(self,material,a,b,c,d,uv=None):
        if uv is None: uv=[(0,1),(1,1),(1,0),(0,0)]
        self.tri(material,a,b,c,uv[:3]);self.tri(material,a,c,d,[uv[0],uv[2],uv[3]])
    def box(self, x0,x1,y0,y1,z0,z1,mat=1,uvspan=1):
        # 6 full-depth surfaces, true volume. Face order chosen for outside normals.
        def p(x,y,z):return (x,y,z)
        v=[p(x0,y0,z0),p(x1,y0,z0),p(x1,y1,z0),p(x0,y1,z0),
           p(x0,y0,z1),p(x1,y0,z1),p(x1,y1,z1),p(x0,y1,z1)]
        for a,b,c,d in ((4,5,6,7),(1,0,3,2),(0,4,7,3),(5,1,2,6),(7,6,2,3),(0,1,5,4)):
            self.quad(mat,v[a],v[b],v[c],v[d])
    def board(self):
        # Backing core with both front and rear surfaces. Map stays at world-fixed +Z.
        self.box(-1.182,1.182,.625,2.474,-.118,.070,1)
        self.quad(0, (MAP_LEFT,MAP_BOTTOM,MAP_Z),(MAP_RIGHT,MAP_BOTTOM,MAP_Z),
                  (MAP_RIGHT,MAP_TOP,MAP_Z),(MAP_LEFT,MAP_TOP,MAP_Z))
        # Raised oak frame: unmistakable thickness around the approved map.
        self.box(-1.245,-1.075,.57,2.555,-.151,.152,1)
        self.box(1.075,1.245,.57,2.555,-.151,.152,1)
        self.box(-1.245,1.245,2.355,2.555,-.151,.152,1)
        self.box(-1.245,1.245,.57,.744,-.151,.152,1)
        # Broad face moulding strips; oil-dark wood with slightly proud edges.
        self.box(-1.265,-1.215,.605,2.530,.112,.167,1)
        self.box(1.215,1.265,.605,2.530,.112,.167,1)
        self.box(-1.265,1.265,2.502,2.557,.112,.165,1)
        self.box(-1.265,1.265,.575,.625,.112,.165,1)
        # Supported A-frame / timber legs, extending to real floor contact.
        for x in (-.975,.975):
            self.box(x-.085,x+.085,.055,.681,-.142,.115,1)
            self.box(x-.205,x+.205,0,.112,-.306,.308,1)
        self.box(-.999,.999,.23,.315,-.132,.065,1)
        # Metal corner patches and structural iron strap below the map.
        for x in (-1.153,1.153):
            for y in (.667,2.446):
                self.box(x-.073,x+.073,y-.073,y+.073,.152,.178,2)
                # Rivet, shallow circular head, faceted octagon.
                r=.022;depth=.012
                for j in range(8):
                    a=j*math.tau/8;b=(j+1)*math.tau/8
                    self.quad(2,(x,y,.178),(x+r*math.cos(a),y+r*math.sin(a),.178+depth),
                        (x+r*math.cos(b),y+r*math.sin(b),.178+depth),(x,y,.178))
        self.box(-.120,.120,.630,.683,.154,.181,2)


def build_glb(image_path: Path, target: Path):
    source=Image.open(image_path).convert('RGB')
    image=source.resize((768,576),Image.Resampling.LANCZOS)
    map_png=io.BytesIO();image.save(map_png,format='PNG',optimize=True,compress_level=9)
    images=[map_png.getvalue(),procedural_surface('oak'),procedural_surface('iron')]
    mesh=Mesh();mesh.board()
    payload=bytearray();views=[];accessors=[];primitives=[]
    def attach(data, target_type=None):
        while len(payload)%4:payload.append(0)
        i=len(views);off=len(payload);payload.extend(data)
        v={'buffer':0,'byteOffset':off,'byteLength':len(data)}
        if target_type:v['target']=target_type
        views.append(v);return i
    for mat in range(3):
        p=mesh.position[mat];uv=mesh.uv[mat];ix=mesh.index[mat]
        if not p: continue
        pbin=b''.join(struct.pack('<fff',*v) for v in p)
        ubin=b''.join(struct.pack('<ff',*v) for v in uv)
        ibin=b''.join(struct.pack('<H',v) for v in ix)
        i=attach(pbin,34962)
        pos=len(accessors)
        accessors.append({'bufferView':i,'componentType':5126,'count':len(p),'type':'VEC3',
             'min':[min(v[k] for v in p) for k in range(3)],
             'max':[max(v[k] for v in p) for k in range(3)]})
        i=attach(ubin,34962);uvref=len(accessors)
        accessors.append({'bufferView':i,'componentType':5126,'count':len(uv),'type':'VEC2'})
        i=attach(ibin,34963);idx=len(accessors)
        accessors.append({'bufferView':i,'componentType':5123,'count':len(ix),'type':'SCALAR'})
        primitives.append({'attributes':{'POSITION':pos,'TEXCOORD_0':uvref},'indices':idx,'material':mat,'mode':4})
    image_views=[attach(png) for png in images]
    json_doc={'asset':{'version':'2.0','generator':'Beckenburg County Board Source Authoring'},
        'scene':0,'scenes':[{'nodes':[0]}], 'nodes':[{'mesh':0,'name':'Beckenburg County fixed campaign notice board'}],
        'meshes':[{'name':'County map / framed floor-standing billboard','primitives':primitives}],
        'buffers':[{'byteLength':len(payload)}],'bufferViews':views,'accessors':accessors,
        'images':[{'bufferView':view,'mimeType':'image/png','name':name} for view,name in zip(image_views,('Approved unlabeled county cartography','Oiled oak','Wrought iron'))],
        'textures':[{'source':i,'sampler':0} for i in range(3)],'samplers':[{'wrapS':33071,'wrapT':33071}],
        'materials':[{'name':name,'pbrMetallicRoughness':{'baseColorTexture':{'index':i},
            'baseColorFactor':[1,1,1,1],'metallicFactor':0.0,'roughnessFactor':0.94},
            'doubleSided':True,'extras':{'beckenburgUvOrigin':'TOP_LEFT'}}
            for i,name in enumerate(('county_campaign_map_approved','county_board_oiled_oak','county_board_wrought_iron'))]}
    doc=json.dumps(json_doc,separators=(',',':'),ensure_ascii=True).encode('utf-8')
    doc+=b' ' * (-len(doc)%4)
    payload+=b'\0' * (-len(payload)%4)
    size=12+8+len(doc)+8+len(payload)
    binary=(struct.pack('<III',0x46546c67,2,size)+struct.pack('<II',len(doc),0x4e4f534a)+doc
        +struct.pack('<II',len(payload),0x004e4942)+payload)
    if len(binary)>4_000_000:raise ValueError(f'GLB exceeds runtime 4MB bound: {len(binary)}')
    target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(binary)
    print(f'{target}: {len(binary)} bytes, {sum(len(a)//3 for a in mesh.index)} triangles, 3 embedded PNG textures, map {image.size}')


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('map',type=Path);parser.add_argument('output',type=Path)
    args=parser.parse_args();build_glb(args.map,args.output)
