"""Original Castle museum/restoration kit, metres; Blender 5 source + GLB review.

Each asset has its own packed material images. Exported models are reimported
for independent dimensions/material inspection and studio captures. This is an
asset construction study, not automatic runtime or artistic acceptance.
"""
from pathlib import Path
import bpy, math, random, json, hashlib, sys, argparse, importlib.util
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'development/assets/castle-museum'
MODELS=OUT/'exports'
MODELS.mkdir(parents=True,exist_ok=True)
OUT.mkdir(parents=True,exist_ok=True)
(OUT/'previews').mkdir(exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
assets=[]
parser=argparse.ArgumentParser();parser.add_argument('--only',default='armour_display');parser.add_argument('--clay',action='store_true')
options=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
current=''

def material(part,base,kind='stone'):
    name=current+' / '+part
    mat=bpy.data.materials.new(name);mat.use_nodes=True
    shader=mat.node_tree.nodes.get('Principled BSDF')
    shader.inputs['Roughness'].default_value=.8 if kind in ['cloth','stone','oak'] else .54
    shader.inputs['Metallic'].default_value=.65 if kind in ['iron','brass'] else 0
    image=bpy.data.images.new(name,128,128);rng=random.Random(name);pixels=[]
    for y in range(128):
        for x in range(128):
            grain=.05*math.sin(x*.52+math.sin(y*.09)*1.8)+.025*math.sin(x*2.6) if kind=='oak' else 0
            weave=((x%3==0)*.04-(y%3==0)*.035) if kind=='cloth' else 0
            scratch=.07 if kind in ['iron','brass'] and (x+int(y*.09))%47==0 else 0
            seam=-.12 if kind=='stone' and y%43<2 else 0
            variation=rng.uniform(-.035,.035)+grain+weave+scratch+seam
            rgb=[max(0,min(1,c+variation)) for c in base]
            if part=='Embroidered hanging':
                # Original castle-and-key heraldry, with woven border and hem.
                border=x<8 or x>119 or y<8 or y>119
                tower=(28<x<44 or 84<x<100) and 34<y<88
                keep=43<x<85 and 34<y<67
                merlon=72<y<84 and ((x//8)%2==0) and 28<x<100
                key=62<x<67 and 15<y<33 or (59<x<73 and 16<y<20)
                if border or tower or keep or merlon or key:rgb=[.64+variation,.48+variation,.19+variation]
            pixels.extend(rgb+[1])
    image.pixels=pixels;image.pack()
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest'
    mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
    return mat

def finish(obj,name,mat):
    obj.name=name;obj.data.materials.append(mat)
    return obj

def box(name,loc,size,mat,bevel=0):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);obj=finish(bpy.context.object,name,mat)
    obj.dimensions=size;bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    if bevel:
        modifier=obj.modifiers.new('Worn arris','BEVEL');modifier.width=bevel;modifier.segments=1
        bpy.ops.object.modifier_apply(modifier=modifier.name)
    # Wood grain follows the longest visible dimension of each separate member.
    if mat.name.endswith('Oak'):
        for poly in obj.data.polygons:
            axis=max(range(3),key=lambda a:abs(poly.normal[a]));directions=[a for a in range(3) if a!=axis]
            major=max(directions,key=lambda a:size[a]);minor=next(a for a in directions if a!=major)
            for loop in poly.loop_indices:
                co=obj.data.vertices[obj.data.loops[loop].vertex_index].co
                obj.data.uv_layers.active.data[loop].uv=(co[minor]/size[minor]+.5,co[major]/size[major]+.5)
    return obj

def ellipsoid(name,loc,scale,mat,segments=12,rings=8):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,radius=1,location=loc)
    obj=finish(bpy.context.object,name,mat);obj.scale=scale
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    return obj

def rod(name,a,b,radius,mat,vertices=10):
    a,b=Vector(a),Vector(b);bpy.ops.mesh.primitive_cylinder_add(vertices=vertices,radius=radius,depth=(b-a).length,location=(a+b)/2)
    obj=finish(bpy.context.object,name,mat);obj.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler()
    return obj

def text(name,body,loc,size,mat):
    bpy.ops.object.text_add(location=loc,rotation=(math.pi/2,0,0));obj=bpy.context.object;obj.name=name
    obj.data.body=body;obj.data.size=size;obj.data.align_x='CENTER';obj.data.extrude=.0005
    obj.data.materials.append(mat);bpy.ops.object.convert(target='MESH');return bpy.context.object

def start(name):
    global current
    bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
    current=name

def export():
    if options.only and current!=options.only:return
    name='level_castle_'+current
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(current+'.blend')))
    bpy.ops.export_scene.gltf(filepath=str(MODELS/(name+'.glb')),export_format='GLB',export_yup=True,export_apply=True)
    assets.append(name)

