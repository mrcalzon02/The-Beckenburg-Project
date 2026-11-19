"""Independent appended-resource/closed-solid inspection and paired native renders."""
from pathlib import Path
import argparse,json,struct,sys
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import read
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild/arrival-bearing-pass';CAP=ROOT/'development/levels/rebuild/captures/arrival-bearing-batch'
def point(p):return Vector((p[0],-p[2],p[1]))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');parser.add_argument('--render',action='store_true');args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []);results=[];captures=[];CAP.mkdir(parents=True,exist_ok=True)
 for path in sorted(BASE.glob('*/construction.json')):
  r=json.loads(path.read_text());site=r['id']
  if args.sites and site not in args.sites:continue
  source=ROOT/r['source_glb'];glb=path.parent/'supported-arrival.glb';native=path.parent/'supported-arrival.hcs2';assert sha(source)==r['source_glb_sha256'] and sha(glb)==r['glb_sha256'] and sha(native)==r['physics_sha256'];old,oldbin=read(source);new,newbin=read(glb)
  assert newbin[:len(oldbin)]==oldbin,(site,'Original GLB resource bytes changed')
  for key in ['nodes','meshes','accessors','bufferViews','materials','images','textures','samplers']:
   a=old.get(key,[]);b=new.get(key,[]);assert b[:len(a)]==a,(site,key,'Original resource/pose/UV changed')
  before=split_native(ROOT/r['source_physics']);after=split_native(native);assert after[:len(before)]==before,(site,'Retained native records changed');proof=r['arrival_bearing'];assert len(after)-len(before)==len(proof['new_parts']);assert json.loads((ROOT/r['layout']).read_text())['links']==json.loads((ROOT/r['before_layout']).read_text())['links'];bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));solids=[]
  for name in proof['new_parts']:
   obj=bpy.data.objects[name];bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges),(site,name,'Open edge');assert bm.calc_volume(signed=False)>1e-5;bm.free();assert obj.data.uv_layers;actual=records(obj);part=next(t for t in after if t[0]==name);raw=part[2];i=2+len(name.encode())+5;native_ts=[struct.unpack_from('>9fB',raw,i+37*j) for j in range(part[1])];assert face_keys(actual)==face_keys([(t[:9],t[9]) for t in native_ts]);vs=[obj.matrix_world@v.co for v in obj.data.vertices];assert abs(max(v.z for v in vs)-proof['top_y'])<1e-5 and abs(min(v.z for v in vs)-proof['source_bottom_y'])<1e-5;solids.append(dict(name=name,triangles=len(actual),closed=True,uv_layers=len(obj.data.uv_layers)))
  bpy.ops.wm.open_mainfile(filepath=str(path.parent/'supported-arrival.blend'));images={n.image for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert all(im.packed_file and im.size[0]>0 and im.size[1]>0 and len(im.pixels)>0 for im in images),(site,'Editable texture missing');assert all(bpy.data.objects.get(n) for n in proof['new_parts']);results.append(dict(id=site,status='PASS_APPEND_PRESERVATION_CLOSED_NATIVE_UV_EDITABLE_SOURCE',glb_sha256=sha(glb),native_sha256=sha(native),blend_sha256=sha(path.parent/'supported-arrival.blend'),source_contract_sha256=sha(path),packed_images=len(images),retained_native_parts=len(before),new_solids=solids));print('ARRIVAL_INDEPENDENT',site,'PASS',flush=True)
  if not args.render:continue
  data=json.loads((ROOT/r['layout']).read_text());link=next(l for l in data['links'] if l['id']==proof['link']);a=proof['anchor'];b=link['points'][1];direction=point(b)-point(a);direction.normalize()
  for phase,model in [('before',source),('after',glb)]:
   bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(model));scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=800;scene.render.resolution_y=600;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=5;target=point(a)+direction*.45;cam.location=target+Vector((3,2.5,3));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();out=CAP/(site.lower()+'-arrival-'+phase+'.png');scene.render.filepath=str(out);bpy.ops.render.render(write_still=True);captures.append(dict(site=site,phase=phase,path=out.relative_to(ROOT).as_posix(),sha256=sha(out),source_glb_sha256=sha(model),type='Native arrival path terminal bearing; neutral workbench construction review, no canonical van or runtime acceptance'))
 v=BASE/('pilot-verification' if args.sites else 'verification');v.mkdir(parents=True,exist_ok=True);(v/'independent-sources.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_SOURCE_NOT_MAP_ACCEPTANCE',sites=results,checker_sha256=sha(Path(__file__))),indent=2)+'\n').encode())
 if captures:(v/'captures.json').write_bytes((json.dumps(captures,indent=2)+'\n').encode())
if __name__=='__main__':main()
