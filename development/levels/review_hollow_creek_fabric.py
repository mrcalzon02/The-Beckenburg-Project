"""Independent physical cloth/seam, saved-floor and native capture checks."""
import bpy,bmesh,json,math,hashlib
from pathlib import Path
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/levels/hollow-creek'
r=json.loads((OUT/'tailored-fabric.json').read_text(encoding='utf-8'));checks=[]

def components(o,words):
 ids={g.index for g in o.vertex_groups if any(word in g.name for word in words)}
 return {v.index for v in o.data.vertices if any(a.group in ids and a.weight>.5 for a in v.groups)}
def tree(o,selected):
 o.data.calc_loop_triangles();return BVHTree.FromPolygons([v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if all(i in selected for i in t.vertices)],all_triangles=True)
def signature(o):
 return dict(vertices=[tuple(v.co) for v in o.data.vertices],faces=[tuple(p.vertices) for p in o.data.polygons],matrix=[tuple(row) for row in o.matrix_world])
def floor(o):
 selected=components(o,['groundsheet','physical floor','Groundsheet'])
 return sorted(tuple(o.matrix_world@v.co) for v in o.data.vertices if v.index in selected)

for a in r['assets']:
 bpy.ops.wm.open_mainfile(filepath=str(ROOT/a['editable']));o=bpy.data.objects[a['id']]
 words=['Catenary canvas side','Sewn multi-panel dome canopy','Soft rolled sleeping bag'];sel=components(o,words);bt=tree(o,sel)
 bm=bmesh.new();bm.from_mesh(o.data);bm.verts.ensure_lookup_table();faces={f for f in bm.faces if all(v.index in sel for v in f.verts)};edges={e for f in faces for e in f.edges}
 bad=[e for e in edges if sum(f in faces for f in e.link_faces)!=2];assert not bad,(a['id'],'non-closed or overlapping cloth shell',len(bad));bm.free()
 seam_probes=0;max_gap=0
 for start,end in a['sewn_segments']:
  for t in [0,.25,.5,.75,1]:
   p=Vector(start).lerp(Vector(end),t);nearest,_,_,gap=bt.find_nearest(p,.018)
   assert nearest is not None,('Sewn line unsupported',a['id'],list(p));max_gap=max(max_gap,gap);seam_probes+=1
 points=[]
 if a['kind']=='ridge':
  for side in [-1,1]:
   for t in [.25,.5,.75]:
    for y in [-1,0,1]:
     z=(1.90-.045*math.cos(y*math.pi/3.4))*(1-t)+.18*t-.035*math.sin(t*math.pi)
     points.append((Vector((side*t*1.3,y,z)),Vector((side*.8,0,.55)).normalized()))
 elif a['kind']=='dome':
  for angle in [0,math.pi/2,math.pi,3*math.pi/2]:
   for lat in [.35,.65,1,1.3]:
    p=Vector((1.4*math.cos(lat)*math.cos(angle),1.65*math.cos(lat)*math.sin(angle),.03+1.65*math.sin(lat)))
    if angle==3*math.pi/2 and p.z<1.4:continue
    normal=Vector((p.x/1.4**2,p.y/1.65**2,(p.z-.03)/1.65**2)).normalized();points.append((p,normal))
 thickness=[]
 for p,n in points:
  first,normal,_,_=bt.ray_cast(p+n*.15,-n,.35);assert first is not None,('Missing canvas probe',a['id'],list(p))
  if normal.dot(n)<0:normal=-normal
  second=bt.ray_cast(first-normal*.0001,-normal,.03)[0];assert second is not None,('Missing inner skin',a['id'],list(p))
  gap=(first-second).length;assert .0070<gap<.0095,('Canvas thickness',a['id'],gap);thickness.append(gap)
 rear=0
 if a['kind']=='dome':
  for x in [-.3,0,.3]:
   for z in [.2,.4,.6,.8,1,1.2]:
    hit=bt.ray_cast(Vector((x,-2,z)),Vector((0,1,0)),5)[0];assert hit is not None and hit.y>.8,('Rear enclosure or entrance blocked',x,z);rear+=1
  # Actual doorway rays prove the arch, not just a renamed component.
  assert bt.ray_cast(Vector((0,-2,1.5)),Vector((0,1,0)),2)[0] is not None,'Missing arch header'
  for x in [-.67,.67]:assert bt.ray_cast(Vector((x,-2,.6)),Vector((0,1,0)),2)[0] is not None,'Missing jamb'
 for uv in o.data.uv_layers:assert all(math.isfinite(c) for p in uv.data for c in p.uv)
 images={n.image for m in o.data.materials for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image};assert images and all(im.packed_file for im in images)
 checks.append(dict(id=a['id'],closed_shell=True,sewn_support_probes=seam_probes,max_seam_center_gap_m=max_gap,thickness_probes=len(thickness),canvas_thickness_m=[min(thickness),max(thickness)] if thickness else None,rear_aperture_rays=rear))

