"""Wharf stair cells: constant flights, monolithic landings, rectangular roof openings."""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from append_static_glb import append
from compose_room_terrain import split_native
from build_roster_foundations import prism,material
from build_roster_floor_closures import difference
from planar_structure import plate
from build_circulation_construction import native_write
from build_deliberate_entrances import write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'wharf-stair-cells-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
def bb(o):
 v=[o.matrix_world@p.co for p in o.data.vertices];return [min(p.x for p in v),min(-p.y for p in v),max(p.x for p in v),max(-p.y for p in v),min(p.z for p in v),max(p.z for p in v)]
def main():
 roster=json.loads((BASE/'site-earthworks-pass/roster.json').read_text());s=next(s for s in roster['active_sources'] if s['id']=='WLD-06');folder=OUT/'wld-06';folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=set();built=[];stair_names=[];oldstairs={n.split(':',1)[-1] for n in meta['stair_components']}
 concrete=material('Wharf cast stair-cell concrete',(.39,.40,.38));wood=material('Wharf constant-width stair treads',(.38,.28,.15));steel=material('Wharf stair bearing steel',(.24,.26,.28));roofmat=material('Wharf continuous undercroft deck',(.27,.22,.16))
 holes=[rect(18,31.7,28.3,34.3),rect(35.7,31.7,46,34.3)];roofholes=[rect(18,31.45,28.3,34.55),rect(35.7,31.45,46,34.55)]
 def make(name,poly,top,depth,mat,role,link=None):
  result=plate(name,[poly],top,depth,mat) or []
  for o in result:
   o['role']=role
   if link:o['link']=link
   built.append(o)
   if role in ['structural-stair-step','structural-stair-landing']:stair_names.append(o.name)
  return result
 def box(name,a,b,c,d,bottom,top,mat,role,link=None):return make(name,rect(a,b,c,d),top,top-bottom,mat,role,link)
 for o in old:
  role=o.get('role','');link=o.get('link');zone=o.get('zone');bounds=bb(o)
  if o.name in oldstairs or link in ['L16','L17'] and role in ['structural-stair-step','structural-stair-landing','fitted library stair guard','structural side stringer']:removed.add(o.name)
  if zone=='undercroft' and role in ['floor','ceiling-cap','rough shell wall']:removed.add(o.name)
  if 'shaft rim' in o.name:removed.add(o.name)
  if role=='authored dock deck' and o.get('area') in ['west jetty','east pier-head landing']:removed.add(o.name)
  # Replace only the portion of the existing raised approach occupied by the new upper landing.
  if role=='authored retained dock-side walk' and bounds[0]<18 and bounds[1]<34.55 and bounds[3]>31.45:
   polys=[];o.data.calc_loop_triangles()
   for t in o.data.loop_triangles:
    ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
    if any(abs(p.z)>.00001 for p in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
    polys.extend(difference([(p.x,-p.y) for p in ps],rect(16,31.45,18,34.55)))
   for replacement in plate('Wharf approach terminating at full upper landing',polys,0,.16,o.data.materials[0]) or []:replacement['role']='authored retained dock-side walk';built.append(replacement)
   removed.add(o.name)
  if role=='continuous dock bearing grid' and any(bounds[0]<max(p[0] for p in cut) and bounds[2]>min(p[0] for p in cut) and bounds[1]<34.3 and bounds[3]>31.7 for cut in holes):
   polys=[];o.data.calc_loop_triangles()
   for t in o.data.loop_triangles:
    ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
    if any(abs(p.z-bounds[5])>.00001 for p in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
    pieces=[[(p.x,-p.y) for p in ps]]
    for cut in holes:pieces=[q for p in pieces for q in difference(p,cut)]
    polys.extend(pieces)
   for replacement in plate('Wharf bearing framed to rectangular well '+o.name,polys,bounds[5],bounds[5]-bounds[4],o.data.materials[0]) or []:replacement['role']='rectangular stair-well bearing';built.append(replacement)
   removed.add(o.name)
  if role=='fitted library circulation guard' and bounds[2]>16 and bounds[0]<28.3 and bounds[1]<34.55 and bounds[3]>31.45:
   removed.add(o.name)
   if bounds[0]<16:
    clone=o.copy();clone.data=o.data.copy();clone.name='Wharf approach guard ending at landing '+o.name;bpy.context.collection.objects.link(clone);bm=bmesh.new();bm.from_mesh(clone.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);plane=clone.matrix_world.inverted()@Vector((16,0,0));normal=clone.matrix_world.to_3x3().transposed()@Vector((1,0,0));bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.000001,plane_co=plane,plane_no=normal,clear_outer=True);bmesh.ops.holes_fill(bm,edges=[e for e in bm.edges if e.is_boundary],sides=0);bm.to_mesh(clone.data);bm.free();built.append(clone)
 # The cellar is one coherent structural cell; circulation is on its actual floor.
 for o in make('Wharf undercroft continuous base',rect(24,24,40,36),-2.5,.30,concrete,'floor'):o['zone']='undercroft'
 for a,c,label in [(16,24,'west'),(40,48,'east')]:box('Wharf '+label+' stair-cell foundation',a,31.45,c,34.55,-2.8,-2.5,concrete,'stair-cell structural base')
 for a,c,label in [(24,40,'north')]:box('Wharf cellar north wall',a,24,c,24.25,-2.8,-.3,concrete,'rectangular cellar wall')
 box('Wharf cellar south wall',24,35.75,40,36,-2.8,-.3,concrete,'rectangular cellar wall')
 for x0,x1,label in [(24,24.25,'west'),(39.75,40,'east')]:
  for z0,z1 in [(24,31.45),(34.55,36)]:box('Wharf cellar '+label+' wall '+str(z0),x0,z0,x1,z1,-2.8,-.3,concrete,'rectangular cellar wall')
 for a,c,label in [(16,24,'west'),(40,48,'east')]:
  for z0,z1 in [(31.45,31.7),(34.3,34.55)]:
   box('Wharf '+label+' stair retaining wall '+str(z0),a,z0,min(c,46),z1,-2.8,0,concrete,'stair-cell retaining wall')
   if c>46:box('Wharf east wall bearing under upper deck '+str(z0),46,z0,c,z1,-2.8,-.20,concrete,'stair-cell upper landing wall bearing')
 box('Wharf west full supported upper landing',16,31.7,18,34.3,-2.5,0,concrete,'structural-stair-landing','L16')
 box('Wharf east supported upper-landing base',46,31.7,48,34.3,-2.5,-.20,concrete,'stair-cell upper landing support')
 # Rectangular penetrations are defined by the stair cells, never the tread outline.
 for name,foot in [('cellar roof deck',rect(24,24,40,36)),('west jetty deck',rect(20,10,24,36)),('east pier-head deck',rect(40,26,50,39))]:
  polys=[foot]
  for cut in roofholes+([rect(20,36,44,54)] if name=='east pier-head deck' else []):polys=[q for p in polys for q in difference(p,cut)]
  for o in plate('Wharf '+name+' rectangular openings',polys,0,.30 if name=='cellar roof deck' else .20,roofmat) or []:o['role']='rectangular stair-cell roof or dock deck';built.append(o)
 # Cast rim beams carry all grade-level guards; the lower room retains2.2m clear height.
 for a,c,label in [(24,28.3,'west'),(35.7,40,'east')]:
  for z0,z1 in [(31.45,31.7),(34.3,34.55)]:box('Wharf '+label+' opening rim beam '+str(z0),a,z0,c,z1,-.30,0,concrete,'stair opening supported coping')
 for x0,x1,label in [(28.05,28.3,'west lower'),(35.7,35.95,'east lower')]:box('Wharf '+label+' opening end beam',x0,31.7,x1,34.3,-.30,0,concrete,'stair opening supported coping')
 flights=[('L16',18,26,True),('L17',38,46,False)]
 for link,a,c,descending in flights:
  n=14;run=(c-a)/n
  for i in range(n):
   top= -2.5*i/n if descending else -2.5+2.5*(i+1)/n
   box('Wharf rectangular '+link+' tread '+str(i),a+i*run,31.8,a+(i+1)*run,34.2,-2.5,top,wood,'structural-stair-step',link)
  # Real constant side stringers sit directly under both guard-post lines.
  for z0,z1 in [(31.77,31.98),(34.02,34.23)]:
   ya,yc=(0,-2.5) if descending else (-2.5,0);verts=[(a,-z0,ya-.05),(c,-z0,yc-.05),(c,-z1,yc-.05),(a,-z1,ya-.05),(a,-z0,ya-.45),(c,-z0,yc-.45),(c,-z1,yc-.45),(a,-z1,ya-.45)];mesh=bpy.data.meshes.new('Constant stair stringer');mesh.from_pydata(verts,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);mesh.materials.append(steel);o=bpy.data.objects.new('Wharf constant '+link+' stringer '+str(z0),mesh);bpy.context.collection.objects.link(o);o['role']='constant supported stair stringer';built.append(o)
 # Place the existing indexed guard model on full walls/coping/stringers.
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));template=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 def rail(name,p,q):
  run=math.hypot(q[0]-p[0],q[2]-p[2]);ux=(q[0]-p[0])/run;uz=(q[2]-p[2])/run;n=math.ceil(run/2)
  for i in range(n):
   o=bpy.data.objects.new('Wharf cell '+name+' '+str(i),template.copy());bpy.context.collection.objects.link(o)
   for v in o.data.vertices:
    along=(v.co.x+1)*run/n/2+i*run/n;v.co=(p[0]+ux*along-uz*v.co.y,-(p[2]+uz*along+ux*v.co.y),p[1]+(q[1]-p[1])*along/run+v.co.z)
   o['role']='fitted library supported stair-cell guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 for link,a,c,descending in flights:
  for z in [31.86,34.14]:rail(link+' flight handrail '+str(z),(a,(0 if descending else -2.5)-.05,z),(c,(-2.5 if descending else 0)-.05,z))
 for a,c,label in [(16,28.175,'west rim'),(35.825,46,'east rim')]:
  for z in [31.575,34.425]:rail(label+str(z),(a,0,z),(c,0,z))
 for x,label in [(28.175,'west lower return'),(35.825,'east lower return')]:rail(label,(x,0,31.575),(x,0,34.425))
 for z in [31.86,34.14]:rail('west upper handrail extension'+str(z),(16,0,z),(18,0,z));rail('east upper handrail extension'+str(z),(46,0,z),(46.6,0,z))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(o.name,'not closed');bm.to_mesh(o.data);bm.free()
 links={l['id']:l for l in data['links']};links['L18']['points']=[[46,0,33],[48,0,33],[48,0,37],[42,0,37],[32,0,45]];next(z for z in data['zones'] if z['id']=='undercroft')['height']=2.5
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'rectangular-stair-cells.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build/wharf-stair-cell-addition.glb';bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'rectangular-stair-cells.glb',remove_mesh_names=sorted(removed));parts=native_write(native,folder/'rectangular-stair-cells.hcs2',removed,built);layout=folder/'authored-layout.json';write(layout,data)
 # Verify actual output; no flared treads, tapered rims or old stair meshes remain.
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'rectangular-stair-cells.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 assert not any(o.get('link') in ['L16','L17'] and o.get('role')=='structural-stair-step' and not o.name.startswith('Wharf rectangular') for o in objs.values())
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap':o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1050;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Wharf rectangular stair cells');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;target=Vector((32,-33,-.5));cam.location=target+Vector((30,-38,42));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=43;cap=BASE/'captures/wharf-stair-cells-batch/wharf-stair-cells.png';cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 for key in ['flared_wing_parts','guard_flights','new_guard_parts','end_builder_sha256']:meta.pop(key,None)
 meta.update(status='WHARF_RECTANGULAR_STAIR_CELLS_RECONSTRUCTED_REVIEW_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],stair_components=stair_names,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'rectangular-stair-cells.glb'),physics_sha256=sha(folder/'rectangular-stair-cells.hcs2'),blend_sha256=sha(folder/'rectangular-stair-cells.blend'),stair_cell_builder='tools/rebuild_wharf_stair_cells.py',stair_cell_builder_sha256=sha(Path(__file__)),removed_failed_stair_parts=sorted(removed),stair_cells=dict(clear_well_width=2.6,gross_tread_width=2.4,walking_width=2,rise=2.5,run=8,risers=14,lower_floor_y=-2.5,roof_top_y=0,roof_depth=.3,cellar_clear_height=2.2,rectangular_roof_penetrations=roofholes,clear_openings=holes,upper_landings='West cast platform16..18; east existing full pier-head platform46..48',landings='Actual shared cellar floor; no route-box landings or flared extensions'),remaining='Independent movement and physical review; full map/load/terrain detail, finish and actual runtime integration remain open. No acceptance inferred from checks.',captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native actual reconstructed stair-cell close view; pitched roofs hidden, not gameplay')]);write(folder/'construction.json',meta)
 row=dict(id=s['id'],construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'rectangular-stair-cells.glb').relative_to(ROOT).as_posix(),native=(folder/'rectangular-stair-cells.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'rectangular-stair-cells.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256']);write(OUT/'roster.json',dict(active_sources=[row if r['id']=='WLD-06' else r for r in roster['active_sources']],sites=[dict(id=r['id'],decision='RECTANGULAR_WHARF_STAIR_CELLS_REBUILT_REVIEW_OPEN' if r['id']=='WLD-06' else r['decision']) for r in roster['sites']],baseline='development/levels/rebuild/site-earthworks-pass/roster.json'));print('WHARF_RECTANGULAR_STAIR_CELLS_BUILT',flush=True)
if __name__=='__main__':main()
