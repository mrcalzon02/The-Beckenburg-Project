"""Build the protected spillway gallery in the existing east crest bay."""

from pathlib import Path
import copy
import json
import math
import sys

import bmesh
import bpy
from mathutils import Matrix, Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_circulation_construction import native_write
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from planar_structure import plate
from review_roster_macroterrain import records, sha

B = ROOT / 'development/levels/rebuild'
BASELINE = B / 'service-ground-bay-pass/roster.json'
PASS = B / 'dam-spillway-gallery-pass'
OUT = PASS / 'wld-09'
CAPTURES = B / 'captures/dam-spillway-gallery-batch'
STEM = 'spillway-gallery'
SCREEN = 'level_blockout_dam_spillway_guard_screen'
PLAN = {
    'footprint': [64, 48, 72, 56],
    'foundation_bottom_y': -10.3,
    'floor_y': -6,
    'crest_y': 0,
    'crest_soffit_y': -.6,
    'clear_room_bounds': [64.4, 48, 71.1, 54.7],
    'door': {'id': 'DAM-SPILLWAY-LOWER-68', 'axis': 'z', 'boundary': 48,
             'along': 68, 'sill_y': -6, 'clear_width': 1.8, 'clear_height': 2.3,
             'frame_span': [47.76, 48], 'frame_depth': .24,
             'side': 'S', 'rooms': ['east-lower-stair', 'spillway']},
    'east_screen': {'span_z': [49, 51.62], 'sill_y': -4.9, 'head_y': -2.59},
}
RETIRED_WALLS = {'WLD-09 hall S pier0 region0', 'WLD-09 hall S sill0 region0',
                 'WLD-09 hall S header0 region0', 'WLD-09 hall S end region0'}


def rel(path):
    return path.relative_to(ROOT).as_posix()


def bounds(obj):
    points = [obj.matrix_world @ v.co for v in obj.data.vertices]
    return [min(p.x for p in points), min(-p.y for p in points),
            max(p.x for p in points), max(-p.y for p in points),
            min(p.z for p in points), max(p.z for p in points)]


def import_model(path):
    existing = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=str(path))
    return [o for o in bpy.context.scene.objects if o not in existing and o.type == 'MESH']


