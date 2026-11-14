"""Reconstruct deliberate entrances; routes follow the building, not corner cuts."""
from pathlib import Path
import sys,json,struct,io,re,copy
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
from build_roster_foundations import prism,material
from build_roster_floor_closures import difference
from planar_structure import plate
from structural_pass import deck_openings
from build_stair_landings import rect
import math
import build_cut_walkway_beds as beds
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'entrance-construction-pass'
PLANS={
'WLD-06':{'walls':{'cottage-a':{'S':[(12,2.6)],'E':[(48,2.6)]},'cottage-b':{'S':[(52,2.6)],'W':[(48,2.6)]}},'routes':{'L02':[(32,61),(12,61),(12,54),(12,48)],'L03':[(32,61),(52,61),(52,54),(52,48)],'L05':[(12,48),(20,48),(32,48),(32,45)],'L06':[(52,48),(44,48),(32,48),(32,45)]}},
'WLD-08':{'walls':{'store':{'N':[(11,2.2)],'S':[(11,2.6)]},'mess':{'N':[(53,2.2)],'S':[(53,2.6)]}},'routes':{'L05':[(11,50),(11,56),(11,61),(32,61),(32,63)],'L08':[(53,50),(53,56),(53,61),(32,61),(32,63)]}},
'WLD-13':{'walls':{'wash':{'N':[(16,2.2)],'S':[(16,2.6)],'W':[(50,2.2)]},'processing':{'N':[(30,2.2),(42,2.2)],'S':[(36,2.8)],'W':[(35,2.2)],'E':[(35,2.2)]}},'routes':{'L02':[(34,69),(36,69),(36,56),(36,41)],'L03':[(34,69),(16,69),(16,56),(16,50)],'L06':[(36,41),(36,35),(48,35),(56,35)],'L11':[(42,20),(42,26),(42,28),(36,28),(36,41)],'L14':[(16,50),(8,50),(0,50),(0,53)]}}}
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(d,indent=2)+'\n').encode())
def solid(name,bounds,low,high,mat,role,zone):
 a,b,c,d=bounds;assert c>a and d>b and high>low
 o=prism('Entrance rebuild '+name,[(a,b),(c,b),(c,d),(a,d)],high,[low]*4,mat);o['role']=role;o['zone']=zone;return o

