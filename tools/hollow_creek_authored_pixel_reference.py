"""Independent glTF/PNG reference pixels for the actual game-loader check.
Run with Pillow available. It does not use the Java sampler or Blender shaders.
"""
from pathlib import Path
import json,struct,io,math,sys
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
manifest=json.loads((ROOT/(sys.argv[1] if len(sys.argv)>1 else 'development/levels/hollow-creek/authored-surfaces.json')).read_text(encoding='utf-8'))
lines=[]
for a in manifest['assets']:
    data=(ROOT/f"assets/runtime/models/{a['id']}.glb").read_bytes()
    length=struct.unpack_from('<I',data,12)[0];g=json.loads(data[20:20+length]);start=28+length
    def at(accessor,index,count):
        v=g['bufferViews'][accessor['bufferView']];code,size={5126:('f',4),5125:('I',4),5123:('H',2),5121:('B',1)}[accessor['componentType']]
        offset=start+v.get('byteOffset',0)+accessor.get('byteOffset',0)+index*v.get('byteStride',count*size)
        return struct.unpack_from('<'+code*count,data,offset)
    offset=0
    for mesh in g['meshes']:
      for primitive in mesh['primitives']:
        texture=g['materials'][primitive['material']]['pbrMetallicRoughness']['baseColorTexture'];assert texture.get('texCoord',0)==0
        image_index=g['textures'][texture['index']]['source'];view=g['bufferViews'][g['images'][image_index]['bufferView']];off=start+view.get('byteOffset',0)
        image=Image.open(io.BytesIO(data[off:off+view['byteLength']])).convert('RGB')
        uv=g['accessors'][primitive['attributes']['TEXCOORD_0']];indices=g['accessors'][primitive['indices']];n=indices['count']//3
        for j in range(72):
            triangle=min(n-1,j*n//72);ids=[at(indices,triangle*3+k,1)[0] for k in range(3)]
            coords=[at(uv,i,2) for i in ids];u=sum(v[0] for v in coords)/3;v=sum(v[1] for v in coords)/3
            x=min(image.width-1,int((u-math.floor(u))*image.width));y=min(image.height-1,int((v-math.floor(v))*image.height))
            rgb=image.getpixel((x,y));color=(rgb[0]<<16)|(rgb[1]<<8)|rgb[2]
            lines.append(f"{a['id']}\t{offset+triangle}\t{u:.17g}\t{v:.17g}\t{color:06x}")
        offset+=n
path=ROOT/(sys.argv[2] if len(sys.argv)>2 else 'development/levels/verification/hollow-creek-authored-pixels.tsv');path.write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('REFERENCE',len(lines),'actual glTF top-left atlas samples')