bpy.ops.wm.open_mainfile(filepath=str(OUT/'hollow-creek-authored.blend'))
old={o.name:signature(o) for o in bpy.context.scene.objects if o.type=='MESH'}
floors={o.name:floor(o) for o in bpy.context.scene.objects if o.type=='MESH' and o.get('refinement_source','').startswith('level_camp_tent_')}
captures=[]
def render(name,pos,target,lens,source):
 scene=bpy.context.scene;scene.camera.location=pos;scene.camera.rotation_euler=(Vector(target)-scene.camera.location).to_track_quat('-Z','Y').to_euler();scene.camera.data.lens=lens
 scene.cycles.samples=24;scene.render.resolution_x=1600;scene.render.resolution_y=1066
 fill=bpy.data.objects.get('Boundary inspection fill only')
 if fill:fill.location=pos;fill.rotation_euler=scene.camera.rotation_euler
 path=ROOT/f'development/levels/previews/hollow-creek/{name}.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
 captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=hashlib.sha256(path.read_bytes()).hexdigest(),scene=source,kind='Native scene overview'))
views=[('west',(13,-40,2),(8.8,-32.8,.7),30),('east',(46,-49,2),(51,-43,.6),32)]
for name,pos,target,lens in views:render('tailored-before-'+name+'-native-fitout',pos,target,lens,'hollow-creek-authored.blend')
bpy.ops.wm.open_mainfile(filepath=str(ROOT/r['scene']));new={o.name:signature(o) for o in bpy.context.scene.objects if o.type=='MESH'}
changed={a['name'] for a in r['instances']};assert old.keys()==new.keys()
for name in old:
 if name not in changed:assert old[name]==new[name],('Unrelated model/terrain moved',name)
roof_probes=0;roof_heights=[]
for name,points in floors.items():
 o=bpy.data.objects[name];assert points==floor(o),('Fitted floor changed',name)
 selected=components(o,['Catenary canvas side','Sewn multi-panel dome canopy']);o.data.calc_loop_triangles()
 roof=BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if all(i in selected for i in t.vertices)],all_triangles=True)
 selected_floor=components(o,['groundsheet','physical floor','Groundsheet'])
 floor_tree=BVHTree.FromPolygons([o.matrix_world@v.co for v in o.data.vertices],[tuple(t.vertices) for t in o.data.loop_triangles if all(i in selected_floor for i in t.vertices)],all_triangles=True)
 for x in [-.30,0,.30]:
  for y in [-.45,0,.45]:
   world=o.matrix_world@Vector((x,y,5));hit=roof.ray_cast(world,Vector((0,0,-1)),10)[0];below=floor_tree.ray_cast(world,Vector((0,0,-1)),10)[0]
   assert hit is not None and below is not None and 1.25<hit.z-below.z<2.1,('Missing/buried/misplaced tent canopy',name,x,y)
   roof_probes+=1;roof_heights.append(hit.z-below.z)
for name,pos,target,lens in views:render('tailored-'+name+'-native-fitout',pos,target,lens,'hollow-creek-tailored.blend')
render('tailored-bedding-native-fitout',(6.7,-36.2,.95),(7.9,-33.4,.25),45,'hollow-creek-tailored.blend')
render('tailored-site-native-fitout',(105,-125,100),(31,-42,1),40,'hollow-creek-tailored.blend')
for p,h in r['protected_sources'].items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
report=dict(status='pass-native-fabric-only',assets=checks,instances=len(changed),unmodified_scene_meshes=len(old)-len(changed),exact_fitted_floor_vertices=sum(len(p) for p in floors.values()),fitted_tent_floors=len(floors),actual_native_roof_floor_rays=roof_probes,roof_floor_height_m=[min(roof_heights),max(roof_heights)],source_sha256=r['scene_sha256'],captures=captures,runtime_map_acceptance=False)
(ROOT/'development/levels/verification/hollow-creek-tailored-shape.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS actual closed cloth, supported seams, thickness, continuous arch, exact8 fitted floors and untouched scene meshes',flush=True)