def build(s):
 site=s['id'];plan=PLANS[site];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];native=ROOT/s['native'];meta=json.loads((ROOT/s['construction']).read_text());data=json.loads((ROOT/s['layout']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));old=list(bpy.context.scene.objects);removed=set();built=[];proof=[]
 mat=next(o.data.materials[0] for o in old if o.type=='MESH' and o.get('role')=='rough shell wall' and o.data.materials)
 for o in old:
  if o.type!='MESH':continue
  role=o.get('role','');zone=o.get('zone');link=o.get('link') or (re.match(r'(L\d+) support',o.name).group(1) if re.match(r'(L\d+) support',o.name) else None)
  if zone in plan['walls'] and role in ['rough shell wall','rough opening header']:removed.add(o.name)
  if role.startswith('rough route support') and link in plan['routes']:removed.add(o.name)
 for z in data['zones']:
  if z['id'] not in plan['walls']:continue
  a,b,c,d=z['bounds'];lo=z['y'];hi=lo+z['height'];doors=plan['walls'][z['id']]
  for side,start,end,fixed in [('N',a,c,b),('S',a,c,d),('W',b,d,a),('E',b,d,c)]:
   openings=sorted(doors.get(side,[]));cursor=start
   def wall(l,r,bottom,top,label):
    bounds=[l,fixed,r,fixed+.24] if side=='N' else [l,fixed-.24,r,fixed] if side=='S' else [fixed,l,fixed+.24,r] if side=='W' else [fixed-.24,l,fixed,r]
    if r-l>.0001:built.append(solid(site+' '+z['id']+' '+side+' '+label,bounds,bottom,top,mat,'deliberate entrance wall',z['id']))
   for i,(center,width) in enumerate(openings):
    left,right=center-width/2,center+width/2;assert start+.24<left<right<end-.24
    wall(cursor,left,lo,hi,'pier'+str(i));wall(left,right,lo+2.45,hi,'lintel'+str(i));proof.append(dict(zone=z['id'],side=side,center=center,width=width,height=2.45,wall_depth=.24));cursor=right
   wall(cursor,end,lo,hi,'end')
 for link in data['links']:
  if link['id'] in plan['routes']:link['points']=[[x,0,z] for x,z in plan['routes'][link['id']]]
 # A real ground-level courtyard deck over the main Research tunnel. Only actual stair shafts cut it.
 if site=='WLD-08':
  tunnel=next(z for z in data['zones'] if z['id']=='tunnel');tunnel['height']=2.5
  for o in old:
   if o.type!='MESH' or o.get('zone')!='tunnel':continue
   if o.get('role')=='ceiling-cap':removed.add(o.name)
   elif o.get('role')=='rough shell wall':
    # Preserve all existing basement door/shaft openings while trimming only the projecting wall tops.
    verts=[o.matrix_world@v.co for v in o.data.vertices]
    if max(v.z for v in verts)>.001 and min(v.z for v in verts)>=-.0001:
     removed.add(o.name);continue
    if max(v.z for v in verts)>.001:
     print('TRIM_BOUNDS',o.name, min(v.z for v in verts),max(v.z for v in verts),flush=True);clone=o.copy();clone.data=o.data.copy();clone.name='Entrance rebuild court '+o.name;bpy.context.collection.objects.link(clone)
     bm=bmesh.new();bm.from_mesh(clone.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001)
     plane=clone.matrix_world.inverted()@Vector((0,0,0));normal=clone.matrix_world.to_3x3().transposed()@Vector((0,0,1))
     bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),dist=.000001,plane_co=plane,plane_no=normal,clear_outer=True,clear_inner=False)
     edges=[e for e in bm.edges if e.is_boundary];bmesh.ops.holes_fill(bm,edges=edges,sides=0);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(clone.data);bm.free()
     clone['role']='ground-level tunnel bearing wall';built.append(clone);removed.add(o.name)
  a,b,c,d=tunnel['bounds'];foot=[(a,b),(c,b),(c,d),(a,d)];polys=[foot]
  for opening in deck_openings(0,data):polys=[q for p in polys for q in difference(p,opening['polygon'])]
  floor_mat=next(o.data.materials[0] for o in old if o.type=='MESH' and o.get('role')=='floor' and o.data.materials)
  for o in plate('Entrance rebuild Research courtyard structural deck',polys,0,.30,floor_mat) or []:o['role']='courtyard structural deck';o['zone']='court';built.append(o)
 # Remove the obsolete procedural wall/support parts before placing physical ground beds.
 for o in old:
  if o.name in removed:bpy.data.objects.remove(o,do_unlink=True)
 terrain=[o for o in bpy.context.scene.objects if o.type=='MESH' and (o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name)];assert len(terrain)==1,(site,'authoritative terrain')
 program=[dict(link=l['id'],points=[p,q]) for l in data['links'] if l['id'] in plan['routes'] for p,q in zip(l['points'],l['points'][1:])]
 # Existing physical floors are deducted; added beds root in exact source terrain triangles, never a basement void.
 extra,bedproof,deferred=[],[],[]
 # Continuous constructed approach slabs, including complete turns and thresholds.
 # Outside room footprints these bear on short spans and actual soil-rooted piers.
 soil=BVHTree.FromPolygons([terrain[0].matrix_world@v.co for v in terrain[0].data.vertices],[tuple(p.vertices) for p in terrain[0].data.polygons]);walkmat=material(site+' exterior approach concrete',(.40,.37,.31));walkpolys=[]
 for request in program:
  p,q=request['points'];half=1.25;poly=rect(p,q,half)
  # Square turn landings are genuine pavement, not independent colored route overlays.
  for point in [p,q]:walkpolys.append([(point[0]-half,point[2]-half),(point[0]+half,point[2]-half),(point[0]+half,point[2]+half),(point[0]-half,point[2]+half)])
  walkpolys.append(poly)
 # Indoor floors remain authoritative, allowing only a .4m threshold overlap.
 for z in data['zones']:
  if z['kind'] in ['outside','staging'] or z['y']<0:continue
  a,b,c,d=z['bounds'];cut=[(a+.4,b+.4),(c-.4,b+.4),(c-.4,d-.4),(a+.4,d-.4)]
  walkpolys=[piece for poly in walkpolys for piece in difference(poly,cut)]
 for opening in deck_openings(0,data):walkpolys=[piece for poly in walkpolys for piece in difference(poly,opening['polygon'])]
 walk=plate('Entrance rebuild '+site+' continuous approach pavement',walkpolys,0,.25,walkmat) or []
 for o in walk:o['role']='supported exterior entrance pavement';extra.append(o)
 # Piers below the slab are located at no more than 2m along each external run.
 pierpoints=set()
 for request in program:
  p,q=request['points'];length=math.dist(p,q);n=max(1,math.ceil(length/2))
  for i in range(n+1):
   x=p[0]+(q[0]-p[0])*i/n;z=p[2]+(q[2]-p[2])*i/n
   if any(room['kind'] not in ['outside','staging'] and room['y']>=0 and room['bounds'][0]+.4<x<room['bounds'][2]-.4 and room['bounds'][1]+.4<z<room['bounds'][3]-.4 for room in data['zones']):continue
   pierpoints.add((round(x,5),round(z,5)))
 for x,z in sorted(pierpoints):
  probes=[(x+dx,z+dz) for dx,dz in [(-.22,-.22),(.22,-.22),(.22,.22),(-.22,.22)]];hits=[soil.ray_cast(Vector((xx,-zz,30)),Vector((0,0,-1)))[0] for xx,zz in probes]
  if not all(h is not None for h in hits):
   owners=[room['id'] for room in data['zones'] if room['kind'] not in ['outside','staging'] and room['y']==0 and room['bounds'][0]-.5<=x<=room['bounds'][2]+.5 and room['bounds'][1]-.5<=z<=room['bounds'][3]+.5]
   assert owners,(site,x,z,'approach has no terrain footing or adjacent structure')
   bedproof.append(dict(threshold=[x,z],bearing='Existing room floor overlap and retained foundation',owners=owners));continue
  ground=min(h.z for h in hits)
  if ground>=-.25:continue
  bottom=ground-.2;o=solid(site+' approach bearing '+str(len(bedproof)),[x-.22,z-.22,x+.22,z+.22],bottom,-.22,walkmat,'soil-rooted approach bearing','exterior');extra.append(o);bedproof.append(dict(name=o.name,ground_y=[h.z for h in hits],probe_points=probes,bottom_y=bottom,top_y=-.22))
 built.extend(extra)

 for o in built:
  for v in o.data.vertices:v.co=tuple(round(x,5) for x in v.co)
  bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(site,o.name,'invalid solid');bm.to_mesh(o.data);bm.free()
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'rebuilt-entrances.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('entrance-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'rebuilt-entrances.glb',remove_mesh_names=sorted(removed))
 keep=[p for p in split_native(native) if p[0].split(':',1)[-1] not in removed];parts=[(o.name,records(o)) for o in built];stream=io.BytesIO();stream.write(struct.pack('>III',0x48435332,len(keep)+len(parts),sum(p[1] for p in keep)+sum(len(ts) for _,ts in parts)))
 for _,_,raw in keep:stream.write(raw)
 for name,ts in parts:
  name=name.encode();stream.write(struct.pack('>H',len(name))+name+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:stream.write(struct.pack('>9fB',*xyz,flags))
 outnative=folder/'rebuilt-entrances.hcs2';outnative.write_bytes(stream.getvalue());layout=folder/'authored-layout.json';write(layout,data)
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'rebuilt-entrances.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
 for name,ts in parts:assert face_keys(records(objects[name]))==face_keys(ts),(site,name,'render/native mismatch')
 # Native overview with roofs hidden ONLY for inspection so all repaired wall corners/doorways are visible.
 for o in objects.values():
  if o.get('role')=='enclosed pitched roof' or o.get('role')=='ceiling-cap' and o.get('zone') in plan['walls']:o.hide_render=True
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.render.resolution_x=1400;scene.render.resolution_y=1100;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Entrance review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,0));cam.location=target+Vector((span*.4,-span*.55,span));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.15;cap=BASE/'captures/entrance-construction-batch'/(site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='DELIBERATE_ENTRANCE_CONSTRUCTION_RUNTIME_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=sha(source),source_physics=s['native'],source_physics_sha256=sha(native),before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],glb_sha256=sha(folder/'rebuilt-entrances.glb'),physics_sha256=sha(outnative),blend_sha256=sha(folder/'rebuilt-entrances.blend'),entrance_builder='tools/build_deliberate_entrances.py',entrance_builder_sha256=sha(Path(__file__)),entrances=proof,removed_parts=sorted(removed),rooted_beds=bedproof,deferred_beds=deferred,changed_routes=list(plan['routes']),captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported entrance cutaway, roofs hidden for inspection, not gameplay')]);write(folder/'construction.json',meta)
 print('ENTRANCES_BUILT',site,'doors',len(proof),'removed',len(removed),'beds',len(bedproof),'deferred',len(deferred),flush=True)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=layout.relative_to(ROOT).as_posix(),glb=(folder/'rebuilt-entrances.glb').relative_to(ROOT).as_posix(),native=outnative.relative_to(ROOT).as_posix(),blend=(folder/'rebuilt-entrances.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'harbor-research-roof-pass/roster.json').read_text());sources=[build(s) if s['id'] in PLANS else s for s in roster['active_sources']];write(OUT/'roster.json',dict(active_sources=sources,sites=[dict(id=s['id'],decision='ENTRANCES_RECONSTRUCTED_RUNTIME_OPEN' if s['id'] in PLANS else s['decision']) for s in roster['sites']],baseline='development/levels/rebuild/harbor-research-roof-pass/roster.json'));print('ENTRANCE_BATCH_COMPLETE',flush=True)
if __name__=='__main__':main()
