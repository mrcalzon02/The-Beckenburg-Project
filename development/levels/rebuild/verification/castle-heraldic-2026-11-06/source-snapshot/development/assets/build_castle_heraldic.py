"""Low-count wall-borne heraldic exhibit; actual joined support, generated atlas."""
from pathlib import Path
import bpy,bmesh,math,json,hashlib,importlib.util
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/heraldic-rebuild';PRE.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
image=bpy.data.images.load(str(OUT/'textures/heraldic-arms-imagegen-master.png'))
image.scale(512,512);image.filepath_raw=str(OUT/'textures/heraldic-arms-runtime.png');image.file_format='PNG';image.save();image.pack()
mat=bpy.data.materials.new('Original conserved heraldry / oak / steel / wrapped leather');mat.use_nodes=True
node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Closest'
shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.78
mat.node_tree.links.new(node.outputs['Color'],shader.inputs['Base Color'])
regions={'front':(.008,.508,.484,.484),'oak':(.508,.508,.484,.484),'steel':(.008,.008,.484,.484),'grip':(.508,.008,.484,.484)}
def mesh(name,verts,faces,region='steel',coords=None):
 m=bpy.data.meshes.new(name);m.from_pydata(verts,[],faces);m.update();m.uv_layers.new();m.materials.append(mat)
 o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o)
 for poly in m.polygons:
  for k,loop in enumerate(poly.loop_indices):
   idx=m.loops[loop].vertex_index
   u,v=coords[idx] if coords else [(0,0),(1,0),(1,1),(0,1)][k%4]
   a,b,w,h=regions[region];m.uv_layers.active.data[loop].uv=(a+w*u,b+h*v)
 return o
def box(name,c,s,region='steel'):
 verts=[(c[0]+x*s[0]/2,c[1]+y*s[1]/2,c[2]+z*s[2]/2) for x,y,z in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
 return mesh(name,verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],region)
def section_tube(name,centres,widths,axis,depth,region='steel'):
 side=Vector((axis.z,0,-axis.x));verts=[];uv=[]
 for j,(p,w) in enumerate(zip(centres,widths)):
  for k in range(6):
   angle=k*math.pi/3;verts.append(tuple(p+side*(w*math.cos(angle))+Vector((0,depth*math.sin(angle),0))));uv.append((k/5,j/(len(centres)-1)))
 n=len(centres);faces=[tuple(range(5,-1,-1)),tuple(range((n-1)*6,n*6))]
 for j in range(n-1):
  for k in range(6):faces.append((j*6+k,j*6+(k+1)%6,(j+1)*6+(k+1)%6,(j+1)*6+k))
 return mesh(name,verts,faces,region,uv)
