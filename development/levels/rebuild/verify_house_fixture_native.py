"""Reopen the actual fitted candidate and compare every runtime triangle and UV."""
import bpy,collections,json,hashlib,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[3]
landscape='--landscape' in sys.argv
envelope='--envelope' in sys.argv
services='--services' in sys.argv
story='--story' in sys.argv
fitout='--fitout' in sys.argv
functions='--functions' in sys.argv
variant='services' if services else 'story' if story else 'fitout' if fitout else 'functions' if functions else 'envelope' if envelope else 'landscape' if landscape else 'fit'
source=ROOT/('build/level-verification/scenes/briar-rebuild-'+variant)
native=ROOT/('development/levels/blender/briar-rebuild-'+variant+'-construction.blend')
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before=digest(native)
bpy.ops.wm.open_mainfile(filepath=str(native))
scene=bpy.context.scene
definition=json.loads((source/'definition.json').read_text(encoding='utf-8'))
faces=json.loads((source/'geometry.json').read_text(encoding='utf-8'))
assert scene['revision']==definition['revision']
assert ('HouseRebuildServices.java' if services else 'HouseRebuildStory.java' if story else 'HouseRebuildFitout.java' if fitout else 'HouseRebuildFunctions.java' if functions else 'HouseRebuildEnvelope.java' if envelope else 'HouseRebuildLandscape.java' if landscape else 'HouseRebuildFittings.java') in scene['source_of_truth']
assert 'candidate' in scene['production_status']
assert scene.unit_settings.system=='METRIC' and scene.unit_settings.scale_length==1
groups=collections.defaultdict(list)
for face in faces:groups[(face['room'],face['material'],face['color'])].append(face)
meshes=[o for o in scene.objects if o.type=='MESH' and o.get('source','').startswith('Exact runtime')]
assert len(meshes)==len(groups)
def close(a,b):assert len(a)==len(b) and all(abs(x-y)<1e-5 for x,y in zip(a,b)),(a,b)
count=0
for obj in meshes:
 expected=groups[(obj['zone'],obj['runtime_material'],obj['runtime_color'])]
 assert len(obj.data.polygons)==len(expected)
 uv=obj.data.uv_layers.active;assert uv is not None
 for polygon,face in zip(obj.data.polygons,expected):
  assert len(polygon.vertices)==3
  for loop,corner in zip(polygon.loop_indices,['a','b','c']):
   p=face[corner];vertex=obj.matrix_world@obj.data.vertices[obj.data.loops[loop].vertex_index].co
   close(vertex,(p['x'],-p['z'],p['y']));close(uv.data[loop].uv,(face[corner+'u'],face[corner+'v']))
  count+=1
assert count==len(faces)
images=[im for im in bpy.data.images if im.source=='FILE']
assert images and all(im.packed_file for im in images)
assert len(definition['props'])==(39 if services else 32 if story else 29 if functions or fitout else 5)
assert sum(o.name.startswith('Portal ') for o in scene.objects)==len(definition['portals'])
assert sum(o.name.startswith('Spawn ') for o in scene.objects)==15
for socket in definition['sockets']:
 close(scene.objects[socket['id']].location,(socket['x'],-socket['z'],socket['y']))
assert digest(native)==before
report=dict(level=definition['id'],revision=definition['revision'],triangles=count,packed_images=len(images),props=len(definition['props']),spawns=15,
 native_sha256=before,definition_sha256=digest(source/'definition.json'),geometry_sha256=digest(source/'geometry.json'),
 result='PASS every source vertex/UV; packed images; original sockets; native file unchanged',acceptance='construction candidate only; terrain/boundary and complete map gates open')
(ROOT/('development/levels/rebuild/house/'+('services' if services else 'story' if story else 'fitout' if fitout else 'functions' if functions else 'envelope' if envelope else 'landscape' if landscape else 'fixture-fit')+'-native-verification.json')).write_bytes((json.dumps(report,indent=2)+'\n').encode('utf-8'))
print('PASS House fitted native parity',count,'triangles',len(images),'packed images; no native resave')
if landscape or envelope or functions or fitout or story or services:
 from mathutils import Vector
 scene.world.node_tree.nodes.get('Background').inputs['Strength'].default_value=.8
 for o in scene.objects:
  if o.type=='LIGHT' and o.data.type=='SUN':o.data.energy=2
 scene.render.resolution_x=1200;scene.render.resolution_y=900
 scene.render.resolution_percentage=100;scene.cycles.samples=24
 scene.camera.location=(37,-46,34)
 scene.camera.rotation_euler=(Vector((14,-19,0))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
 scene.camera.data.lens=30
 scene.render.filepath=str(ROOT/('development/captures/house-'+('heating-water' if services else 'story' if story else 'fitout' if fitout else 'functions' if functions else 'envelope' if envelope else 'landscape')+'/native-parcel-overview.png'))
 bpy.ops.render.render(write_still=True)
 if envelope:
  scene.camera.location=(27,-27,11);scene.camera.rotation_euler=(Vector((16.8,-16,3))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
  scene.camera.data.lens=36;scene.render.filepath=str(ROOT/'development/captures/house-envelope/native-front-envelope.png');bpy.ops.render.render(write_still=True)
  scene.camera.location=(31,-40,7.5);scene.camera.rotation_euler=(Vector((22,-32,1.2))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
  scene.camera.data.lens=50;scene.render.filepath=str(ROOT/'development/captures/house-envelope/native-van-texture-corrected.png');bpy.ops.render.render(write_still=True)
 assert digest(native)==before
