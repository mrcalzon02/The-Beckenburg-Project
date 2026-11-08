"""Real flat-route shell apertures and indoor threshold coverage.
Preserve original terrain/holes, stairs, fixed seals, models and GLB resources.
"""
from pathlib import Path
import argparse,copy,json,math,struct,sys
import bpy,bmesh
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from build_stair_openings import corridor,bounds,volume
from build_stair_bays import intersection,subtract_tops,add_plate
from build_stair_landings import rect
from build_roster_floor_closures import footprint,difference
from structural_pass import deck_openings
from build_blockouts import box,wall_crossings
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'flat-circulation-pass'
def source(site):
 options=[('stair-connection-pass','connected-stairs'),('stair-terrain-pass/assemblies','benched-terrain'),('stair-bay-pass','bayed-stairs'),('stair-opening-pass','opened-stairs'),('stair-landing-pass','supported-stairs'),('floor-closure-pass','closed-floors')]
 return next((BASE/f/site.lower(),stem) for f,stem in options if (BASE/f/site.lower()/(stem+'.glb')).exists())
def square_threshold_approaches(data,preserve=()):
 zones={z['id']:z for z in data['zones']};changes=[]
 for link in data['links']:
  if link.get('player_access')=='SEALED_NO_ROUTE' or link['id'] in preserve or len(link['points'])!=3:continue
  points=link['points']
  if any(abs(p[1]-points[0][1])>.001 for p in points):continue
  threshold=link.get('threshold',link.get('threshold_reservation',{}).get('point'))
  if threshold is None or list(threshold)!=points[1]:continue
  a=zones[link['a']];b=zones[link['b']]
  if a.get('shape') or b.get('shape'):continue
  aa,ab,ac,ad=a['bounds'];ba,bb,bc,bd=b['bounds'];x,y,z=threshold;axis=None
  if ((abs(ac-ba)<.001 and abs(x-ac)<.001) or (abs(bc-aa)<.001 and abs(x-aa)<.001)) and max(ab,bb)<z<min(ad,bd):axis=0
  if ((abs(ad-bb)<.001 and abs(z-ad)<.001) or (abs(bd-ab)<.001 and abs(z-ab)<.001)) and max(aa,ba)<x<min(ac,bc):axis=2
  if axis is None:continue
  start,end=points[0],points[-1];first=[start[0],y,z] if axis==0 else [x,y,start[2]];last=[end[0],y,z] if axis==0 else [x,y,end[2]];revised=[]
  for p in [start,first,list(threshold),last,end]:
   if not revised or math.dist(p,revised[-1])>.001:revised.append(p)
  if revised==points:continue
  assert revised[0]==points[0] and revised[-1]==points[-1]
  link['points']=revised;changes.append(dict(link=link['id'],before=points,after=revised,threshold=list(threshold),crossing_axis=axis,reason='Normal approach to actual shared wall; preserve source endpoints/threshold and avoid grazing-wall slots'))
 return changes

def stacked_ceiling_revisions(data):
 changes={}
 for lower in data['zones']:
  if lower['kind'] in ['outside','staging'] or lower.get('shape'):continue
  a,b,c,d=lower['bounds'];old_top=lower['y']+lower['height'];candidates=[]
  for upper in data['zones']:
   if upper['id']==lower['id'] or (upper['kind'] in ['outside','staging'] and not upper.get('constructed_surface')) or upper.get('shape'):continue
   ua,ub,uc,ud=upper['bounds'];top=upper['y']
   if lower['y']+2.36<=top<old_top-.001 and ua<=a+.001 and ub<=b+.001 and uc>=c-.001 and ud>=d-.001:candidates.append(upper)
  if candidates:
   upper=min(candidates,key=lambda z:z['y']);lower['height']=upper['y']-lower['y'];changes[lower['id']]=dict(zone=lower['id'],old_ceiling_top=old_top,new_ceiling_top=upper['y'],height=lower['height'],upper_floor_owner=upper['id'],reason='Existing closed upper floor owns the shared ceiling; remove conflicting raised lower cap and trim lower walls to that shared datum')
 return changes

