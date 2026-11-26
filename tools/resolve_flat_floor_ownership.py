"""Remove only original rough route prisms fully owned by real equal-height floors.
Never remove props, stair treads/landings, partial exterior bearings or any floor.
"""
from pathlib import Path
import argparse,json,struct,sys
import bpy
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from build_roster_floor_closures import difference
from build_roster_decks import area
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import sha
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass'
def empty_glb(path):
 doc=dict(asset=dict(version='2.0'),buffers=[dict(byteLength=0)],scenes=[dict(nodes=[])],scene=0);raw=json.dumps(doc,separators=(',',':')).encode();raw+=b' '*((-len(raw))%4);path.write_bytes(struct.pack('<III',0x46546c67,2,12+8+len(raw)+8)+struct.pack('<II',len(raw),0x4e4f534a)+raw+struct.pack('<II',0,0x004e4942))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []);empty=ROOT/'build/empty-flat-static.glb';empty_glb(empty)
 for path in sorted(BASE.glob('*/construction.json')):
  r=json.loads(path.read_text())
  if args.sites and r['id'] not in args.sites:continue
  if 'floor_surface_ownership' in r:continue
  glb=path.parent/'open-circulation.glb';native=path.parent/'open-circulation.hcs2';input_hashes=dict(glb_sha256=sha(glb),native_sha256=sha(native));bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));floors={};proof=[];removed=[]
  source_parts={n:(ct,raw) for n,ct,raw in split_native(ROOT/r['source_physics'])};parts=split_native(native);current_parts={n:(ct,raw) for n,ct,raw in parts};names={n.split(':',1)[-1]:n for n in source_parts}
  for obj in bpy.context.scene.objects:
   if obj.type!='MESH' or obj.get('role')!='floor':continue
   obj.data.calc_loop_triangles()
   for tri in obj.data.loop_triangles:
    ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
    if max(v.z for v in ps)-min(v.z for v in ps)>.00001 or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
    floors.setdefault(round(ps[0].z,4),[]).append((obj.name,[(v.x,-v.y) for v in ps],min((obj.matrix_world@v.co).z for v in obj.data.vertices)))
  for obj in list(bpy.context.scene.objects):
   if obj.type!='MESH' or not obj.get('role','').startswith('rough route support') or obj.name not in names:continue
   native_name=names[obj.name];assert current_parts[native_name]==source_parts[native_name];assert native_name not in r['stair_components'];points=[obj.matrix_world@v.co for v in obj.data.vertices];top=max(v.z for v in points);bottom=min(v.z for v in points)
   if len({round(v.z,5) for v in points})!=2 or {(round(v.x,5),round(v.y,5)) for v in points}!={(round(v.x,5),round(v.y,5)) for v in points if abs(v.z-top)<.00001}:continue
   polys=[];obj.data.calc_loop_triangles()
   for tri in obj.data.loop_triangles:
    ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
    if all(abs(v.z-top)<.00001 for v in ps) and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:polys.append([(v.x,-v.y) for v in ps])
   if not polys:continue
   original_area=sum(area(p) for p in polys);owners=[];remaining=polys
   for name,cut,floor_bottom in floors.get(round(top,4),[]):
    if floor_bottom>bottom+.00001:continue
    before=sum(area(p) for p in remaining);remaining=[q for p in remaining for q in difference(p,cut)];after=sum(area(p) for p in remaining)
    if before-after>1e-9:owners.append(name)
    if not remaining:break
   remaining_area=sum(area(p) for p in remaining)
   if remaining_area>1e-9:continue
   proof.append(dict(name=obj.name,native_name=native_name,top_y=top,bottom_y=bottom,complete_floor_depth_ownership=True,original_top_polygons=polys,original_top_area_m2=original_area,unowned_top_area_m2=remaining_area,actual_floor_owners=sorted(set(owners)),source_native_record_sha256=__import__('hashlib').sha256(source_parts[native_name][1]).hexdigest(),reason='Complete actual equal-height floor coverage, not a reservation rectangle; no geometry needed for duplicate rough prism'));removed.append(obj.name);bpy.data.objects.remove(obj,do_unlink=True)
  if removed:
   append(glb,empty,glb,remove_mesh_names=removed);removed_native={names[n] for n in removed};kept=[p for p in parts if p[0] not in removed_native];native.write_bytes(struct.pack('>III',0x48435332,len(kept),sum(ct for n,ct,raw in kept))+b''.join(raw for n,ct,raw in kept));bpy.ops.wm.save_as_mainfile(filepath=str(path.parent/'open-circulation.blend'),compress=True);r['removed_source_meshes'].extend(removed);r['removed_native_parts'].extend(sorted(removed_native))
  r['floor_surface_ownership']=dict(removed_redundant_prisms=proof,input_hashes=input_hashes,generator_sha256=sha(Path(__file__)),limitation='Only completely covered original rough flat-route prisms; partial overlays, other duplicate structural surfaces, all optical/runtime and whole-map support acceptance remain open');layout=ROOT/r['layout'];data=json.loads(layout.read_text());data['floor_surface_ownership']=r['floor_surface_ownership'];layout.write_bytes((json.dumps(data,indent=2)+'\n').encode());r['layout_sha256']=sha(layout);r['glb_sha256']=sha(glb);r['physics_sha256']=sha(native);path.write_bytes((json.dumps(r,indent=2)+'\n').encode());print('FLAT_FLOOR_OWNERSHIP',r['id'],len(removed),flush=True)
if __name__=='__main__':main()
