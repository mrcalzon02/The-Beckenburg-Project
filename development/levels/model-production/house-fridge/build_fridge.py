"""Original180triangle refrigerator; explicit panel UVs and attached hardware.
Build a staged GLB, independently reimport it, render, then review before promotion.
"""
from pathlib import Path
import sys,json,hashlib
import bpy
ROOT=Path(__file__).resolve().parents[4]
sys.path.insert(0,str(ROOT/'development/assets'))
import rebuild_support as s
HERE=Path(__file__).resolve().parent
STAGE=HERE/'staging';STAGE.mkdir(exist_ok=True)
NAME='level_house_fridge_worn'
s.reset()
image=bpy.data.images.load(str(HERE/'atlas-v1.png'));image.pack();image_size=list(image.size)
material=bpy.data.materials.new('House refrigerator original generated enamel atlas');material.use_nodes=True
shader=material.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.88
node=material.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Closest'
material.node_tree.links.new(node.outputs['Color'],shader.inputs['Base Color'])
# Measured generated boundaries differ from the prompt: front40.4%, side ends75%.
SIDE=(.408,.006,.746,.994);TOP=(.755,.695,.995,.994);BLACK=(.762,.015,.990,.235)
FRONT_UPPER=(.003,.739,.401,.995);FRONT_LOWER=(.003,.047,.401,.727)
BACK=(.755,.253,.995,.684);KICK=(.003,.003,.401,.045)
parts=[];bounds=[]
def box(name,lo,hi,uv=SIDE,faces_uv=None):
 x0,y0,z0=lo;x1,y1,z1=hi
 vertices=[(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]
 faces=[(0,1,5,4),(2,3,7,6),(3,0,4,7),(1,2,6,5),(3,2,1,0),(4,5,6,7)]
 mesh=bpy.data.meshes.new(name);mesh.from_pydata(vertices,[],faces);mesh.materials.append(material);mesh.update()
 obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj)
 layer=mesh.uv_layers.new(name='Panel surfaces')
 for index,polygon in enumerate(mesh.polygons):
  u0,v0,u1,v1=(faces_uv or {}).get(index,uv)
  for loop,point in zip(polygon.loop_indices,[(u0,v0),(u1,v0),(u1,v1),(u0,v1)]):layer.data[loop].uv=point
 parts.append(obj);bounds.append(dict(name=name,low=list(lo),high=list(hi)))
box('Insulated enamel body',(-.335,-.31,.04),(.335,.31,1.68),faces_uv={5:TOP,4:BLACK})
box('Recessed connected plinth',(-.325,-.32,.025),(.325,.30,.10),BLACK,{0:KICK})
box('Upper freezer door',(-.327,-.355,1.225),(.327,-.300,1.67),faces_uv={0:FRONT_UPPER})
box('Lower refrigerator door',(-.327,-.355,.105),(.327,-.300,1.210),faces_uv={0:FRONT_LOWER})
for x in [-.25,.25]:
 for y in [-.23,.23]:box('Attached rubber foot', (x-.025,y-.025,0),(x+.025,y+.025,.055),BLACK)
for label,bottom,top in [('Freezer',1.4,1.51),('Fridge',.79,1.0)]:
 box(label+' pull grip',(-.266,-.419,bottom),(-.234,-.395,top),BLACK)
 for z in [bottom+.014,top-.014]:box(label+' attached handle mount',(-.264,-.411,z-.0125),(-.236,-.349,z+.0125),BLACK)
# A real shallow connected back panel preserves the condenser pattern aspect ratio.
box('Attached condenser service panel',(-.31,.304,.29),(.31,.316,1.41),BLACK,{1:BACK})
def touches(a,b):return all(min(a['high'][i],b['high'][i])-max(a['low'][i],b['low'][i])>1e-6 for i in range(3))
reached={0}
while True:
 added={j for i in reached for j in range(len(bounds)) if touches(bounds[i],bounds[j])}-reached
 if not added:break
 reached|=added
assert len(reached)==len(bounds),'Detached source component'
obj=s.join(parts,NAME);bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
obj['mount_origin']='Actual four feet at floor Z=0, original metre coordinates, front -Y; no source rescale'
obj['surface_plan']='Explicit generated-atlas freezer/fridge/side/top/black hardware and aspect-matched condenser rectangles'
obj['triangle_budget']=200;obj.data.calc_loop_triangles();assert len(obj.data.loop_triangles)==180
out=STAGE/(NAME+'.glb')
bpy.ops.export_scene.gltf(filepath=str(out),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
bpy.ops.wm.save_as_mainfile(filepath=str(HERE/(NAME+'.blend')),compress=True)
sha=hashlib.sha256(out.read_bytes()).hexdigest()
# Inspect actual exported geometry, rather than render only the construction scene.
s.reset();bpy.ops.import_scene.gltf(filepath=str(out))
models=[o for o in bpy.context.scene.objects if o.type=='MESH'];count=0
for model in models:
 model.data.calc_loop_triangles();count+=len(model.data.loop_triangles)
 assert model.data.uv_layers.active
 for loop in model.data.uv_layers.active.data:assert all(-1e-7<=v<=1+1e-7 for v in loop.uv)
assert count==180
for tag,view in [('front',(2,-3,1.7)),('rear',(-2,3,1.7)),('front-straight',(0,-3,1.05))]:
 s.render_family(NAME,models,path=HERE/'previews'/(tag+'.png'),view=view)
report=dict(name=NAME,source_glb_sha256=sha,atlas_sha256=hashlib.sha256((HERE/'atlas-v1.png').read_bytes()).hexdigest(),atlas_size=image_size,triangles=count,budget=200,connected_box_components=len(bounds),source_components=bounds,result='PASS exported triangle/UV/attachment contract; actual reimported render visual review required before promotion',uv_regions=dict(side=SIDE,top=TOP,hardware=BLACK,upper_door=FRONT_UPPER,lower_door=FRONT_LOWER,condenser=BACK,kick=KICK))
(HERE/'verification.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
print('PASS original fridge180triangles; all15box components physically attached; exported/reimported UVs finite; inspect renders before promotion')
