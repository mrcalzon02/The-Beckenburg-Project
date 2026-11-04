"""Low-count retained museum barrier: two weighted posts and continuous sagging rope.
Reference: Visiontron classic-post specification and photographs (manufacturer),
https://www.visiontron.com/queuing-crowd-control/classic-post-and-rope-stanchions-and-accessories/prime-classic-rope-stanchions/
Original geometry/textures; no product imagery is redistributed.
"""
from pathlib import Path
import bpy,math,json,hashlib,bmesh,importlib.util,random
ROOT=Path(__file__).resolve().parents[2];OUT=ROOT/'development/assets/castle-museum'
bpy.ops.wm.read_factory_settings(use_empty=True)

def material(name,base,rope=False):
    mat=bpy.data.materials.new(name);mat.use_nodes=True;bsdf=mat.node_tree.nodes.get('Principled BSDF')
    bsdf.inputs['Roughness'].default_value=.87 if rope else .51;bsdf.inputs['Metallic'].default_value=0 if rope else .5
    image=bpy.data.images.new(name,128,64);rng=random.Random(name);pixels=[]
    for y in range(64):
        for x in range(128):
            detail=(.014*math.sin((x+y*3)*.27) if rope else .012*math.sin(y*.3))+rng.uniform(-.005,.005)
            # Baked circumferential value changes carry the curved metal/velvet
            # form through the diffuse-only software path at low triangle count.
            u=x/127;shade=.68+.32*math.cos(2*math.pi*(u-(.25 if rope else .75)))
            pixels.extend([max(0,c*shade+detail) for c in base]+[1])
    image.pixels=pixels;image.pack();tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
    return mat
brass=material('Museum post / tarnished brass',(.39,.285,.11));velvet=material('Museum rope / burgundy velvet braid',(.24,.028,.044),True)
vertices=[];faces=[];coords=[];mats=[]
def patch(points,uv,mat):
    first=len(vertices);vertices.extend(points);faces.append(tuple(range(first,first+len(points))));coords.append(uv);mats.append(mat)
profile=[(0,.16),(.035,.16),(.055,.09),(.085,.027),(.88,.027),(.925,.052),(.98,.042),(1.0,.005)]
for centre in [-.9,.9]:
    rows=[[(centre+r*math.cos(2*math.pi*k/6),r*math.sin(2*math.pi*k/6),z) for k in range(6)] for z,r in profile]
    for j in range(len(rows)-1):
        for k in range(6):
            q=(k+1)%6;patch([rows[j][k],rows[j][q],rows[j+1][q],rows[j+1][k]],[(k/6,j/7),((k+1)/6,j/7),((k+1)/6,(j+1)/7),(k/6,(j+1)/7)],0)
    for row in [list(reversed(rows[0])),rows[-1]]:patch(row,[(.5+.48*math.cos(2*math.pi*k/6),.5+.48*math.sin(2*math.pi*k/6)) for k in range(6)],0)
rows=[]
for j in range(7):
    t=j/6;x=-.9+1.8*t;z=.93-.16*math.sin(math.pi*t)
    rows.append([(x,.020*math.cos(2*math.pi*k/6),z+.020*math.sin(2*math.pi*k/6)) for k in range(6)])
for j in range(6):
    for k in range(6):
        q=(k+1)%6;patch([rows[j][k],rows[j+1][k],rows[j+1][q],rows[j][q]],[(k/6,j/6),(k/6,(j+1)/6),((k+1)/6,(j+1)/6),((k+1)/6,j/6)],1)
for row in [rows[0],list(reversed(rows[-1]))]:patch(row,[(.5+.4*math.cos(2*math.pi*k/6),.5+.4*math.sin(2*math.pi*k/6)) for k in range(6)],1)
mesh=bpy.data.meshes.new('Two weighted brass posts and continuous velvet rope');mesh.from_pydata(vertices,[],faces);mesh.uv_layers.new();mesh.materials.append(brass);mesh.materials.append(velvet)
for poly,uv,mat in zip(mesh.polygons,coords,mats):
    poly.material_index=mat
    for loop,p in zip(poly.loop_indices,uv):mesh.uv_layers.active.data[loop].uv=p
obj=bpy.data.objects.new('Retained museum velvet barrier',mesh);bpy.context.collection.objects.link(obj)
bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free();mesh.calc_loop_triangles()
assert len(mesh.loop_triangles)<=300
spec=importlib.util.spec_from_file_location('audit',Path(__file__).with_name('castle_armour_lowpoly.py'));m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
report=m.audit_contacts(mesh,'rope-source')
rope=next(c for c in report['components'] if c['bounds'][0][2]>.7)
assert len(rope['contacts'])==2,'Both rope ends must actually meet the stanchions'
bpy.context.view_layer.objects.active=obj;obj.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'museum_rope.blend'))
path=OUT/'exports/level_castle_museum_rope.glb';bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH')
copy=obj.data.copy();bm=bmesh.new();bm.from_mesh(copy);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(copy);bm.free();copy.calc_loop_triangles()
report=m.audit_contacts(copy,'rope-export',hashlib.sha256(path.read_bytes()).hexdigest());rope=next(c for c in report['components'] if c['bounds'][0][2]>.7);assert len(rope['contacts'])==2
(OUT/'rope-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('PASS museum barrier',len(copy.loop_triangles),'triangles; both rope ends contact posts; weighted bases rest on floor; source/export checked')