def validate_solid(obj):
    mesh = bmesh.new()
    mesh.from_mesh(obj.data)
    bmesh.ops.remove_doubles(mesh, verts=list(mesh.verts), dist=1e-6)
    assert all(e.is_manifold for e in mesh.edges) and mesh.calc_volume(signed=True) > 0, obj.name
    mesh.free()


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    roster = json.loads(BASELINE.read_text(encoding='utf-8'))
    source = next(row for row in roster['active_sources'] if row['id'] == 'WLD-09')
    meta = json.loads((ROOT / source['construction']).read_text(encoding='utf-8'))
    layout = json.loads((ROOT / source['layout']).read_text(encoding='utf-8'))
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.preferences.filepaths.save_version = 0
    original = {o.name: o for o in import_model(ROOT / source['glb'])}

    # The existing excavation has the full footprint under the crest. Confirm
    # head and floor space from the actual terrain mesh before constructing.
    terrain = next(o for o in original.values() if o.get('role', '').startswith('saved editor terrain'))
    saved_native = ROOT / meta['saved_world'] / 'terrain-physics.hcs2'
    assert face_keys(records(terrain)) == face_keys(native_faces(saved_native))
    ground = BVHTree.FromPolygons([terrain.matrix_world @ v.co for v in terrain.data.vertices],
                                  [tuple(f.vertices) for f in terrain.data.polygons])
    terrain_probes = 0
    for i in range(27):
        for j in range(26):
            x, z = 64.5 + i * .25, 48.25 + j * .25
            hit = ground.ray_cast(Vector((x, -z, -.61)), Vector((0, 0, -1)), 5.38)[0]
            assert hit is None, ('Existing terrain intersects gallery interior', x, z, hit)
            terrain_probes += 1
    roof = original['WLD-09 complete structural crest deck region0']
    assert bounds(roof)[:4] == [8, 48, 72, 56] and abs(bounds(roof)[4] - PLAN['crest_soffit_y']) < 1e-5
    lower_return = original['WLD-09 restored return full floor region0']
    assert abs(bounds(lower_return)[4] - PLAN['foundation_bottom_y']) < 1e-5
    concrete = material('Spillway gallery board-formed concrete', (.36, .39, .39))
    built = []

    def box(label, x0, z0, x1, z1, bottom, top, role='spillway gallery bearing wall'):
        rectangle = [(x0, z0), (x1, z0), (x1, z1), (x0, z1)]
        objects = plate('Dam spillway ' + label, [rectangle], top, top-bottom, concrete)
        assert len(objects) == 1
        obj = objects[0]
        obj['role'], obj['zone'] = role, 'spillway'
        built.append(obj)

    # A full concrete cell shares the lower civil foundation level and bears
    # the retained structural crest deck on its continuous perimeter.
    box('foundation and floor', 64, 48, 72, 56, -10.3, -6, 'solid civil foundation and finished walkable floor')
    box('west full bearing wall', 64, 48, 64.4, 56, -6, -.6)
    box('south full retaining wall', 64.4, 54.7, 71.1, 56, -6, -.6)
    box('north culvert return beyond stair hall', 70.5, 48, 71.1, 48.4, -6, -.6)
    z0, z1 = PLAN['east_screen']['span_z']
    sill, head = PLAN['east_screen']['sill_y'], PLAN['east_screen']['head_y']
    box('east north jamb', 71.1, 48, 72, z0, -6, -.6)
    box('east south jamb', 71.1, z1, 72, 56, -6, -.6)
    box('east observation parapet', 71.1, z0, 72, z1, -6, sill)
    box('east observation lintel', 71.1, z0, 72, z1, head, -.6)

    # Rebuild the full shared wall. It has two independent doors on the
    # same axis, one at the crest and one at the lower gallery floor.
    door = PLAN['door']
    left, right = [door['along'] + sign * .62 * door['clear_width'] for sign in (-1, 1)]
    box('shared hall west pier', 64, 47.76, left, 48, -6, 3, 'complete hall wall')
    box('shared hall east pier', right, 47.76, 70.5, 48, -6, 3, 'complete hall wall')
    box('shared hall bridge beam between doors', left, 47.76, right, 48,
        -6 + 2.3 * 2.275/2.125, 0, 'complete hall wall')
    box('shared hall retained crest opening lintel', left, 47.76, right, 48,
        2.3 * 2.275/2.125, 3, 'complete hall wall')
    frame = frame_template()
    frame.name = door['id'] + ' indexed frame'
    for vertex in frame.data.vertices:
        x, y, h = vertex.co
        vertex.co = (68 - x*1.8, -47.88 - y*.24/.18, -6 + h*2.3/2.125)
    frame['role'] = 'indexed architectural doorway'
    frame['portal_id'], frame['source_asset'] = door['id'], 'level_blockout_doorway'
    built.append(frame)

    # This exact indexed screen is assigned to the Dam. Its base sits on the
    # parapet and both outer uprights meet the masonry jambs.
    screen_path = B.parent / 'primitive-props-v1/models' / (SCREEN + '.glb')
    screen, = import_model(screen_path)
    screen.name = 'Dam spillway indexed protective screen'
    screen.matrix_world = (Matrix.Translation((71.75, -(z0+z1)/2, sill))
                           @ Matrix.Rotation(math.pi/2, 4, 'Z') @ screen.matrix_world)
    screen['role'], screen['source_asset'], screen['zone'] = 'fixed observation opening screen', SCREEN, 'spillway'
    built.append(screen)
    bpy.context.view_layer.update()
    screen_bounds = bounds(screen)
    assert abs(screen_bounds[1]-z0) < 1e-4 and abs(screen_bounds[3]-z1) < 1e-4, screen_bounds
    assert abs(screen_bounds[4]-sill) < 1e-4 and abs(screen_bounds[5]-head) < 1e-4, screen_bounds

    for name in RETIRED_WALLS:
        bpy.data.objects.remove(original[name], do_unlink=True)
    for obj in built:
        validate_solid(obj)

    # Guidance follows the stair already in service, its lower side aisle,
    # and the new doorway. Keep both existing flight identities unchanged.
    link = next(link for link in layout['links'] if link['id'] == 'L12')
    rejected = copy.deepcopy(link)
    link['points'] = link['points'][:8] + [[69.2,-6,46.4], [68,-6,46.4],
                                         [68,-6,48], [68,-6,50.3], [70.8,-6,50.3]]
    link['construction'] = 'Use the existing east stair and lower aisle. Pass through DAM-SPILLWAY-LOWER-68 into the protected observation gallery.'
    link['construction_status'] = 'BUILT_PROTECTED_OBSERVATION_GALLERY'
    zone = next(zone for zone in layout['zones'] if zone['id'] == 'spillway')
    zone.update(bounds=PLAN['footprint'], kind='inside', height=5.4, label_point=[70.8,-6,50.3],
                purpose='Inspect the spillway through a fixed screen from a founded gallery under the crest')
    layout['authored_doorways'].append(door)
    layout['architectural_service_bay']['spillway'] = 'Protected observation gallery reached from the east lower hall'
    layout['spillway_gallery'] = PLAN
    layout_path = OUT / 'authored-layout.json'
    write(layout_path, layout)
    blend, glb, native = [OUT / (STEM + ext) for ext in ('.blend', '.glb', '.hcs2')]
    bpy.ops.wm.save_as_mainfile(filepath=str(blend), compress=True)
    bpy.ops.object.select_all(action='DESELECT')
    for obj in built:
        obj.select_set(True)
    addition = ROOT / 'build/dam-spillway-addition.glb'
    bpy.ops.export_scene.gltf(filepath=str(addition), export_format='GLB', export_extras=True, use_selection=True)
    append(ROOT / source['glb'], addition, glb, remove_mesh_names=sorted(RETIRED_WALLS))
    native_write(ROOT / source['native'], native, RETIRED_WALLS, built)

    meta.update(status='SPILLWAY_GALLERY_NATIVE_REVIEW_RUNTIME_OPEN',
                source_glb=source['glb'], source_glb_sha256=source['glb_sha256'],
                source_physics=source['native'], source_physics_sha256=source['native_sha256'],
                before_layout=source['layout'], before_layout_sha256=sha(ROOT / source['layout']),
                before_segment_map=copy.deepcopy(meta['source_segment_map']),
                before_stair_components=meta['stair_components'],
                layout=rel(layout_path), layout_sha256=sha(layout_path),
                removed_source_meshes=sorted(RETIRED_WALLS), new_interface_parts=[o.name for o in built],
                builder=rel(Path(__file__)), builder_sha256=sha(Path(__file__)),
                spillway_gallery=PLAN, empty_saved_terrain_samples=terrain_probes,
                screen_source=rel(screen_path), screen_source_sha256=sha(screen_path),
                glb_sha256=sha(glb), physics_sha256=sha(native), blend_sha256=sha(blend))
    meta['doorways'].append(door)
    meta['architectural_plan']['spillway'] = layout['architectural_service_bay']['spillway']
    write(OUT / 'construction.json', meta)
    write(OUT / 'quarantine.json', {'original_source': source, 'rejected_link': rejected,
                                    'retired_complete_wall': sorted(RETIRED_WALLS)})
    new_row = dict(source, construction=rel(OUT / 'construction.json'), layout=rel(layout_path),
                   glb=rel(glb), native=rel(native), blend=rel(blend),
                   glb_sha256=sha(glb), native_sha256=sha(native))
    roster['active_sources'] = [new_row if row['id'] == 'WLD-09' else row for row in roster['active_sources']]
    for row in roster['sites']:
        if row['id'] == 'WLD-09':
            row['decision'] = meta['status']
    roster['baseline'] = rel(BASELINE)
    write(PASS / 'roster.json', roster)