start('armour_display')
spec=importlib.util.spec_from_file_location('castle_armour_lowpoly',Path(__file__).with_name('castle_armour_lowpoly.py'))
armour=importlib.util.module_from_spec(spec);spec.loader.exec_module(armour)
armour.build(material,box,rod,ellipsoid)
export()

start('museum_rope')
brass=material('Tarnished brass stanchions',(.37,.28,.13),'brass')
velvet=material('Braided red velvet',(.24,.035,.045),'cloth')
for x in [-.9,.9]:
    rod('Weighted brass foot',(x,0,.035),(x,0,.085),.15,brass,16)
    rod('Tapered brass stem',(x,0,.085),(x,0,.94),.028,brass)
    ellipsoid('Finial',(x,0,.96),(.055,.055,.055),brass)
    for z in [.12,.88]:rod('Collar',(x,0,z-.015),(x,0,z+.015),.045,brass)
points=[(-.9+1.8*i/24,0,.91-.18*math.sin(math.pi*i/24)) for i in range(25)]
for i in range(24):rod('Sagged velvet rope',points[i],points[i+1],.024,velvet,8)
export()

start('museum_plaque')
bronze=material('Aged cast bronze plate',(.27,.18,.075),'brass')
ink=material('Engraved cream lettering',(.71,.66,.48))
oak=material('Oak',(.16,.105,.055),'oak')
box('Beveled plaque frame',(0,0,.16),(.62,.045,.32),oak,.012)
box('Cast bronze face',(0,-.028,.16),(.59,.016,.29),bronze,.006)
text('Exhibit title','BECKENBURG', (0,-.040,.23),.044,ink)
text('Collection label','CASTLE COLLECTION', (0,-.040,.16),.028,ink)
text('Accession label','GALLERY 02  /  RESTORED', (0,-.040,.10),.020,ink)
for x in [-.265,.265]:
    for z in [.055,.265]:ellipsoid('Fixing stud',(x,-.043,z),(.009,.005,.009),bronze,8,4)
export()

start('tapestry')
cloth=material('Embroidered hanging',(.18,.035,.055),'cloth')
oak=material('Oak',(.20,.12,.055),'oak');brass=material('Rod finials',(.38,.27,.1),'brass')
verts=[];uvs=[];faces=[]
for row in range(17):
    for col in range(13):
        u=col/12;v=row/16
        verts.append(((u-.5)*1.5,.045*math.cos(u*math.pi*8),.15+v*2.25-.07*(1-v)*math.cos(u*math.pi*6)))
        uvs.append((u,v))
for row in range(16):
    for col in range(12):
        i=row*13+col;faces.append((i,i+1,i+14,i+13))
mesh=bpy.data.meshes.new('Woven draped hanging');mesh.from_pydata(verts,[],faces);mesh.uv_layers.new()
obj=bpy.data.objects.new('Embroidered castle hanging',mesh);bpy.context.collection.objects.link(obj);obj.data.materials.append(cloth)
for poly in mesh.polygons:
    for loop in poly.loop_indices:mesh.uv_layers.active.data[loop].uv=uvs[mesh.loops[loop].vertex_index]
solid=obj.modifiers.new('Cloth thickness','SOLIDIFY');solid.thickness=.007
rod('Hanging rod',(-.84,0,2.46),(.84,0,2.46),.024,oak)
for x in [-.88,.88]:ellipsoid('Rod finial',(x,0,2.46),(.055,.04,.04),brass)
for x in [-.65,-.32,0,.32,.65]:box('Suspension loop',(x,0,2.40),(.07,.035,.16),cloth)
export()

start('four_poster_bed')
oak=material('Oak',(.18,.105,.055),'oak')
cloth=material('Worn burgundy bed curtains',(.18,.035,.05),'cloth')
linen=material('Aged linen mattress and pillows',(.56,.51,.40),'cloth')
brass=material('Small bed fittings',(.35,.24,.08),'brass')
for x in [-1.10,1.10]:
    for y in [-.73,.73]:
        box('Square carved bed post',(x,y,1.17),(.09,.09,2.34),oak,.008)
        ellipsoid('Turned finial',(x,y,2.40),(.07,.07,.09),oak)
        for z in [.24,1.94,2.25]:box('Post collar',(x,y,z),(.115,.115,.065),oak,.008)
