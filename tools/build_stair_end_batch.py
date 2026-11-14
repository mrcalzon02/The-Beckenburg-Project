from pathlib import Path
import sys,json,math,struct,io,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from review_roster_macroterrain import records,sha
from append_static_glb import append,read
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import prism
from compose_room_terrain import split_native
from build_stair_openings import bounds,volume,corridor
from planar_structure import plate
from build_roster_floor_closures import difference,clean_polygon
from build_roster_decks import area
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-end-pass';MODEL=ROOT/'development/levels/primitive-props-v1/models/level_blockout_guardrail.glb'
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];assert sha(source)==s['glb_sha256'] and sha(native)==s['native_sha256'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());before_segment_map=copy.deepcopy(meta.get('source_segment_map',{}));bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));originals=[o for o in bpy.context.scene.objects if o.type=='MESH'];source_names={o.name for o in originals};removed=[]
 for o in list(originals):
  if o.get('role') in ['fitted library stair guard','structural side stringer']:
   removed.append(o.name);bpy.data.objects.remove(o,do_unlink=True);originals.remove(o)
 before=set(bpy.context.scene.objects);bpy.ops.import_scene.gltf(filepath=str(MODEL));rail=next(o for o in set(bpy.context.scene.objects)-before if o.type=='MESH');template=rail.data.copy();mat=template.materials[0]
 for o in set(bpy.context.scene.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
 built=[];flights=[];cutters=[];wing_count=0
 # The wharf undercroft roof overlapped its surface circulation. Re-seat
 # that roof below the quay deck and shorten its supporting walls to match.
 if site=='WLD-06':
  for obj in list(originals):
   if 'undercroft' not in obj.name.lower():continue
   role=obj.get('role','')
   if role not in ['ceiling-cap','rough shell wall']:continue
   replacement=obj.copy();replacement.data=obj.data.copy();bpy.context.collection.objects.link(replacement)
   for v in replacement.data.vertices:
    wp=replacement.matrix_world@v.co
    if role=='ceiling-cap':wp.z-=.8
    else:wp.z=min(wp.z,-.3)
    v.co=replacement.matrix_world.inverted()@wp
   removed.append(obj.name);originals.remove(obj);name=obj.name;bpy.data.objects.remove(obj,do_unlink=True);replacement.name='Re-seated '+name;originals.append(replacement);built.append(replacement)

 for link in data['links']:
  if link['id'] not in meta['design']['build']:continue
  for index,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
   if abs(a[1]-b[1])<.01:continue
   length=math.hypot(b[0]-a[0],b[2]-a[2]);ux=(b[0]-a[0])/length;uz=(b[2]-a[2])/length;slope=(b[1]-a[1])/length;half=link['width']/2;setback=0;usable=length-2*setback;bay_count=max(1,math.ceil(usable/2));assert usable>=2
   cutters.append(corridor('Full flight headroom '+link['id'],a,b,link['width'],.025))
   flare=lambda run: 2.0*max(0,1-min(run,length-run)/1.6)
   for side in [-1,1]:
    nx=-uz*side;nz=ux*side
    for j in range(bay_count):
     start=setback+j*(usable-2)/max(1,bay_count-1)
     if start<1.5 or start+2>length-1.5:continue
     mesh=template.copy();o=bpy.data.objects.new('End-flared guard '+site+' '+link['id']+' '+str(index)+' '+str(side)+' '+str(j),mesh);bpy.context.collection.objects.link(o)
     for v in mesh.vertices:
      run=start+v.co.x+1;off=half+flare(run)+.08+v.co.y*side;x=a[0]+ux*run+nx*off;z=a[2]+uz*run+nz*off;y=a[1]+slope*run-.05+v.co.z;v.co=(x,-z,y)
     o['role']='fitted library stair guard';o['source_asset']='level_blockout_guardrail';o['link']=link['id'];built.append(o)
    # Expand actual tread surfaces to support flared guard ends, preserving each rise.
    for step in originals:
     if step.get('role')!='structural-stair-step' or step.get('link')!=link['id']:continue
     ps=[step.matrix_world@v.co for v in step.data.vertices];runs=[(v.x-a[0])*ux+(-v.y-a[2])*uz for v in ps];r0=max(0,min(runs));r1=min(length,max(runs))
     if r1-r0<.001:continue
     top=max(v.z for v in ps);positions=sorted(set([r0,r1]+[r for r in [1.6,length-1.6] if r0<r<r1]))
     for u,v in zip(positions,positions[1:]):
      poly=[(a[0]+ux*r+nx*off,a[2]+uz*r+nz*off) for r,off in [(u,half-.025),(v,half-.025),(v,half+flare(v)+.18),(u,half+flare(u)+.18)]]
      made=plate('Flared tread '+site+' '+link['id']+' '+str(side)+' '+str(wing_count),[poly],top,.25,mat);wing_count+=1
      for o in made:o['role']='structural-stair-step';o['link']=link['id'];built.append(o)
      # Physical body clearance through existing floor/cap geometry. Terrain is retained.
      cutter=prism('Flared shaft cutter',poly,top+2.05,[top+.025]*4,mat);cutters.append(cutter)
    # Continuous outside stringers follow both flares and seat every guard post.
    stops=[0,1.6,length-1.6,length]
    for u,v in zip(stops,stops[1:]):
     poly=[(a[0]+ux*r+nx*off,a[2]+uz*r+nz*off) for r,off in [(u,half+flare(u)-.02),(v,half+flare(v)-.02),(v,half+flare(v)+.18),(u,half+flare(u)+.18)]]
     ys=[a[1]+slope*r for r in [u,v,v,u]];stringer_top=-.2 if u==0 or v==length else .02;vs=[(x,-z,y+delta) for delta in [-.5,stringer_top] for (x,z),y in zip(poly,ys)];m=bpy.data.meshes.new('Flared stringer');m.from_pydata(vs,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);m.materials.append(mat);o=bpy.data.objects.new('Flared stringer '+site+' '+link['id']+' '+str(side)+' '+str(u),m);bpy.context.collection.objects.link(o);o['role']='structural side stringer';o['link']=link['id'];built.append(o)
   flights.append(dict(link=link['id'],segment=index,width=link['width'],bay_count_per_side=bay_count,source_asset=MODEL.relative_to(ROOT).as_posix(),source_sha256=sha(MODEL),method='Source profile retained on central guards; flared tread wings and lower end stringers provide wider turn approaches. Full guard returns remain unfinished. Original library master unchanged.'))
 # Cut only intersecting original floors and caps; retain terrain/props/walls.
 bpy.context.view_layer.update()
 for obj in list(originals):
  if obj.get('role') not in ['floor','ceiling-cap','rough shell wall']:continue
  if obj.get('role')=='rough shell wall' and site!='WLD-06':continue
  near=[c for c in cutters if all(x[0]<y[1] and x[1]>y[0] for x,y in zip(bounds(obj),bounds(c)))]
  if not near:continue
  duplicate=obj.copy();duplicate.data=obj.data.copy();bpy.context.collection.objects.link(duplicate);old_volume=volume(duplicate);bm=bmesh.new();bm.from_mesh(duplicate.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.0000001);bm.to_mesh(duplicate.data);bm.free()
  for cutter in near:
   bpy.context.view_layer.objects.active=duplicate;mod=duplicate.modifiers.new('Flared tread headroom','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=cutter;bpy.ops.object.modifier_apply(modifier=mod.name)
  if old_volume-volume(duplicate)<1e-7:bpy.data.objects.remove(duplicate,do_unlink=True);continue
  if obj.get('role')=='rough shell wall':
   removed.append(obj.name);name=obj.name;built.remove(obj) if obj in built else None;bpy.data.objects.remove(obj,do_unlink=True)
   if duplicate.data.polygons:duplicate.name='Opened flared wall '+name;built.append(duplicate)
   else:bpy.data.objects.remove(duplicate,do_unlink=True)
   continue
  # Reconstruct this procedural plate from its actual top triangles minus the
  # measured shaft footprints; repeated coplanar Boolean cuts can be unstable.
  bpy.data.objects.remove(duplicate,do_unlink=True);obj.data.calc_loop_triangles();polys=[];e=max((obj.matrix_world@v.co).z for v in obj.data.vertices);lo=min((obj.matrix_world@v.co).z for v in obj.data.vertices)
  for tri in obj.data.loop_triangles:
   ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
   if all(abs(v.z-e)<.00001 for v in ps) and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:polys.append([(v.x,-v.y) for v in ps])
  for cutter in near:
   xy=[]
   for v in cutter.data.vertices:
    xy.append((v.co.x,-v.co.y))
   from build_stair_landings import convex_top
   cut=convex_top(xy);polys=[q for poly in polys for q in difference(poly,cut)]
  for i,poly in enumerate(polys):
   poly=clean_polygon([(round(x,5),round(z,5)) for x,z in poly])
   if len(poly)<3 or area(poly)<.000001:continue
   span=max(math.dist(a,b) for a in poly for b in poly)
   if 2*area(poly)/max(span,.000001)<.0001:continue
   made=plate('Closed flared opening '+obj.name+' '+str(i),[poly],e,e-lo,obj.data.materials[0])
   for o in made:o['role']=obj.get('role');o['zone']=obj.get('zone','');built.append(o)
  removed.append(obj.name);built.remove(obj) if obj in built else None;bpy.data.objects.remove(obj,do_unlink=True);continue
 for c in cutters:bpy.data.objects.remove(c,do_unlink=True)
 # Snap only generated/reconstructed geometry to a 10-micron grid before
 # triangulation so native float32 records cannot collapse tiny Boolean edges.
 kept_built=[]
 # Wharf west-pier approach must pass over the low undercroft, not through
 # the headroom opening above a descending flight. Build its bypass deck.
 if site=='WLD-06':
  layout_copy=json.loads(json.dumps(data));next(z for z in layout_copy['zones'] if z['id']=='undercroft')['height']=2.2;next(l for l in layout_copy['links'] if l['id']=='L07')['points']=[[32,0,45],[28,0,45],[28,0,23],[22,0,23]]
  next(l for l in layout_copy['links'] if l['id']=='L16')['points']=[[18,0,33],[26,-2.5,33],[27.2,-2.5,33],[27.2,-2.5,30],[32,-2.5,30]]
  next(l for l in layout_copy['links'] if l['id']=='L17')['points']=[[32,-2.5,30],[36.8,-2.5,30],[36.8,-2.5,33],[38,-2.5,33],[46,0,33]]
  source_mapping=copy.deepcopy(meta.get('source_segment_map',{}));identity=source_mapping.get('L17',{}).get('2',2);source_mapping['L17']={'3':identity};meta['source_segment_map']=source_mapping
  layout=folder/'authored-layout.json';write(layout,layout_copy)
  for i,poly in enumerate([[(26.8,22),(29.2,22),(29.2,46),(26.8,46)],[(20.8,22),(28,22),(28,24),(20.8,24)]]):
   made=plate('Wharf bypass deck '+str(i),[poly],0,.3,mat)
   for o in made:o['role']='floor';o['zone']='quay';built.append(o)
  for x,z in [(27,27),(29,27),(27,35),(29,35)]:
   poly=[(x-.12,z-.12),(x+.12,z-.12),(x+.12,z+.12),(x-.12,z+.12)]
   o=prism('Wharf bypass bearing '+str(x)+' '+str(z),poly,-.2,[-2.5]*4,mat);o['role']='structural undercroft deck bearing';built.append(o)
 else:layout=ROOT/s['layout']
 for o in built:
  for v in o.data.vertices:v.co=tuple(round(float(c),5) for c in v.co)
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=.00001);bmesh.ops.dissolve_limit(bm,angle_limit=.00001,use_dissolve_boundaries=False,verts=list(bm.verts),edges=list(bm.edges));bm.to_mesh(o.data);bm.free()
  if len(o.data.polygons):kept_built.append(o)
  else:bpy.data.objects.remove(o,do_unlink=True)
 built=kept_built
 for o in built:
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.0000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,o.name;bm.to_mesh(o.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'finished-ends.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('guard-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'finished-ends.glb',remove_mesh_names=[n for n in removed if n in source_names]);parts=[(o.name,records(o)) for o in built];oldparts=split_native(native);names={n.split(':',1)[-1]:n for n,_,_ in oldparts};removed_native=[names[n] for n in removed if n in names];kept=[p for p in oldparts if p[0] not in removed_native];payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(n for _,n,_ in kept)+sum(len(rs) for _,rs in parts))+b''.join(raw for _,_,raw in kept)
 for name,rs in parts:
  encoded=name.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(rs))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in rs)
 target=folder/'finished-ends.hcs2';target.write_bytes(payload);new_stairs=[o.name for o in built if o.get('role')=='structural-stair-step'];assert len(kept)+len(removed_native)==len(oldparts);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'finished-ends.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,rs in parts:assert face_keys(records(objects[name]))==face_keys(rs),(site,name)
 meta.update(status='WIDENED_STAIR_APPROACH_AND_QUAY_ROOF_CANDIDATE_RUNTIME_OPEN',end_builder_sha256=sha(Path(__file__)),removed_native_parts=removed_native,stair_components=[n for n in meta['stair_components'] if n not in removed_native]+new_stairs,flared_wing_parts=wing_count,layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),source_glb=s['glb'],source_glb_sha256=sha(source),source_physics=s['native'],source_physics_sha256=sha(native),before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),before_segment_map=before_segment_map,before_stair_components=meta['stair_components'],guard_flights=flights,new_guard_parts=len(parts),glb_sha256=sha(folder/'finished-ends.glb'),physics_sha256=sha(target),remaining='Terrain interfaces, landing perimeter guards/returns, frame/anchor detailing and runtime integration remain open. No terrain-hole change or map unlock.');write(folder/'construction.json',meta)
 print('GUARDED_FLIGHTS',site,len(flights),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=meta['layout'],glb=(folder/'finished-ends.glb').relative_to(ROOT).as_posix(),native=target.relative_to(ROOT).as_posix(),blend=(folder/'finished-ends.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'stair-guard-pass/roster.json').read_text());active=[build(s) if s['id'] in ['WLD-06','WLD-08','WLD-12'] else s for s in roster['active_sources']];write(OUT/'roster.json',dict(active_sources=active,sites=[dict(id=s['id'],decision='FLARED_END_CONSTRUCTION_CANDIDATE' if s['id'] in ['WLD-06','WLD-08','WLD-12'] else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']]))
if __name__=='__main__':main()



