"""Extend finite arrival path ends behind unchanged standing anchors.
Small physically attached slabs only; no terrain editing or zone-sized pads.
"""
from pathlib import Path
import argparse,copy,csv,gzip,json,math,struct,sys
import bpy
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from build_roster_floor_closures import difference,clean_polygon
from build_roster_decks import area
from build_roster_foundations import prism
from structural_pass import deck_openings
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from append_static_glb import append
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'arrival-bearing-pass'
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(data,indent=2)+'\n').encode())
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--sites',nargs='+');args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
 lane_path=BASE/'flat-circulation-pass/verification/current-flat-lanes.tsv.gz';lanes=list(csv.DictReader(gzip.decompress(lane_path.read_bytes()).decode().splitlines(),delimiter='\t'));decisions=[]
 for source in sorted((BASE/'flat-circulation-pass').glob('*/construction.json')):
  old=json.loads(source.read_text());site=old['id']
  if args.sites and site not in args.sites:continue
  layout=ROOT/old['layout'];data=json.loads(layout.read_text());stages={z['id'] for z in data['zones'] if z['kind']=='staging'};links=[l for l in data['links'] if l['a'] in stages and l.get('player_access')!='SEALED_NO_ROUTE'];assert len(links)==1,(site,links);link=links[0];anchor=link['points'][0];before=next(r for r in lanes if r['site']==site and r['link']==link['id']+':0' and r['lane']=='centre' and float(r['x'])==anchor[0] and float(r['z'])==anchor[2]);source_glb=source.parent/'open-circulation.glb';native=source.parent/'open-circulation.hcs2';assert sha(source_glb)==old['glb_sha256'] and sha(native)==old['physics_sha256'] and sha(layout)==old['layout_sha256']
  if site=='WLD-09':decisions.append(dict(id=site,decision='REJECT_SLAB_ONLY_TERRAIN_INTRUSION_REQUIRES_REDESIGN',source=source.relative_to(ROOT).as_posix(),reason='Actual [36,0,82] native anchor remains PHYSICAL_COMPONENT terrain-site-0-1:surface after slab repair; see rejected/terrain-blocked-wld09'));continue
  if before['after_valid']=='true':decisions.append(dict(id=site,decision='RETAIN_EXISTING_SUPPORTED_ARRIVAL',source=source.relative_to(ROOT).as_posix(),reason='Current strict production footprint already passes; no redundant slab'));continue
  bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source_glb));original=list(o for o in bpy.context.scene.objects if o.type=='MESH');e=anchor[1];b=link['points'][1];dx=b[0]-anchor[0];dz=b[2]-anchor[2];run=math.hypot(dx,dz);assert run>0 and abs(e-b[1])<.01;tx,tz=dx/run,dz/run;half=link['width']/2
  # The original path end owns the entire shared contact plane. Retain its
  # thickness/material and extend .35m, enough for the actual .22m footprint.
  parents=[]
  for obj in original:
   if not obj.get('role','').startswith('rough route support'):continue
   obj.data.calc_loop_triangles();points=[obj.matrix_world@v.co for v in obj.data.vertices];top=max(p.z for p in points)
   if abs(top-e)>.00001:continue
   if not any(abs((p.x-anchor[0])*tx+(-p.y-anchor[2])*tz)<.00001 for p in points):continue
   if not all((p.x-anchor[0])*tx+(-p.y-anchor[2])*tz>=-.00001 for p in points):continue
   transverse=[(p.x-anchor[0])*(-tz)+(-p.y-anchor[2])*tx for p in points]
   if min(transverse)>-half+.0001 or max(transverse)<half-.0001:continue
   if min((p.x-anchor[0])*tx+(-p.y-anchor[2])*tz for p in points)>.0001:continue
   parents.append((obj,min(p.z for p in points)))
  assert len(parents)==1,(site,'Need one measured terminal support owner',[(o.name,y) for o,y in parents]);parent,bottom=parents[0];assert .08<=e-bottom<=.2
  polygon=[(anchor[0]+along*tx+side*(-tz),anchor[2]+along*tz+side*tx) for along,side in [(-.35,-half),(0,-half),(0,half),(-.35,half)]];polys=[polygon];owners=[]
  for obj in original:
   obj.data.calc_loop_triangles()
   for tri in obj.data.loop_triangles:
    ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
    if any(abs(p.z-e)>.00001 for p in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
    cut=[(p.x,-p.y) for p in ps];previous=sum(area(p) for p in polys);polys=[q for p in polys for q in difference(p,cut)]
    if previous-sum(area(p) for p in polys)>1e-9:owners.append(obj.name)
  for opening in deck_openings(e,data):
   polys=[q for p in polys for q in difference(p,opening['polygon'])]
  polys=[clean_polygon(p) for p in polys if area(p)>1e-8];assert polys and sum(area(p) for p in polys)>.01,(site,'No actual uncovered arrival footprint');built=[]
  for i,poly in enumerate(polys):
   o=prism('Arrival end bearing '+site+' '+str(i),poly,e,[bottom]*len(poly),parent.data.materials[0]);o['role']='structural-arrival-end-bearing';o['source_contact_owner']=parent.name;o['link']=link['id'];built.append(o)
  folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);scene=folder/'supported-arrival.blend';bpy.ops.wm.save_as_mainfile(filepath=str(scene),compress=True);bpy.ops.object.select_all(action='DESELECT')
  for obj in built:obj.select_set(True)
  addition=ROOT/'build'/('arrival-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source_glb,addition,folder/'supported-arrival.glb');parts=split_native(native);added=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(parts)+len(added),sum(n for name,n,raw in parts)+sum(len(ts) for name,ts in added))+b''.join(raw for name,n,raw in parts)
  for name,ts in added:
   text=name.encode();payload+=struct.pack('>H',len(text))+text+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
  (folder/'supported-arrival.hcs2').write_bytes(payload);proof=dict(link=link['id'],anchor=anchor,route_width=link['width'],extension_metres=.35,contact_owner=parent.name,source_bottom_y=bottom,top_y=e,original_polygon=polygon,actual_polygons=polys,coplanar_receivers=sorted(set(owners)),new_parts=[o.name for o in built],triangles=sum(len(ts) for name,ts in added),terrain_and_basement_holes='EXACT_RETAINED_NATIVE_RECORDS_NO_WORLD_EDITS',limitation='Small path-end cantilever attached along original full-depth end plane; overall terrain/foundation/art/canonical-van/runtime acceptance remains separate')
  data['arrival_end_bearing']=proof;newlayout=folder/'authored-layout.json';write(newlayout,data);r={k:copy.deepcopy(old[k]) for k in ['id','stair_components','source_segment_map']};r.update(new_parts=[o.name for o in built],new_triangles=proof['triangles'],removed_source_meshes=[],removed_native_parts=[],status='PHYSICAL_ARRIVAL_END_BEARING_SOURCE_CANDIDATE_NOT_MAP_ACCEPTANCE',source_contract=source.relative_to(ROOT).as_posix(),source_contract_sha256=sha(source),source_glb=source_glb.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source_glb),source_physics=native.relative_to(ROOT).as_posix(),source_physics_sha256=sha(native),before_layout=layout.relative_to(ROOT).as_posix(),before_layout_sha256=sha(layout),before_segment_map=old.get('source_segment_map',{}),before_stair_components=old['stair_components'],layout=newlayout.relative_to(ROOT).as_posix(),layout_sha256=sha(newlayout),glb_sha256=sha(folder/'supported-arrival.glb'),physics_sha256=sha(folder/'supported-arrival.hcs2'),arrival_bearing=proof,generator_sha256=sha(Path(__file__)));write(folder/'construction.json',r);decisions.append(dict(id=site,decision='BUILD_ATTACHED_END_BEARING',source=source.relative_to(ROOT).as_posix(),construction=(folder/'construction.json').relative_to(ROOT).as_posix(),parts=len(built),triangles=proof['triangles']));print('ARRIVAL_BEARING',site,len(built),proof['triangles'],flush=True)
 if not args.sites:
  for site in ['CASTLE','HOUSE','WLD-01','WLD-02']:decisions.append(dict(id=site,decision='RETAIN_MATURE_SOURCE_INDIVIDUAL_ACCEPTANCE_OPEN',reason='Not a finite rough path endpoint; no inferred correction'))
  write(OUT/'roster.json',dict(status='BROAD_ARRIVAL_BEARING_SOURCE_PASS_NOT_MAP_ACCEPTANCE',sites=decisions,generator_sha256=sha(Path(__file__))))
if __name__=='__main__':main()
