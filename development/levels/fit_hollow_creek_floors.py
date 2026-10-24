"""Clip actual terrain triangles into eight authored tent floor footprints.

No uniform floor subdivision, flat replacement pad or relaxed asset budget.
Keep original floor material/UV mapping, explicit12mm depth and every nonfloor
part/pose. Export the actual fitted assemblies as separate bounded game assets.
"""
import bpy,bmesh,sys,json,math,hashlib
from pathlib import Path
from mathutils import Vector,Matrix
from mathutils.geometry import convex_hull_2d
ROOT=Path(__file__).resolve().parents[2];sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
OUT=ROOT/'development/levels/hollow-creek';source=OUT/'hollow-creek-tailored.blend'
protected={source.relative_to(ROOT).as_posix():hashlib.sha256(source.read_bytes()).hexdigest()}
TOP=.028;BOTTOM=.016

def cross(a,c):return a[0]*c[1]-a[1]*c[0]
def area(poly):return sum(cross(a,c) for a,c in zip(poly,poly[1:]+poly[:1]))*.5
def clip(poly,hull):
 for a,c in zip(hull,hull[1:]+hull[:1]):
  result=[];edge=(c[0]-a[0],c[1]-a[1])
  for p,q in zip(poly,poly[1:]+poly[:1]):
   dp=cross(edge,(p[0]-a[0],p[1]-a[1]));dq=cross(edge,(q[0]-a[0],q[1]-a[1]));ip=dp>=0;iq=dq>=0
   if ip:result.append(p)
   if ip!=iq:
    t=dp/(dp-dq);result.append((p[0]+t*(q[0]-p[0]),p[1]+t*(q[1]-p[1])))
  poly=[]
  for p in result:
   if not poly or math.dist(p,poly[-1])>1e-9:poly.append(p)
  if len(poly)>1 and math.dist(poly[0],poly[-1])<1e-9:poly.pop()
  if len(poly)<3:return []
 return poly

def explicit_triangles(o,diagonals):
 """Use specified rendered diagonals; retain the original split normals."""
 normals=[tuple(n.vector) for n in o.data.corner_normals]
 bm=bmesh.new();bm.from_mesh(o.data);bm.verts.ensure_lookup_table();bm.faces.ensure_lookup_table()
 layer=bm.loops.layers.float_vector.new('RetainedSplitNormal')
 for f,p in zip(bm.faces,o.data.polygons):
  for loop,index in zip(f.loops,p.loop_indices):loop[layer]=normals[index]
 for a,c in sorted(diagonals):
  va,vc=bm.verts[a],bm.verts[c]
  if bm.edges.get((va,vc)) is None:bmesh.ops.connect_verts(bm,verts=[va,vc],check_degenerate=True)
 assert all(len(f.verts)==3 for f in bm.faces),'Missing explicit rendered diagonal'
 bm.to_mesh(o.data);bm.free();o.data.update()
 values=[tuple(p.vector) for p in o.data.attributes['RetainedSplitNormal'].data]
 o.data.normals_split_custom_set(values);o.data.attributes.remove(o.data.attributes['RetainedSplitNormal'])

bpy.ops.wm.open_mainfile(filepath=str(source));bpy.context.preferences.filepaths.save_version=0;bpy.context.view_layer.update()
terrain=bpy.data.objects['level_camp_terrain_amenities'];terrain.data.calc_loop_triangles();triangles=[]
for t in terrain.data.loop_triangles:
 points=[terrain.matrix_world@terrain.data.vertices[i].co for i in t.vertices];xy=[(p.x,p.y) for p in points];signed=area(xy)
 if abs(signed)<1e-12:continue
 if signed<0:points.reverse();xy.reverse()
 triangles.append((points,xy,(min(p.x for p in points),min(p.y for p in points),max(p.x for p in points),max(p.y for p in points))))