for y in [-.73,.73]:
    box('Side rail',(0,y,.39),(2.24,.07,.20),oak,.01)
    box('Canopy side cornice',(0,y,2.26),(2.28,.11,.15),oak,.012)
for x in [-1.10,1.10]:
    box('End rail',(x,0,.39),(.07,1.50,.20),oak,.01)
    box('Canopy end cornice',(x,0,2.26),(.11,1.55,.15),oak,.012)
box('Carved headboard',(1.09,0,.85),(.075,1.41,.68),oak,.015)
for y in [-.48,0,.48]:box('Raised headboard panel',(1.04,y,.85),(.018,.35,.43),oak,.007)
box('Linen mattress',(0,0,.54),(2.12,1.36,.18),linen,.05)
box('Burgundy coverlet',(-.22,0,.65),(1.65,1.34,.09),cloth,.03)
for y in [-.35,.35]:ellipsoid('Linen pillow',(.76,y,.69),(.25,.29,.095),linen)
box('Canopy fabric roof',(0,0,2.30),(2.20,1.44,.025),cloth)
for x in [-.99,.99]:
    for y in [-.68,.68]:
        for fold in range(4):rod('Gathered curtain fold',(x+(fold-1.5)*.018,y,2.20),(x+(fold-1.5)*.03,y, .80),.025,cloth,6)
        rod('Curtain tie',(x-.05,y-.01,1.22),(x+.05,y-.01,1.22),.013,brass,6)
export()

# Heraldic arms are an actual shield and two separate pointed swords, not a printed rectangle.
start('heraldic_arms')
steel=material('Blade and shield rim',(.31,.33,.35),'iron')
oak=material('Oak',(.14,.075,.035),'oak')
red=material('Painted burgundy shield',(.25,.035,.045),'cloth')
brass=material('Heraldic tower and sword guards',(.49,.35,.12),'brass')
for sign in [-1,1]:
    guard=Vector((sign*-.29,.09,.65));tip=Vector((sign*.59,.09,1.83));direction=(tip-guard).normalized()
    side=Vector((direction.z,0,-direction.x))*.035
    vertices=[]
    for y in [-.012,.012]:
        for point in [guard-side,guard+side,tip]:vertices.append(tuple(point+Vector((0,y,0))))
    mesh=bpy.data.meshes.new('Tapered steel sword blade');mesh.from_pydata(vertices,[],[(0,2,1),(3,4,5),(0,1,4,3),(1,2,5,4),(2,0,3,5)])
    sword=bpy.data.objects.new('Crossed sword blade',mesh);bpy.context.collection.objects.link(sword);sword.data.materials.append(steel)
    mesh.uv_layers.new()
    for poly in mesh.polygons:
        for loop in poly.loop_indices:
            co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=(co.x+.7,co.z/2)
    rod('Sword guard',guard-side*4,guard+side*4,.025,brass,8)
    rod('Leather wrapped grip',guard-direction*.22,guard,.026,oak,8)
    ellipsoid('Sword pommel',guard-direction*.25,(.045,.035,.045),brass,8,6)
outline=[(-.43,1.43),(.43,1.43),(.43,.99),(.28,.67),(0,.47),(-.28,.67),(-.43,.99)]
vertices=[(x,y,z) for y in [-.05,.035] for x,z in outline]
faces=[tuple(reversed(range(7))),tuple(range(7,14))]+[(i,(i+1)%7,(i+1)%7+7,i+7) for i in range(7)]
mesh=bpy.data.meshes.new('Heater shield');mesh.from_pydata(vertices,[],faces);mesh.uv_layers.new()
shield=bpy.data.objects.new('Heater shield body',mesh);bpy.context.collection.objects.link(shield);mesh.materials.append(red)
for poly in mesh.polygons:
    for loop in poly.loop_indices:
        co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=(co.x+.5,co.z)
for i,(x,z) in enumerate(outline):
    nx,nz=outline[(i+1)%len(outline)];rod('Riveted shield rim',(x,-.065,z),(nx,-.065,nz),.022,steel,6)
    ellipsoid('Shield rivet',(x*.93,-.083,z),(.012,.008,.012),brass,8,4)
