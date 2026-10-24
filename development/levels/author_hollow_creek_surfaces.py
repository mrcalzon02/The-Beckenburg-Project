"""Part-aligned, spatially authored wear baked to portable diffuse atlases.

Source UVs, geometry, normals, pivots and all fitted scene vertices are preserved.
The added UV atlas is a separate layer; runtime GLBs need no procedural shader.
"""
import bpy, sys, json, math, hashlib
from pathlib import Path
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[2]
sys.path[:0]=[str(ROOT/'development/levels'),str(ROOT/'development/assets')]
import rebuild_support as s
import build_lived_assets as b
OUT=ROOT/'development/levels/hollow-creek'
previous=json.loads((OUT/'refined-models.json').read_text(encoding='utf-8'))
SKIP=('shrub_decorative','creek_engineering','dock_utilities','tent_')
selected=[a for a in previous['assets'] if not any(k in a['id'] for k in SKIP)]
protected={str(p.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(p.read_bytes()).hexdigest()
           for a in previous['assets'] for p in [ROOT/a['editable'],ROOT/f"assets/runtime/models/{a['id']}.glb"]}
scene_source=OUT/'hollow-creek-refined.blend'
protected[scene_source.relative_to(ROOT).as_posix()]=hashlib.sha256(scene_source.read_bytes()).hexdigest()
records=[]

def linear(c):
    return c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4

def scalar(nodes,links,operation,a,c):
    n=nodes.new('ShaderNodeMath');n.operation=operation
    for value,i in [(a,0),(c,1)]:
        if isinstance(value,(int,float)):n.inputs[i].default_value=value
        else:links.new(value,n.inputs[i])
    return n.outputs[0]

def mix(nodes,links,factor,a,c):
    n=nodes.new('ShaderNodeMixRGB');links.new(factor,n.inputs[0])
    for value,i in [(a,1),(c,2)]:
        if isinstance(value,(tuple,list)):n.inputs[i].default_value=(*(linear(c) for c in value[:3]),1)
        else:links.new(value,n.inputs[i])
    return n.outputs[0]

def noise(nodes,links,coordinate,scale,detail=2):
    n=nodes.new('ShaderNodeTexNoise');n.inputs['Scale'].default_value=scale
    n.inputs['Detail'].default_value=detail;n.inputs['Roughness'].default_value=.65
    links.new(coordinate,n.inputs['Vector']);return n.outputs['Fac']

def material(old,atlas,id):
    m=old.copy();m.name='Authored '+old.name;m.use_nodes=True;n=m.node_tree.nodes;l=m.node_tree.links
    oldimage=next((q.image for q in n if q.type=='TEX_IMAGE' and q.image),None)
    shader=next(q for q in n if q.type=='BSDF_PRINCIPLED')
    if oldimage:
        raw=list(oldimage.pixels);base=[sum(raw[c::4])/max(1,len(raw)//4) for c in range(3)]
    else:base=list(shader.inputs['Base Color'].default_value[:3])
    # Source image buffers contain encoded diffuse values; shader colors are
    # linear. Avoid baking the display transfer function into the palette twice.
    encoded_base=base[:]
    if oldimage and oldimage.colorspace_settings.name=='sRGB':base=[linear(c) for c in base]
    n.clear();out=n.new('ShaderNodeOutputMaterial');em=n.new('ShaderNodeEmission');l.new(em.outputs[0],out.inputs['Surface'])
    coord=n.new('ShaderNodeTexCoord');xyz=coord.outputs['Object'];sep=n.new('ShaderNodeSeparateXYZ');l.new(xyz,sep.inputs[0])
    fiber=n.new('ShaderNodeAttribute');fiber.attribute_name='ConstructionFiber'
    mapping=n.new('ShaderNodeVectorMath');mapping.operation='MULTIPLY';mapping.inputs[1].default_value=(33,33,1.3);l.new(fiber.outputs['Vector'],mapping.inputs[0])
    broad=noise(n,l,xyz,3.8,3);fine=noise(n,l,xyz,105,1)
    factor=scalar(n,l,'ADD',scalar(n,l,'MULTIPLY',broad,.16),.91)
    factor=scalar(n,l,'ADD',factor,scalar(n,l,'MULTIPLY',fine,.045))
    name=old.name.lower();wood=any(k in name for k in ['oak','pine','timber','wood','crossing'])
    metal=any(k in name for k in ['iron','steel','brass','bronze','rust','hardware'])
    cloth=any(k in name for k in ['cloth','canvas','woven','quilt','groundsheet'])
    stone=any(k in name for k in ['stone','masonry','course','mortar'])
    # Restrained real part grain: no repeating image-border light bands.
    if wood:
        grain=noise(n,l,mapping.outputs[0],1,2)
        factor=scalar(n,l,'ADD',factor,scalar(n,l,'MULTIPLY',scalar(n,l,'SUBTRACT',grain,.5),.16))
    rgb=n.new('ShaderNodeRGB');rgb.outputs[0].default_value=(*base,1);color=rgb.outputs[0]
    multiply=n.new('ShaderNodeMixRGB');multiply.blend_type='MULTIPLY';multiply.inputs[0].default_value=1
    l.new(color,multiply.inputs[1]);l.new(factor,multiply.inputs[2]);color=multiply.outputs[0]
    edge=n.new('ShaderNodeAttribute');edge.attribute_name='ConstructionEdge'
    normal=n.new('ShaderNodeNewGeometry');ns=n.new('ShaderNodeSeparateXYZ');l.new(normal.outputs['Normal'],ns.inputs[0])
    exposure=scalar(n,l,'MAXIMUM',ns.outputs['Z'],0)
    # Lower edge splash/damp zone in physical metres, rather than a UV border.
    lower=scalar(n,l,'MAXIMUM',scalar(n,l,'SUBTRACT',1,scalar(n,l,'MULTIPLY',sep.outputs['Z'],3.2)),0)
    lower=scalar(n,l,'MINIMUM',lower,1)
    if wood:
        color=mix(n,l,scalar(n,l,'MULTIPLY',exposure,.22),color,(.27,.255,.225))
        color=mix(n,l,scalar(n,l,'MULTIPLY',lower,.17),color,(.075,.092,.053))
        color=mix(n,l,scalar(n,l,'MULTIPLY',edge.outputs['Fac'],.10),color,(.38,.31,.22))
    elif metal:
        patch=scalar(n,l,'MAXIMUM',scalar(n,l,'SUBTRACT',broad,.57),0)
        patch=scalar(n,l,'MINIMUM',scalar(n,l,'MULTIPLY',patch,2.1),.38)
        # Bronze develops muted green patina; steel gets sparse warm oxidation.
        tint=(.12,.21,.15) if 'bronze' in name or 'brass' in name else (.23,.115,.055)
        color=mix(n,l,patch,color,tint)
        color=mix(n,l,scalar(n,l,'MULTIPLY',edge.outputs['Fac'],.16),color,(.32,.33,.30))
    elif cloth:
        color=mix(n,l,scalar(n,l,'MULTIPLY',exposure,.13),color,tuple(min(1,c*1.17+.025) for c in encoded_base))
        color=mix(n,l,scalar(n,l,'MULTIPLY',lower,.14),color,(.10,.085,.05))
    elif not stone:
        color=mix(n,l,scalar(n,l,'MULTIPLY',exposure,.12),color,tuple(min(1,c*1.12+.018) for c in encoded_base))
        color=mix(n,l,scalar(n,l,'MULTIPLY',lower,.12),color,(.12,.115,.075))
    if 'fireplace' in id and stone:
        # Soot is confined to the mouth/lintel, never a global black material.
        sub=n.new('ShaderNodeVectorMath');sub.operation='SUBTRACT';l.new(xyz,sub.inputs[0]);sub.inputs[1].default_value=(0,-.73,1.38)
        div=n.new('ShaderNodeVectorMath');div.operation='DIVIDE';l.new(sub.outputs[0],div.inputs[0]);div.inputs[1].default_value=(1.35,.27,.88)
        length=n.new('ShaderNodeVectorMath');length.operation='LENGTH';l.new(div.outputs[0],length.inputs[0])
        soot=scalar(n,l,'MAXIMUM',scalar(n,l,'SUBTRACT',1,length.outputs['Value']),0)
        color=mix(n,l,scalar(n,l,'MULTIPLY',soot,.72),color,(.045,.039,.030))
    l.new(color,em.inputs['Color'])
    tex=n.new('ShaderNodeTexImage');tex.image=atlas;n.active=tex
    return m

for a in selected:
    print('BEGIN',a['id'],flush=True)
    s.reset();source=ROOT/a['editable']
    with bpy.data.libraries.load(str(source),link=False) as (src,dst):dst.objects=[a['id']]
    o=dst.objects[0];bpy.context.scene.collection.objects.link(o);o.data=o.data.copy()
    bpy.context.view_layer.objects.active=o;o.select_set(True)
    original_vertex=[tuple(v.co) for v in o.data.vertices]
    # Group bounds describe individual actual timbers, not whole-object axes.
    members={g.index:[] for g in o.vertex_groups}
    for v in o.data.vertices:
        for g in v.groups:
            if g.weight>.5:members[g.group].append(v.co.copy())
    bounds={}
    for k,vv in members.items():
        if not vv:continue
        lo=Vector(tuple(min(v[i] for v in vv) for i in range(3)));hi=Vector(tuple(max(v[i] for v in vv) for i in range(3)))
        axis=max(range(3),key=lambda i:hi[i]-lo[i]);axes=[i for i in range(3) if i!=axis]+[axis];bounds[k]=(lo,hi,axes)
    o.data.attributes.new('ConstructionFiber','FLOAT_VECTOR','CORNER');o.data.attributes.new('ConstructionEdge','FLOAT','CORNER')
    fiber=o.data.attributes['ConstructionFiber'];edge=o.data.attributes['ConstructionEdge']
    for loop in o.data.loops:
        v=o.data.vertices[loop.vertex_index];group=next((g.group for g in v.groups if g.weight>.5 and g.group in bounds),None)
        if group is None:lo=Vector((0,0,0));hi=Vector((1,1,1));axes=[0,1,2]
        else:lo,hi,axes=bounds[group]
        fiber.data[loop.index].vector=tuple(v.co[i]-lo[i] for i in axes)
        distances=[min(abs(v.co[i]-lo[i]),abs(hi[i]-v.co[i])) for i in range(3) if hi[i]-lo[i]>.025]
        # Two near edges identify corners; a panel surface alone is not worn white.
        distances.sort();edge.data[loop.index].value=max(0,1-(distances[1] if len(distances)>1 else .03)/.018)
    # Keep all original UV layers. Bake/export uses a dedicated non-overlap layer.
    print('ATTRIBUTES',a['id'],flush=True)
    o.data.uv_layers.new(name='AuthoredWearAtlas');o.data.uv_layers.active_index=len(o.data.uv_layers)-1
    bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(68),island_margin=.007,area_weight=.6)
    bpy.ops.object.mode_set(mode='OBJECT')
    for uv in o.data.uv_layers:uv.active_render=uv.name=='AuthoredWearAtlas'
    image=bpy.data.images.new(a['id']+' construction wear atlas',width=1024,height=1024,alpha=False)
    image.generated_color=(.12,.12,.10,1)
    originals=list(o.data.materials)
    for i,m in enumerate(originals):o.data.materials[i]=material(m,image,a['id'])
    scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=1;scene.render.bake.margin=6
    bpy.ops.object.bake(type='EMIT');image.pack()
    # All distinct part finishes are now real baked pixels in one portable atlas.
    m=bpy.data.materials.new('Baked authored construction wear');m.use_nodes=True
    shader=m.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.85
    tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Linear'
    uv=m.node_tree.nodes.new('ShaderNodeUVMap');uv.uv_map='AuthoredWearAtlas';m.node_tree.links.new(uv.outputs['UV'],tex.inputs['Vector']);m.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
    o.data.materials.clear();o.data.materials.append(m)
    for p in o.data.polygons:p.material_index=0
    assert original_vertex==[tuple(v.co) for v in o.data.vertices]
    id=a['id'].replace('_refined','_authored');o.name=id
    view=(5,-7,4) if not any(k in id for k in ['pavilion','fireplace']) else (18,-22,14)
    b.export(id,[o],'campground-authored-surfaces',view=view)
    # The unchanged game reader consumes TEXCOORD_0. Retain source UVs in the
    # editable .blend, but export only the new atlas on a temporary mesh copy.
    source_mesh=o.data;o.data=source_mesh.copy()
    for name in [layer.name for layer in o.data.uv_layers if layer.name!='AuthoredWearAtlas']:
        o.data.uv_layers.remove(o.data.uv_layers[name])
    bpy.context.view_layer.objects.active=o;bpy.ops.object.select_all(action='DESELECT');o.select_set(True)
    glb=ROOT/f'assets/runtime/models/{id}.glb'
    bpy.ops.export_scene.gltf(filepath=str(glb),export_format='GLB',use_selection=True,export_yup=True,export_extras=True)
    disposable=o.data;o.data=source_mesh;bpy.data.meshes.remove(disposable)
    b.LOG[-1]['sha256']=hashlib.sha256(glb.read_bytes()).hexdigest();b.LOG[-1]['bytes']=glb.stat().st_size
    r=b.LOG[-1];r.update(previous_source=a['id'],geometry_unchanged=True,source_uvs_preserved=True,atlas_size=1024,features=['Part-aligned restrained grain','Physical lower-edge damp/dirt and upper-surface fade','Localized sparse oxidation and corner contact wear','Baked portable diffuse; original smooth/flat normals preserved'])
    records.append(r);print('AUTHORED',id,flush=True)

# Transfer only materials/atlas loops onto fitted instances: never replace their geometry.
bpy.ops.wm.open_mainfile(filepath=str(scene_source));bpy.context.preferences.filepaths.save_version=0
templates={}
for r in records:
    with bpy.data.libraries.load(str(ROOT/r['editable']),link=False) as (src,dst):dst.objects=[r['id']]
    templates[r['previous_source']]=dst.objects[0]
instances=[]
for o in list(bpy.context.scene.objects):
    source=o.get('refinement_source')
    if source not in templates:continue
    t=templates[source];assert len(o.data.loops)==len(t.data.loops),(o.name,'loop topology mismatch')
    before=[tuple(v.co) for v in o.data.vertices];o.data=o.data.copy()
    o.data.materials.clear();o.data.materials.append(t.data.materials[0])
    for p in o.data.polygons:p.material_index=0
    target=o.data.uv_layers.new(name='AuthoredWearAtlas')
    for p,q in zip(target.data,t.data.uv_layers['AuthoredWearAtlas'].data):p.uv=q.uv
    o.data.uv_layers.active_index=len(o.data.uv_layers)-1
    for uv in o.data.uv_layers:uv.active_render=uv.name=='AuthoredWearAtlas'
    assert before==[tuple(v.co) for v in o.data.vertices]
    o['authored_surface_source']=t.name;instances.append(dict(name=o.name,previous=source,current=t.name))
bpy.context.scene['production_status']='Third authored surface pass; runtime gates remain open'
target=OUT/'hollow-creek-authored.blend';bpy.ops.wm.save_as_mainfile(filepath=str(target),compress=True)
for p,h in protected.items():assert hashlib.sha256((ROOT/p).read_bytes()).hexdigest()==h,p
report=dict(status='authored surfaces; independent checks pending',assets=records,instances=instances,protected_sources=protected,scene=target.relative_to(ROOT).as_posix(),scene_sha256=hashlib.sha256(target.read_bytes()).hexdigest(),runtime_acceptance=False)
(OUT/'authored-surfaces.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print('COMPLETE',len(records),'families',len(instances),'fitted instances; geometry/source UVs unchanged',flush=True)
