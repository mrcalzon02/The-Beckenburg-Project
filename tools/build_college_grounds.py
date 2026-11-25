"""Compose saved campus ground, a whole tunnel cap and real public entrances."""

from collections import defaultdict
import copy
import csv
import json
import math
from pathlib import Path
import struct
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT/'tools'))
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_circulation_construction import native_write
from build_deliberate_entrances import write
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from planar_structure import plate
from prepare_college_grounds import BASE, BASELINE, OLD_WORLD, PASS, PROGRAM, VAN, WORLD
from review_roster_macroterrain import records, sha

OUT = PASS/'college'
CAPTURES = BASE/'captures/college-ground-batch'
STEM = 'campus-grounds'
DOORS = [
    # Room, exterior face span, opening axis, boundary, centre, clear width.
    ('teaching-lobby', 'S', (20,28), 'z', 62, 24, 1.6),
    ('issue', 'S', (28,34), 'z', 20, 31, 1.5),
    ('cafe', 'W', (50,66), 'x', 42, 58, 1.8),
    ('rear-hall', 'S', (50,54), 'z', 78, 52, 1.5),
]
THRESHOLD_AREAS = [
    ('Teaching forecourt landing', (20,62,28,64)),
    ('Library issue landing', (28,20,34,22)),
    ('Cafe courtyard landing', (40,54,42,62)),
    ('Residence rear landing', (50,78,54,80)),
]


def rel(path):
    return path.relative_to(ROOT).as_posix()


def import_scene(path):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.preferences.filepaths.save_version = 0
    bpy.ops.import_scene.gltf(filepath=str(path))
    return {o.name:o for o in bpy.context.scene.objects if o.type == 'MESH'}


def terrain_tree(obj):
    coords = [obj.matrix_world @ v.co for v in obj.data.vertices]
    return BVHTree.FromPolygons(coords, [tuple(face.vertices) for face in obj.data.polygons])


def assemble_terrain(original):
    old_tree = terrain_tree(original)
    vertices, faces = [], []
    for obj_file in sorted((WORLD/'scene-package/meshes').glob('site*.obj')):
        offset = len(vertices)
        for line in obj_file.read_text(encoding='utf-8').splitlines():
            fields = line.split()
            if fields and fields[0] == 'v':
                x,y,z = map(float,fields[1:])
                vertices.append((x,-z,y))
            elif fields and fields[0] == 'f':
                faces.append(tuple(offset+int(i.split('/')[0])-1 for i in fields[1:]))
    mesh = bpy.data.meshes.new('College saved campus landscape')
    mesh.from_pydata(vertices,[],faces)
    mesh.update()
    terrain = bpy.data.objects.new('College saved campus earthworks', mesh)
    bpy.context.collection.objects.link(terrain)
    terrain['role'] = 'saved editor terrain / College campus geographic earthworks'
    terrain['saved_world'] = rel(WORLD)
    for mat in original.data.materials:
        mesh.materials.append(mat)
    gravel_index = len(mesh.materials)
    mesh.materials.append(material('College forecourt compacted gravel', (.40,.37,.32)))
    with (PROGRAM/'saved-grid.tsv').open(encoding='utf-8') as stream:
        paint = {(float(row['x']),float(row['z'])):int(row['material'])
                 for row in csv.DictReader(stream,delimiter='\t')}
    uv = mesh.uv_layers.new(name='World metre terrain UV')
    for polygon in mesh.polygons:
        x,z = polygon.center.x,-polygon.center.y
        if paint.get((round(x/2)*2, round(z/2)*2)) == 6:
            polygon.material_index = gravel_index
        else:
            old_face = old_tree.find_nearest(polygon.center)[2]
            if old_face is not None:
                polygon.material_index = original.data.polygons[old_face].material_index
        for index in polygon.loop_indices:
            v = mesh.vertices[mesh.loops[index].vertex_index].co
            uv.data[index].uv = (v.x/8,v.y/8)
    assert face_keys(records(terrain)) == face_keys(native_faces(WORLD/'terrain-physics.hcs2'))
    return terrain


def compose_native(source, target, retired, built):
    native_write(source,target,retired,built)
    parts = [p for p in split_native(target) if not p[0].startswith('terrain-')]
    terrain = (WORLD/'terrain-physics.hcs2').read_bytes()
    magic,count,total = struct.unpack_from('>III',terrain)
    target.write_bytes(struct.pack('>III',magic,len(parts)+count,sum(p[1] for p in parts)+total)
                       + b''.join(p[2] for p in parts) + terrain[12:])


