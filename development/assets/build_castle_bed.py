"""Original compact tester bed; reference-led construction, texture detail, <=1250 triangles.

The V&A Great Bed of Ware informs joinery, tester/panelling and hanging hierarchy,
not its exceptional size or copied ornament. Its copyrighted photos stay outside
the repository. This is a fictional retained Castle furnishing adapted for use.
"""
from pathlib import Path
import bpy, bmesh, math, json, hashlib, importlib.util
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'development/assets/castle-museum'
PRE=OUT/'previews/four-poster-rebuild'
PRE.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)

# Four authored 256px regions: long-grain oak, carved headboard, woven cover,
# and stitched linen. Inlay, relief shadows, seams and wear are texture detail.
image=bpy.data.images.load(str(OUT/'textures/bed-imagegen-master.png'))
image.name='Castle tester bed / generated original component atlas';image.scale(512,512)
image.filepath_raw=str(OUT/'textures/bed-runtime-atlas.png');image.file_format='PNG';image.save();image.pack()
mat=bpy.data.materials.new('Restored oak / worn woven burgundy / linen');mat.use_nodes=True
shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.85
tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest'
mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])

def mesh(name,verts,faces,tile=0,uvs=None):
    m=bpy.data.meshes.new(name);m.from_pydata(verts,[],faces);m.update();m.uv_layers.new()
    o=bpy.data.objects.new(name,m);bpy.context.collection.objects.link(o);m.materials.append(mat)
    for p in m.polygons:
        normal=p.normal;axis=max(range(3),key=lambda a:abs(normal[a]));axes=[a for a in range(3) if a!=axis]
        lo=[min(m.vertices[i].co[a] for i in p.vertices) for a in axes]
        hi=[max(m.vertices[i].co[a] for i in p.vertices) for a in axes]
        # Carving belongs only to the headboard's broad inward-facing face.
        face_tile=tile if tile!=1 or (axis==0 and normal.x<0) else 0
        # One consistent physical scale for all long-grain timber members:
        # atlas tile represents1.42m across grain and2.40m along it.
        if face_tile==0 and hi[0]-lo[0]>hi[1]-lo[1]:axes.reverse();lo.reverse();hi.reverse()
        for loop in p.loop_indices:
            index=m.loops[loop].vertex_index;co=m.vertices[index].co
            if uvs:uv=uvs[index]
            elif face_tile==0:uv=[.03+(co[axes[0]]-lo[0])/1.42,(co[axes[1]]-lo[1])/2.40]
            else:uv=[(co[a]-lo[i])/max(1e-6,hi[i]-lo[i]) for i,a in enumerate(axes)]
            m.uv_layers.active.data[loop].uv=((face_tile%2+.018+uv[0]*.964)/2,(face_tile//2+.018+uv[1]*.964)/2)
    return o

def box(name,centre,size,tile=0):
    verts=[tuple(centre[a]+s[a]*size[a]/2 for a in range(3)) for s in [(-1,-1,-1),(1,-1,-1),(1,1,-1),(-1,1,-1),(-1,-1,1),(1,-1,1),(1,1,1),(-1,1,1)]]
    return mesh(name,verts,[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)],tile)

posts=[]
for x in [-1.025,1.025]:
    for y in [-.64,.64]:
        profile=[(0,.072),(.13,.072),(.18,.055),(.42,.055),(.46,.070),(.53,.070),(.60,.048),(1.56,.044),(1.62,.064),(1.70,.064),(2.16,.050),(2.26,.050),(2.34,.074)]
        verts=[(x+r*math.cos(i*math.pi/3),y+r*math.sin(i*math.pi/3),z) for z,r in profile for i in range(6)]
        faces=[tuple(range(5,-1,-1)),tuple((len(profile)-1)*6+i for i in range(6))]
        faces += [(j*6+i,j*6+(i+1)%6,(j+1)*6+(i+1)%6,(j+1)*6+i) for j in range(len(profile)-1) for i in range(6)]
        uv=[(.03+i/5*.328,z/2.40) for z,r in profile for i in range(6)]
        posts.append(mesh('Continuous shaped oak post',verts,faces,0,uv))
for y in [-.64,.64]:
    box('Mortised side rail',(0,y,.42),(2.06,.10,.20))

for x in [-1.025,1.025]:
    box('End rail',(x,0,.42),(.10,1.30,.20))

# Mitered closed tester ring: shared corners, no overlapping beam end caps.
# The former four full boxes duplicated coplanar faces at every junction.
outer=[(-1.095,-.71),(1.095,-.71),(1.095,.71),(-1.095,.71)]
inner=[(-.955,-.57),(.955,-.57),(.955,.57),(-.955,.57)]
ring_verts=[(x,y,z) for z in [2.14,2.32] for loop in [outer,inner] for x,y in loop]
ring_faces=[]
for i in range(4):
    j=(i+1)%4
    ring_faces += [(i,j,j+8,i+8),(i+4,i+12,j+12,j+4),
                   (i,j,j+4,i+4),(i+8,i+12,j+12,j+8)]
mesh('Mitered tester cornice with shared continuous corner surfaces',ring_verts,ring_faces)
box('Supported mattress deck',(0,0,.49),(2.04,1.22,.06))
box('Panelled oak headboard',(1.025,0,1.01),(.09,1.25,1.02),1)
box('Headboard upper moulding',(1.025,0,1.54),(.12,1.34,.08))
box('Solid timber tester',(0,0,2.25),(2.07,1.29,.04))

def cushion(name,centre,length,width,height,tile):
    # Two octagonal rings, enclosing sides and closed top/bottom, no loose strips.
    ring=[(-.5,-.38),(-.38,-.5),(.38,-.5),(.5,-.38),(.5,.38),(.38,.5),(-.38,.5),(-.5,.38)]
    verts=[(centre[0]+a*length,centre[1]+b*width,centre[2]+z*height) for z in [-.5,.5] for a,b in ring]
    faces=[tuple(range(7,-1,-1)),tuple(range(8,16))]+[(i,(i+1)%8,(i+1)%8+8,i+8) for i in range(8)]
    obj=mesh(name,verts,faces,tile)
    for poly in obj.data.polygons:
        for loop in poly.loop_indices:
            co=obj.data.vertices[obj.data.loops[loop].vertex_index].co
            # Shared top coordinates, with a narrow inward fold on side walls.
            # No whole coverlet/pillow artwork squeezed onto each thin edge.
            u=.055+.89*((co.x-centre[0])/length+.5)
            v=.055+.89*((co.y-centre[1])/width+.5)
            if abs(poly.normal.z)<.9:
                fold=(centre[2]+height/2-co.z)/height*.022
                u+=(.5-u)*fold;v+=(.5-v)*fold
            obj.data.uv_layers.active.data[loop].uv=((tile%2+.018+u*.964)/2,(tile//2+.018+v*.964)/2)
    return obj
cushion('Closed linen mattress',(0,0,.59),2.03,1.19,.16,3)
cushion('Bound woven coverlet',(-.27,0,.70),1.49,1.20,.06,2)
cushion('Resting linen pillow',(.70,0,.73),.45,.98,.12,3)
# Owner direction: flat alpha-cutout curtain faces form the four sides of
# an outer box; the actual timber bed, mattress and pillow remain inside.
# Clear centres reveal the interior rather than replacing it with a painted bed.
curtain_image=bpy.data.images.load(str(OUT/'textures/bed-curtains-imagegen-master.png'))
curtain_image.name='Castle tester bed / generated alpha-cutout curtains';curtain_image.scale(256,256)
curtain_image.filepath_raw=str(OUT/'textures/bed-curtains-runtime.png');curtain_image.file_format='PNG';curtain_image.save();curtain_image.pack()
curtain_mat=bpy.data.materials.new('Burgundy curtain sprites / alpha cutout');curtain_mat.use_nodes=True
curtain_mat.surface_render_method='DITHERED';curtain_mat.use_backface_culling=False
nodes=curtain_mat.node_tree.nodes;links=curtain_mat.node_tree.links
ct=nodes.new('ShaderNodeTexImage');ct.image=curtain_image;ct.interpolation='Closest'
cs=nodes.get('Principled BSDF');cs.inputs['Roughness'].default_value=.95
links.new(ct.outputs['Color'],cs.inputs['Base Color'])
cut=nodes.new('ShaderNodeMath');cut.operation='GREATER_THAN';cut.inputs[1].default_value=.5
links.new(ct.outputs['Alpha'],cut.inputs[0]);links.new(cut.outputs[0],cs.inputs['Alpha'])
faces=[[(x,-.64,.79),(x,.64,.79),(x,.64,2.16),(x,-.64,2.16)] for x in [-1.025,1.025]]
faces += [[(-1.025,y,.79),(1.025,y,.79),(1.025,y,2.16),(-1.025,y,2.16)] for y in [-.64,.64]]
for points in faces:
    o=mesh('Curtain sprite attached to cornice',points,[(0,1,2,3),(3,2,1,0)])
    o.data.materials.clear();o.data.materials.append(curtain_mat)
    for poly in o.data.polygons:
        for loop in poly.loop_indices:
            o.data.uv_layers.active.data[loop].uv=[(0,0),(1,0),(1,1),(0,1)][o.data.loops[loop].vertex_index]

bpy.ops.object.select_all(action='SELECT');bpy.context.view_layer.objects.active=posts[0];bpy.ops.object.join();obj=bpy.context.object
bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free();obj.data.calc_loop_triangles()
assert len(obj.data.loop_triangles)<=1250,len(obj.data.loop_triangles)
spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
contact.audit_contacts(obj.data,'bed-source')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'four_poster_bed.blend'))
path=OUT/'exports/level_castle_four_poster_bed.glb'
bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path))
obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');copy=obj.data.copy()
bm=bmesh.new();bm.from_mesh(copy);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001);bm.to_mesh(copy);bm.free();copy.calc_loop_triangles()
report=contact.audit_contacts(copy,'bed-export',hashlib.sha256(path.read_bytes()).hexdigest())
report['status']='GEOMETRY_VERIFIED_VISUAL_REVIEW_PENDING';report['dimensions_metres']=list(obj.dimensions)
report['reference']='https://www.vam.ac.uk/articles/great-bed-of-ware'
report['contract']='Original compact lodging adaptation; <=1250 triangles, actual support, component atlas and four alpha-cutout curtain sides around solid bed/pillow interior; no copied museum imagery'
(OUT/'bed-review.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')

# Studio review uses the actual reimported artifact, separate from native runtime.
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.005));floor=bpy.context.object
floor_mat=bpy.data.materials.new('Review floor');floor_mat.diffuse_color=(.12,.14,.16,1);floor.data.materials.append(floor_mat)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=20
scene.render.resolution_x=900;scene.render.resolution_y=900;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Bed review world');scene.world.color=(.18,.18,.18)
for location,energy,size in [((2,-3,5),650,4),((-3,2,4),450,3)]:
    bpy.ops.object.light_add(type='AREA',location=location);bpy.context.object.data.energy=energy;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=size
bpy.ops.object.camera_add();cam=bpy.context.object;scene.camera=cam;cam.data.type='ORTHO';cam.data.ortho_scale=3.6
for name,pos in [('quarter',(-3.8,-4.2,3.1)),('head',(3.8,-4.2,3.1)),('side',(0,-5,2.3)),('rear',(4.5,0,2.3))]:
    cam.location=pos;cam.rotation_euler=(Vector((0,0,1.15))-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(PRE/(name+'.png'));bpy.ops.render.render(write_still=True)
print('PASS bed source/export contacts;',report['triangle_count'],'triangles; independent reimported four-view renders; visual/runtime acceptance pending')
