"""Compose actual saved site earthworks into the current authored maps."""
from pathlib import Path
import sys,json,csv,math,struct,copy,io
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import material,prism
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'institution-earthworks-pass'
ASYLUM_GUIDES={
 'L71':[[7,0,35],[7,0,11],[16,0,11],[16,0,6]],
 'L72':[[7,0,35],[18,0,35],[18,0,34.5]],
 'L73':[[7,0,35],[7,0,52],[18,0,52],[18,0,64],[34,0,64]],
 'L74':[[7,0,35],[7,0,52],[18,0,52],[18,0,64],[30,0,64],[30,0,54],[27.5,0,54],[27.5,0,16],[32,0,16],[32,0,18],[44.5,0,18],[54,0,18],[54.5,0,18],[55,0,18.5],[55,0,19.5],[56.5,0,19.5],[57,0,19]],
 'L76':[[7,0,35],[7,0,52],[18,0,52],[18,0,64],[62,0,64],[62,0,48],[55.5,0,48],[55.5,0,37]]}

# Service access follows the existing staff entrance and supported stair-head bypass.
ASYLUM_GUIDES['L76']=[*ASYLUM_GUIDES['L74'],[56,0,20],[55.5,0,24],[55.5,0,37]]

APRONS={'ASYLUM':(26,61,35,62),'PRISON':(10,71,18,72)}