def context_protection(data,material):
 guards=[];proof=[]
 for zone in data['zones']:
  if zone.get('player_access')!='SEALED_CONTEXT' and zone.get('gameplay_eligible') is not False:continue
  a,b,c,d=zone['bounds'];obj=box('Closed context volume '+zone['id'],(a+c)/2,zone['y']-.16,(b+d)/2,c-a+.24,zone['height']+.32,d-b+.24,material,bpy.context.scene.collection);guards.append(obj);proof.append(dict(kind='complete sealed room shell',zone=zone['id'],bounds=[a-.12,b-.12,c+.12,d+.12],bottom=zone['y']-.16,top=zone['y']+zone['height']+.16))
 for link in data['links']:
  if link.get('player_access')!='SEALED_NO_ROUTE':continue
  for zone in data['zones']:
   if zone['id'] not in [link['a'],link['b']]:continue
   a,b,c,d=zone['bounds']
   for side,cuts in wall_crossings(zone,dict(links=[link])).items():
    for start,width,_ in cuts:
     x=start+width/2 if side in ['N','S'] else a if side=='W' else c;z=b if side=='N' else d if side=='S' else start+width/2;wx,wz=(width+.4,.5) if side in ['N','S'] else (.5,width+.4);obj=box('Fixed boundary protection '+link['id']+' '+zone['id']+' '+side,x,zone['y']-.16,z,wx,zone['height']+.32,wz,material,bpy.context.scene.collection);guards.append(obj);proof.append(dict(kind='fixed sealed mouth',link=link['id'],zone=zone['id'],side=side,centre=[x,zone['y'],z],width=width+.4))
 return guards,proof

def overlaps(a,b):return all(x[0]<y[1] and x[1]>y[0] for x,y in zip(bounds(a),bounds(b)))