def build():
    OUT.mkdir(parents=True,exist_ok=True)
    roster = json.loads(BASELINE.read_text(encoding='utf-8'))
    source = next(row for row in roster['active_sources'] if row['id']=='COLLEGE')
    layout = json.loads((ROOT/source['layout']).read_text(encoding='utf-8'))
    meta = json.loads((ROOT/source['construction']).read_text(encoding='utf-8'))
    objects = import_scene(ROOT/source['glb'])
    original = next(o for o in objects.values() if o.get('role','').startswith('saved editor terrain'))
    assert face_keys(records(original)) == face_keys(native_faces(OLD_WORLD/'terrain-physics.hcs2'))
    terrain = assemble_terrain(original)
    surface = terrain_tree(terrain)
    retired = {o.name for o in objects.values() if o.get('role','').startswith('rough route support')
               or o.get('role') == 'structural-arrival-end-bearing'}
    assert sum('support' in name for name in retired) == 11
    retired.update([original.name,'Ceiling cap cross-tunnel',
                    'COLLEGE bay retained outside L03 support 0 region0'])
    additions, portals, anchors = [], [], []
    concrete = material('College board formed tunnel crown',(.35,.39,.40))
    wall = objects['Wall cafe W'].data.materials[0]

    def slab(name,bounds,top,depth,mat,role,zone=None):
        a,b,c,d = bounds
        obj, = plate('COLLEGE '+name,[[(a,b),(c,b),(c,d),(a,d)]],top,depth,mat)
        obj['role'] = role
        if zone:
            obj['zone'] = zone
        additions.append(obj)

    # Full structural roof follows the occupied cross-tunnel bay. The wing
    # floor already covers x42..46; retain it and make one flush junction.
    slab('cross-tunnel reinforced crown',(30,38,42,42),0,.30,concrete,
         'full depth cover bearing on cross-tunnel walls','cross-tunnel')

    # The survey confirms that saved ground already reaches each entrance.
    # A second flush paving slab would create duplicate walking surfaces.
    for name,bounds in THRESHOLD_AREAS:
        a,b,c,d = bounds
        samples = []
        for fx in (.18,.50,.82):
            for fz in (.18,.50,.82):
                x,z = a+(c-a)*fx,b+(d-b)*fz
                hit = surface.ray_cast(Vector((x,-z,10)),Vector((0,0,-1)),20)[0]
                if hit is not None:
                    samples.append([x,z,hit.z])
        assert len(samples)==9 and all(abs(s[2])<.05 for s in samples), (name,samples)
        anchors.append(dict(name=name,bounds=bounds,saved_ground_controls=samples))

    def retire_face(room, face):
        fragments = [name for name in objects if name.startswith((f'Wall {room} {face}',
                     f'Flat route shell Wall {room} {face}', f'Header {room} {face}'))]
        assert fragments, (room,face)
        retired.update(fragments)

    def wall_segment(name, axis, fixed, left, right, bottom, top, room):
        bounds = (left,fixed-.12,right,fixed+.12) if axis=='z' else (fixed-.12,left,fixed+.12,right)
        slab(name,bounds,top,top-bottom,wall,'complete masonry face',room)

    template = frame_template()
    for room,face,(start,end),axis,fixed,centre,width in DOORS:
        retire_face(room,face)
        left,right = centre-.62*width,centre+.62*width
        head = 2.3*2.275/2.125
        wall_segment(room+' left pier',axis,fixed,start,left,0,3.6,room)
        wall_segment(room+' right pier',axis,fixed,right,end,0,3.6,room)
        wall_segment(room+' lintel',axis,fixed,left,right,head,3.6,room)
        portal = dict(id='COLLEGE-COURT-'+room,axis=axis,boundary=fixed,along=centre,
                      sill_y=0,clear_width=width,clear_height=2.3,frame_span=[fixed-.12,fixed+.12],
                      frame_depth=.24,rooms=[room,'green' if room!='rear-hall' else 'entry'])
        frame = template.copy()
        frame.data = template.data.copy()
        bpy.context.collection.objects.link(frame)
        frame.name = portal['id']+' indexed frame'
        for vertex in frame.data.vertices:
            x,y,h = vertex.co
            u,n,v = x*width,y*.24/.18,h*2.3/2.125
            vertex.co = (centre-u,-fixed-n,v) if axis=='z' else (fixed+n,-centre-u,v)
        frame['role'],frame['portal_id'] = 'indexed exterior doorway',portal['id']
        frame['source_asset'] = 'level_blockout_doorway'
        additions.append(frame)
        portals.append(portal)
    bpy.data.objects.remove(template,do_unlink=True)
    # The former outside slit was halfway up the old stair's flight. The
    # existing north cross landing provides the actual lower approach.
    retire_face('north-stair','E')
    wall_segment('north stair east enclosure','x',68,30,42,0,3.6,'north-stair')

    links = {l['id']:l for l in layout['links']}
    def level(points):
        return [[x,0,z] for x,z in points]

    green, entry = (38,40),(38,84)
    guides = {
        'L01': level([(28.372,96),(27,94),(29,87),(32,84),entry]),
        'L02': level([entry,(38,78),green]),
        'L03': level([green,(38,65),(24,65),(24,62),(24,58)]),
        'L17': level([green,(38,26),(31,26),(31,20),(31,18)]),
        'L24': level([green,(38,58),(42,58),(46,58)]) + list(reversed(links['L28']['points']))[1:],
        'L44': level([(52,74),(52,78),(52,83),(38,83),(38,78),green]),
        'L04': level([(24,58),(22,57),(22,53),(22,32)]),
        'L08': level([(13,23),(12,23),(12,21),(11,16)]),
        'L14': level([(24,58),(26,56),(29,56),(32,56)]),
        'L76': [[52,3.6,30],[52,3.6,33],[56,3.6,33],[58,3.6,34]],
    }
    guides['L45'] = list(reversed(links['L34']['points'])) + list(reversed(links['L33']['points']))[1:] + list(reversed(guides['L24']))[1:]
    thresholds = {'L01':[27,0,94],'L02':[38,0,78],'L03':[24,0,62],'L17':[31,0,20],
                  'L24':[42,0,58],'L44':[52,0,78],'L45':[62,0,40.5]}
    old_links = {key:copy.deepcopy(links[key]) for key in guides}
    for link_id,points in guides.items():
        links[link_id]['points'] = points
        if link_id in thresholds:
            links[link_id]['threshold'] = thresholds[link_id]
        links[link_id]['construction'] = 'Circulation guidance follows the built floor and crosses square to the doorway.'
    layout['authored_doorways'].extend(portals)
    layout['staging']['cargo_route'] = guides['L01'] + guides['L02'][1:] + guides['L03'][1:]
    layout['staging']['note'] = 'Canonical van at original placement on the saved forecourt. Native arrival view uses the actual runtime van triangles.'
    files = {rel(p):sha(p) for p in WORLD.rglob('*') if p.is_file() and not p.name.endswith('.lock')}
    layout['authoritative_saved_terrain'] = dict(world=rel(WORLD),source_world=rel(OLD_WORLD),files_sha256=files)
    layout['campus_entrances'] = dict(doorways=portals,ground_landings=anchors,removed_unfounded_stair_exit=True)

    for name in retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    layout_file = OUT/'authored-layout.json'
    write(layout_file,layout)
    blend,glb,native = [OUT/(STEM+ext) for ext in ('.blend','.glb','.hcs2')]
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action='DESELECT')
    for obj in [terrain,*additions]:
        obj.select_set(True)
    temporary = ROOT/'build/college-ground-additions.glb'
    bpy.ops.export_scene.gltf(filepath=str(temporary),export_format='GLB',export_extras=True,use_selection=True)
    append(ROOT/source['glb'],temporary,glb,remove_mesh_names=sorted(retired))
    compose_native(ROOT/source['native'],native,retired,additions)
    terrain_receipt = json.loads((WORLD/'verification.json').read_text(encoding='utf-8'))
    meta.update(status='COLLEGE_SAVED_GROUNDS_NATIVE_REVIEW_RUNTIME_OPEN',source_glb=source['glb'],
                source_glb_sha256=source['glb_sha256'],source_physics=source['native'],
                source_physics_sha256=source['native_sha256'],before_layout=source['layout'],
                before_layout_sha256=sha(ROOT/source['layout']),before_stair_components=meta['stair_components'],
                before_segment_map=copy.deepcopy(meta['source_segment_map']),layout=rel(layout_file),
                layout_sha256=sha(layout_file),saved_world=rel(WORLD),saved_world_files_sha256=files,
                removed_source_meshes=sorted(retired),new_interface_parts=[o.name for o in additions],
                new_portals=portals,terrain_mesh=terrain.name,ground_landings=anchors,terrain_receipt=terrain_receipt,
                builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)),
                glb_sha256=sha(glb),physics_sha256=sha(native),blend_sha256=sha(blend))
    write(OUT/'construction.json',meta)
    write(OUT/'quarantine.json',dict(source=source,removed=sorted(retired),superseded_guides=old_links))
    new_row = dict(source,construction=rel(OUT/'construction.json'),layout=rel(layout_file),glb=rel(glb),
                   native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))
    roster['active_sources'] = [new_row if row['id']=='COLLEGE' else row for row in roster['active_sources']]
    next(row for row in roster['sites'] if row['id']=='COLLEGE')['decision'] = meta['status']
    roster['baseline'] = rel(BASELINE)
    write(PASS/'roster.json',roster)


