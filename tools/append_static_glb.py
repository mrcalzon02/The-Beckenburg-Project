"""Append generated static geometry without re-exporting retained GLB assets."""
import copy,json,struct
from pathlib import Path


def read(path):
    raw=Path(path).read_bytes();magic,version,size=struct.unpack_from('<III',raw);assert magic==0x46546c67 and version==2 and size==len(raw)
    offset=12;document=None;binary=None
    while offset<len(raw):
        n,kind=struct.unpack_from('<II',raw,offset);offset+=8;chunk=raw[offset:offset+n];offset+=n
        if kind==0x4e4f534a:document=json.loads(chunk)
        elif kind==0x004e4942:binary=chunk
        else:raise ValueError('Unexpected GLB chunk')
    assert document is not None and binary is not None and len(document['buffers'])==1 and 'uri' not in document['buffers'][0]
    return document,binary


def append(source,addition,target,remove_mesh_names=()):
    old,oldbin=read(source);new,newbin=read(addition);result=copy.deepcopy(old)
    assert not any(new.get(k) for k in ['animations','skins','cameras']),'Only generated static mesh additions'
    offsets={k:len(old.get(k,[])) for k in ['bufferViews','accessors','samplers','images','textures','materials','meshes','nodes']}
    assert len(oldbin)%4==0
    for key in offsets:
        result.setdefault(key,[])
        for original in new.get(key,[]):
            row=copy.deepcopy(original)
            if key=='bufferViews':
                assert row.get('buffer',0)==0;row['buffer']=0;row['byteOffset']=row.get('byteOffset',0)+len(oldbin)
            elif key=='accessors':
                if 'bufferView' in row:row['bufferView']+=offsets['bufferViews']
                if 'sparse' in row:
                    for k in ['indices','values']:row['sparse'][k]['bufferView']+=offsets['bufferViews']
            elif key=='images':
                assert 'uri' not in row,'Embedded generated image required';row['bufferView']+=offsets['bufferViews']
            elif key=='textures':
                if 'source' in row:row['source']+=offsets['images']
                if 'sampler' in row:row['sampler']+=offsets['samplers']
                assert not row.get('extensions'),'Generated texture extensions require explicit resource handling'
            elif key=='materials':
                def remap(value):
                    if isinstance(value,dict):
                        for name,child in value.items():
                            if name.endswith('Texture') and isinstance(child,dict) and 'index' in child:child['index']+=offsets['textures']
                            remap(child)
                    elif isinstance(value,list):
                        for child in value:remap(child)
                remap(row)
            elif key=='meshes':
                for primitive in row['primitives']:
                    assert not primitive.get('extensions') and not primitive.get('targets'),'Static uncompressed primitive required'
                    primitive['attributes']={k:v+offsets['accessors'] for k,v in primitive['attributes'].items()}
                    if 'indices' in primitive:primitive['indices']+=offsets['accessors']
                    if 'material' in primitive:primitive['material']+=offsets['materials']
            elif key=='nodes':
                assert not any(k in row for k in ['skin','camera','extensions'])
                if 'mesh' in row:row['mesh']+=offsets['meshes']
                if 'children' in row:row['children']=[v+offsets['nodes'] for v in row['children']]
            result[key].append(row)
    # Retain every original node/mesh/accessor and its transform verbatim.
    # Disconnect the explicitly replaced terrain node from scene roots/parents;
    # its immutable binary resource remains preserved for exact asset retention.
    removed={i for i,n in enumerate(old.get('nodes',[])) if n.get('name') in set(remove_mesh_names)}
    assert len(removed)==len(remove_mesh_names),('Missing explicit terrain node',remove_mesh_names)
    for i in removed:result['nodes'][i].pop('mesh',None)
    for scene in result['scenes']:scene['nodes']=[i for i in scene.get('nodes',[]) if i not in removed]
    for node in result.get('nodes',[]):
        if 'children' in node:node['children']=[i for i in node['children'] if i not in removed]
    result['scenes'][result.get('scene',0)].setdefault('nodes',[]).extend(i+offsets['nodes'] for i in new['scenes'][new.get('scene',0)]['nodes'])
    for key in ['extensionsUsed','extensionsRequired']:
        if key in old or key in new:result[key]=sorted(set(old.get(key,[]))|set(new.get(key,[])))
    binary=oldbin+newbin;result['buffers']=[dict(byteLength=len(binary))]
    encoded=json.dumps(result,separators=(',',':'),ensure_ascii=False).encode();encoded+=b' '*((-len(encoded))%4);binary+=b'\0'*((-len(binary))%4)
    raw=struct.pack('<III',0x46546c67,2,12+8+len(encoded)+8+len(binary))+struct.pack('<II',len(encoded),0x4e4f534a)+encoded+struct.pack('<II',len(binary),0x004e4942)+binary
    Path(target).write_bytes(raw)
    check,checkbin=read(target);assert checkbin[:len(oldbin)]==oldbin
    for k in ['meshes','accessors','bufferViews','images','textures','materials']:
        assert check.get(k,[])[:len(old.get(k,[]))]==old.get(k,[]),(k,'source resource mutated')
    return dict(original_binary_bytes_preserved=len(oldbin),original_static_resources_preserved=True,replaced_scene_nodes=sorted(removed))
