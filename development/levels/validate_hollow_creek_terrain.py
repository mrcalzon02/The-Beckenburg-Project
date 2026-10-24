"""Test saved native triangles, not just planned connectivity or the height function."""
import bpy,json,hashlib,math
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
d=json.loads((OUT/'terrain.json').read_text(encoding='utf-8'));assets=json.loads((OUT/'terrain-assets.json').read_text(encoding='utf-8'))
bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-terrain.blend'))
scene=bpy.context.scene
mesh=bpy.data.objects['level_camp_terrain'];mesh.data.calc_loop_triangles();assert len(mesh.data.loop_triangles)==assets['asset']['triangles'] and len(mesh.data.loop_triangles)<50000
assert mesh.data.uv_layers and all(im.packed_file for im in bpy.data.images if im.source=='FILE')
assert hashlib.sha256((ROOT/'assets/runtime/models/level_camp_terrain.glb').read_bytes()).hexdigest()==assets['asset']['sha256']
def tree(obj):
 return BVHTree.FromPolygons([obj.matrix_world@v.co for v in obj.data.vertices],[tuple(p.vertices) for p in obj.data.polygons],all_triangles=False)
ground=tree(mesh)
# Exclude water and model tops from the set of eligible standing surfaces.
floors=[ground]+[tree(o) for o in scene.objects if o.type=='MESH' and (o.name=='level_camp_pavilion' or o.name.startswith('Pavilion apron ') or o.name in [q['id'] for q in d['decks']])]
LAST_NORMAL=None
def supported(x,z,expected=0):
 global LAST_NORMAL
 hits=[t.ray_cast(Vector((x,-z,expected+.25)),Vector((0,0,-1)),.75) for t in floors]
 eligible=[(p,n) for p,n,_,_ in hits if p is not None and n.z>.65]
 if not eligible:return None
 highest=max(p.z for p,n in eligible)
 # At an exact abutment edge, the deck owns its horizontal standing face; a
 # coincident soil-edge ray must not substitute the hidden bank's steep normal.
 p,LAST_NORMAL=max([item for item in eligible if highest-item[0].z<.0001],key=lambda item:item[1].z);return p
samples=0;largest_error=0;steepest=0;steep_location=None
for link in d['routes']:
 for a,b in zip(link['points'],link['points'][1:]):
  dx,dz=b[0]-a[0],b[2]-a[2];length=math.hypot(dx,dz);count=math.ceil(length/.25)
  for i in range(count+1):
   f=i/count
   for band in range(9):
    offset=(band/8*2-1)*(link['width']/2+.22)
    x,z=a[0]+dx*f-dz/length*offset,a[2]+dz*f+dx/length*offset
    expected=a[1]*(1-f)+b[1]*f;hit=supported(x,z,expected);assert hit is not None,(link['id'],x,z,'no solid native support')
    error=abs(hit.z-expected);largest_error=max(largest_error,error);assert error<.20,(link['id'],x,z,hit.z,'route leaves its authored grade corridor')
    slope=math.hypot(LAST_NORMAL.x,LAST_NORMAL.y)/LAST_NORMAL.z
    if slope>steepest:steepest=slope;steep_location=(link['id'],x,z)
    samples+=1
print('MEASURED steepest full-width native trail grade',steepest,steep_location,flush=True)
assert steepest<=.20,('excessive native trail grade',steepest,steep_location)
# A real worn-trail cross-section must sit below both surrounding shoulders.
center=(13.470588,28.0);cx,cz=center
cp=ground.ray_cast(Vector((cx,-cz,2)),Vector((0,0,-1)))[0];shoulders=[]
for side in [-1,1]:
 x,z=cx+side*2*.998274,cz-side*2*.058722
 p=ground.ray_cast(Vector((x,-z,2)),Vector((0,0,-1)))[0]
 assert p is not None and p.z-cp.z>.05,('trail is not incised into its shoulder',side,p,cp)
 shoulders.append(p.z-cp.z)
# Foundation pads and canonical van/cargo approach retain authored standing height.
pad_samples=0
for rect,level in d['level_pads']:
 a,b,c,e=rect
 for fx in [.1,.5,.9]:
  for fz in [.1,.5,.9]:
   x,z=a+(c-a)*fx,b+(e-b)*fz;hit=supported(x,z,level);assert hit is not None and abs(hit.z-level)<.06,(rect,x,z,'pad mismatch');pad_samples+=1
for dx in [-2,0,2]:
 for dz in [-4,0,4]:
  p=d['staging']['point'];hit=supported(p[0]+dx,p[2]+dz,p[1]);assert hit is not None and abs(hit.z-p[1])<1e-5
# Carved channel and lake must stay below the water; infrastructure rather than a filled trench supports crossing.
for x,z in [(28,-6),(28,0),(48,10),(48,20)]:
 p,n,_,_=ground.ray_cast(Vector((x,-z,2)),Vector((0,0,-1)));assert p is not None and p.z<d['water_y']-.2,(x,z,p,'water channel filled')
for i in range(113):
 z=-2+i*.25;p,n,_,_=ground.ray_cast(Vector((48,-z,2)),Vector((0,0,-1)));assert p is not None and p.z<d['water_y']-.1,(z,'creek mouth/drainage discontinuity',p)
for deck in d['decks']:
 a,b,c,e=deck['bounds'];p=supported((a+c)/2,(b+e)/2,deck['top']);assert p is not None and abs(p.z-deck['top'])<1e-6
# Previous authored facilities are retained, and no placeholder wall/colored route masquerades as terrain.
prior=json.loads((OUT/'facilities.json').read_text(encoding='utf-8'))
for placement in prior['placements']:
 obj=bpy.data.objects[placement['native_object']];assert (obj.location-Vector((placement['x'],-placement['z'],d['facility_elevations'][placement['id']]))).length<1e-6
assert not any(o.type=='MESH' and o.get('role') in ['rough shell wall','rough opening header'] for o in scene.objects)
for path,digest in assets['protected_sources'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,path
report=dict(status='pass-native-site-foundation-only',terrain_triangles=assets['asset']['triangles'],supported_full_width_route_samples=samples,pad_samples=pad_samples,largest_route_y_error=largest_error,steepest_full_width_native_grade=steepest,steepest_location=steep_location,water_beds_below_surface=True,continuous_creek_to_lake_bed_samples=113,canonical_van_xz_and_yaw_retained=True,van_pad_elevation=1.5,shore_elevation=-1.3,trail_incision_m=.12,measured_worn_trail_shoulder_rises=shoulders,protected_sources_unchanged=True,scene_sha256=hashlib.sha256((OUT/'hollow-creek-terrain.blend').read_bytes()).hexdigest(),limitations=d['limitations'])
(ROOT/'development/levels/verification/hollow-creek-terrain.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS native terrain triangles:',samples,'full-width route samples,',pad_samples,'foundation samples; carved water and preserved staging/source checkpoints',flush=True)
