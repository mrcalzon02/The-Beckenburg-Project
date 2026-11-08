"""Independent saved-world/mesh/native/footing checks and actual paired renders."""
from pathlib import Path
import argparse,collections,json,math,struct,sys
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from plan_flat_terrain_repairs import sha,write
from compose_flat_terrain import tree
from compose_room_terrain import split_native
from append_static_glb import read
from check_macroterrain_exports import inspect
from review_roster_macroterrain import records
from export_roster_collision import face_keys,native_faces
from build_stair_bays import intersection
from structural_pass import deck_openings
from build_roster_decks import area
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-terrain-pass';CAP=BASE/'captures/flat-terrain-batch'
def point(p):return Vector((p[0],-p[2],p[1]))
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');parser.add_argument('--render',action='store_true');args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []);results=[];captures=[];CAP.mkdir(parents=True,exist_ok=True);plan=json.loads((OUT/'terrain-repair-plan.json').read_text())
 for path in sorted((OUT/'assemblies').glob('*/construction.json')):
  r=json.loads(path.read_text());site=r['id']
  if args.sites and site not in args.sites:continue
  author=json.loads((OUT/(site.lower()+'-world.json')).read_text());world=ROOT/r['world'];oldworld=ROOT/author['source_world'];source=ROOT/r['source_glb'];glb=path.parent/'cut-terrain.glb';native=path.parent/'cut-terrain.hcs2';old=ROOT/r['source_physics'];assert sha(source)==r['source_glb_sha256'] and sha(old)==r['source_physics_sha256'];assert sha(glb)==r['glb_sha256'] and sha(native)==r['physics_sha256'];assert all(sha(ROOT/p)==h for p,h in r['world_files_sha256'].items());assert all(sha(ROOT/p)==h for p,h in author['source_world_files_preserved'].items());assert sha(world/'tile_0_0.png')==sha(oldworld/'tile_0_0.png') and sha(world/'adaptive-topology.bin')==sha(oldworld/'adaptive-topology.bin');assert json.loads((ROOT/r['layout']).read_text())['links']==json.loads((ROOT/r['before_layout']).read_text())['links']
  a=inspect(source,False);b=inspect(glb,False);removed=set(r['removed_source_meshes']);assert set(a)-set(b)==removed;assert set(b)-set(a)==set(r['new_closed_parts'])|{r['new_terrain_mesh']}
  for name in set(a)-removed:assert a[name]==b[name],(site,name,'Source geometry/UV/image/pivot changed')
  doc,blob=read(source);after,binary=read(glb);assert binary[:len(blob)]==blob
  for key in ['meshes','accessors','bufferViews','materials','images','textures','samplers']:assert after.get(key,[])[:len(doc.get(key,[]))]==doc.get(key,[])
  for i,node in enumerate(doc['nodes']):
   expected=dict(node)
   if node.get('name') in removed:expected.pop('mesh',None)
   assert after['nodes'][i]==expected
  prior=split_native(old);current=split_native(native);kept=[p for p in prior if p[0] not in set(r['removed_native_parts'])];actualterrain=split_native(world/'terrain-physics.hcs2');assert current[:len(kept)]==kept and current[len(kept):len(kept)+len(actualterrain)]==actualterrain;objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};assert face_keys(records(objects[r['new_terrain_mesh']]))==face_keys(native_faces(world/'terrain-physics.hcs2'));closed=[]
  for name in r['new_closed_parts']:
   obj=objects[name];part=next(p for p in current if p[0]==name);assert part[0] not in {p[0] for p in prior};bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=False)>1e-8;bm.free();assert obj.data.uv_layers;raw=part[2];start=2+len(name.encode())+5;ts=[struct.unpack_from('>9fB',raw,start+37*i) for i in range(part[1])];assert face_keys(records(obj))==face_keys([(t[:9],t[9]) for t in ts]),(site,name,'Export/native mismatch');closed.append(name)
  ground=tree(world/'terrain-physics.hcs2');prior_ground=tree(oldworld/'terrain-physics.hcs2');oldvertices={(t[i],t[i+2]):t[i+1] for t,f in native_faces(oldworld/'terrain-physics.hcs2') for i in [0,3,6]};newvertices={(t[i],t[i+2]):t[i+1] for t,f in native_faces(world/'terrain-physics.hcs2') for i in [0,3,6]};assert oldvertices.keys()==newvertices.keys();assert all(newvertices[k]<=v+1e-6 for k,v in oldvertices.items());lowered=sum(newvertices[k]<v-1e-6 for k,v in oldvertices.items());footing_rows=[]
  for ext in r['footing_extensions']:
   for i,((x,z),top,bottom) in enumerate(zip(ext['footprint'],ext['top_y'],ext['bottom_y'])):
    h=ground.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0];assert h is not None and bottom<=h.y-.34;parent=objects[ext['source_bearing']];ps=[parent.matrix_world@v.co for v in parent.data.vertices];oldbottom=min(v.z for v in ps if math.hypot(v.x-x,-v.y-z)<.0001);assert abs(top-oldbottom)<1e-5;footing_rows.append(dict(parent=ext['source_bearing'],extension=ext['name'],point=[x,z],top_y=top,bottom_y=bottom,soil_y=h.y))
  receiver_triangles=native_faces(world/'terrain-physics.hcs2');walkway_rows=[];data=json.loads((ROOT/r['layout']).read_text());openings=deck_openings(0,data)
  for bed in r.get('rooted_walkway_beds',[]):
   obj=objects[bed['name']];vs=[obj.matrix_world@v.co for v in obj.data.vertices];assert abs(max(v.z for v in vs))<1e-5 and abs(min(v.z for v in vs)-bed['bottom_y'])<1e-5
   for opening in openings:assert area(intersection(bed['footprint'],opening['polygon']))<1e-8,(site,bed['name'],'Real stair opening filled')
   xyz,flags=receiver_triangles[bed['receiver_native_triangle_index']];assert list(xyz)==bed['receiver_native_triangle_xyz'];tri=[Vector(xyz[i:i+3]) for i in [0,3,6]];normal=(tri[1]-tri[0]).cross(tri[2]-tri[0]);assert abs(normal.y)>1e-10
   projected=[(p.x,p.z) for p in tri];assert abs(area(intersection(bed['footprint'],projected))-area(bed['footprint']))<1e-7
   actual={(float(v.x),float(-v.y)) for v in vs if abs(v.z)<1e-5};expected={(struct.unpack('f',struct.pack('f',x))[0],struct.unpack('f',struct.pack('f',z))[0]) for x,z in bed['footprint']};assert actual==expected
   for x,z in bed['terrain_probe_points']:
    h=tri[0].y-(normal.x*(x-tri[0].x)+normal.z*(z-tri[0].z))/normal.y;assert bed['bottom_y']<=h-.14;walkway_rows.append(dict(name=bed['name'],point=[x,z],bottom_y=bed['bottom_y'],soil_y=h,receiver_native_triangle=bed['receiver_native_triangle_index']))
  verification=json.loads((world/'verification.json').read_text());assert verification['disk_reopen_verified'] and verification['punched_body_clear_and_unpunched_body_blocked'];bpy.ops.wm.open_mainfile(filepath=str(path.parent/'cut-terrain.blend'));assert face_keys(records(bpy.data.objects[r['new_terrain_mesh']]))==face_keys(native_faces(world/'terrain-physics.hcs2'));images={n.image for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert all(i.packed_file and i.size[0]>0 and i.size[1]>0 and len(i.pixels)>0 for i in images);results.append(dict(id=site,status='PASS_SCOPED_SAVED_WORLD_EXPORT_NATIVE_EDITABLE_PRESERVATION',glb_sha256=sha(glb),native_sha256=sha(native),blend_sha256=sha(path.parent/'cut-terrain.blend'),contract_sha256=sha(path),packed_images=len(images),lowered_native_vertices=lowered,native_vertices=len(newvertices),raised_native_vertices=0,source_height_and_topology_bytes_unchanged=True,clear_saved_hole_probes=verification['clear_hole_probes'],retained_native_parts=len(kept),new_closed_parts=closed,footing_contact_samples=footing_rows,rooted_walkway_samples=walkway_rows));print('FLAT_TERRAIN_INDEPENDENT',site,'PASS',lowered,'vertices lowered',flush=True)
  if not args.render:continue
  data=json.loads((ROOT/r['layout']).read_text());case=next(c for c in plan['sites'] if c['id']==site);hit=case['actual_failed_contacts'][len(case['actual_failed_contacts'])//2];eye=[float(hit['x']),float(hit['y'])+1.62,float(hit['z'])];link=next(l for l in data['links'] if l['id']==hit['link'].split(':')[0]);segment=int(hit['link'].split(':')[1].split('/')[0]);a,b=link['points'][segment:segment+2];dx,dz=b[0]-a[0],b[2]-a[2];run=math.hypot(dx,dz);target=[eye[0]+dx/run*4,eye[1]-.1,eye[2]+dz/run*4]
  for phase,model in [('before',source),('after',glb)]:
   bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(model));scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=900;scene.render.resolution_y=650;scene.render.resolution_percentage=100;scene.view_settings.view_transform='Standard';bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.clip_start=.03;cam.data.type='ORTHO';x0,z0,x1,z1=data['site_bounds'];span=max(x1-x0,z1-z0);centre=point([(x0+x1)/2,1,(z0+z1)/2]);cam.data.ortho_scale=span*1.5;cam.location=centre+Vector((span*.8,-span*.9,span*.8));cam.rotation_euler=(centre-cam.location).to_track_quat('-Z','Y').to_euler()
   for view in ['whole-site','terrain-only','body-contact']:
    if view=='terrain-only':
     for obj in bpy.context.scene.objects:
      if obj.type=='MESH':obj.hide_render=not obj.get('role','').startswith('saved editor terrain')
    if view=='body-contact':
     for obj in bpy.context.scene.objects:
      if obj.type=='MESH':obj.hide_render=False
     cam.data.type='PERSP';cam.data.lens=24;cam.location=point(eye);cam.rotation_euler=(point(target)-cam.location).to_track_quat('-Z','Y').to_euler()
    out=CAP/(site.lower()+'-'+view+'-'+phase+'.png');scene.render.filepath=str(out);bpy.ops.render.render(write_still=True);captures.append(dict(site=site,phase=phase,view=view,path=out.relative_to(ROOT).as_posix(),sha256=sha(out),source_glb_sha256=sha(model),eye=eye if view=='body-contact' else None,type='Native saved terrain/circulation construction review; before contact camera may be buried, no canonical van or runtime/landscape acceptance'))
 destination=OUT/('pilot-verification' if args.sites else 'verification');write(destination/'independent-sources.json',dict(status='PASS_SCOPED_TERRAIN_NATIVE_SOURCE_NOT_MAP_ACCEPTANCE',sites=results,checker_sha256=sha(Path(__file__))))
 if captures:write(destination/'captures.json',captures)
if __name__=='__main__':main()