def build(row):
 s=row['source'];site=s['id'];world=ROOT/row['world'];sourceworld=ROOT/row['source_world'];(world/'verification.json').write_bytes((world/'earthworks-verification.json').read_bytes());write(world/'source-provenance.json',dict(source_world=row['source_world'],source_files_sha256=row['source_files_sha256'],earthworks_features=row['features'],source_height_and_punched_topology_preserved=True,authoring='Actual TerrainLayerStack geographic sculpt, not route support'));folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());assert all(sha(sourceworld/p)==h for p,h in row['source_files_sha256'].items());verification=json.loads((world/'earthworks-verification.json').read_text());assert sha(sourceworld/'tile_0_0.png')==sha(world/'tile_0_0.png')==verification['source_height_sha256'];assert sha(sourceworld/'adaptive-topology.bin')==sha(world/'adaptive-topology.bin');assert (sourceworld/'authored-cuts.tsv').read_bytes()==(world/'authored-cuts.tsv').read_bytes();assert sha(world/'terrain-physics.hcs2')==verification['physics_sha256']
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role','').startswith('saved editor terrain'));assert face_keys(records(old))==face_keys(native_faces(sourceworld/'terrain-physics.hcs2'));oldname=old.name;removed_walks=[o.name for o in bpy.context.scene.objects if site=='WLD-06' and o.type=='MESH' and o.get('role')=='authored retained dock-side walk' and min(-(o.matrix_world@v.co).y for v in o.data.vertices)>=53.999];
 for name in removed_walks:bpy.data.objects.remove(bpy.data.objects[name],do_unlink=True)
 mats=list(old.data.materials);bpy.data.objects.remove(old,do_unlink=True);vs=[];faces=[]
 for path in sorted((world/'scene-package/meshes').glob('site*.obj')):
  offset=len(vs)
  for line in path.read_text().splitlines():
   p=line.split()
   if p and p[0]=='v':x,y,z=map(float,p[1:]);vs.append((x,-z,y))
   elif p and p[0]=='f':faces.append(tuple(offset+int(i.split('/')[0])-1 for i in p[1:]))
 mesh=bpy.data.meshes.new(site+' saved geographic sculpt');mesh.from_pydata(vs,[],faces);mesh.update();obj=bpy.data.objects.new('Saved editor geographic earthworks '+site,mesh);bpy.context.collection.objects.link(obj);obj['role']='saved editor terrain / geographic arrival and site clearings';obj['saved_world']=row['world'];obj['saved_physics_sha256']=sha(world/'terrain-physics.hcs2');obj['saved_topology_sha256']=sha(world/'adaptive-topology.bin')
 for mat in mats:mesh.materials.append(mat)
 gravel=material('Existing site earthworks gravel',(.36,.34,.30));dirt=material('Woodland clearing earth',(.30,.24,.17));gravel_index=len(mesh.materials);mesh.materials.append(gravel);dirt_index=len(mesh.materials);mesh.materials.append(dirt)
 grid=list(csv.DictReader((ROOT/row['program']/'saved-grid.tsv').open(),delimiter='\t'));samples={(round(float(r['x']),4),round(float(r['z']),4)):r for r in grid};xs=sorted({float(r['x']) for r in grid});zs=sorted({float(r['z']) for r in grid});x0,z0=xs[0],zs[0];step=xs[1]-xs[0];uv=mesh.uv_layers.new(name='World metre terrain UV')
 for f in mesh.polygons:
  points=[mesh.vertices[i].co for i in f.vertices];x=sum(p.x for p in points)/len(points);z=-sum(p.y for p in points)/len(points);key=(round(x0+round((x-x0)/step)*step,4),round(z0+round((z-z0)/step)*step,4));sample=samples.get(key);paint=int(sample['material']) if sample else 0
  f.material_index=gravel_index if paint==6 else dirt_index if paint==2 else 1 if len(mats)>1 and f.normal.z<.93 else 2 if len(mats)>2 and sum(p.z for p in points)/len(points)<-.9 else 0
  for i in f.loop_indices:
   p=mesh.vertices[mesh.loops[i].vertex_index].co;uv.data[i].uv=(p.x/8,p.y/8)
 expected=face_keys(native_faces(world/'terrain-physics.hcs2'));assert face_keys(records(obj))==expected
 aprons=[]
 if site in APRONS:
  a,b,c,d=APRONS[site];mat=next(o.data.materials[0] for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='soil-bearing entrance landing');apron=prism(site+' full-width public entry forecourt apron',[(a,b),(c,b),(c,d),(a,d)],0,[-.35]*4,mat);apron['role']='soil-bearing entrance forecourt apron';apron['support']='Continuous full-width forecourt extension, foundation embedded below saved graded earth';aprons.append(apron)
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'saved-earthworks.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT');obj.select_set(True);
 for apron in aprons:apron.select_set(True)
 addition=ROOT/'build'/('earthworks-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'saved-earthworks.glb',remove_mesh_names=[oldname]+removed_walks);parts=split_native(native);kept=[p for p in parts if not p[0].startswith('terrain-') and p[0].split(':',1)[-1] not in [oldname]+removed_walks];ground=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',ground);target=folder/'saved-earthworks.hcs2';extra=io.BytesIO();apron_parts=[]
 for apron in aprons:
  ts=records(apron);name=apron.name.encode();extra.write(struct.pack('>H',len(name))+name+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:extra.write(struct.pack('>9fB',*xyz,flags))
  apron_parts.append((apron.name,ts))
 target.write_bytes(struct.pack('>III',magic,len(kept)+count+len(apron_parts),sum(p[1] for p in kept)+total+sum(len(ts) for _,ts in apron_parts))+b''.join(p[2] for p in kept)+ground[12:]+extra.getvalue());assert split_native(target)[:len(kept)]==kept
 if site=='ASYLUM':
  data.setdefault('authored_exterior_anchors',{})['grounds']=dict(point=[7,0,35],reason='Actual west patient garden outside all buildings; north bypass remains in saved landform')
  for link in data['links']:
   if link['id'] in ASYLUM_GUIDES:link['points']=ASYLUM_GUIDES[link['id']]
   if link['id'] in ['L74','L76']:link['threshold']=[54,0,18]
 if site=='PRISON':
  next(l for l in data['links'] if l['id']=='L066')['points']=[[38,0,81],[34,0,81],[34,0,73],[14,0,73],[14,0,65]]
 world_files={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')};data['authoritative_saved_terrain']=dict(world=row['world'],files_sha256=world_files,source_world=row['source_world'],earthworks_features=row['features'],basement_punches_preserved_exactly=True);layout=folder/'authored-layout.json';write(layout,data)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'saved-earthworks.glb'));terrain=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Saved editor geographic earthworks'));assert face_keys(records(terrain))==expected
 for name,ts in apron_parts:assert face_keys(records(bpy.data.objects[name]))==face_keys(ts)
 for o in bpy.context.scene.objects:
  if o.type=='MESH' and (o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']):o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Saved site earthworks review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);targetPoint=Vector(((a+c)/2,-(b+d)/2,0));cam.location=targetPoint+Vector((span*.45,-span*.6,span));cam.rotation_euler=(targetPoint-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/institution-earthworks-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='SAVED_GEOGRAPHIC_EARTHWORKS_CONSTRUCTION_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),saved_world=row['world'],saved_world_files_sha256=world_files,earthworks=row['features'],entrance_apron_extension=dict(bounds=list(APRONS[site]),top_y=0,foundation_bottom_y=-.35) if site in APRONS else None,earthworks_verification=verification,source_terrain=row['source_world'],removed_redundant_cottage_walks=removed_walks,source_terrain_physics_sha256=sha(sourceworld/'terrain-physics.hcs2'),terrain_writer='tools/java/RebuildInstitutionEarthworks.java',terrain_writer_sha256=sha(ROOT/'tools/java/RebuildInstitutionEarthworks.java'),terrain_composer='tools/compose_institution_earthworks.py',terrain_composer_sha256=sha(Path(__file__)),glb_sha256=sha(folder/'saved-earthworks.glb'),physics_sha256=sha(folder/'saved-earthworks.hcs2'),blend_sha256=sha(folder/'saved-earthworks.blend'),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported actual saved terrain earthworks with roof cutaway; not gameplay')]);write(folder/'construction.json',meta);print('COMPOSED_SAVED_EARTHWORKS',site,'nonterrain records retained byte-identical',len(kept),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'saved-earthworks.glb').relative_to(ROOT).as_posix(),native=(folder/'saved-earthworks.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'saved-earthworks.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 program=json.loads((OUT/'earthworks-program.json').read_text());rows={r['id']:r for r in program['sites']};r=json.loads((BASE/'institution-courts-pass/roster.json').read_text());sources=[build(rows[s['id']]) if s['id'] in rows else s for s in r['active_sources']]
 for site in r['sites']:
  if site['id'] in rows:site['decision']='SAVED_INSTITUTION_EARTHWORKS_CONSTRUCTED_RUNTIME_OPEN'
 write(OUT/'roster.json',dict(active_sources=sources,sites=r['sites'],baseline='development/levels/rebuild/institution-courts-pass/roster.json'));print('SAVED_INSTITUTION_EARTHWORKS_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