def build(site):
 prior,stem=source(site);oldmeta=json.loads((prior/'construction.json').read_text());original_layout=ROOT/oldmeta['layout'];data=json.loads(original_layout.read_text());established_path=OUT/'established-flat-links.json';established=json.loads(established_path.read_text())['sites'][site];assert sha(prior/(stem+'.hcs2'))==established['source_native_sha256'];route_changes=square_threshold_approaches(data,established['preserve_complete_paths']);ceiling_revisions=stacked_ceiling_revisions(data);source_glb=prior/(stem+'.glb');source_native=prior/(stem+'.hcs2');bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source_glb));old=[o for o in bpy.context.scene.objects if o.type=='MESH'];guards,guard_proof=context_protection(data,old[0].data.materials[0]);sealed_zones={z['id'] for z in data['zones'] if z.get('player_access')=='SEALED_CONTEXT' or z.get('gameplay_eligible') is False};cutters=[];requests=[];proof=[]
 for link in data['links']:
  if link.get('player_access')=='SEALED_NO_ROUTE':continue
  for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
   run=math.hypot(b[0]-a[0],b[2]-a[2])
   if abs(a[1]-b[1])>.01 or run<.01:continue
   dx=(b[0]-a[0])/run;dz=(b[2]-a[2])/run;p=[a[0]-dx*.35,a[1],a[2]-dz*.35];q=[b[0]+dx*.35,b[1],b[2]+dz*.35]
   for wall in [False,True]:
    c=corridor('Flat body '+link['id']+' '+str(i)+' '+str(wall),p,q,link['width'],0 if wall else .005);c['wall_cutter']=wall;cutters.append(c)
   requests.append((link['id'],i,p,q,link['width']));proof.append(dict(link=link['id'],segment=i,points=[a,b],width=link['width']+.04,terminal_extension=.35,wall_bottom=0,slab_bottom=.005,headroom=2.2))
 bpy.context.view_layer.update();removed=[];built=[];changes=[]
 for obj in old:
  role=obj.get('role')
  if role not in ['rough shell wall','rough opening header','floor','ceiling-cap']:continue
  trim=ceiling_revisions.get(obj.get('zone'))
  if trim and role=='ceiling-cap' and bounds(obj)[2][1]>trim['new_ceiling_top']+.001:
   name=obj.name;removed.append(name);changes.append(dict(source_mesh=name,new_mesh=None,source_role=role,removed_volume_m3=volume(obj),ceiling_ownership=trim));bpy.data.objects.remove(obj,do_unlink=True);continue
  if role in ['floor','ceiling-cap']:continue # Flat routes never carve shared upper floor/roof slabs.
  wall=role in ['rough shell wall','rough opening header'];needs_trim=trim and wall and bounds(obj)[2][1]>trim['new_ceiling_top']+.001;near=[c for c in cutters if c['wall_cutter']==wall and all(a[0]<b[1] and a[1]>b[0] for a,b in zip(bounds(obj),bounds(c)))]
  if obj.get('zone') in sealed_zones:near=[]
  if not near and not needs_trim:continue
  duplicate=obj.copy();duplicate.data=obj.data.copy();bpy.context.scene.collection.objects.link(duplicate);before=volume(duplicate);bm=bmesh.new();bm.from_mesh(duplicate.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);bm.to_mesh(duplicate.data);bm.free()
  if needs_trim:
   xs,ys,zs=bounds(obj);bottom=zs[0]-.1;top=trim['new_ceiling_top'];cut=box('Shared floor ownership clipping solid',(xs[0]+xs[1])/2,bottom,-(ys[0]+ys[1])/2,xs[1]-xs[0]+2,top-bottom,ys[1]-ys[0]+2,obj.data.materials[0],bpy.context.scene.collection);bpy.context.view_layer.update();bpy.context.view_layer.objects.active=duplicate;mod=duplicate.modifiers.new('Shared stacked floor/ceiling datum','BOOLEAN');mod.operation='INTERSECT';mod.solver='EXACT';mod.object=cut;bpy.ops.object.modifier_apply(modifier=mod.name);bpy.data.objects.remove(cut,do_unlink=True)
  for c in near:
   bpy.context.view_layer.objects.active=duplicate;mod=duplicate.modifiers.new('Actual flat circulation opening','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=c;bpy.ops.object.modifier_apply(modifier=mod.name)
  for guard in guards:
   if not overlaps(obj,guard) or not any(overlaps(c,guard) for c in near):continue
   keep=obj.copy();keep.data=obj.data.copy();bpy.context.scene.collection.objects.link(keep);bm=bmesh.new();bm.from_mesh(keep.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);bm.to_mesh(keep.data);bm.free();bpy.context.view_layer.objects.active=keep;mod=keep.modifiers.new('Exact original sealed shell portion','BOOLEAN');mod.operation='INTERSECT';mod.solver='EXACT';mod.object=guard;bpy.ops.object.modifier_apply(modifier=mod.name)
   if keep.data.polygons:
    if duplicate.data.polygons:
     bpy.context.view_layer.objects.active=duplicate;mod=duplicate.modifiers.new('Preserve fixed context architecture','BOOLEAN');mod.operation='UNION';mod.solver='EXACT';mod.object=keep;bpy.ops.object.modifier_apply(modifier=mod.name)
    else:duplicate.data=keep.data.copy()
   bpy.data.objects.remove(keep,do_unlink=True)
  delta=before-volume(duplicate)
  if delta<1e-7:bpy.data.objects.remove(duplicate,do_unlink=True);continue
  name=obj.name;removed.append(name);bpy.data.objects.remove(obj,do_unlink=True);new=None
  if duplicate.data.polygons:
   duplicate.name='Flat route shell '+name;duplicate['source_mesh']=name;duplicate['opening_purpose']='Actual active flat route body corridor; fixed context and terrain unchanged';built.append(duplicate);new=duplicate.name
  else:bpy.data.objects.remove(duplicate,do_unlink=True)
  changes.append(dict(source_mesh=name,new_mesh=new,source_role=role,removed_volume_m3=delta,cutters=[c.name for c in near]))
 for c in cutters+guards:bpy.data.objects.remove(c,do_unlink=True)
 # Repair only indoor/constructed floor ownership. Do not paint outdoor route
 # ribbons or refill actual stair/earth openings to manufacture route passes.
 coverage=[];tops={}
 for obj in bpy.context.scene.objects:
  if obj.type!='MESH' or not(obj.get('role') in ['floor','ceiling-cap','structural-stair-step','structural-stair-landing'] or obj.get('role','').startswith('rough route support')):continue
  obj.data.calc_loop_triangles()
  for tri in obj.data.loop_triangles:
   ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
   if max(v.z for v in ps)-min(v.z for v in ps)<.0001 and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:tops.setdefault(round(ps[0].z,3),[]).append([(v.x,-v.y) for v in ps])
 for zone in data['zones']:
  if zone['kind'] in ['outside','staging'] and not zone.get('constructed_surface'):continue
  if zone.get('shape')=='annulus':continue
  e=zone['y'];surface=footprint(zone);mat=next((o.data.materials[0] for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='floor' and o.get('zone')==zone['id'] and o.data.materials),None)
  if mat is None:continue
  relevant=[(key,index,a,b,width) for key,index,a,b,width in requests if abs(a[1]-e)<.001 and intersection(rect(a,b,width/2),surface)]
  if not relevant:continue
  missing=[surface]
  for h in deck_openings(e,data):missing=[q for p in missing for q in difference(p,h['polygon'])]
  for cut in tops.get(round(e,3),[]):
   missing=[q for p in missing for q in difference(p,cut)]
   if not missing:break
  if not missing:continue
  for key,index,a,b,width in relevant:
   polys=[intersection(poly,rect(a,b,width/2)) for poly in missing];polys=[p for p in polys if p];made=add_plate('Indoor route bearing '+site+' '+zone['id']+' '+key+' '+str(index),polys,e,mat,'floor',key)
   for o in made:
    o['zone']=zone['id'];built.append(o);coverage.append(dict(name=o.name,zone=zone['id'],link=key,segment=index,top_y=e,depth=.16));o.data.calc_loop_triangles()
    for tri in o.data.loop_triangles:
     ps=[o.matrix_world@o.data.vertices[i].co for i in tri.vertices]
     if all(abs(v.z-e)<.0001 for v in ps) and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:missing=[q for p in missing for q in difference(p,[(v.x,-v.y) for v in ps])]
 for obj in built:
  bpy.ops.object.select_all(action='DESELECT');obj.select_set(True);bpy.context.view_layer.objects.active=obj;bpy.ops.object.transform_apply(location=False,rotation=True,scale=True);bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,obj.name,'new shell is open or inverted');bm.to_mesh(obj.data);bm.free()
 folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);layout=folder/'authored-layout.json';data['physical_flat_circulation_revision']=dict(context_protection=guard_proof,ceiling_revisions=ceiling_revisions,route_changes=route_changes,corridors=proof,shell_changes=changes,indoor_bearings=coverage,limitation='Source coordinate construction only; actual portals/services/runtime integration and finish remain open');layout.write_bytes((json.dumps(data,indent=2)+'\n').encode());bpy.ops.wm.save_as_mainfile(filepath=str(folder/'open-circulation.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for obj in built:obj.select_set(True)
 assert built,(site,'No actual source geometry changed')
 addition=ROOT/'build'/('flat-circulation-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);retained=append(source_glb,addition,folder/'open-circulation.glb',remove_mesh_names=removed);parts=split_native(source_native);byname={n.split(':',1)[-1]:n for n,count,raw in parts};removed_native=[byname[n] for n in removed];kept=[p for p in parts if p[0] not in removed_native];added=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(kept)+len(added),sum(n for name,n,raw in kept)+sum(len(ts) for name,ts in added))+b''.join(raw for name,n,raw in kept)
 for name,ts in added:
  raw=name.encode();payload+=struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
 (folder/'open-circulation.hcs2').write_bytes(payload);stairs=oldmeta.get('stair_components',json.loads((BASE/'furnishings'/site.lower()/'physics-contract.json').read_text())['stair_components']);assert not set(stairs)&set(removed_native)
 report=dict(id=site,status='PHYSICAL_FLAT_CIRCULATION_SOURCE_CANDIDATE_NOT_MAP_ACCEPTANCE',source_glb=source_glb.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source_glb),source_physics=source_native.relative_to(ROOT).as_posix(),source_physics_sha256=sha(source_native),before_layout=original_layout.relative_to(ROOT).as_posix(),before_layout_sha256=sha(original_layout),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),before_segment_map=oldmeta.get('source_segment_map',{}),source_segment_map=oldmeta.get('source_segment_map',{}),before_stair_components=stairs,stair_components=stairs,removed_source_meshes=removed,removed_native_parts=removed_native,new_parts=len(added),new_triangles=sum(len(ts) for n,ts in added),context_protection=guard_proof,ceiling_revisions=ceiling_revisions,route_changes=route_changes,shell_changes=changes,indoor_bearings=coverage,corridors=proof,retained_glb_resources=retained,glb_sha256=sha(folder/'open-circulation.glb'),physics_sha256=sha(folder/'open-circulation.hcs2'),generator_sha256=sha(Path(__file__)),established_approach_control_sha256=sha(established_path),remaining='Independent native/export/editable checks; all flat/vertical comparisons; terrain, intersecting stair/flat reservations, physical connections, framing/roof/light/material/art/services/runtime remain open.')
 (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('PHYSICAL_FLAT_CIRCULATION',site,len(removed),len(added),len(coverage),flush=True)
def main():
 p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+');args=p.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else []);sites=args.sites or json.loads((BASE/'furnishings/room-programs.json').read_text())['sites']
 for site in sites:build(site)
if __name__=='__main__':main()
