"""Compose exact saved editor terrain into latest map sources; retain all assets.
Extend only previously rooted footings exposed by the new cut. Reuse the saved
WLD09 arrival slab study after the physical terrain conflict is removed.
"""
from pathlib import Path
import argparse,json,math,struct,sys
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from plan_flat_terrain_repairs import sha,write
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records
from export_roster_collision import face_keys,native_faces
from build_cut_walkway_beds import build as build_walkway_beds
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-terrain-pass'
def tree(path):
 points=[];fs=[]
 for xyz,flags in native_faces(path):
  i=len(points);points.extend([xyz[a:a+3] for a in [0,3,6]]);fs.append((i,i+1,i+2))
 return BVHTree.FromPolygons(points,fs)
def build(site):
 author=json.loads((OUT/(site.lower()+'-world.json')).read_text());source=author['source'];glb=ROOT/source['glb'];native=ROOT/source['native'];oldworld=ROOT/author['source_world'];world=ROOT/author['world'];assert sha(glb)==source['glb_sha256'] and sha(native)==source['native_sha256'];assert all(sha(ROOT/p)==h for p,h in author['world_files_sha256'].items());prior=json.loads((ROOT/source['construction']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(glb));soils=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role','').startswith('saved editor terrain')];assert len(soils)==1,(site,'Need one exact saved terrain mesh');oldsoil=soils[0];assert face_keys(records(oldsoil))==face_keys(native_faces(oldworld/'terrain-physics.hcs2'));removed_mesh=oldsoil.name;mats=list(oldsoil.data.materials);bpy.data.objects.remove(oldsoil,do_unlink=True);points=[];faces=[]
 for chunk in sorted((world/'scene-package/meshes').glob('site*.obj')):
  offset=len(points)
  for row in chunk.read_text().splitlines():
   p=row.split()
   if p and p[0]=='v':x,y,z=map(float,p[1:]);points.append((x,-z,y))
   elif p and p[0]=='f':faces.append(tuple(offset+int(v.split('/')[0])-1 for v in p[1:]))
 mesh=bpy.data.meshes.new('Actual editor circulation cut terrain');mesh.from_pydata(points,[],faces);mesh.update();soil=bpy.data.objects.new('Saved editor circulation terrain '+site,mesh);bpy.context.scene.collection.objects.link(soil);soil['role']='saved editor terrain / lower-only circulation cuts and retained basement punches'
 for mat in mats:mesh.materials.append(mat)
 uv=mesh.uv_layers.new(name='World metre terrain UV')
 for face in mesh.polygons:
  if len(mats)>2:face.material_index=1 if face.normal.z<.93 else 2 if sum(mesh.vertices[i].co.z for i in face.vertices)/len(face.vertices)<-.9 else 0
  for li in face.loop_indices:
   v=mesh.vertices[mesh.loops[li].vertex_index].co;uv.data[li].uv=(v.x/8,v.y/8)
 assert face_keys(records(soil))==face_keys(native_faces(world/'terrain-physics.hcs2'));oldground=tree(oldworld/'terrain-physics.hcs2');newground=tree(world/'terrain-physics.hcs2');built=[];extensions=[];skipped=[]
 bearings=json.loads((BASE/'terrain-fit-pass'/site.lower()/'construction.json').read_text())['construction']
 for bearing in bearings:
  if bearing.get('kind') not in ['skirt','pile'] or not bpy.data.objects.get(bearing['name']):continue
  obj=bpy.data.objects[bearing['name']];poly=bearing['footprint'];verts=[obj.matrix_world@v.co for v in obj.data.vertices];tops=[]
  for x,z in poly:
   near=[v.z for v in verts if math.hypot(v.x-x,-v.y-z)<.0001];assert near,(site,obj.name,x,z);tops.append(min(near))
  hits=[newground.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0] for x,z in poly];oldhits=[oldground.ray_cast(Vector((x,40,z)),Vector((0,-1,0)),80)[0] for x,z in poly]
  newly_exposed=[i for i,h in enumerate(hits) if h is not None and oldhits[i] is not None and oldhits[i].y>=tops[i]-.02 and h.y<tops[i]-.02]
  if not newly_exposed:continue
  if any(h is None for h in hits):skipped.append(dict(name=obj.name,reason='Existing actual punched void at a footing corner; do not invent a soil root'));continue
  bottoms=[min(t-.05,h.y-.35) for t,h in zip(tops,hits)];n=len(poly);vs=[(x,-z,y) for levels in [bottoms,tops] for (x,z),y in zip(poly,levels)];fs=[tuple(range(n)),tuple(reversed(range(n,2*n)))]+[(i,(i+1)%n,(i+1)%n+n,i+n) for i in range(n)];me=bpy.data.meshes.new('Terrain-cut rooted extension');me.from_pydata(vs,[],fs);me.update();bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.to_mesh(me);bm.free();ext=bpy.data.objects.new('Circulation cut footing '+site+' '+str(len(built)),me);bpy.context.scene.collection.objects.link(ext);me.materials.append(obj.data.materials[0]);ext['role']='structural soil-bearing extension';ext['source_bearing']=obj.name;layer=me.uv_layers.new(name='Metre footing UV')
  for f in me.polygons:
   for li in f.loop_indices:
    v=me.vertices[me.loops[li].vertex_index].co;layer.data[li].uv=(v.x+v.y,v.z) if abs(f.normal.z)<.5 else (v.x,v.y)
  built.append(ext);extensions.append(dict(name=ext.name,source_bearing=obj.name,footprint=poly,top_y=tops,bottom_y=bottoms,old_soil_y=[h.y if h else None for h in oldhits],new_soil_y=[h.y for h in hits],newly_exposed_corners=newly_exposed,embedding_metres=.35))
 layout=json.loads((ROOT/source['layout']).read_text());walkways,walkway_proof,walkway_deferred=build_walkway_beds(site,layout,author['program'],newground,native_faces(world/'terrain-physics.hcs2'));built.extend(walkways)
 cap=[]
 if site=='WLD-09':
  rejected=BASE/'arrival-bearing-pass/rejected/terrain-blocked-wld09';saved=json.loads((rejected/'construction.json').read_text());assert saved['source_glb_sha256']==source['glb_sha256'] and saved['source_physics_sha256']==source['native_sha256'];before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(rejected/'supported-arrival.glb'));added=set(bpy.context.scene.objects)-before
  for obj in added:
   if obj.type=='MESH' and obj.get('role')=='structural-arrival-end-bearing':cap.append(obj);built.append(obj)
   else:bpy.data.objects.remove(obj,do_unlink=True)
  assert len(cap)==1;expected=[p for p in split_native(rejected/'supported-arrival.hcs2') if p[0]==cap[0].name];assert len(expected)==1;record=expected[0][2];offset=2+len(cap[0].name.encode())+5;ts=[struct.unpack_from('>9fB',record,offset+37*i) for i in range(expected[0][1])];assert face_keys(records(cap[0]))==face_keys([(t[:9],t[9]) for t in ts]),'Reused cap geometry changed'
 folder=OUT/'assemblies'/site.lower();folder.mkdir(parents=True,exist_ok=True);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'cut-terrain.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT');soil.select_set(True)
 for obj in built:obj.select_set(True)
 addition=ROOT/'build'/('flat-terrain-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(glb,addition,folder/'cut-terrain.glb',remove_mesh_names=[removed_mesh]);oldparts=split_native(native);terrain_names=set(next(r['terrain_parts'] for r in json.loads((OUT/'terrain-repair-plan.json').read_text())['sites'] if r['id']==site));kept=[p for p in oldparts if p[0] not in terrain_names];newterrain=split_native(world/'terrain-physics.hcs2');addedparts=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(kept)+len(newterrain)+len(addedparts),sum(n for name,n,raw in kept+newterrain)+sum(len(ts) for name,ts in addedparts))+b''.join(raw for name,n,raw in kept+newterrain)
 for name,ts in addedparts:
  text=name.encode();payload+=struct.pack('>H',len(text))+text+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
 (folder/'cut-terrain.hcs2').write_bytes(payload);layout=json.loads((ROOT/source['layout']).read_text());layout['saved_circulation_terrain_revision']=dict(world=author['world'],source_world=author['source_world'],program=author['program'],footing_extensions=extensions,reused_arrival_study=bool(cap),rooted_walkway_beds=walkway_proof);newlayout=folder/'authored-layout.json';write(newlayout,layout)
 r={k:prior[k] for k in ['id','stair_components','source_segment_map']};r.update(status='SAVED_EDITOR_CIRCULATION_TERRAIN_SOURCE_CANDIDATE_NOT_MAP_ACCEPTANCE',source_contract=source['construction'],source_contract_sha256=sha(ROOT/source['construction']),source_glb=source['glb'],source_glb_sha256=source['glb_sha256'],source_physics=source['native'],source_physics_sha256=source['native_sha256'],before_layout=source['layout'],before_layout_sha256=sha(ROOT/source['layout']),before_segment_map=prior.get('source_segment_map',{}),before_stair_components=prior['stair_components'],layout=newlayout.relative_to(ROOT).as_posix(),layout_sha256=sha(newlayout),glb_sha256=sha(folder/'cut-terrain.glb'),physics_sha256=sha(folder/'cut-terrain.hcs2'),removed_source_meshes=[removed_mesh],removed_native_parts=sorted(terrain_names),new_terrain_mesh=soil.name,native_terrain_parts=[n for n,ct,raw in newterrain],new_closed_parts=[o.name for o in built],new_closed_triangles=sum(len(ts) for name,ts in addedparts),world=author['world'],world_files_sha256=author['world_files_sha256'],footing_extensions=extensions,unresolved_footings=skipped,rooted_walkway_beds=walkway_proof,deferred_walkway_beds=walkway_deferred,walkway_builder_sha256=sha(ROOT/'tools/build_cut_walkway_beds.py'),reused_rejected_arrival_cap=[o.name for o in cap],composer_sha256=sha(Path(__file__)),remaining='Actual native/current-route/standing/vertical regression, physical footing probes, exported/editable/visual checks and all per-map terrain/edge/runtime/services/art/network/hardware gates; no unlock or production-ready claim.');write(folder/'construction.json',r);print('FLAT_TERRAIN_COMPOSITE',site,len(newterrain),'soil parts',len(built),'closed additions',flush=True)
def main():
 p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
 for site in args.sites:build(site)
if __name__=='__main__':main()
