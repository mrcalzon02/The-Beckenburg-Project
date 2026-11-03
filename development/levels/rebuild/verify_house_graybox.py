"""Reopen the actual House pilot, compare every exported triangle/UV, then inspect.

The daylight front/rear renders use the saved geometry. The labeled cutaway
clips a temporary in-memory copy; the delivery .blend is never resaved.
"""
import bpy
import bmesh
import collections
import hashlib
import json
from pathlib import Path
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / 'build/level-verification/scenes/briar-rebuild'
NATIVE = ROOT / 'development/levels/blender/briar-rebuild-graybox.blend'
OUT = ROOT / 'development/levels/rebuild'


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def close(actual, expected):
    assert len(actual) == len(expected)
    assert all(abs(a-b) < 1e-5 for a, b in zip(actual, expected)), (actual, expected)


before = digest(NATIVE)
bpy.ops.wm.open_mainfile(filepath=str(NATIVE))
scene = bpy.context.scene
definition = json.loads((SOURCE / 'definition.json').read_text(encoding='utf-8'))
faces = json.loads((SOURCE / 'geometry.json').read_text(encoding='utf-8'))
assert scene['revision'] == definition['revision']
assert scene['level_id'] == 'briar-rebuild'
assert scene.unit_settings.system == 'METRIC' and scene.unit_settings.scale_length == 1
assert 'graybox' in scene['production_status'] and 'HouseRebuild.java' in scene['source_of_truth']
groups = collections.defaultdict(list)
for face in faces:
    groups[(face['room'], face['material'], face['color'])].append(face)
meshes = [obj for obj in scene.objects if obj.type == 'MESH'
          and obj.get('source', '').startswith('Exact runtime')]
assert len(meshes) == len(groups)
triangles = 0
for obj in meshes:
    expected = groups[(obj['zone'], obj['runtime_material'], obj['runtime_color'])]
    assert len(obj.data.polygons) == len(expected)
    uv = obj.data.uv_layers.active
    assert uv is not None
    for polygon, face in zip(obj.data.polygons, expected):
        assert len(polygon.vertices) == 3
        for loop_index, corner in zip(polygon.loop_indices, ['a', 'b', 'c']):
            point = face[corner]
            vertex = obj.matrix_world @ obj.data.vertices[obj.data.loops[loop_index].vertex_index].co
            close(vertex, (point['x'], -point['z'], point['y']))
            close(uv.data[loop_index].uv, (face[corner+'u'], face[corner+'v']))
        triangles += 1
assert triangles == len(faces)
assert all(image.packed_file for image in bpy.data.images if image.source == 'FILE')
assert sum(1 for obj in scene.objects if obj.name.startswith('Portal ')) == len(definition['portals'])
assert sum(1 for obj in scene.objects if obj.name.startswith('Spawn ')) == 15
for socket in definition['sockets']:
    close(scene.objects[socket['id']].location, (socket['x'], -socket['z'], socket['y']))

# Ordinary inspection lighting, with no claim of runtime lighting parity.
world = scene.world.node_tree.nodes.get('Background')
world.inputs['Color'].default_value = (.65, .72, .82, 1)
world.inputs['Strength'].default_value = .65
for obj in scene.objects:
    if obj.type == 'LIGHT' and obj.data.type == 'SUN':
        obj.data.energy = 2.5
        obj.data.color = (1, .96, .88)
scene.view_settings.exposure = 0
scene.cycles.samples = 24
scene.render.resolution_x = 1280
scene.render.resolution_y = 800
scene.render.resolution_percentage = 100
previews = OUT / 'previews'
previews.mkdir(exist_ok=True)
captures = []


def render(name, position, target):
    camera = scene.camera
    camera.location = position
    camera.rotation_euler = (Vector(target)-camera.location).to_track_quat('-Z', 'Y').to_euler()
    camera.data.lens = 38
    destination = previews / ('briar-rebuild-blender-'+name+'.png')
    scene.render.filepath = str(destination)
    bpy.ops.render.render(write_still=True)
    captures.append({'path': destination.relative_to(ROOT).as_posix(), 'sha256': digest(destination),
                     'type': 'Native cutaway' if name.endswith('cutaway') else 'Native inspection view'})


render('front', (29, -32, 12), (17, -16, 2))
render('rear', (3, 3, 11), (17, -15, 2))
inspection_data = bpy.data.lights.new('Temporary cellar inspection light', 'AREA')
inspection_data.energy = 100
inspection_data.shape = 'DISK'
inspection_data.size = 2
inspection = bpy.data.objects.new('Temporary cellar inspection light', inspection_data)
scene.collection.objects.link(inspection)
inspection.location = (13.1, -14.1, .9)
inspection.rotation_euler = (Vector((13.1, -11, -1.5))-inspection.location).to_track_quat('-Z', 'Y').to_euler()
render('cellar-stair', (13.1, -14.25, 1.5), (13.1, -11, -1.8))
inspection.hide_render = True
for obj in meshes:
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.bisect_plane(bm, geom=list(bm.verts)+list(bm.edges)+list(bm.faces),
                          dist=.0001, plane_co=(0, 0, 1.2), plane_no=(0, 0, 1),
                          clear_outer=True, clear_inner=False)
    bm.to_mesh(obj.data)
    bm.free()
render('ground-cutaway', (30, -34, 27), (17, -15, 0))
for obj in meshes:
    bm = bmesh.new()
    bm.from_mesh(obj.data)
    bmesh.ops.bisect_plane(bm, geom=list(bm.verts)+list(bm.edges)+list(bm.faces),
                          dist=.0001, plane_co=(0, 0, -.1), plane_no=(0, 0, 1),
                          clear_outer=True, clear_inner=False)
    bm.to_mesh(obj.data)
    bm.free()
render('basement-terrain-cutaway', (30, -34, 23), (17, -15, -1.2))
assert digest(NATIVE) == before, 'Inspection changed the delivered source'
report = {'status': 'NATIVE_PARITY_PASS_NOT_MAP_ACCEPTANCE', 'level': definition['id'],
          'revision': definition['revision'], 'native_sha256': before,
          'runtime_geometry_sha256': digest(SOURCE / 'geometry.json'),
          'definition_sha256': digest(SOURCE / 'definition.json'),
          'triangles_compared_vertex_by_vertex_and_uv': triangles,
          'packed_images': sum(image.source == 'FILE' for image in bpy.data.images),
          'portal_anchors': len(definition['portals']), 'spawn_anchors': 15,
          'captures': captures, 'native_file_unchanged_after_inspection': True,
          'inspection_lighting_note': 'Cellar-stair view uses a temporary area light for geometry review; not runtime lighting. Cutaways clip only in-memory inspection geometry.',
          'remaining': ['terrain and boundaries', 'structural refinement', 'facilities and furniture',
                        'story and wear', 'complete mission/LAN/GPU and human acceptance']}
(OUT / 'house/native-graybox-verification.json').write_bytes(
    (json.dumps(report, indent=2)+'\n').encode('utf-8'))
print('PASS_HOUSE_GRAYBOX_NATIVE_PARITY', triangles, 'actual triangles plus original UVs; editable source unchanged')
