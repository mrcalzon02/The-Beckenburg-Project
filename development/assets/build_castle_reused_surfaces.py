"""Reuse shipped frame/rug geometry; replace only embedded PNG imagery.

Blender supplies technical image sizing, editable packed source and independent
reimport/render. Runtime GLB buffers keep exact original mesh/UV/index bytes.
"""
from pathlib import Path
import bpy,json,struct,hashlib
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/reused-surfaces';PRE.mkdir(parents=True,exist_ok=True)
def decode(raw):
 n=struct.unpack_from('<I',raw,12)[0];return json.loads(raw[20:20+n]),raw[28+n:]
def geometry_digest(j,data):
 parts=[]
 for m in j['meshes']:
  for p in m['primitives']:
   for key,index in sorted({**p['attributes'],'indices':p['indices']}.items()):
    a=j['accessors'][index];v=j['bufferViews'][a['bufferView']]
    n={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']];size={5126:4,5125:4,5123:2,5121:1}[a['componentType']]*n
    offset=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',size)
    parts.append(key.encode())
    parts.extend(data[offset+i*stride:offset+i*stride+size] for i in range(a['count']))
 return hashlib.sha256(b''.join(parts)).hexdigest()
def replace_png(raw,png):
 j,data=decode(raw);before=geometry_digest(j,data)
 assert len(j['images'])==1 and len(j['buffers'])==1
 view_index=j['images'][0]['bufferView'];view=j['bufferViews'][view_index]
 start=view.get('byteOffset',0);end=(start+view['byteLength']+3)//4*4
 padded=png+b'\0'*((-len(png))%4);shift=len(padded)-(end-start)
 for i,v in enumerate(j['bufferViews']):
  if i==view_index:continue
  offset=v.get('byteOffset',0)
  assert offset>=end or offset+v['byteLength']<=start,'Image overlaps mesh data'
  if offset>=end:v['byteOffset']=offset+shift
 view['byteLength']=len(png);data=data[:start]+padded+data[end:]
 j['buffers'][0]['byteLength']=len(data);j['images'][0]['mimeType']='image/png'
 assert geometry_digest(j,data)==before
 encoded=json.dumps(j,separators=(',',':')).encode('utf-8');encoded+=b' '*((-len(encoded))%4)
 result=struct.pack('<4sII',b'glTF',2,28+len(encoded)+len(data))+struct.pack('<I4s',len(encoded),b'JSON')+encoded+struct.pack('<I4s',len(data),b'BIN\0')+data
 jj,bb=decode(result);assert geometry_digest(jj,bb)==before
 return result,before
records=[]
for original,name,texture,size in [('painting_1','restoration_painting','restoration-painting',(512,287)),('carpet_mp_1','library_rug','library-rug',(512,324))]:
 bpy.ops.wm.read_factory_settings(use_empty=True)
 image=bpy.data.images.load(str(OUT/('textures/'+texture+'-imagegen-master.png')))
 image.scale(*size);runtime=OUT/('textures/'+texture+'-runtime.png');image.filepath_raw=str(runtime);image.file_format='PNG';image.save()
 source=OUT/('references/reused-models/'+original+'.glb');before=source.read_bytes()
 raw,digest=replace_png(before,runtime.read_bytes());target=OUT/('exports/level_castle_'+name+'.glb');target.write_bytes(raw)
 bpy.ops.import_scene.gltf(filepath=str(target));meshes=[o for o in bpy.context.scene.objects if o.type=='MESH']
 for im in bpy.data.images:
  if im.source=='FILE' and not im.packed_file:im.pack()
 assert all(n.image and n.image.packed_file for o in meshes for m in o.data.materials for n in m.node_tree.nodes if n.type=='TEX_IMAGE'),'Imported source image lost packed data'
 for o in meshes:o.data.calc_loop_triangles()
 triangles=sum(len(o.data.loop_triangles) for o in meshes);assert triangles==(92 if original=='painting_1' else 18)
 corners=[o.matrix_world@Vector(c) for o in meshes for c in o.bound_box]
 lo=[min(c[a] for c in corners) for a in range(3)];hi=[max(c[a] for c in corners) for a in range(3)]
 bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(name+'.blend')))
 rec=dict(id='level_castle_'+name,source_id=original,source_sha256=hashlib.sha256(before).hexdigest(),sha256=hashlib.sha256(raw).hexdigest(),unchanged_geometry_UV_normal_indices_sha256=digest,triangles=triangles,dimensions_metres=[hi[a]-lo[a] for a in range(3)],runtime_png_sha256=hashlib.sha256(runtime.read_bytes()).hexdigest(),texture_dimensions=size,status='REUSED_GEOMETRY_VERIFIED_VISUAL_NATIVE_REVIEW_PENDING')
 records.append(rec)
 scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16
 scene.render.resolution_x=900;scene.render.resolution_y=800;scene.render.resolution_percentage=100
 scene.world=bpy.data.worlds.new('Reused surface review');scene.world.color=(.18,.18,.18)
 centre=Vector([(lo[a]+hi[a])/2 for a in range(3)]);span=max(hi[a]-lo[a] for a in range(3))
 for pos,power in [((1,-3,4),500),((-3,1,3),300)]:
  bpy.ops.object.light_add(type='AREA',location=centre+Vector(pos));bpy.context.object.data.energy=power;bpy.context.object.data.size=3
 bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.35
 # Imported glTF appears in Blender's Z-up authoring frame.
 for angle,offset in [('front',(0,-4,1.8)),('quarter',(2,-4,2.2)),('rear',(0,4,1.8))] if original=='painting_1' else [('top',(0,-.01,5)),('quarter',(2,-3,3)),('edge',(3,-3,.8))]:
  cam.location=centre+Vector(offset);cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'-'+angle+'.png'));bpy.ops.render.render(write_still=True)
(OUT/'reused-surfaces-review.json').write_text(json.dumps(dict(status='SOURCE_GEOMETRY_PRESERVED',models=records),indent=2)+'\n',encoding='utf-8')
print('PASS reused frame92/rug18 triangles: original geometry/normals/UVs/indices unchanged; new generated images, packed editable scenes and actual-export views')
