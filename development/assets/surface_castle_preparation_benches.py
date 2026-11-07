"""Generated model-specific bench atlas; physical wood grain and material-aware UVs."""
from pathlib import Path
import bpy,math,hashlib,json,importlib.util
from mathutils import Vector
OUT=Path(__file__).with_name('castle-training');PRE=OUT/'previews/generated-bench-materials';PRE.mkdir(parents=True,exist_ok=True)
def shape(mesh):
 mesh.calc_loop_triangles()
 tris=sorted(tuple(sorted(tuple(round(float(x),6) for x in mesh.vertices[i].co) for i in t.vertices)) for t in mesh.loop_triangles)
 return hashlib.sha256(json.dumps(tris).encode()).hexdigest()
for suffix,master in [('', 'preparation-bench-imagegen-master.png'),('_worn','preparation-bench-worn-imagegen-master.png')]:
 image_path=OUT/'textures'/master
 if not image_path.exists():continue
 name='level_castle_preparation_bench'+suffix;path=OUT/(name+'.glb')
 # Always start with the supported, original-material study. A second execution
 # must not reinterpret the already-unified atlas as eight source materials.
 source=OUT/'previews/rejected-generic-bench-materials'/(name+'.glb')
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');mesh=obj.data;before=shape(mesh)
 old=[m.name for m in mesh.materials]
 runtime=OUT/'textures'/('preparation-bench'+suffix+'-runtime.png')
 image=bpy.data.images.load(str(image_path));image.scale(512,512);image.filepath_raw=str(runtime);image.file_format='PNG';image.save();image.pack()
 material=bpy.data.materials.new('Generated bench timber / worn cast steel / bare steel / grips / cartons');material.use_nodes=True
 node=material.node_tree.nodes.new('ShaderNodeTexImage');node.image=image;node.interpolation='Closest';shader=material.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.82;material.node_tree.links.new(node.outputs['Color'],shader.inputs['Base Color'])
 regions={'wood':(.006,.506,.988,.488),'end':(.506,.256,.238,.238),'paint':(.006,.006,.238,.488),'steel':(.256,.006,.238,.488),'grip':(.506,.006,.238,.238),'carton':(.756,.006,.238,.488)}
 assignments=[]
 for poly in mesh.polygons:
  source_material=old[poly.material_index].lower();normal=poly.normal;dominant=max(range(3),key=lambda i:abs(normal[i]));coords=[mesh.vertices[mesh.loops[l].vertex_index].co for l in poly.loop_indices]
  axes={0:(1,2),1:(0,2),2:(0,1)}[dominant];centre=[(min(co[i] for co in coords)+max(co[i] for co in coords))/2 for i in range(3)]
  spans=[max(co[i] for co in coords)-min(co[i] for co in coords) for i in axes]
  wood='pine' in source_material or 'oak' in source_material
  region='end' if wood and dominant==0 else 'wood' if wood else 'carton' if 'paper' in source_material else 'grip' if 'red' in source_material else 'paint' if 'iron' in source_material or 'blue' in source_material else 'steel'
  assignments.append(region)
  for loop in poly.loop_indices:
   co=mesh.vertices[mesh.loops[loop].vertex_index].co
   if region=='wood':
    u=.5+co.x/2.;v=.5+co.y if dominant==2 else .03+co.z
   elif region=='end':u=.5+co.y;v=.5+(co.z-centre[2])
   else:
    u=.5+(co[axes[0]]-centre[axes[0]])/max(.5,spans[0]);v=.5+(co[axes[1]]-centre[axes[1]])/max(1.,spans[1])
   assert -.0001<=u<=1.0001 and -.0001<=v<=1.0001,(region,u,v)
   a,b,w,h=regions[region];mesh.uv_layers.active.data[loop].uv=(a+w*u,b+h*v)
  poly.material_index=0
 mesh.materials.clear();mesh.materials.append(material)
 assert shape(mesh)==before,'Surface pass changed geometry'
 bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(name+'.blend')));bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',export_yup=True)
 bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.ops.import_scene.gltf(filepath=str(path));obj=next(o for o in bpy.context.scene.objects if o.type=='MESH');assert shape(obj.data)==before
 spec=importlib.util.spec_from_file_location('contact',Path(__file__).with_name('castle_armour_lowpoly.py'));contact=importlib.util.module_from_spec(spec);spec.loader.exec_module(contact)
 import bmesh
 data=obj.data.copy();bm=bmesh.new();bm.from_mesh(data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(data);bm.free();data.calc_loop_triangles()
 phase=name+'-generated-export';r=contact.audit_contacts(data,phase,hashlib.sha256(path.read_bytes()).hexdigest());assert r['status']=='PASS';temp=OUT.parent/'castle-museum'/('armour-contact-audit-'+phase+'.json');temp.unlink()
 r.update(status='EXPORTED_GEOMETRY_SUPPORT_VERIFIED_VISUAL_NATIVE_REVIEW_PENDING',geometry_sha256=before,atlas_sha256=hashlib.sha256(runtime.read_bytes()).hexdigest(),surface_plan=regions,criterion='Generated model-specific material/UV replacement; original repaired688tri shape unchanged; actual export contact retained')
 (OUT/(name+'-generated-review.json')).write_text(json.dumps(r,indent=2)+'\n',encoding='utf-8')
 scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=16;scene.render.resolution_x=1100;scene.render.resolution_y=800;scene.render.resolution_percentage=100
 scene.world=bpy.data.worlds.new('Neutral bench material review');scene.world.color=(.2,.2,.2)
 for pos,power in [((2,-3,4),450),((-2,-1,3),200)]:
  bpy.ops.object.light_add(type='AREA',location=pos);bpy.context.object.data.energy=power;bpy.context.object.data.size=3
 bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=2.35;scene.camera=cam
 for label,pos in [('front',(0,-4,1.8)),('quarter',(2,-3,2.3)),('rear',(0,4,1.8)),('side',(4,-.3,1.7)),('top',(0,-.01,5))]:
  cam.location=pos;cam.rotation_euler=(Vector((0,0,.65))-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(PRE/(name+'-'+label+'.png'));bpy.ops.render.render(write_still=True)
 print('PASS',name,'688tri unchanged repaired geometry, generated512px atlas, component material/wood grain UVs, actual exported support; five rendered views await review')