def place_review_van(staging):
    # Read the exact runtime faces with their material UVs. The van is kept in
    # the inspection scene only; shipping map placement is owned by VanStaging.
    faces = json.loads((VAN/'geometry.json').read_text(encoding='utf-8'))
    groups = defaultdict(list)
    for face in faces:
        groups[(face['material'],face['color'])].append(face)
    x0,y0,z0 = staging['point']
    cosine,sine = math.cos(staging['yaw']),math.sin(staging['yaw'])
    for (material_id,tint),group in groups.items():
        rgb = tuple(((tint >> shift)&255)/255 for shift in (16,8,0))
        mat = bpy.data.materials.new(f'Canonical van {material_id} {tint}')
        mat.diffuse_color = (*rgb,1)
        mat.use_nodes = True
        shader = mat.node_tree.nodes.get('Principled BSDF')
        shader.inputs['Base Color'].default_value = (*rgb,1)
        if material_id >= 0:
            tex = mat.node_tree.nodes.new('ShaderNodeTexImage')
            tex.image = bpy.data.images.load(str(VAN/f'textures/{material_id}.png'),check_existing=True)
            mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
        vertices,polygons,uvs = [],[],[]
        for face in group:
            start = len(vertices)
            for key in 'abc':
                v=face[key]
                vertices.append((x0+cosine*v['x']-sine*v['z'],-z0-sine*v['x']-cosine*v['z'],y0+v['y']))
                uvs.append((face[key+'u'],face[key+'v']))
            polygons.append(tuple(range(start,start+3)))
        mesh = bpy.data.meshes.new(mat.name)
        mesh.from_pydata(vertices,[],polygons)
        mesh.update()
        mesh.materials.append(mat)
        uv = mesh.uv_layers.new()
        for loop in mesh.loops:
            uv.data[loop.index].uv = uvs[loop.vertex_index]
        obj = bpy.data.objects.new(mat.name,mesh)
        bpy.context.collection.objects.link(obj)
    return len(faces)