for x in [-.19,.19]:box('Applied castle tower',(x,-.095,1.05),(.10,.025,.35),brass)
box('Applied castle keep',(0,-.095,.97),(.30,.025,.20),brass)
for x in [-.22,-.16,-.03,.03,.16,.22]:box('Heraldic merlon',(x,-.095,1.25 if abs(x)>.1 else 1.10),(.035,.025,.06),brass)
export()

start('founder_statue')
stone=material('Weathered limestone carving',(.40,.39,.34))
patch=material('Pale restoration mortar',(.51,.49,.42))
box('Statue plinth foot',(0,0,.08),(.85,.76,.16),stone,.025)
box('Statue plinth die',(0,0,.24),(.71,.64,.16),stone,.015)
rod('Folded robe mass',(0,0,.42),(0,0,1.45),.27,stone,12)
for i in range(10):
    angle=2*math.pi*i/10
    rod('Carved cloak fold',(.25*math.cos(angle),.22*math.sin(angle),.38),(.19*math.cos(angle),.17*math.sin(angle),1.40),.055,stone,6)
ellipsoid('Shoulder mantle',(0,0,1.47),(.34,.23,.19),stone)
ellipsoid('Hood',(0,.015,1.79),(.19,.17,.24),stone)
ellipsoid('Carved face',(0,-.116,1.81),(.125,.07,.16),stone)
box('Carved brow',(0,-.173,1.86),(.20,.025,.025),stone,.006)
ellipsoid('Nose',(0,-.187,1.81),(.025,.033,.055),stone,8,6)
for sign in [-1,1]:
    rod('Mantle sleeve',(sign*.27,-.015,1.47),(sign*.21,-.23,1.24),.09,stone,8)
    ellipsoid('Hand supporting codex',(sign*.16,-.28,1.25),(.065,.055,.055),patch if sign==1 else stone)
book=box('Stone codex',(0,-.28,1.29),(.39,.24,.075),stone,.008);book.rotation_euler.x=.18
box('Codex central seam',(0,-.288,1.333),(.009,.21,.006),patch)
text('Carved pedestal inscription','THE KEEPER', (0,-.327,.215),.048,patch)
export()

start('great_fireplace')
stone=material('Smoke aged sandstone',(.34,.30,.25))
patch=material('Replacement voussoirs',(.44,.40,.33))
soot=material('Soot-darkened fireback',(.055,.045,.038))
iron=material('Forged fire grate',(.075,.08,.085),'iron')
coal=material('Cold charcoal remains',(.045,.04,.035))
box('Broad stone hearth',(0,-.10,.07),(2.95,1.04,.14),stone,.025)
for sign in [-1,1]:
    for i in range(5):box('Jamb ashlar',(sign*1.04,0,.28+i*.29),(.48,.66,.28),stone,.012)