names=['west-pitched-ridge','east-pitched-dome']+['Additional pitched tent '+str(i) for i in range(1,7)]
ids=['level_camp_tent_fitted_west_ridge','level_camp_tent_fitted_east_dome']+['level_camp_tent_fitted_pitch_'+str(i).zfill(2) for i in range(1,7)]
rows=[];snapshots=[];temp=ROOT/'build/fitted-tent-snapshots';temp.mkdir(exist_ok=True)
template_hulls={}
for name,id in zip(names,ids):
 o=bpy.data.objects[name];pose=(tuple(o.location),tuple(o.rotation_euler),tuple(o.scale));matrix=o.matrix_world.copy();inverse=matrix.inverted()
 groups={g.index for g in o.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()}
 selected={v.index for v in o.data.vertices if any(a.group in groups and a.weight>.5 for a in v.groups)};assert selected
 common=set.intersection(*[{a.group for a in o.data.vertices[i].groups if a.weight>.5} for i in selected])
 # The original4/64-corner source footprint is authoritative. Dense authoring
 # subdivisions contain tiny floating-point boundary wrinkles, not new hems.
 template=o['tailored_source']
 if template not in template_hulls:
  with bpy.data.libraries.load(str(ROOT/f'development/levels/blender/{template}.blend'),link=False) as (src,dst):dst.objects=[template]
  prototype=dst.objects[0];gids={g.index for g in prototype.vertex_groups if 'groundsheet' in g.name.lower() or 'physical floor' in g.name.lower()}
  unique=sorted({(v.co.x,v.co.y) for v in prototype.data.vertices if any(a.group in gids and a.weight>.5 for a in v.groups)});vectors=[Vector(p) for p in unique]
  template_hulls[template]=[unique[i] for i in convex_hull_2d(vectors)];bpy.data.objects.remove(prototype,do_unlink=True)
 hull=[tuple((matrix@Vector((x,y,0)))[:2]) for x,y in template_hulls[template]]
 if area(hull)<0:hull.reverse()
 bounds=(min(p[0] for p in hull),min(p[1] for p in hull),max(p[0] for p in hull),max(p[1] for p in hull))
 material=None;uvs={layer.name:[] for layer in o.data.uv_layers};o.data.calc_loop_triangles()
 old_floor_triangles=sum(all(i in selected for i in t.vertices) for t in o.data.loop_triangles)
 original_diagonals=set()
 for t in o.data.loop_triangles:
  if all(i in selected for i in t.vertices):continue
  boundary={tuple(sorted(e)) for e in o.data.polygons[t.polygon_index].edge_keys}
  original_diagonals.update(tuple(sorted((a,c))) for a,c in zip(tuple(t.vertices),tuple(t.vertices)[1:]+tuple(t.vertices)[:1]) if tuple(sorted((a,c))) not in boundary)
 for poly in o.data.polygons:
  if poly.normal.z<.2 or not all(i in selected for i in poly.vertices):continue
  material=poly.material_index
  for loop in poly.loop_indices:
   co=o.data.vertices[o.data.loops[loop].vertex_index].co
   for layer in o.data.uv_layers:uvs[layer.name].append((co.x,co.y,tuple(layer.data[loop].uv)))
 assert material is not None
 coefficients={};uv_fit_error=0
 for layer,values in uvs.items():
  a=min(values,key=lambda p:(p[0],p[1]));c=max(values,key=lambda p:(p[0]-a[0])**2+(p[1]-a[1])**2)
  d=max(values,key=lambda p:abs(cross((c[0]-a[0],c[1]-a[1]),(p[0]-a[0],p[1]-a[1]))))
  inv=Matrix([(p[0],p[1],1) for p in [a,c,d]]).inverted()
  coeff=[inv@Vector([p[2][axis] for p in [a,c,d]]) for axis in [0,1]];coefficients[layer]=coeff
  for x,y,uv in values:uv_fit_error=max(uv_fit_error,max(abs(coeff[k].dot(Vector((x,y,1)))-uv[k]) for k in [0,1]))
 assert uv_fit_error<3e-5,(name,'non-affine source floor UV',uv_fit_error)
 vertices=[];top_faces=[];cache={};clipped_triangles=0
 def index(x,y,z):
  key=(round(x,7),round(y,7))
  if key not in cache:cache[key]=len(vertices);vertices.append((x,y,z))
  return cache[key]
 for ground,xy,box in triangles:
  if box[2]<bounds[0] or box[0]>bounds[2] or box[3]<bounds[1] or box[1]>bounds[3]:continue
  polygon=clip(xy,hull)
  if len(polygon)<3 or abs(area(polygon))<1e-12:continue
  clipped_triangles+=1;ga,gb,gc=ground;ab=gb-ga;ac=gc-ga;den=cross((ab.x,ab.y),(ac.x,ac.y));face=[]
  for x,y in polygon:
   ap=(x-ga.x,y-ga.y);u=cross(ap,(ac.x,ac.y))/den;v=cross((ab.x,ab.y),ap)/den;z=ga.z+u*ab.z+v*ac.z
   i=index(x,y,z)
   if not face or face[-1]!=i:face.append(i)
  if len(face)>1 and face[0]==face[-1]:face.pop()
  for j in range(1,len(face)-1):
   tri=(face[0],face[j],face[j+1])
   if len(set(tri))==3:
    signed=area([(vertices[i][0],vertices[i][1]) for i in tri])
    assert signed>=-1e-12,(name,'reversed clipping triangle',signed)
    if signed>1e-12:top_faces.append(tri)
 assert vertices and top_faces
 edge_counts={};oriented={}
 for f in top_faces:
  for a,c in zip(f,f[1:]+f[:1]):key=tuple(sorted((a,c)));edge_counts[key]=edge_counts.get(key,0)+1;oriented[key]=(a,c)
 if any(n not in [1,2] for n in edge_counts.values()):
  bad=[dict(edge=list(key),adjacent=n,triangles=[dict(indices=list(f),area=area([(vertices[i][0],vertices[i][1]) for i in f])) for f in top_faces if all(i in f for i in key)]) for key,n in edge_counts.items() if n not in [1,2]]
  (ROOT/'development/quality/evidence/DEV-070-clipped-floor-degenerates.json').write_text(json.dumps(dict(name=name,hull_vertices=len(hull),bad=bad),indent=2)+'\n',encoding='utf-8')
  raise AssertionError((name,'overlapping top triangles',bad[:2]))
 boundary=[oriented[key] for key,n in edge_counts.items() if n==1]
 bm=bmesh.new();bm.from_mesh(o.data);deform=bm.verts.layers.deform.active;original_index=bm.verts.layers.int.new('FloorRetopoOriginalVertex')
 for v in bm.verts:v[original_index]=v.index
 bmesh.ops.delete(bm,geom=[v for v in bm.verts if v.index in selected],context='VERTS')
 top=[bm.verts.new(inverse@Vector((x,y,z+TOP))) for x,y,z in vertices]
 bottom=[bm.verts.new(inverse@Vector((x,y,z+BOTTOM))) for x,y,z in vertices]
 for v in top+bottom:
  v[original_index]=-1
  for group in common:v[deform][group]=1
 layers={key:bm.loops.layers.uv.get(key) for key in coefficients}
 def face(vv,side=False):
  f=bm.faces.new(vv);f.material_index=material;f.smooth=False
  for layer,coeff in coefficients.items():
   if side:
    direction=vv[-1].co-vv[0].co;axis=0 if abs(direction.x)>=abs(direction.y) else 1
   for i,loop in enumerate(f.loops):
    v=loop.vert.co;uv=[c.dot(Vector((v.x,v.y,1))) for c in coeff]
    if side:uv=[uv[axis],.025 if i in [1,2] else .975]
    loop[layers[layer]].uv=uv
 for f in top_faces:face([top[i] for i in f]);face([bottom[i] for i in f[::-1]])
 for a,c in boundary:face([top[a],bottom[a],bottom[c],top[c]],True)
 bmesh.ops.remove_doubles(bm,verts=top+bottom,dist=1e-6)
 bm.normal_update();o.data=o.data.copy();bm.to_mesh(o.data);bm.free();o.data.update();o['runtime_geometry_source']=id
 old_to_new={p.value:i for i,p in enumerate(o.data.attributes['FloorRetopoOriginalVertex'].data) if p.value>=0}
 desired={(old_to_new[a],old_to_new[c]) for a,c in original_diagonals}
 o.data.calc_loop_triangles()
 for t in o.data.loop_triangles:
  if any(o.data.attributes['FloorRetopoOriginalVertex'].data[i].value>=0 for i in t.vertices):continue
  boundary_edges={tuple(sorted(e)) for e in o.data.polygons[t.polygon_index].edge_keys}
  desired.update(tuple(sorted((a,c))) for a,c in zip(tuple(t.vertices),tuple(t.vertices)[1:]+tuple(t.vertices)[:1]) if tuple(sorted((a,c))) not in boundary_edges)
 explicit_triangles(o,desired);o.data.attributes.remove(o.data.attributes['FloorRetopoOriginalVertex'])
 assert pose==(tuple(o.location),tuple(o.rotation_euler),tuple(o.scale))
 o.data.calc_loop_triangles();selected_new={v.index for v in o.data.vertices if any(a.group in groups and a.weight>.5 for a in v.groups)}
 new_floor_triangles=sum(all(i in selected_new for i in t.vertices) for t in o.data.loop_triangles)
 row=dict(name=name,id=id,old_floor_triangles=old_floor_triangles,new_floor_triangles=new_floor_triangles,clipped_terrain_triangles=clipped_triangles,footprint_native_xy=[list(p) for p in hull],source_uv_affine_error=uv_fit_error,native_pose=dict(location=list(o.location),rotation_euler=list(o.rotation_euler),scale=list(o.scale)),standing_offset_m=TOP,underside_offset_m=BOTTOM)
 rows.append(row)
 # A dependency-only snapshot avoids putting the entire site in every model file.
 asset=o.copy();asset.data=o.data.copy();asset.parent=None;asset.location=(0,0,0);asset.rotation_euler=(0,0,0);asset.scale=(1,1,1);asset.name=id
 file=temp/(id+'.blend');bpy.data.libraries.write(str(file),{asset},compress=True);snapshots.append((id,file));bpy.data.objects.remove(asset,do_unlink=True)
 print('FLOOR',name,old_floor_triangles,'->',new_floor_triangles,flush=True)
target=OUT/'hollow-creek-fitted.blend';bpy.context.scene['production_status']='Terrain-clipped tent assemblies; runtime integration/acceptance pending';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
assets=[]
for id,file in snapshots:
 s.reset()
 with bpy.data.libraries.load(str(file),link=False) as (src,dst):dst.objects=[id]
 o=dst.objects[0];bpy.context.scene.collection.objects.link(o)
 b.export(id,[o],'campground-terrain-fitted',view=(5,-7,3.8));assets.append(b.LOG[-1])
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
r=dict(status='terrain-clipped source and eight fitted assets; independent validation pending',source=source.relative_to(ROOT).as_posix(),scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),instances=rows,assets=assets,protected_sources=protected,old_floor_triangles=sum(p['old_floor_triangles'] for p in rows),new_floor_triangles=sum(p['new_floor_triangles'] for p in rows),runtime_gameplay_acceptance=False)
(OUT/'fitted-floors.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');print('FITTED',len(rows),'actual assemblies; floors',r['old_floor_triangles'],'->',r['new_floor_triangles'],flush=True)