def review():
    meta = json.loads((OUT/'construction.json').read_text(encoding='utf-8'))
    layout = json.loads((ROOT/meta['layout']).read_text(encoding='utf-8'))
    glb,native = OUT/(STEM+'.glb'),OUT/(STEM+'.hcs2')
    old_doc,old_buffer = read(ROOT/meta['source_glb'])
    new_doc,new_buffer = read(glb)
    assert new_buffer[:len(old_buffer)] == old_buffer
    for key in ('meshes','materials','accessors','bufferViews','images','textures'):
        prior = old_doc.get(key,[])
        assert new_doc.get(key,[])[:len(prior)] == prior
    retired = set(meta['removed_source_meshes'])
    for index,node in enumerate(old_doc['nodes']):
        expected = dict(node)
        if node.get('name') in retired:
            expected.pop('mesh',None)
        assert new_doc['nodes'][index] == expected
    retained = [p for p in split_native(ROOT/meta['source_physics']) if not p[0].startswith('terrain-') and p[0].split(':',1)[-1] not in retired]
    assert split_native(native)[:len(retained)] == retained
    objects = import_scene(glb)
    terrain = objects[meta['terrain_mesh']]
    assert face_keys(records(terrain)) == face_keys(native_faces(WORLD/'terrain-physics.hcs2'))
    parts = read_parts(native)
    for name in meta['new_interface_parts']:
        assert face_keys(records(objects[name])) == face_keys(parts[name][1]), name
        solid = bmesh.new()
        solid.from_mesh(objects[name].data)
        bmesh.ops.remove_doubles(solid,verts=list(solid.verts),dist=1e-6)
        assert all(e.is_manifold for e in solid.edges) and solid.calc_volume(signed=True)>0, name
        solid.free()
    vertices, faces = [], []
    for obj in objects.values():
        offset = len(vertices)
        vertices.extend(obj.matrix_world @ v.co for v in obj.data.vertices)
        faces.extend(tuple(offset+i for i in face.vertices) for face in obj.data.polygons)
    tree = BVHTree.FromPolygons(vertices,faces)

    def ray(a,b):
        a,b = Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]

    open_rays, sill_rays = 0,0
    for portal in meta['new_portals']:
        fixed, centre = portal['boundary'],portal['along']
        def point(distance,along,h):
            if portal['axis']=='z':
                return (along,-fixed-distance,h)
            return (fixed+distance,-along,h)
        for side in (-.38,0,.38):
            for height in (.20,1.10,2.15):
                assert ray(point(-.46,centre+side,height),point(.46,centre+side,height)) is None, portal['id']
                open_rays += 1
            for distance in (-.30,-.05,.05,.30):
                hit = ray(point(distance,centre+side,.15),point(distance,centre+side,-.20))
                assert hit is not None and abs(hit.z)<.00001, (portal['id'],hit)
                sill_rays += 1
    for x in (30.4,34,38,41.6):
        for z in (38.3,40,41.7):
            hit = ray((x,-z,-1),(x,-z,.1))
            assert hit is not None and abs(hit.z+.30)<1e-6, ('Tunnel crown',x,z,hit)
    van_count = place_review_van(layout['staging'])
    look = [('campus-overview',(47,-114,110),(37,-53,0),126),
            ('canonical-arrival',(22,-101,1.62),(34,-87,1.3),0),
            ('teaching-entrance',(25,-67,1.62),(24,-58,1.45),0),
            ('cafe-entrance',(37.8,-60.5,1.62),(44,-58,1.5),0),
            ('library-court',(33,-29,1.62),(31,-18,1.5),0),
            ('cross-tunnel-crown',(32,-40,-1.38),(39,-40,-1.1),0)]
    CAPTURES.mkdir(parents=True,exist_ok=True)
    sc = bpy.context.scene
    sc.render.engine = 'BLENDER_WORKBENCH'
    sc.display.shading.color_type = 'MATERIAL'
    sc.display.shading.show_cavity = True
    sc.render.resolution_x,sc.render.resolution_y,sc.render.resolution_percentage = 1400,1050,100
    bpy.ops.object.camera_add()
    cam = bpy.context.object
    sc.camera = cam
    captured = []
    for name,pos,target,scale in look:
        cam.data.type = 'ORTHO' if scale else 'PERSP'
        cam.data.ortho_scale = scale or 1
        cam.data.lens = 26
        cam.data.clip_start = .02
        cam.location = pos
        cam.rotation_euler = (Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler()
        path = CAPTURES/(name+'.png')
        sc.render.filepath = str(path)
        bpy.ops.render.render(write_still=True)
        captured.append(dict(path=rel(path),sha256=sha(path),glb_sha256=sha(glb),hidden=[]))
    write(OUT/'export-verification.json',dict(glb_sha256=sha(glb),native_sha256=sha(native),retained_native_records=len(retained),
          added_parts=len(meta['new_interface_parts']),terrain_matches_saved_editor_mesh=True,
          clear_door_profile_rays=open_rays,level_sill_control_rays=sill_rays,crown_soffit_control_rays=12,
          review_van_triangle_count=van_count,review_van_source_sha256=sha(VAN/'geometry.json'),captures=captured))
    print('COLLEGE_EXPORT_REVIEW',len(retained),'retained native records',van_count,'runtime van faces')


if __name__ == '__main__':
    if '--review-only' not in sys.argv:
        build()
    if '--build-only' not in sys.argv:
        review()
