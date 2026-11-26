"""Rebuild four precision-degenerate generated wall profiles as closed slabs.
Actual front/back silhouettes intersect; original assets and movement stay exact.
"""
from pathlib import Path
import json,struct,sys
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_stair_bays import intersection
from build_roster_floor_closures import clean_polygon
from build_roster_decks import area
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass'
def invalid(ts):
 for xyz,flags in ts:
  a=[xyz[3+i]-xyz[i] for i in range(3)];b=[xyz[6+i]-xyz[i] for i in range(3)];c=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
  if sum(v*v for v in c)<1e-24:return True
 return False
def main():
 folder=BASE/'asylum';path=folder/'construction.json';r=json.loads(path.read_text());glb=folder/'open-circulation.glb';native=folder/'open-circulation.hcs2';source_kept={n for n,ct,raw in split_native(ROOT/r['source_physics'])}-set(r['removed_native_parts']);current=split_native(native);owned={n for n,ct,raw in current}-source_kept;inputs=dict(glb_sha256=sha(glb),native_sha256=sha(native));bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));built=[];removed=[];proof=[]
 for obj in list(bpy.context.scene.objects):
  if obj.type!='MESH' or obj.name not in owned or not invalid(records(obj)):continue
  assert obj.get('role')=='rough shell wall' and len(obj.data.materials)==1;oldname=obj.name;extras=dict(obj.items());ps=[obj.matrix_world@v.co for v in obj.data.vertices];spans=[max(p[i] for p in ps)-min(p[i] for p in ps) for i in [0,1]];thin=min([0,1],key=lambda i:spans[i]);along=1-thin;low=min(p[thin] for p in ps);high=max(p[thin] for p in ps);assert .1<high-low<.3,(oldname,'Not an authored planar wall');obj.data.calc_loop_triangles();sides=[]
  for depth in [low,high]:
   polys=[]
   for tri in obj.data.loop_triangles:
    vs=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
    if any(abs(v[thin]-depth)>.00001 for v in vs):continue
    poly=[(round(v[along],5),round(v.z,5)) for v in vs]
    if area(poly)>1e-10:polys.append(poly)
   assert polys,(oldname,'Missing actual face silhouette');sides.append(polys)
  common=[]
  for a in sides[0]:
   for b in sides[1]:
    poly=intersection(a,b)
    if len(poly)<3:continue
    poly=clean_polygon([(round(u,5),round(v,5)) for u,v in poly])
    if area(poly)>1e-10:common.append(poly)
  assert common,(oldname,'No shared physical front/back wall section');made=plate('Profile '+oldname,common,high,high-low,obj.data.materials[0]);assert made
  for new in made:
   for vertex in new.data.vertices:
    p=vertex.co.copy();vertex.co=(p.x,p.z,-p.y) if thin==1 else (p.z,p.x,-p.y)
   bm=bmesh.new();bm.from_mesh(new.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.to_mesh(new.data);bm.free()
   for key,value in extras.items():new[key]=value
   assert not invalid(records(new)),(oldname,'Profile still has rejected native triangle');built.append(new)
  proof.append(dict(before=oldname,after=[o.name for o in made],wall_normal_axis=thin,thickness=high-low,source_front_triangles=len(sides[0]),source_back_triangles=len(sides[1]),method='Closed extrusion of actual front/back silhouette intersection; ten-micrometre profile grid, no source-resource or movement tolerance changes',maximum_profile_coordinate_snap=.000005));removed.append(oldname);bpy.data.objects.remove(obj,do_unlink=True)
 assert len(removed)==4,(len(removed),'Expected actual four bad generated parts');bpy.ops.wm.save_as_mainfile(filepath=str(folder/'open-circulation.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for obj in built:obj.select_set(True)
 addition=ROOT/'build/flat-wall-profiles.glb';bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',use_selection=True,export_extras=True);append(glb,addition,glb,remove_mesh_names=removed);kept=[p for p in current if p[0] not in removed];parts=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(ct for n,ct,raw in kept)+sum(len(ts) for n,ts in parts))+b''.join(raw for n,ct,raw in kept)
 for name,ts in parts:
  raw=name.encode();payload+=struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
 native.write_bytes(payload);added={n:ct for n,ct,raw in split_native(native) if n not in source_kept};mapping={p['before']:p['after'] for p in proof}
 for change in r['shell_changes']:
  oldname=change.get('new_mesh')
  if oldname in mapping:
   names=mapping[oldname];change['new_mesh']=names[0] if len(names)==1 else None;change['new_mesh_parts']=names
 r['wall_profile_reconstruction']=dict(inputs=inputs,parts=proof,generator_sha256=sha(Path(__file__)));layout=ROOT/r['layout'];data=json.loads(layout.read_text());data['wall_profile_reconstruction']=r['wall_profile_reconstruction'];data['physical_flat_circulation_revision']['shell_changes']=r['shell_changes'];layout.write_bytes((json.dumps(data,indent=2)+'\n').encode());r.update(new_parts=len(added),new_triangles=sum(added.values()),glb_sha256=sha(glb),physics_sha256=sha(native),layout_sha256=sha(layout));path.write_bytes((json.dumps(r,indent=2)+'\n').encode());print('CLOSED_WALL_PROFILES',len(removed),len(parts),r['new_triangles'],flush=True)
if __name__=='__main__':main()