def review():
    meta = json.loads((OUT / 'construction.json').read_text(encoding='utf-8'))
    glb, native = OUT / (STEM + '.glb'), OUT / (STEM + '.hcs2')
    source_json, source_buffer = read(ROOT / meta['source_glb'])
    export_json, export_buffer = read(glb)
    assert export_buffer[:len(source_buffer)] == source_buffer
    for key in ('meshes', 'materials', 'accessors', 'bufferViews', 'images', 'textures'):
        prior = source_json.get(key, [])
        assert export_json.get(key, [])[:len(prior)] == prior
    for index, node in enumerate(source_json['nodes']):
        expected = dict(node)
        if node.get('name') in RETIRED_WALLS:
            expected.pop('mesh', None)
        assert export_json['nodes'][index] == expected
    keep = [p for p in split_native(ROOT / meta['source_physics']) if p[0].split(':',1)[-1] not in RETIRED_WALLS]
    assert split_native(native)[:len(keep)] == keep
    assert all(sha(ROOT / path) == digest for path,digest in meta['unchanged_saved_world_files'].items())
    bpy.ops.wm.read_factory_settings(use_empty=True)
    objects = {o.name:o for o in import_model(glb)}
    native_parts = read_parts(native)
    for name in meta['new_interface_parts']:
        validate_solid(objects[name])
        assert face_keys(records(objects[name])) == face_keys(native_parts[name][1]), name

    vertices, faces = [], []
    for obj in objects.values():
        offset = len(vertices)
        vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(offset+i for i in face.vertices) for face in obj.data.polygons)
    scene_bvh = BVHTree.FromPolygons(vertices, faces)

    def obstruction(start, end):
        a, b = (Vector((x, -z, y)) for x,y,z in (start, end))
        return scene_bvh.ray_cast(a, (b-a).normalized(), (b-a).length)[0]

    # Test the actual exported joints, including the independently retained
    # upper door and roof. Use the same collision triangles checked above.
    checks = {}
    for x in (67.25, 68, 68.75):
        for y in (-5.85, -4.5, -3.82, .2, 1.6, 2.2):
            assert obstruction((x,y,47.45), (x,y,48.4)) is None, ('Jamb intrudes into opening', x, y)
        for z in (47.7, 47.95, 48.05, 48.5):
            hit = obstruction((x,-5.8,z), (x,-6.2,z))
            assert hit is not None and abs(hit.z+6) < 1e-5, ('Sill floor is discontinuous', x, z)
    checks['open_door_seam_rays'] = 18
    checks['level_sill_rays'] = 12
    for x,y in ((66.7,-4.5), (69.3,-4.5), (68,-3.3), (68,-.15), (68,2.65)):
        assert obstruction((x,y,47.6), (x,y,48.4)) is not None, ('Missing wall at doorway', x, y)
    checks['closed_pier_and_header_controls'] = 5
    for x in (64.5, 68, 71):
        for z in (48.5, 51, 54.6):
            hit = obstruction((x,-1,z), (x,.1,z))
            assert hit is not None and abs(hit.z+.6) < 1e-5, ('Roof does not meet bay', x, z)
    checks['retained_crest_soffit_rays'] = 9

    CAPTURES.mkdir(parents=True, exist_ok=True)
    scene = bpy.context.scene
    scene.render.engine = 'BLENDER_WORKBENCH'
    scene.display.shading.color_type = 'MATERIAL'
    scene.display.shading.show_cavity = True
    scene.render.resolution_x, scene.render.resolution_y, scene.render.resolution_percentage = 1400, 1050, 100
    bpy.ops.object.camera_add()
    camera = bpy.context.object
    scene.camera = camera
    cameras = [('lower-door-standing', (69,-44.6,-4.38), (68.3,-49,-4.52), 'standing'),
               ('screen-standing', (68.5,-52.5,-4.38), (72,-50.2,-4.02), 'standing'),
               ('crest-and-gallery-cutaway', (88,-71,17), (67,-48,-3.3), 'cutaway'),
               ('spillway-exterior', (83,-67,11), (67,-49,-3), 'exterior')]
    captures = []
    for name, position, target, view in cameras:
        hidden = []
        for obj in objects.values():
            low = bounds(obj)[4]
            obj.hide_render = view == 'cutaway' and (low >= -.61 or 'saved editor terrain' in obj.get('role',''))
            if obj.hide_render:
                hidden.append(obj.name)
        camera.data.type = 'PERSP' if view == 'standing' else 'ORTHO'
        camera.data.ortho_scale = 27 if view == 'exterior' else 22
        camera.data.lens = 25
        camera.data.clip_start = .025
        camera.location = position
        camera.rotation_euler = (Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler()
        path = CAPTURES / (name+'.png')
        scene.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        captures.append(dict(path=rel(path), sha256=sha(path), glb_sha256=sha(glb), hidden=hidden))
    write(OUT / 'export-verification.json', {'glb_sha256':sha(glb), 'native_sha256':sha(native),
          'new_closed_solids':len(meta['new_interface_parts']), 'retained_native_records': len(keep),
          'saved_terrain_samples':meta['empty_saved_terrain_samples'], 'joint_controls':checks, 'captures':captures})
    print('SPILLWAY_GALLERY_EXPORT_REVIEW_PASS')


if __name__ == '__main__':
    if '--review-only' not in sys.argv:
        build()
    review()
