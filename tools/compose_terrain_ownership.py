"""Compose actual saved site earthworks into the current authored maps."""
from pathlib import Path
import sys,json,csv,math,struct,copy,io
import bmesh
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from planar_structure import plate
from build_architectural_doorways import frame_template
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import material
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'terrain-ownership-construction-pass'
def build(row):
 s=row['source'];site=s['id'];world=ROOT/row['world'];sourceworld=ROOT/row['source_world'];(world/'verification.json').write_bytes((world/'earthworks-verification.json').read_bytes());write(world/'source-provenance.json',dict(source_world=row['source_world'],source_files_sha256=row['source_files_sha256'],earthworks_features=row['features'],source_height_and_punched_topology_preserved=True,authoring='Actual TerrainLayerStack geographic sculpt, not route support'));folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());assert all(sha(sourceworld/p)==h for p,h in row['source_files_sha256'].items());verification=json.loads((world/'earthworks-verification.json').read_text());assert sha(sourceworld/'tile_0_0.png')==sha(world/'tile_0_0.png')==verification['source_height_sha256'];assert sha(sourceworld/'adaptive-topology.bin')==sha(world/'adaptive-topology.bin');assert (sourceworld/'authored-cuts.tsv').read_bytes()==(world/'authored-cuts.tsv').read_bytes();assert sha(world/'terrain-physics.hcs2')==verification['physics_sha256']
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=bpy.data.objects[row['original_terrain_mesh']];oldname=old.name;removed_walks=[];extras=[];portal=None
 if site!='WLD-16':assert face_keys(records(old))==face_keys(native_faces(sourceworld/'terrain-physics.hcs2'))
 else:
  original_parts=[p for p in split_native(native) if p[0].split(':',1)[-1]==oldname];assert len(original_parts)==1;probe=ROOT/'build/motel-original-owner.hcs2';probe.write_bytes(struct.pack('>III',0x48435332,1,original_parts[0][1])+original_parts[0][2]);assert face_keys(records(old))==face_keys(native_faces(probe))
 concrete=material('Constructed geographic interface concrete',(.39,.39,.37))
 def make(name,poly,y,depth,role,zone=None):
  for part in plate(site+' '+name,[poly],y,depth,concrete):
   part['role']=role
   if zone:part['zone']=zone
   extras.append(part)
 if site=='WLD-16':
  removed_walks.extend(o.name for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='floor' and o.get('zone')!='stage')
  for z in data['zones']:
   if z['kind']=='inside' or z['id'] in ['westwalk','eastwalk','northwalk']:
    a,b,c,d=z['bounds'];make(z['id']+' continuous founded floor',[(a,b),(c,b),(c,d),(a,d)],0,.30 if z['kind']=='inside' else .22,'constructed continuous room or arcade floor',z['id'])
 for o in bpy.context.scene.objects:
  if o.type=='MESH' and (o.get('role','').startswith('rough route support') or o.get('role') in ['structural-rooted-circulation-bed','structural-arrival-end-bearing']):removed_walks.append(o.name)
 if site=='ACADEMY':
  removed_walks.extend(o.name for o in bpy.context.scene.objects if o.type=='MESH' and any(n in o.name for n in ['Wall reception S','Header reception S']))
  left,right=27-1.8*.62,27+1.8*.62;head=2.3*2.275/2.125
  for label,a,c,lo,hi in [('west pier',22,left,0,3.6),('east pier',right,32,0,3.6),('lintel',left,right,head,3.6)]:make('reception south '+label,[(a,52.76),(c,52.76),(c,53),(a,53)],hi,hi-lo,'constructed normal arrival doorway','reception')
  frame=frame_template();frame.name='ACADEMY reception south indexed arrival frame'
  for v in frame.data.vertices:
   p=frame.matrix_world@v.co;v.co=(27-p.x*1.8,-52.88-p.y*.24/.18,p.z*2.3/2.125)
  frame.matrix_world.identity();frame['role']='indexed arrival doorway frame';frame['portal_id']='ACADEMY-arrival-reception-south-27';extras.append(frame)
  portal=dict(id=frame['portal_id'],axis='z',boundary=53,along=27,clear_width=1.8,clear_height=2.3,sill_y=0,frame_depth=.24,frame_span=[52.76,53],frame_center=[27,0,52.88],rooms=['reception'],door_type='service')
  guides={'L02':[[32.5,0,61.5],[27,0,61.5],[27,0,49],[27,0,43.5],[32,0,43.5],[32,0,32],[32.5,0,32],[32.5,0,27.5]],'L03':[[32.5,0,27.5],[32.5,0,32],[32,0,32],[32,0,43.5],[27,0,43.5],[27,0,49]]}
  for link in data['links']:
   if link['id'] in guides:link['points']=guides[link['id']]
  data['authored_doorways']=data.get('authored_doorways',[])+[portal]
 if site=='WLD-16':make('north arcade full-width flush service apron',[(39,17),(41,17),(41,20),(39,20)],0,.22,'soil-bearing arcade transition apron')
 if site=='ACADEMY':make('reception full-width visitor landing',[(25.5,53),(28.5,53),(28.5,55),(25.5,55)],0,.25,'soil-bearing visitor entrance landing')
 if site=='WLD-09':
  make('complete bridge arrival forecourt',[(28,56),(54,56),(54,66),(28,66)],0,.60,'constructed bridge arrival forecourt')
  for x in [35.76,53.76]:make('arrival bearing on workshop side wall '+str(x),[(x,56),(x+.48,56),(x+.48,62),(x,62)],-.60,2.5,'arrival load-bearing pier on original lower wall')
  for z in [57.85,61.85]:make('transverse arrival girder '+str(z),[(35.76,z),(54.24,z),(54.24,z+.30),(35.76,z+.30)],-.60,1.2,'arrival structural girder')
  removed_walks.extend(o.name for o in bpy.context.scene.objects if o.type=='MESH' and o.get('zone')=='crest' and o.get('role')=='floor')
  make('complete structural crest deck',[(8,48),(72,48),(72,56),(8,56)],0,.60,'constructed dam crest deck','crest')
  for x in [35.76,53.76]:make('crest bearing on workshop side wall '+str(x),[(x,49.75),(x+.48,49.75),(x+.48,55),(x,55)],-.60,2.5,'crest load-bearing pier on original lower wall')
  for z in [49.85,53.85]:make('transverse crest girder '+str(z),[(35.76,z),(54.24,z),(54.24,z+.30),(35.76,z+.30)],-.60,1.2,'crest structural girder')
  previous=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'));template=next(o for o in set(bpy.context.scene.objects)-previous if o.type=='MESH').data.copy()
  for o in set(bpy.context.scene.objects)-previous:bpy.data.objects.remove(o,do_unlink=True)
  for label,p,q in [('arrival west void edge',(28.14,0,55.86),(28.14,0,65.86)),('crest south west void edge',(8.14,0,55.86),(28.14,0,55.86)),('crest west return',(8.14,0,48.14),(8.14,0,55.86)),('crest south east void edge',(53.86,0,55.86),(71.86,0,55.86)),('crest east return',(71.86,0,48.14),(71.86,0,55.86)),('arrival east void edge',(53.86,0,55.86),(53.86,0,65.86)),('north west void edge',(36,0,48.14),(66.8,0,48.14)),('north east void return',(69.2,0,48.14),(71.86,0,48.14)),('south west return',(36,0,55.86),(38.6,0,55.86)),('south east return',(41.4,0,55.86),(54,0,55.86))]:
   length=math.dist(p,q);n=math.ceil(length/2);ux=(q[0]-p[0])/length;uz=(q[2]-p[2])/length
   for i in range(n):
    mesh=template.copy()
    for v in mesh.vertices:
     t=(v.co.x+1)*length/n/2+i*length/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[2]+uz*t+ux*off),v.co.z)
    part=bpy.data.objects.new(site+' supported crest guard '+label+' '+str(i),mesh);bpy.context.collection.objects.link(part);part['role']='indexed supported crest edge guard';part['source_asset']='level_blockout_guardrail';extras.append(part)
  next(link for link in data['links'] if link['id']=='L02')['points']=[[36,0,69],[40,0,69],[40,0,52]]
  next(link for link in data['links'] if link['id']=='L10')['points']=[[70,0,35],[70,0,42],[68,0,42],[68,0,52],[40,0,52]]
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
 for part in extras:
  bm=bmesh.new();bm.from_mesh(part.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,part.name;bm.to_mesh(part.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'saved-earthworks.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT');obj.select_set(True);
 for part in extras:part.select_set(True)
 addition=ROOT/'build'/('earthworks-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'saved-earthworks.glb',remove_mesh_names=[oldname]+removed_walks);parts=split_native(native);kept=[p for p in parts if not p[0].startswith('terrain-') and p[0].split(':',1)[-1] not in [oldname]+removed_walks];ground=(world/'terrain-physics.hcs2').read_bytes();magic,count,total=struct.unpack_from('>III',ground);target=folder/'saved-earthworks.hcs2';extra=io.BytesIO();extra_count=0
 for part in extras:
  ts=records(part);name=part.name.encode();extra.write(struct.pack('>H',len(name))+name+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:extra.write(struct.pack('>9fB',*xyz,flags))
  extra_count+=len(ts)
 target.write_bytes(struct.pack('>III',magic,len(kept)+count+len(extras),sum(p[1] for p in kept)+total+extra_count)+b''.join(p[2] for p in kept)+ground[12:]+extra.getvalue());assert split_native(target)[:len(kept)]==kept
 world_files={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')};data['authoritative_saved_terrain']=dict(world=row['world'],files_sha256=world_files,source_world=row['source_world'],earthworks_features=row['features'],basement_punches_preserved_exactly=True);layout=folder/'authored-layout.json';write(layout,data)
 extra_keys={o.name:face_keys(records(o)) for o in extras}
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'saved-earthworks.glb'));
 for name,key in extra_keys.items():assert face_keys(records(bpy.data.objects[name]))==key
 terrain=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith('Saved editor geographic earthworks'));assert face_keys(records(terrain))==expected
 for o in bpy.context.scene.objects:
  if o.type=='MESH' and (o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']):o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Saved site earthworks review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);targetPoint=Vector(((a+c)/2,-(b+d)/2,0));cam.location=targetPoint+Vector((span*.45,-span*.6,span));cam.rotation_euler=(targetPoint-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/terrain-ownership-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(world=row['world'],world_files_sha256=world_files,terrain_seed_builder='tools/java/RosterTerrainBuild.java' if site=='WLD-16' else None,terrain_seed_builder_sha256=sha(ROOT/'tools/java/RosterTerrainBuild.java') if site=='WLD-16' else None,seed_input_files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/row['seed_input']).iterdir() if p.is_file()} if site=='WLD-16' else {},original_terrain_mesh=oldname,removed_interface_parts=removed_walks,new_interface_parts=list(extra_keys),doorways=meta.get('doorways',[])+([portal] if portal else []),program_files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/row['program']).iterdir() if p.is_file()},status='SAVED_GEOGRAPHIC_EARTHWORKS_CONSTRUCTION_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),saved_world=row['world'],saved_world_files_sha256=world_files,earthworks=row['features'],earthworks_verification=verification,source_terrain=row['source_world'],removed_redundant_cottage_walks=removed_walks,source_terrain_physics_sha256=sha(sourceworld/'terrain-physics.hcs2'),terrain_writer='tools/java/RebuildInstitutionEarthworks.java',terrain_writer_sha256=sha(ROOT/'tools/java/RebuildInstitutionEarthworks.java'),terrain_composer='tools/compose_terrain_ownership.py',terrain_composer_sha256=sha(Path(__file__)),glb_sha256=sha(folder/'saved-earthworks.glb'),physics_sha256=sha(folder/'saved-earthworks.hcs2'),blend_sha256=sha(folder/'saved-earthworks.blend'),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported actual saved terrain earthworks with roof cutaway; not gameplay')]);write(folder/'construction.json',meta);print('COMPOSED_SAVED_EARTHWORKS',site,'nonterrain records retained byte-identical',len(kept),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'saved-earthworks.glb').relative_to(ROOT).as_posix(),native=(folder/'saved-earthworks.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'saved-earthworks.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 program=json.loads((OUT/'earthworks-program.json').read_text());rows={r['id']:r for r in program['sites']};r=json.loads((BASE/'service-portal-construction-pass/roster.json').read_text());sources=[build(rows[s['id']]) if s['id'] in rows else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=sources,sites=[dict(s,decision='SAVED_SITE_EARTHWORKS_CONSTRUCTED_RUNTIME_OPEN' if s['id'] in rows else s['decision']) for s in r['sites']],baseline='development/levels/rebuild/service-portal-construction-pass/roster.json'));print('SAVED_EARTHWORKS_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