box('Blackened brick fireback',(0,.30,.88),(1.63,.14,1.50),soot,.005)
for i in range(11):
    a=i*math.pi/11+.008;b=(i+1)*math.pi/11-.008
    vertices=[(r*math.cos(t),y,1.49+r*math.sin(t)) for y in [-.33,.33] for r,t in [(.78,a),(1.18,a),(1.18,b),(.78,b)]]
    mesh=bpy.data.meshes.new('Radial arch stone');mesh.from_pydata(vertices,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);mesh.uv_layers.new()
    obj=bpy.data.objects.new('Replacement keystone' if i==5 else 'Arch voussoir',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(patch if i==5 else stone)
    for poly in mesh.polygons:
        for loop in poly.loop_indices:
            co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=(co.x,co.z)
# Closed spandrel masonry transfers the broad mantel to the arch and jambs;
# do not balance a rectangular shelf only on the centre keystone.
for i in range(12):
    left=-1.28+i*2.56/12;right=left+2.56/12
    bottom=lambda x:1.49+math.sqrt(max(0,1.18**2-x*x))
    vertices=[(x,y,z) for y in [-.33,.33] for x,z in [(left,bottom(left)),(right,bottom(right)),(right,2.67),(left,2.67)]]
    mesh=bpy.data.meshes.new('Closed arch spandrel');mesh.from_pydata(vertices,[],[(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);mesh.uv_layers.new()
    obj=bpy.data.objects.new('Spandrel ashlar',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(stone)
    for poly in mesh.polygons:
        for loop in poly.loop_indices:
            co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=(co.x,co.z)
box('Stone mantel shelf',(0,0,2.76),(2.90,.82,.18),stone,.025)
box('Sealed flue above arch',(0,.18,2.60),(1.10,.30,.20),soot,.01)
for x in [-.52,-.26,0,.26,.52]:rod('Fire grate bar',(x,-.29,.23),(x,-.29,.65),.018,iron,6)
for z in [.28,.56]:rod('Fire grate cross rail',(-.63,-.29,z),(.63,-.29,z),.02,iron,6)
for i in range(4):
    x=-.48+i*.29;rod('Cold charcoal log',(x,-.03,.24),(x+.22,.20,.27),.06,coal,7)
export()

# Independent exported-artifact inspection, with neutral front/three-quarter renders.
report=[]
for name in assets:
    bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
    bpy.ops.import_scene.gltf(filepath=str(MODELS/(name+'.glb')))
    meshes=[o for o in bpy.context.scene.objects if o.type=='MESH']
    corners=[o.matrix_world@Vector(c) for o in meshes for c in o.bound_box]
    lo=[min(c[i] for c in corners) for i in range(3)];hi=[max(c[i] for c in corners) for i in range(3)]
    for obj in meshes:obj.data.calc_loop_triangles()
    entry=dict(id=name,dimensions_blender=[hi[i]-lo[i] for i in range(3)],meshes=len(meshes),triangles=sum(len(o.data.loop_triangles) for o in meshes),sha256=hashlib.sha256((MODELS/(name+'.glb')).read_bytes()).hexdigest(),status='EXPORTED_STUDY_REQUIRES_RENDER_REVIEW_AND_RUNTIME_FIT')
    assert all(math.isfinite(v) and v>0 for v in entry['dimensions_blender'])
    if name=='level_castle_armour_display':assert entry['triangles']<=1250,entry
    assert all(m.use_nodes and any(n.type=='TEX_IMAGE' and n.image for n in m.node_tree.nodes) for o in meshes for m in o.data.materials)
    report.append(entry)
    scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16
    scene.render.resolution_x=800;scene.render.resolution_y=800;scene.render.resolution_percentage=100
    if scene.world is None:scene.world=bpy.data.worlds.new('Neutral inspection world')
    scene.world.color=(.16,.16,.16)
    centre=Vector([(lo[i]+hi[i])/2 for i in range(3)]);span=max(entry['dimensions_blender'])
    bpy.ops.object.light_add(type='AREA',location=centre+Vector((-2,-3,4)));bpy.context.object.data.energy=450;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=4
    bpy.ops.object.light_add(type='AREA',location=centre+Vector((3,1,2)));bpy.context.object.data.energy=220;bpy.context.object.data.size=3
    bpy.ops.object.camera_add();camera=bpy.context.object;scene.camera=camera;camera.data.type='ORTHO';camera.data.ortho_scale=span*1.36
    if options.clay:
        clay=bpy.data.materials.new('Neutral clay geometry review');clay.diffuse_color=(.48,.48,.48,1);clay.use_nodes=True
        clay.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.85
        scene.view_layers[0].material_override=clay
    for angle,offset in [('front',(0,-3,.25)),('quarter',(2,-3,.5)),('side',(3,0,.25)),('rear',(0,3,.25))]:
        camera.location=centre+Vector(offset).normalized()*span*2
        camera.rotation_euler=(centre-camera.location).to_track_quat('-Z','Y').to_euler()
        scene.render.filepath=str(OUT/'previews'/(name+('-clay-' if options.clay else '-')+angle+'.png'));bpy.ops.render.render(write_still=True)
    if name=='level_castle_armour_display':
        centre=Vector((0,-.06,.32));camera.data.ortho_scale=.68
        for angle,offset in [('feet-front',(0,-2,.3)),('feet-quarter',(1,-2,.7)),('feet-side',(3,0,.15))]:
            camera.location=centre+Vector(offset).normalized()*1.4
            camera.rotation_euler=(centre-camera.location).to_track_quat('-Z','Y').to_euler()
            scene.render.filepath=str(OUT/'previews'/(name+('-clay-' if options.clay else '-')+angle+'.png'));bpy.ops.render.render(write_still=True)
(OUT/('inspection-'+options.only+'.json' if options.only else 'inspection.json')).write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('EXPORTED',len(assets),'original Castle museum studies; inspect independent renders before runtime integration')
