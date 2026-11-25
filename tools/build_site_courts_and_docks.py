"""Build architectural courtyards and dock areas, never extruded route lines."""
from pathlib import Path
import sys,json,math,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'development/levels/blockouts')]
from build_roster_foundations import prism,material
from build_roster_floor_closures import difference
from planar_structure import plate
from structural_pass import deck_openings
from review_roster_macroterrain import records,sha
from append_static_glb import append
from export_roster_collision import face_keys
from build_circulation_construction import native_write
from build_deliberate_entrances import solid,write
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'courts-and-docks-pass';RAIL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def rect(a,b,c,d):return [(a,b),(c,b),(c,d),(a,d)]
AREAS={
'WLD-06':[('quay platform',rect(20,36,44,54),'timber'),('west jetty',rect(20,10,24,36),'timber'),('east jetty and stair-head platform',rect(40,10,44,26),'timber'),('east pier-head landing',rect(40,26,50,39),'timber'),('cross dock',rect(20,6,44,10),'timber')],
'WLD-08':[('raised courtyard terrace',rect(20,36,44,56),'paved'),('compound service forecourt',rect(2,56,62,64),'paved'),('arrival apron',rect(24,64,40,70),'paved')],
'WLD-13':[('shore loading dock',rect(24,0,48,14),'timber'),('cannery service yard',[(8,56),(48,56),(48,44),(64,44),(64,64),(44,72),(24,72),(8,64)],'paved')]}
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0;bpy.ops.import_scene.gltf(filepath=str(source));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=[];built=[];proof=[]
 for o in old:
  if site=='WLD-06' and o.get('role')=='floor' and o.get('zone') in ['quay','pier-w','pier-e','crossdock']:removed.append(o.name)
  if site in ['WLD-08','WLD-13'] and o.get('role') in ['supported exterior entrance pavement','soil-rooted approach bearing']:removed.append(o.name)
  if site=='WLD-08' and o.get('role') in ['rough shell wall','aligned service-aisle wall'] and o.get('zone') in ['lab','dorm']:
   vs=[o.matrix_world@v.co for v in o.data.vertices];xs=[v.x for v in vs]
   if (o.get('zone')=='lab' and min(xs)>19.7) or (o.get('zone')=='dorm' and max(xs)<44.3):removed.append(o.name)
 terrain=next(o for o in old if o.get('role','').startswith('saved editor terrain'));soil=BVHTree.FromPolygons([terrain.matrix_world@v.co for v in terrain.data.vertices],[tuple(f.vertices) for f in terrain.data.polygons]);timber=material(site+' dock structural timber',(.27,.22,.16));paving=material(site+' courtyard concrete',(.40,.39,.36));rubble=material(site+' compacted courtyard fill and retaining footing',(.31,.29,.25));steel=material(site+' dock structural steel',(.23,.25,.26))
 cuts=[h['polygon'] for h in deck_openings(0,data)];previous_areas=[]
 # Existing cottage walks terminate at the reconstructed quay; remove all coplanar overlap.
 if site=='WLD-06':
  for obj in old:
   if obj.get('role') not in ['supported exterior entrance pavement','piled perimeter boardwalk deck']:continue
   pieces=[];obj.data.calc_loop_triangles()
   for tri in obj.data.loop_triangles:
    vs=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
    if any(abs(v.z)>.00001 for v in vs) or (vs[1]-vs[0]).cross(vs[2]-vs[0]).z<=0:continue
    parts=[[(v.x,-v.y) for v in vs]]
    for cut in [a[1] for a in AREAS[site]]+[rect(*z['bounds']) for z in data['zones'] if z['kind'] not in ['outside','staging'] and z['y']==0]:parts=[q for poly in parts for q in difference(poly,cut)]
    pieces.extend(parts)
   for o in plate('Wharf retained walk outside reconstructed dock floors '+obj.name,pieces,0,.16 if obj.get('role')=='piled perimeter boardwalk deck' else .25,obj.data.materials[0]) or []:o['role']='authored retained dock-side walk';built.append(o)
   removed.append(obj.name)
  for obj in old:
   if obj.get('role')!='fitted library circulation guard':continue
   vs=[obj.matrix_world@v.co for v in obj.data.vertices]
   if max(v.x for v in vs)<=20:continue
   removed.append(obj.name)
   if min(v.x for v in vs)>=20:continue
   replacement=obj.copy();replacement.data=obj.data.copy();replacement.name='Wharf perimeter guard ending at joined jetty '+obj.name;bpy.context.collection.objects.link(replacement)
   bm=bmesh.new();bm.from_mesh(replacement.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001)
   plane=replacement.matrix_world.inverted()@Vector((20,0,0));normal=replacement.matrix_world.to_3x3().transposed()@Vector((1,0,0));bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.000001,plane_co=plane,plane_no=normal,clear_outer=True,clear_inner=False);bmesh.ops.holes_fill(bm,edges=[e for e in bm.edges if e.is_boundary],sides=0);bm.to_mesh(replacement.data);bm.free();built.append(replacement)
 for name,foot,kind in AREAS[site]:
  polys=[foot]
  for cut in cuts:polys=[p for poly in polys for p in difference(poly,cut)]
  for prior in previous_areas:polys=[p for poly in polys for p in difference(poly,prior)]
  previous_areas.append(foot)
  mat=timber if kind=='timber' else paving
  for o in plate(site+' '+name+' structural surface',polys,0,.20,mat) or []:o['role']='authored dock deck' if kind=='timber' else 'authored courtyard pavement';o['area']=name;built.append(o)
  a,b=min(p[0] for p in foot),min(p[1] for p in foot);c,d=max(p[0] for p in foot),max(p[1] for p in foot)
  if kind=='paved':
   # Filled terrace foundation; selected area contains no below-ground room.
   hits=[]
   for x in range(math.floor(a),math.ceil(c)+1):
    for z in range(math.floor(b),math.ceil(d)+1):
     h=soil.ray_cast(Vector((x,-z,30)),Vector((0,0,-1)))[0]
     if h is not None:hits.append(h.z)
   assert hits and max(hits)<-.05,(site,name,'earth intrudes through intended pavement')
   bottom=min(hits)-.35
   for o in plate(site+' '+name+' continuous retaining foundation',polys,-.2,-.2-bottom,rubble) or []:o['role']='terrain-embedded terrace foundation';o['area']=name;built.append(o)
   proof.append(dict(area=name,footprint=foot,kind=kind,surface_y=0,depth=.20,foundation_bottom_y=bottom,actual_soil_range=[min(hits),max(hits)],construction='Area retaining/fill foundation below pavement; not a centerline support'))
  else:
   piers=[];missing=[]
   # Orthogonal framing grid belongs to the entire dock footprint, not its paths.
   xs=[a+.4+(c-a-.8)*i/max(1,math.ceil((c-a-.8)/3)) for i in range(max(1,math.ceil((c-a-.8)/3))+1)];zs=[b+.4+(d-b-.8)*i/max(1,math.ceil((d-b-.8)/3)) for i in range(max(1,math.ceil((d-b-.8)/3))+1)]
   def inside(x,z,polygon):
    # Polygons here are convex rectangles; actual shaft footprints are convex.
    signs=[(q[0]-p[0])*(z-p[1])-(q[1]-p[1])*(x-p[0]) for p,q in zip(polygon,polygon[1:]+polygon[:1])];return all(v>=-1e-6 for v in signs) or all(v<=1e-6 for v in signs)
   for x in xs:
    for z in zs:
     if any(inside(x,z,cut) for cut in cuts):continue
     probes=[(x+dx,z+dz) for dx,dz in [(-.17,-.17),(.17,-.17),(.17,.17),(-.17,.17)]];hits=[soil.ray_cast(Vector((xx,-zz,30)),Vector((0,0,-1)))[0] for xx,zz in probes]
     if any(h is None for h in hits):missing.append([x,z]);continue
     bottom=min(h.z for h in hits)-.35;o=prism(site+' '+name+' driven pile '+str(len(piers)),rect(x-.17,z-.17,x+.17,z+.17),-.20,[bottom]*4,timber);o['role']='native-soil-founded dock pile';o['area']=name;built.append(o);piers.append(dict(x=x,z=z,bottom_y=bottom,soil_y=[h.z for h in hits]))
   for axis,positions in [(0,xs),(1,zs)]:
    for i,value in enumerate(positions):
     strip=rect(value-.12,b,value+.12,d) if axis==0 else rect(a,value-.12,c,value+.12);pieces=[strip]
     for cut in cuts:pieces=[p for poly in pieces for p in difference(poly,cut)]
     for o in plate(site+' '+name+' bearing '+str(axis)+' '+str(i),pieces,-.20,.40,steel) or []:o['role']='continuous dock bearing grid';o['area']=name;built.append(o)
   assert piers,(site,name,'no actual foundation');proof.append(dict(area=name,footprint=foot,kind=kind,surface_y=0,depth=.20,piers=piers,omitted_pier_positions_in_saved_soil_voids=missing,framing_grid_x=xs,framing_grid_z=zs,only_actual_stair_shafts_cut=True))
 if site=='WLD-08':
  # Normal, separate court doors; the adjacent stair slots remain stair slots.
  wallmat=next(o.data.materials[0] for o in old if o.get('role') in ['rough shell wall','aligned service-aisle wall'] and o.data.materials)
  for zone,x0,x1 in [('lab',19.76,20),('dorm',44,44.24)]:
   for za,zb,low,high,label in [(26,39.8,0,3,'north pier'),(42.2,44,0,3,'south pier'),(39.8,42.2,2.45,3,'door lintel')]:built.append(solid('Research '+zone+' court door '+label,[x0,za,x1,zb],low,high,wallmat,'deliberate courtyard doorway',zone))
  links={l['id']:l for l in data['links']};links['L03']['points']=[[32,0,41],[20,0,41],[9,0,41],[9,0,35],[11,0,35]];links['L06']['points']=[[32,0,41],[44,0,41],[55,0,41],[55,0,35],[53,0,35]]
 if site=='WLD-06':
  links={l['id']:l for l in data['links']};links['L10']['points']=[[42,0,23],[42,0,29],[48,0,29],[48,0,37],[42,0,37],[42,0,45],[32,0,45]]
 if site=='WLD-13':
  next(l for l in data['links'] if l['id']=='L03')['points']=[[34,0,69],[34,0,62],[16,0,62],[16,0,56],[16,0,50]]
 # Existing indexed library rails are fitted to constructed deck/shaft edges.
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(RAIL));template=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH').data.copy()
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 def rail(name,p,q):
  run=math.dist(p,q);ux=(q[0]-p[0])/run;uz=(q[1]-p[1])/run;n=math.ceil(run/2)
  for i in range(n):
   mesh=template.copy();o=bpy.data.objects.new(site+' '+name+' '+str(i),mesh);bpy.context.collection.objects.link(o)
   for v in mesh.vertices:
    t=(v.co.x+1)*run/n/2+i*run/n;off=v.co.y;v.co=(p[0]+ux*t-uz*off,-(p[1]+uz*t+ux*off),v.co.z)
   o['role']='fitted library dock or terrace guard';o['source_asset']='level_blockout_guardrail';built.append(o)
 if site=='WLD-06':
  for z in [31.70,34.30]:rail('west pier shaft rim',(20.15,z),(23.85,z));rail('east pier shaft rim',(40.15,z),(45.75,z))
  rail('east platform outer edge',(49.86,26),(49.86,38.86));rail('crossdock outer edge',(20.14,6.14),(43.86,6.14))
 if site=='WLD-08':
  for x in [2.14,61.86]:rail('forecourt side retaining edge',(x,56.24),(x,63.86))
  for a,c in [(2.14,23.86),(40.14,61.86)]:rail('forecourt front retaining edge',(a,63.86),(c,63.86))
  for x in [24.14,39.86]:rail('arrival apron retaining edge',(x,64.14),(x,69.86))
 if site=='WLD-13':
  rail('loading dock sea edge',(24.14,.14),(47.86,.14));rail('loading dock west edge',(24.14,.14),(24.14,13.76));rail('loading dock east edge',(47.86,.14),(47.86,13.76))
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 for o in built:
  for v in o.data.vertices:v.co=tuple(round(x,5) for x in v.co)
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name);bm.to_mesh(o.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'courts-and-docks.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('court-dock-addition-'+site+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'courts-and-docks.glb',remove_mesh_names=removed);parts=native_write(native,folder/'courts-and-docks.hcs2',removed,built);layout=folder/'authored-layout.json';write(layout,data)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'courts-and-docks.glb'));objs={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objs[name]))==face_keys(ts)
 assert not any(o.get('role','').startswith('rough route support') for o in objs.values())
 for o in objs.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') not in ['tunnel','tunnel-n','undercroft']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Courts and docks review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.45,-span*.6,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/courts-and-docks-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='ARCHITECTURAL_COURT_DOCK_CONSTRUCTION_REMEDIATION_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=s['glb_sha256'],source_physics=s['native'],source_physics_sha256=s['native_sha256'],before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),glb_sha256=sha(folder/'courts-and-docks.glb'),physics_sha256=sha(folder/'courts-and-docks.hcs2'),blend_sha256=sha(folder/'courts-and-docks.blend'),area_construction=proof,removed_parts=removed,area_builder='tools/build_site_courts_and_docks.py',area_builder_sha256=sha(Path(__file__)),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported courtyard/dock cutaway; roofs hidden; not gameplay')]);write(folder/'construction.json',meta);print('COURTS_DOCKS_BUILT',site,len(proof),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'courts-and-docks.glb').relative_to(ROOT).as_posix(),native=(folder/'courts-and-docks.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'courts-and-docks.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 r=json.loads((BASE/'study-solid-removal-pass/roster.json').read_text());rows=[build(s) if s['id'] in AREAS else s for s in r['active_sources']];write(OUT/'roster.json',dict(active_sources=rows,sites=[dict(id=s['id'],decision='COURTS_DOCKS_CONSTRUCTED_REMEDIATION_OPEN' if s['id'] in AREAS else s['decision']) for s in r['sites']],baseline='development/levels/rebuild/study-solid-removal-pass/roster.json'));print('COURTS_DOCKS_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
