"""Enclosed wing roofs on current sources; no regeneration of terrain or interiors."""
from pathlib import Path
import sys,json,struct,io,copy,math
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from append_static_glb import append,read
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys,native_faces
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'roof-construction-pass'
HIP_WINGS=set();LONG_AXIS_RIDGES=False;FACADE_CLOSURES=False
# x/z footprint, ceiling top, ridge rise. Separate wings retain their own heights.
WINGS={
'FARMHOUSE':[('house',12,10,26,28,6,2.2),('barn-stalls',31,9,43,16,3,1.3),('barn-loft',31,16,43,24,6,2.0),('workshop',27,26,39,33,3,1.4)],
'MANSION':[('main',8,24,28,50,7.2,2.8),('attic',28,14,43,28,10.2,2),('stair-tower',40,2,46,14,10.2,1.4),('ballroom',40,30,54,50,7.2,2.2),('east-reception',28,32,40,50,3.6,1.6),('gallery-join',28,28,40,32,7.2,.8),('receiving',43,20,53,26,3.6,1)],
'SCHOOL':[('teaching',10,12,30,36,7.2,2.4),('assembly',30,24,42,36,8.1,1.8),('gym',38,8,50,24,7.2,2),('health',30,12,38,24,3.6,1.4),('receiving',42,28,46,36,3.6,.7)]}
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2)+'\n',encoding='utf-8')
def mesh(name,vs,fs,mat):
 m=bpy.data.meshes.new(name);m.from_pydata([(x,-z,y) for x,y,z in vs],[],fs);m.update();bm=bmesh.new();bm.from_mesh(m);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,name;bm.to_mesh(m);bm.free();o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o);m.materials.append(mat);o['role']='enclosed pitched roof';uv=m.uv_layers.new(name='Metre roofing UV')
 for p in m.polygons:
  for i in p.loop_indices:
   v=m.vertices[m.loops[i].vertex_index].co;uv.data[i].uv=(v.x,v.y+v.z)
 return o
FACES=[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]
def slab(name,corners,depth,mat):
 faces=FACES if len(corners)==4 else [(2,1,0),(3,4,5),(0,1,4,3),(1,2,5,4),(2,0,3,5)]
 return mesh(name,[(x,y-depth,z) for x,y,z in corners]+corners,faces,mat)

def facade_closures(site,name,bounds,top,data,material):
 a,b,c,d=bounds;built=[];proof=[]
 for axis,fixed,lo,hi,inward in [(0,a,b,d,1),(0,c,b,d,-1),(1,b,a,c,1),(1,d,a,c,-1)]:
  stops=sorted({lo,hi}|{v for z in data['zones'] if z['kind'] not in ['outside','staging'] for v in [z['bounds'][1-axis],z['bounds'][3-axis]] if lo<v<hi})
  for start,end in zip(stops,stops[1:]):
   mid=(start+end)/2;x=fixed+inward*.04 if axis==0 else mid;zz=mid if axis==0 else fixed+inward*.04
   owners=[z for z in data['zones'] if z['kind'] not in ['outside','staging'] and z['y']>=0 and z['bounds'][0]-.0001<=x<=z['bounds'][2]+.0001 and z['bounds'][1]-.0001<=zz<=z['bounds'][3]+.0001]
   assert owners,(site,name,axis,fixed,start,end,'roof has no occupied support footprint')
   receiver=max(z['y']+z['height'] for z in owners)
   if receiver>=top-.01:continue
   x0,x1=(min(fixed,fixed+inward*.24),max(fixed,fixed+inward*.24)) if axis==0 else (start,end);z0,z1=(start,end) if axis==0 else (min(fixed,fixed+inward*.24),max(fixed,fixed+inward*.24))
   obj=slab(name+' supported upper facade '+str(len(built)),[(x0,top+.05,z0),(x1,top+.05,z0),(x1,top+.05,z1),(x0,top+.05,z1)],top+.05-(receiver-.08),material);obj['role']='roof-bearing facade closure';built.append(obj);proof.append(dict(name=obj.name,bottom_y=receiver-.08,top_y=top+.05,receiver_zones=[z['id'] for z in owners if abs(z['y']+z['height']-receiver)<.001]))
 return built,proof
