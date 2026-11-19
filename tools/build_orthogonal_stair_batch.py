from pathlib import Path
import sys,json
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import build_stair_bays as builder
original_plate=builder.plate
def component_plate(name,polygons,top,depth,material):
 import bpy
 before=set(bpy.data.objects)
 try:return original_plate(name,polygons,top,depth,material)
 except AssertionError:
  for o in set(bpy.data.objects)-before:bpy.data.objects.remove(o,do_unlink=True)
  result=[]
  for i,poly in enumerate(polygons):result.extend(original_plate(name+' patch'+str(i),[poly],top,depth,material))
  return result
builder.plate=component_plate
BASE=ROOT/'development/levels/rebuild';builder.OUT=BASE/'orthogonal-stair-pass'
PLANS={
'WLD-06':dict(points={'L16':[[18,0,33],[26,-2.5,33],[26,-2.5,30],[32,-2.5,30]],'L17':[[32,-2.5,30],[38,-2.5,30],[38,-2.5,33],[46,0,33]]},build=['L16','L17'],shared={},reason='Wharf: opposed straight access flights follow west/east stair bays; level turns enter undercroft instead of diagonal columns crossing the quay.'),
'WLD-08':dict(points={'L17':[[11,0,35],[22,-2.5,35],[22,-2.5,31],[32,-2.5,31]],'L18':[[32,-2.5,31],[42,-2.5,31],[42,-2.5,35],[53,0,35]]},build=['L17','L18'],shared={},reason='Research station: align lab/dorm flights with building axes and join the tunnel through level end landings; remove skewed stair columns below the court.'),
'WLD-12':dict(points={'L17':[[31,-3,32],[31,-3,38],[13,0,38],[11,0,38],[11,0,42]]},build=['L17'],shared={},reason='Railway: straight transverse return stair ends on a level west-side landing before the north/south return path; no diagonal ascent across the trestle approach.')}
def main():
 roster=json.loads((BASE/'roof-construction-pass/roster.json').read_text());active=[]
 for s in roster['active_sources']:
  if s['id'] not in PLANS:active.append(s);continue
  site=s['id'];PLANS[site]['preserve_upper_hubs']=False;builder.build(site,PLANS[site],[((ROOT/s['glb']).parent,Path(s['glb']).stem)],'aligned-stairs')
  folder=builder.OUT/site.lower();p=folder/'construction.json';d=json.loads(p.read_text());d['status']='ORTHOGONAL_PHYSICAL_STAIR_CANDIDATE_REQUIRES_TERRAIN_AND_MOVEMENT_REVIEW';p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8');active.append(dict(id=site,construction=p.relative_to(ROOT).as_posix(),layout=d['layout'],glb=(folder/'aligned-stairs.glb').relative_to(ROOT).as_posix(),native=(folder/'aligned-stairs.hcs2').relative_to(ROOT).as_posix(),blend=(folder/'aligned-stairs.blend').relative_to(ROOT).as_posix(),glb_sha256=d['glb_sha256'],native_sha256=d['physics_sha256']))
 (builder.OUT/'roster.json').write_text(json.dumps(dict(active_sources=active,sites=[dict(id=s['id'],decision='PHYSICAL_STAIR_REVISION_PENDING_CHECKS' if s['id'] in PLANS else 'RETAIN_PRIOR_SOURCE') for s in roster['sites']]),indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