# Actual wall-borne subframe: back facesY=0, other parts remain forward of wall.
box('Wall bearing steel spine',(0,-.006,.84),(.24,.012,.70))
box('Upper retained crossrail',(0,-.023,1.14),(.69,.034,.036))
box('Shield lower standoff',(0,-.09,.61),(.065,.168,.045))
outline=[(-.41,1.24),(.41,1.24),(.41,.92),(.32,.65),(.17,.46),(0,.36),(-.17,.46),(-.32,.65),(-.41,.92)]
# Shallow compound curvature, continuous closed skin. Detail lives in atlas.
front=[(x,-.185+.028*(abs(x)/.41)**2,z) for x,z in outline]+[(0,-.19,.86)]
back=[(x,-.145+.028*(abs(x)/.41)**2,z) for x,z in outline]+[(0,-.15,.86)]
uv=[((x+.41)/.82,(z-.36)/.88) for x,y,z in front]
mesh('Continuous painted convex heater face',front,[(9,i,(i+1)%9) for i in range(9)],'front',uv)
mesh('Oak rear and closed edge',back+front,[(9,(i+1)%9,i) for i in range(9)]+[(i,(i+1)%9,(i+1)%9+10,i+10) for i in range(9)],'oak',uv+uv)
for sign,depth in [(-1,-.065),(1,-.105)]:
 guard=Vector((sign*.34,depth,.40));tip=Vector((-sign*.59,depth,1.65));axis=(tip-guard).normalized();side=Vector((axis.z,0,-axis.x));length=(tip-guard).length
 # Diamond blade has real bevel silhouette and continuous tang into the hilt.
 verts=[];uv=[]
 for d,w in [(-.018,.033),(length*.80,.021)]:
  p=guard+axis*d
  for offset,t in [(side*w,.0),(Vector((0,-.008,0)),.5),(-side*w,1.),(Vector((0,.008,0)),.5)]:
   verts.append(tuple(p+offset));uv.append((t,(d+.018)/(length+.018)))
 verts.append(tuple(tip));uv.append((.5,1.))
 faces=[(3,2,1,0)]+[(i,(i+1)%4,(i+1)%4+4,i+4) for i in range(4)]+[(i+4,(i+1)%4+4,8) for i in range(4)]
 mesh('Forged diamond blade '+str(sign),verts,faces,'steel',uv)
 centres=[guard+side*t+axis*(-.022*abs(t)/.16) for t in [-.16,-.08,0,.08,.16]]
 section_tube('Continuous gently swept quillons '+str(sign),centres,[.012,.013,.014,.013,.012],side,.012)
 section_tube('Wrapped grip '+str(sign),[guard-axis*.225,guard+axis*.005],[.017,.021],axis,.017,'grip')
 section_tube('Faceted connected pommel '+str(sign),[guard-axis*.254,guard-axis*.217],[.036,.035],axis,.022)
 # Positive support saddles terminate on the actual blade, not empty air.
 t=(1.14-guard.z)/axis.z;p=guard+axis*t
 rear=depth+.008
 box('Blade retention saddle '+str(sign),(p.x,(-.04+rear)/2,1.14),(.047,abs(rear+.04)+.004,.045))
bpy.ops.object.select_all(action='SELECT');bpy.context.view_layer.objects.active=next(o for o in bpy.context.scene.objects if o.type=='MESH');bpy.ops.object.join()
obj=bpy.context.object;bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free();obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)<=600,len(obj.data.loop_triangles)
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
def audit(data,phase,digest=None):
 data=data.copy();bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(data);bm.free()
 assert max(v.co.y for v in data.vertices)<.00001
 for v in data.vertices:v.co=(v.co.x,v.co.z,-v.co.y)
 data.calc_loop_triangles();r=contact.audit_contacts(data,phase,digest)
 r['criterion']='Rigid wall audit(x,z,-y): every real triangle component reaches rear steel spine on wallY=0 within1mm'
 (OUT/('armour-contact-audit-'+phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
 (OUT/(phase+'.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8');return r
audit(obj.data,'heraldic-wall-source')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'heraldic_arms.blend'))
path=OUT/'exports/level_castle_heraldic_arms.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');digest=hashlib.sha256(path.read_bytes()).hexdigest();r=audit(obj.data,'heraldic-wall-export',digest)
r.update(status='EXPORTED_GEOMETRY_VERIFIED_VISUAL_NATIVE_REVIEW_PENDING',bounds=[list(obj.dimensions)],source_sha256=digest,reference_swords=['https://www.metmuseum.org/art/collection/search/27966','https://www.metmuseum.org/art/collection/search/23367'],reference_shield_material='https://www.metmuseum.org/art/collection/search/27889')
(OUT/'heraldic-review.json').write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16;scene.render.resolution_x=850;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Heraldic inspection world');scene.world.color=(.25,.25,.25)
for pos,power in [((1,-3,4),400),((-2,-1,2),200)]:
 bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=2.;scene.camera=cam
for name,pos in [('front',(0,-4,1)),('quarter',(2,-4,1.7)),('side',(4,-.5,1.4)),('rear',(0,4,1))]:
 cam.location=pos;cam.rotation_euler=(Vector((0,-.08,.85))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
print('PASS source/export heraldic support',r['triangle_count'],'triangles; actual exported renders await visual inspection')
