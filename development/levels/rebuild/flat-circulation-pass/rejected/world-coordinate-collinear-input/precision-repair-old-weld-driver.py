"""Repair generated CSG precision at actual native float coordinates.
Only current newly authored wall/cap parts can change; source assets stay exact.
"""
from pathlib import Path
import argparse,json,struct,sys
import bpy,bmesh
from mathutils import Matrix
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records,sha
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass'
def repair(site):
 folder=BASE/site.lower();path=folder/'construction.json';r=json.loads(path.read_text());glb=folder/'open-circulation.glb';native=folder/'open-circulation.hcs2';old=split_native(ROOT/r['source_physics']);kept_names={n for n,ct,raw in old}-set(r['removed_native_parts']);current=split_native(native);owned={n for n,ct,raw in current}-kept_names;input_hashes=dict(glb_sha256=sha(glb),native_sha256=sha(native));assert len(owned)==r['new_parts'];bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));built=[];replacements=[]
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH' or obj.name not in owned:continue
  assert obj.get('role') in ['rough shell wall','rough opening header','ceiling-cap','floor'];before=obj.name
  for vertex in obj.data.vertices:vertex.co=obj.matrix_world@vertex.co
  obj.matrix_world=Matrix.Identity(4);bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00002);bmesh.ops.dissolve_degenerate(bm,dist=.00001,edges=list(bm.edges));bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(before,'precision repair is open or inverted');bm.to_mesh(obj.data);bm.free();obj.name='Precision '+before;built.append(obj);replacements.append(dict(before=before,after=obj.name,weld_metres=.00002,degenerate_edge_metres=.00001))
 assert len(built)==len(owned)
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'open-circulation.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for obj in built:obj.select_set(True)
 extra=ROOT/'build/flat-precision-addition.glb';bpy.ops.export_scene.gltf(filepath=str(extra),export_format='GLB',use_selection=True,export_extras=True);append(glb,extra,glb,remove_mesh_names=sorted(owned));kept=[p for p in current if p[0] not in owned];parts=[(o.name,records(o)) for o in built]
 for name,ts in parts:
  for xyz,flags in ts:
   a=[xyz[3+i]-xyz[i] for i in range(3)];b=[xyz[6+i]-xyz[i] for i in range(3)];cross=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];assert sum(v*v for v in cross)>=1e-24,(name,'Actual native float triangle still degenerate')
 payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(ct for n,ct,raw in kept)+sum(len(ts) for n,ts in parts))+b''.join(raw for n,ct,raw in kept)
 for name,ts in parts:
  raw=name.encode();payload+=struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
 native.write_bytes(payload);r['precision_repair']=dict(input_hashes=input_hashes,replacements=replacements,generator_sha256=sha(Path(__file__)));mapping={v['before']:v['after'] for v in replacements}
 for row in r['shell_changes']:
  if row.get('new_mesh') in mapping:row['new_mesh']=mapping[row['new_mesh']]
 layout=ROOT/r['layout'];data=json.loads(layout.read_text());data['physical_flat_circulation_revision']['shell_changes']=r['shell_changes'];data['native_precision_repair']=r['precision_repair'];layout.write_bytes((json.dumps(data,indent=2)+'\n').encode());r['layout_sha256']=sha(layout);r['new_triangles']=sum(len(ts) for name,ts in parts);r['glb_sha256']=sha(glb);r['physics_sha256']=sha(native);path.write_bytes((json.dumps(r,indent=2)+'\n').encode());print('NATIVE_PRECISION_REPAIR',len(parts),r['new_triangles'],flush=True)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+',default=['WLD-09']);args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
 for site in args.sites:repair(site)
if __name__=='__main__':main()