def build(s):
 site=s['id'];folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);source=ROOT/s['glb'];oldnative=ROOT/s['native'];assert sha(source)==s['glb_sha256'] and sha(oldnative)==s['native_sha256'];data=json.loads((ROOT/s['layout']).read_text());meta=json.loads((ROOT/s['construction']).read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));oldobjects=list(bpy.context.scene.objects)
 mat=bpy.data.materials.new(site+' slate roof blocking');mat.diffuse_color=(.16,.19,.21,1);mat.use_nodes=True;mat.node_tree.nodes['Principled BSDF'].inputs['Base Color'].default_value=mat.diffuse_color;mat.node_tree.nodes['Principled BSDF'].inputs['Roughness'].default_value=.86
 built=[];proof=[]
 for name,a,b,c,d,top,rise in WINGS[site]:
  mid=(a+c)/2;eave=top+.24;ridge=eave+rise
  hip=(site,name) in HIP_WINGS;ridge_x=LONG_AXIS_RIDGES and c-a>d-b;midz=(b+d)/2
  if hip:
   corners=[(a,eave,b),(c,eave,b),(c,eave,d),(a,eave,d)];peak=(mid,ridge,midz)
   for i in range(4):built.append(slab(name+' hip face '+str(i),[corners[i],corners[(i+1)%4],peak],.22,mat))
   for x0,x1,label in [(a,a+.2,'west'),(c-.2,c,'east')]:built.append(slab(name+' fascia '+label,[(x0,eave,b),(x1,eave,b),(x1,eave,d),(x0,eave,d)],eave-top+.12,mat))
   for z0,z1,label in [(b,b+.2,'north'),(d-.2,d,'south')]:built.append(slab(name+' fascia '+label,[(a,eave,z0),(c,eave,z0),(c,eave,z1),(a,eave,z1)],eave-top+.12,mat))
  elif ridge_x:
   built+=[slab(name+' roof north',[(a,eave,b),(c,eave,b),(c,ridge,midz),(a,ridge,midz)],.22,mat),slab(name+' roof south',[(a,ridge,midz),(c,ridge,midz),(c,eave,d),(a,eave,d)],.22,mat)]
   for x0,x1,label in [(a,a+.24,'west'),(c-.24,c,'east')]:
    vs=[(x0,top-.12,b),(x0,top-.12,d),(x0,eave-.20,d),(x0,ridge-.20,midz),(x0,eave-.20,b)];vs+=[(x1,y,z) for x,y,z in vs];fs=[(4,3,2,1,0),(5,6,7,8,9)]+[(i,(i+1)%5,(i+1)%5+5,i+5) for i in range(5)];built.append(mesh(name+' gable '+label,vs,fs,mat))
   for z0,z1,label in [(b,b+.2,'north'),(d-.2,d,'south')]:built.append(slab(name+' eave closure '+label,[(a,eave,z0),(c,eave,z0),(c,eave,z1),(a,eave,z1)],eave-top+.12,mat))
  else:
   built+=[slab(name+' roof west',[(a,eave,b),(mid,ridge,b),(mid,ridge,d),(a,eave,d)],.22,mat),slab(name+' roof east',[(mid,ridge,b),(c,eave,b),(c,eave,d),(mid,ridge,d)],.22,mat)]
   for z0,z1,label in [(b,b+.24,'north'),(d-.24,d,'south')]:
    vs=[(a,top-.12,z0),(c,top-.12,z0),(c,eave-.20,z0),(mid,ridge-.20,z0),(a,eave-.20,z0)];vs+= [(x,y,z1) for x,y,z in vs];fs=[(4,3,2,1,0),(5,6,7,8,9)]+[(i,(i+1)%5,(i+1)%5+5,i+5) for i in range(5)];built.append(mesh(name+' gable '+label,vs,fs,mat))
   for x0,x1,label in [(a,a+.2,'west'),(c-.2,c,'east')]:built.append(slab(name+' eave closure '+label,[(x0,eave,b),(x1,eave,b),(x1,eave,d),(x0,eave,d)],eave-top+.12,mat))
  closures=[]
  if FACADE_CLOSURES:
   wallmat=next(o.data.materials[0] for o in oldobjects if o.get('role')=='rough shell wall' and o.data.materials)
   extra,closures=facade_closures(site,name,[a,b,c,d],top,data,wallmat);built.extend(extra)
  proof.append(dict(wing=name,bounds=[a,b,c,d],ceiling=top,ridge=ridge,roof_depth=.22,gable_depth=.24,roof_shape='hip' if hip else 'gable',ridge_axis='x' if ridge_x else 'z',facade_closures=closures))
 # Every roof addition stays above the occupied ceiling plane of overlapping rooms.
 for o in built:
  for v in o.data.vertices:
   p=o.matrix_world@v.co
   for z in data['zones']:
    if z['kind'] in ['outside','staging']:continue
    a,b,c,d=z['bounds']
    assert not (a+.001<p.x<c-.001 and b+.001<-p.y<d-.001 and z['y']+.001<p.z<z['y']+z['height']-.161),(site,o.name,z['id'],tuple(p))
 bpy.ops.wm.save_as_mainfile(filepath=str(folder/'enclosed-roofs.blend'),compress=True);bpy.ops.object.select_all(action='DESELECT')
 for o in built:o.select_set(True)
 addition=ROOT/'build'/('roof-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);append(source,addition,folder/'enclosed-roofs.glb')
 parts=[(o.name,records(o)) for o in built];raw=oldnative.read_bytes();magic,n,t=struct.unpack_from('>III',raw);stream=io.BytesIO();stream.write(struct.pack('>III',magic,n+len(parts),t+sum(len(ts) for _,ts in parts)));stream.write(raw[12:])
 for name,ts in parts:
  encoded=name.encode();stream.write(struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(ts)))
  for xyz,flags in ts:stream.write(struct.pack('>9fB',*xyz,flags))
 native=folder/'enclosed-roofs.hcs2';native.write_bytes(stream.getvalue());assert face_keys(native_faces(native))==face_keys(native_faces(oldnative)+[t for _,ts in parts for t in ts])
 # Reopen the exported file, check closed geometry, and ray-test roof coverage.
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'enclosed-roofs.glb'));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};vs=[];fs=[]
 for name,ts in parts:
  o=objects[name];assert face_keys(records(o))==face_keys(ts);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.00001);assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0;bm.free();offset=len(vs);vs.extend(o.matrix_world@v.co for v in o.data.vertices);fs.extend(tuple(offset+i for i in p.vertices) for p in o.data.polygons)
 tree=BVHTree.FromPolygons(vs,fs);rays=0
 for wing in proof:
  a,b,c,d=wing['bounds']
  for i in range(math.ceil(c-a)):
   for j in range(math.ceil(d-b)):
    x=a+(i+.5)*(c-a)/math.ceil(c-a);z=b+(j+.5)*(d-b)/math.ceil(d-b);hit=tree.ray_cast(Vector((x,-z,wing['ceiling']-.2)),Vector((0,0,1)),30)[0];assert hit is not None,(site,wing,x,z);rays+=1
 olddoc,oldbin=read(source);newdoc,newbin=read(folder/'enclosed-roofs.glb');assert newbin[:len(oldbin)]==oldbin
 for k in ['nodes','meshes','accessors','bufferViews','images','textures','materials']:assert newdoc.get(k,[])[:len(olddoc.get(k,[]))]==olddoc.get(k,[])
 # One native overview per map; no UI and no runtime acceptance claim.
 scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_shadows=True;scene.display.shading.show_cavity=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.world=bpy.data.worlds.new('Roof construction review');bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;a,b,c,d=data['site_bounds'];span=max(c-a,d-b);target=Vector(((a+c)/2,-(b+d)/2,3));cam.location=target+Vector((span*.8,-span*.8,span*.8));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.type='ORTHO';cam.data.ortho_scale=span*1.4;cap=BASE/'captures/roof-construction-batch'/ (site.lower()+'.png');cap.parent.mkdir(parents=True,exist_ok=True);scene.render.filepath=str(cap);bpy.ops.render.render(write_still=True)
 meta.update(status='ENCLOSED_ROOF_CONSTRUCTION_RUNTIME_INTEGRATION_OPEN',source_contract=s['construction'],source_glb=s['glb'],source_glb_sha256=sha(source),source_physics=s['native'],source_physics_sha256=sha(oldnative),before_layout=s['layout'],before_layout_sha256=sha(ROOT/s['layout']),layout=s['layout'],layout_sha256=sha(ROOT/s['layout']),before_segment_map=meta.get('source_segment_map',{}),before_stair_components=meta['stair_components'],glb_sha256=sha(folder/'enclosed-roofs.glb'),physics_sha256=sha(native),blend_sha256=sha(folder/'enclosed-roofs.blend'),roof_generator_sha256=sha(Path(__file__)),geometry_unchanged=False,roof_wings=proof,new_parts=len(parts),roof_coverage_rays=rays,captures=[dict(path=cap.relative_to(ROOT).as_posix(),sha256=sha(cap),type='native exported roof construction overview, not gameplay')]);write(folder/'construction.json',meta)
 return dict(id=site,construction=(folder/'construction.json').relative_to(ROOT).as_posix(),layout=s['layout'],glb=(folder/'enclosed-roofs.glb').relative_to(ROOT).as_posix(),native=native.relative_to(ROOT).as_posix(),blend=(folder/'enclosed-roofs.blend').relative_to(ROOT).as_posix(),glb_sha256=meta['glb_sha256'],native_sha256=meta['physics_sha256'])
def main():
 roster=json.loads((BASE/'access-route-pass/roster.json').read_text());sources=[]
 for s in roster['active_sources']:sources.append(build(s) if s['id'] in WINGS else s)
 decisions=[dict(id=s['id'],decision='ENCLOSED_ROOFS_BUILT_RUNTIME_OPEN' if s['id'] in WINGS else s['decision']) for s in roster['sites']];write(OUT/'roster.json',dict(status='THREE_MAP_ROOF_CONSTRUCTION_RUNTIME_OPEN',active_sources=sources,sites=decisions,baseline='development/levels/rebuild/access-route-pass/roster.json'));print('ENCLOSED_ROOFS_COMPLETE',list(WINGS),flush=True)
if __name__=='__main__':main()
