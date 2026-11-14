"""Physical perimeter walk, aligned service doorway, and mezzanine stair bay."""
from pathlib import Path
import sys,json,copy,math,re,io,struct
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
import build_stair_bays as bays
from build_deliberate_entrances import solid,write
from build_roster_foundations import material
from build_stair_landings import rect
from planar_structure import plate
from build_roster_floor_closures import difference
from review_roster_macroterrain import records,sha
from append_static_glb import append
from compose_room_terrain import split_native
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'circulation-construction-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
ROUTES={'WLD-06':{'L07':[[32,0,45],[22,0,45],[22,0,39],[14,0,39],[14,0,29],[22,0,29],[22,0,23]],'L14':[[12,0,19],[16,0,19],[16,0,28],[16,0,29],[14,0,29],[14,0,33],[18,0,33]],'L15':[[18,0,33],[14,0,33],[14,0,39],[22,0,39],[22,0,45],[32,0,45]]},'WLD-08':{'L13':[[11,0,17],[11,0,16],[7,0,16],[7,0,26],[7,0,35],[11,0,35]]},'WLD-13':{'L09':[[36,0,41],[36,0,28],[30,0,28],[30,0,26],[30,0,20]]}}
WALLS={'generator':{'E':[(19,2.2)],'S':[(7,2.4)]},'lab':{'N':[(7,2.4)],'E':[(35,2.2)],'S':[(11,2.2)]}}
def native_write(source,target,removed,built):
 keep=[p for p in split_native(source) if p[0].split(':',1)[-1] not in removed];parts=[(o.name,records(o)) for o in built];stream=io.BytesIO();stream.write(struct.pack('>III',0x48435332,len(keep)+len(parts),sum(p[1] for p in keep)+sum(len(ts) for _,ts in parts)))
 for _,_,raw in keep:stream.write(raw)
 for name,ts in parts:
  raw=name.encode();stream.write(struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:stream.write(struct.pack('>9fB',*xyz,flags))
 target.write_bytes(stream.getvalue());return parts

def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];data=json.loads((ROOT/s['layout']).read_text());meta=json.loads((ROOT/s['construction']).read_text());intermediate=None
 if site=='WLD-13':
  bays.OUT=OUT/'stair-source';plan=dict(points={'L19':[[36,0,41],[36,0,53],[27,0,53],[27,0,51],[27,3,43],[27,3,41]]},build=['L19'],shared={},preserve_upper_hubs=False,reason='House the gallery flight within the mezzanine strip; close the former transverse shaft and reserve the new longitudinal opening.')
  bays.build(site,plan,[(source.parent,source.stem)],'gallery-stair');source=bays.OUT/site.lower()/'gallery-stair.glb';native=source.with_suffix('.hcs2');intermediate=json.loads((source.parent/'construction.json').read_text());data=json.loads((ROOT/intermediate['layout']).read_text());meta=copy.deepcopy(intermediate)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));originals=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];proof=[]
 for link in data['links']:
  if link['id'] in ROUTES[site]:link['points']=ROUTES[site][link['id']]
  if site=='WLD-06' and link['id'] in ROUTES[site]:link['width']=1.5
 for o in originals:
  key=o.get('link') or (re.match(r'(L\d+) support',o.name).group(1) if re.match(r'(L\d+) support',o.name) else None)
  if o.get('role','').startswith('rough route support') and key in ROUTES[site]:removed.add(o.name)
 if site=='WLD-08':
  wallmat=next(o.data.materials[0] for o in originals if o.get('role')=='rough shell wall' and o.data.materials)
  for o in originals:
   if o.get('zone') in WALLS and o.get('role') in ['rough shell wall','rough opening header']:removed.add(o.name)
  for z in data['zones']:
   if z['id'] not in WALLS:continue
   a,b,c,d=z['bounds'];lo=z['y'];hi=lo+z['height']
   for side,start,end,fixed in [('N',a,c,b),('S',a,c,d),('W',b,d,a),('E',b,d,c)]:
    def wall(l,r,bottom,top,label):
     if r-l<.00001:return
     bounds=[l,fixed,r,fixed+.24] if side=='N' else [l,fixed-.24,r,fixed] if side=='S' else [fixed,l,fixed+.24,r] if side=='W' else [fixed-.24,l,fixed,r]
     built.append(solid('Research aligned aisle '+z['id']+' '+side+label,bounds,bottom,top,wallmat,'aligned service-aisle wall',z['id']))
    cursor=start
    for center,width in WALLS[z['id']].get(side,[]):wall(cursor,center-width/2,lo,hi,'pier');wall(center-width/2,center+width/2,2.45,hi,'lintel');cursor=center+width/2
    wall(cursor,end,lo,hi,'end')
  station=next(o for o in originals if o.name=='level_blockout_research_sample_station.001');moved=station.copy();moved.data=station.data.copy();bpy.context.collection.objects.link(moved);matrix=station.matrix_world.copy();matrix.translation.x-=2;matrix.translation.y-=.24;moved.matrix_world=matrix;moved.name='Relocated library sample station west of Research service aisle';removed.add(station.name);built.append(moved);proof.append(dict(asset='level_blockout_research_sample_station',translation_world_x=-2,translation_world_z=.24,reason='Retain the actual indexed asset on the existing lab floor; clear the doorway/aisle at x7',bottom_y=0))
 survivors=[o for o in originals if o.name not in removed]
 for o in originals:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 originals=survivors
 # Retain and transform the actual indexed guard model; never substitute generic bars.
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));rail=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH');template=rail.data.copy()
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 def rail_run(label,p,q,side,half):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux=(q[0]-p[0])/run;uz=(q[2]-p[2])/run;n=max(1,math.ceil(run/2));piece=run/n
  for i in range(n):
   o=bpy.data.objects.new(label+' '+str(i),template.copy());bpy.context.collection.objects.link(o)
   for v in o.data.vertices:
    along=(v.co.x+1)*piece/2+i*piece;off=side*(half+v.co.y);v.co=(p[0]+ux*along-uz*off,-(p[2]+uz*along+ux*off),p[1]+(q[1]-p[1])*along/run+v.co.z)
   o['role']='fitted library circulation guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 if site=='WLD-06':
  terrain=next(o for o in originals if o.get('role','').startswith('saved editor terrain'));soil=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(p.vertices) for p in terrain.data.polygons]);mat=material('Wharf perimeter piled boardwalk',(.28,.22,.15));steel=material('Wharf perimeter bridge bearing steel',(.22,.24,.24));ps=ROUTES[site]['L07'];polys=[];segments=list(zip(ps,ps[1:]))+[( [16,0,28],[16,0,29]),([14,0,33],[18,0,33])]
  allpoints=[p for edge in segments for p in edge]
  for p,q in segments:polys.append(rect(p,q,.9))
  for p in allpoints:polys.append([(p[0]-.9,p[2]-.9),(p[0]+.9,p[2]-.9),(p[0]+.9,p[2]+.9),(p[0]-.9,p[2]+.9)])
  perimeter=plate('Wharf temporary complete walk perimeter',polys,0,.16,mat) or []
  boundary=[]
  for o in perimeter:
   uses={}
   for f in o.data.polygons:
    points=[o.data.vertices[i].co for i in f.vertices]
    if any(abs(v.z)>.00001 for v in points):continue
    ids=list(f.vertices)
    for a,b in zip(ids,ids[1:]+ids[:1]):
     edge=tuple(sorted((a,b)));uses[edge]=uses.get(edge,0)+1
   for (a,b),count in uses.items():
    if count!=1:continue
    p,q=[o.data.vertices[i].co for i in [a,b]];boundary.append(([p.x,0,-p.y],[q.x,0,-q.y]))
  for o in perimeter:bpy.data.objects.remove(o,do_unlink=True)
  # Deduct existing exact top faces so joints do not double the floor finish.
  polys=[piece for poly in polys for piece in difference(poly,[(4,10),(20,10),(20,28),(4,28)])]
  # The landing ends at the stair lip; it must not cap the first descending treads.
  polys=[piece for poly in polys for piece in difference(poly,[(18,31.8),(27,31.8),(27,34.2),(18,34.2)])];deck=plate('Wharf continuous west perimeter boardwalk',polys,0,.16,mat) or []
  for o in deck:o['role']='piled perimeter boardwalk deck';built.append(o)
  def pier(x,z):
   probes=[(x+dx,z+dz) for dx,dz in [(-.2,-.2),(.2,-.2),(.2,.2),(-.2,.2)]];hits=[soil.ray_cast(Vector((xx,-zz,30)),Vector((0,0,-1)))[0] for xx,zz in probes];assert all(h is not None for h in hits),(site,'unfounded pier',x,z);bottom=min(h.z for h in hits)-.35;built.append(solid('Wharf native-soil pier '+str(len(proof)),[x-.2,z-.2,x+.2,z+.2],bottom,-.14,mat,'terrain-rooted boardwalk pile','exterior'));proof.append(dict(pier=[x,z],terrain_y=[h.z for h in hits],bottom_y=bottom))
  # The short return beside the punched stair bay is carried by a real steel bridge.
  for x,z in [(32,45),(30,45),(27,45),(24,45),(22,45),(22,42),(22,39),(20,39),(17,39),(14,39),(14,36),(14,33),(14,30),(14,29),(22,27),(22,25),(22,23)]:pier(x,z)
  for i,(p,q) in enumerate(segments):
   if p==[16,0,28]:continue
   footprint=rect(p,q,.15);built.append(solid('Wharf deck spine '+str(i),[min(x for x,z in footprint),min(z for x,z in footprint),max(x for x,z in footprint),max(z for x,z in footprint)],-.46,-.14,steel,'continuous deck bearing spine','exterior'))
  built.append(solid('Wharf bridge west crosshead',[13.7,28,14.3,30],-.9,-.5,steel,'pier-to-girder crosshead','exterior'))
  for z in [28.25,29.75]:built.append(solid('Wharf eight-metre bridge beam '+str(z),[13.8,z-.15,22.2,z+.15],-.66,-.14,steel,'perimeter bridge girder','exterior'))
  built.append(solid('Wharf bridge east cantilever bearing',[21.7,26.8,22.3,29.95],-.86,-.14,steel,'perimeter bridge crosshead','exterior'))
  built.append(solid('Wharf stair-head branch bearing',[13.8,32.8,18,33.2],-.66,-.14,steel,'perimeter bridge crosshead','exterior'))
  built.append(solid('Wharf boathouse threshold tie beam',[13.8,27.8,18.2,28.2],-.46,-.14,steel,'perimeter bridge crosshead','exterior'))
  openings=[[32,0,45],[22,0,23],[16,0,28],[18,0,33]]
  for i,(p,q) in enumerate(boundary):
   if any(min(math.dist(p,a),math.dist(q,a))<1.3 for a in openings):continue
   rail_run('Wharf continuous perimeter guard '+str(i),p,q,1,0)
 if site=='WLD-13':
  # A housed longitudinal flight and actual open mezzanine edges receive continuous library guards.
  for side in [-1,1]:rail_run('Cannery housed gallery flight guard '+str(side),[27,0,51],[27,3,43],side,.88)
  rail_run('Cannery continuous mezzanine open-edge guard',[29.88,3,26.24],[29.88,3,55.76],1,0)
  for x in [25.98,28.02]:rail_run('Cannery actual shaft-edge guard '+str(x),[x,3,43],[x,3,51.12],1,0)
  rail_run('Cannery shaft lower return guard',[25.98,3,51.12],[28.02,3,51.12],1,0)
 # Validate all generated closed solids in their final world positions.
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name);bm.to_mesh(o.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'constructed-circulation.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 add=ROOT/'build'/('circulation-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(add),export_format='GLB',export_extras=True,use_selection=True);append(source,add,folder/'constructed-circulation.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'constructed-circulation.hcs2',removed,built);layout=folder/'authored-layout.json';write(layout,data)
 # Independently import final exported additions and compare their exact native triangles.
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'constructed-circulation.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objects[name]))==face_keys(ts),(site,name,'native mismatch')
 for o in objects.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Circulation construction cutaway');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/circulation-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 oldmeta=json.loads((ROOT/s['construction']).read_text());meta.update(status='PHYSICAL_CIRCULATION_RECONSTRUCTION_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=oldmeta.get('source_segment_map',{}),before_stair_components=oldmeta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'constructed-circulation.glb'),physics_sha256=sha(folder/'constructed-circulation.hcs2'),blend_sha256=sha(folder/'constructed-circulation.blend'),circulation_builder='tools/build_circulation_construction.py',circulation_builder_sha256=sha(Path(__file__)),intermediate_stair_source=intermediate,construction_proof=proof,removed_parts=sorted(removed),changed_flat_routes=list(ROUTES[site]),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported circulation cutaway, upper roof context hidden, not gameplay')]);write(folder/'construction.json',meta);print('CIRCULATION_BUILT',site,len(built),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'constructed-circulation.glb').relative_to(ROOT).as_posix(),native=(folder/'constructed-circulation.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'constructed-circulation.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads((BASE/'entrance-construction-pass/roster.json').read_text());rows=[build(s) if s['id'] in ROUTES else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(id=s['id'],decision='PHYSICAL_CIRCULATION_RECONSTRUCTED_RUNTIME_OPEN' if s['id'] in ROUTES else s['decision']) for s in r['sites']],baseline='development/levels/rebuild/entrance-construction-pass/roster.json'));print('CIRCULATION_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
