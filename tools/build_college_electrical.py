"""Fit the College emergency electrical installation to the native campus exports."""
from pathlib import Path
import argparse
import json
import math
import sys

import bpy
from mathutils import Matrix, Vector

ROOT = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(ROOT / "tools"), str(ROOT / "development/levels/blockouts")]
from append_static_glb import append
from build_circulation_construction import native_write
from build_deliberate_entrances import solid
from build_roster_foundations import material
from review_roster_macroterrain import sha

BASE = ROOT / "development/levels/rebuild/college-ground-pass/college"
ASSETS = ROOT / "assets/runtime/models"

# Standing points and mounting directions belong to actual compartments.
SWITCHES = (
    ("teaching-lobby", 27, 55.15, 1, 0), ("admin", 29.2, 51.1, -1, 0),
    ("seminar", 18.6, 26.8, 1, 0), ("lab", 14.8, 16.4, 1, 0),
    ("cafe", 43.35, 56.1, -1, 0), ("lecture", 18.7, 42.6, 1, 0),
    ("issue", 32.6, 18.7, 0, 1), ("bridge-lobby", 41.8, 17.25, 0, -1),
    ("upper-library-landing", 29.4, 17.5, -1, 0), ("upper-res-hall", 51.25, 46.6, -1, 0),
    ("upper-teaching", 22.8, 32.6, 1, 0), ("maintenance", 18.2, 32.65, 0, 1),
)
# Double-height halls and vertical stair wells take accessible wall fittings.
WALL_LIGHTS = {
    "lecture": [(9, 2.7, 40.6, -1, 0, 3.5), (9, 2.7, 47.2, -1, 0, 3.5)],
    "library-stair": [(32, 2.85, 7.96, 1, 0, 2.5), (32, 2.85, 11.92, 1, 0, 2.5)],
    "north-stair": [(64, 2.85, 33.96, -1, 0, 2.5), (64, 2.85, 37.92, -1, 0, 2.5)],
    "east-cellar-stair": [(44, -.40, 69.96, -1, 0, 2.6), (44, -.40, 73.92, -1, 0, 2.6)],
    "green": [(25, 2.65, 63, 0, -1, 2.2), (38.5, 2.65, 64, 1, 0, 4.4)],
}
GROUND_COURT = (26.05, 29.25, 28.95, 31.65)
SOUTH_WALL = 33.92

def point(x, y, z):
    return Vector((x, -z, y))

def world(p):
    return [round(p.x, 6), round(p.z, 6), round(-p.y, 6)]

def bounds(objects):
    xyz = [world(o.matrix_world @ v.co) for o in objects for v in o.data.vertices]
    return [[min(v[i] for v in xyz) for i in range(3)], [max(v[i] for v in xyz) for i in range(3)]]

class Assembly:
    def __init__(self):
        self.scene = bpy.context.scene
        self.deps = bpy.context.evaluated_depsgraph_get()
        self.originals = {o.name for o in self.scene.objects}
        self.built = []
        self.assets = {}
        self.sockets = []
        self.loads = []
        self.controllers = []
        self.routes = []
        self.concrete = material("College worn electrical yard concrete", (.35, .36, .33))
        self.mount = material("College galvanized equipment brackets", (.26, .29, .30))
        self.housing = material("College electrical enamel", (.39, .41, .38))
        self.conduit = material("College surface conduit", (.18, .22, .23))
        self.marker = material("College circuit identification enamel", (.73, .55, .19))

    def contact(self, origin, direction, distance):
        hit, p, n, _, obj, _ = self.scene.ray_cast(self.deps, origin, direction, distance=distance)
        if not hit or obj.name not in self.originals:
            return None
        return p, n, obj

    def measured(self, start, along, span, tag):
        hit = self.contact(start, along, span)
        if hit is None or hit[1].dot(along) > -.98:
            raise AssertionError(f"{tag} needs a real load-bearing face, got {hit}")
        return hit

    def box(self, name, rect, low, high, mat, role, zone):
        obj = solid(name, rect, low, high, mat, role, zone)
        obj.name = "College " + name
        self.built.append(obj)
        return obj

    def place(self, key, frame, name, role, zone):
        asset = ASSETS / (key + ".glb")
        if key not in self.assets:
            before = set(self.scene.objects)
            bpy.ops.import_scene.gltf(filepath=str(asset))
            source = sorted((o for o in set(self.scene.objects) - before if o.type == "MESH"), key=lambda o: o.name)
            self.assets[key] = [
                (o.data, o.matrix_world.copy()) for o in source
            ]
            for obj in set(self.scene.objects) - before:
                bpy.data.objects.remove(obj, do_unlink=True)
        placed = []
        for i, (mesh, base) in enumerate(self.assets[key]):
            obj = bpy.data.objects.new(f"College {name} {i+1:02d}", mesh)
            self.scene.collection.objects.link(obj)
            obj.matrix_world = frame @ base
            obj["source_asset"] = key
            obj["role"] = role
            obj["zone"] = zone
            placed.append(obj)
        self.built.extend(placed)
        bpy.context.view_layer.update()
        return placed

    def effect(self, key, typ, owner, xyz, yaw, circuit):
        self.sockets.append(dict(id=key, type=typ, room=owner, position=xyz, yaw=yaw, style=circuit))

    def tube(self, name, begin, end, radius=.021, mat=None):
        a, b = point(*begin), point(*end)
        axis = b - a
        bpy.ops.mesh.primitive_cylinder_add(vertices=10, radius=radius, depth=axis.length + radius * .35, location=(a+b)/2)
        obj = bpy.context.object
        obj.name = "College " + name
        obj.rotation_mode = 'QUATERNION'
        obj.rotation_quaternion = axis.to_track_quat('Z', 'Y')
        obj.data.materials.append(mat or self.conduit)
        obj["role"] = "closed electrical service conduit"
        self.built.append(obj)
        return obj

    def raceway(self, name, points, radius=.022, visible=True):
        self.routes.append(dict(id=name, points=points, radius=radius, visible=visible))
        for i, (a, b) in enumerate(zip(points, points[1:]), 1):
            self.tube(f"{name} {i:02d}", a, b, radius)

    def fixture(self, zone, key, contact, support, frame, face_down):
        self.place("level_utility_light", frame, key, "supported electrical fixture", zone["id"])
        emitter = contact + face_down * .184
        circuit = f"EM-{zone['owner']:02d}" if zone["owner"] < 12 else "EM-YARD"
        self.effect(key, "light", zone["owner"], world(emitter), 0, "native-string")
        self.loads.append(dict(id=key, room=zone["owner"], space=zone["id"], circuit=circuit,
                               mount=world(contact), emitter=world(emitter), support=support))

    def mounted_lamp(self, zone, key, start, toward, distance, tangent):
        real, _, support = self.measured(start, toward, distance, key)
        lateral = toward.cross(tangent)
        for x in (-.565, .565):
            for y in (-.110, .110):
                query = real + tangent*x + lateral*y - toward*.18
                edge, _, _ = self.measured(query, toward, .26, key+" mounting edge")
                if abs((edge-real).dot(toward)) > .006:
                    raise AssertionError(f"{key} spans dissimilar mounting faces")
        rotation = Matrix((tangent, lateral, toward)).transposed().to_4x4()
        frame = Matrix.Translation(real - .0007*toward) @ rotation
        self.fixture(zone, key, real, support.name, frame, -toward)

    def switch(self, zone, x, z, dx, dz):
        y = zone["y"] + 1.25
        direction = point(dx, 0, dz)
        key = f"college-switch-{zone['owner']:02d}"
        origin, _, wall = self.measured(point(x, y, z), direction, 1.95, key)
        for offset in (point(-dz*.047, .046, dx*.047), point(dz*.047, -.046, -dx*.047)):
            edge, _, _ = self.measured(origin + offset - direction*.13, direction, .20, key)
            assert abs((edge-origin).dot(direction)) < .008
        face = origin - direction*.001
        yaw = math.atan2(-dx, -dz)
        self.place("light_switch_1", Matrix.Translation(face) @ Matrix.Rotation(yaw, 4, 'Z'),
                   key, "supported room circuit switch", zone["id"])
        circuit = f"EM-{zone['owner']:02d}"
        self.effect(key, "switch", zone["owner"], world(face), yaw, circuit)
        self.controllers.append(dict(id=key, circuit=circuit, space=zone["id"], room=zone["owner"],
                                     at=world(face), support=wall.name, feet=[x, zone["y"], z]))
        # Individual vertical drops hug their measured mounting face; leave door widths clear.
        route_at = face - direction*.028
        overhead = self.contact(route_at, Vector((0,0,1)), 4.2)
        if overhead is None or overhead[1].z > -.98 or overhead[0].z > zone["y"]+zone["height"]:
            # Tall halls use the nearest interior surface raceway just below the normal storey datum.
            terminal_y = zone["y"]+2.78
        else:
            terminal_y = min(overhead[0].z-.07, zone["y"]+3.2)
        start = world(route_at + Vector((0,0,.052)))
        if terminal_y > start[1]+.30:
            end = [start[0], terminal_y, start[2]]
            self.raceway(f"{circuit} switch drop", [start, end], .012)
            self.box(f"{circuit} junction", [end[0]-.047,end[2]-.047,end[0]+.047,end[2]+.047],
                     end[1]-.035,end[1]+.045,self.housing,"surface conduit junction",zone["id"])

    def install_equipment(self):
        ground = []
        x0,z0,x1,z1 = GROUND_COURT
        for x in (x0,(x0+x1)/2,x1):
            for z in (z0,(z0+z1)/2,z1):
                p, _, bearing = self.measured(point(x,1,z), Vector((0,0,-1)), 1.5, "emergency footing")
                assert bearing.get("role").startswith("saved editor terrain") and -.07<p.z<.02
                ground.append(round(p.z,6))
        for x in (14.65,15.80,16.95):
            p, _, wall = self.measured(point(x,-1.70,32.8), Vector((0,-1,0)), 1.20, "main board wall")
            assert wall.name=="Wall maintenance S" and abs(-p.y-SOUTH_WALL)<.002
            p, _, _ = self.measured(point(x,-2.2,32.8), Vector((0,0,-1)), 1, "main board floor")
            assert abs(p.z+3)<.001
        self.box("emergency supply footing", GROUND_COURT, -.25, .08, self.concrete, "founded electrical base", "green")
        self.box("main board plinth", [14.62,33.42,16.98,SOUTH_WALL], -3, -2.94, self.concrete, "floor-bearing equipment base", "maintenance")
        for low in (-2.54,-1.45):
            self.box("main board rear mounting rail", [14.66,33.90,16.94,33.94], low, low+.09,
                     self.mount, "anchored equipment rail", "maintenance")
        equipment = {}
        placements = [
            ("level_trace_generator_refined", (27.5,.08,30.36), 0, "emergency generator", "green"),
            ("level_electrical_bank_worn", (15.8,-2.94,33.76), math.pi, "maintenance distribution board", "maintenance"),
        ]
        for key, origin, yaw, name, zone in placements:
            items = self.place(key, Matrix.Translation(point(*origin)) @ Matrix.Rotation(yaw,4,'Z'), name,
                               "fixed electrical equipment",zone)
            equipment[key] = dict(sha256=sha(ASSETS/(key+".glb")), origin=origin, yaw=yaw, placed_bounds=bounds(items))
        # Backplate bears on the surveyed flat front casing at Z30.055.
        self.box("generator starter backing plate", [27.37,29.994,27.56,30.058], .522, .812,
                 self.housing, "mounted generator starter backing plate","green")
        self.effect("college-generator", "generator", 12, [27.465,.555,29.993], math.pi, "EMERGENCY-MAIN")
        self.effect("college-breaker", "breaker", 11, [15.80,-2.10,33.53], 0, "EMERGENCY-MAIN")
        # The bars extend to the three exact existing cable terminals (Z33.80).
        for tap,x in enumerate((14.9022,15.6622,16.4222),1):
            self.raceway(f"bank terminal {tap}", [(x,-.832,33.80),(x,-.387,33.80)],.017)
        self.raceway("maintenance main raceway",
                     [(14.78,-.387,33.80),(19.73,-.387,33.80),(19.73,-.387,28.00)], .033)
        for x in (14.84,16.04,17.24,18.44,19.59):
            self.box(f"maintenance rear conduit saddle {x}", [x-.033,33.765,x+.033,33.927],
                     -.421,-.353,self.mount,"wall-anchored conduit saddle","maintenance")
        for z in (29.7,31.02,32.34,33.53):
            self.box(f"maintenance east conduit saddle {z}", [19.696,z-.033,19.927,z+.033],
                     -.421,-.353,self.mount,"wall-anchored conduit saddle","maintenance")
        self.box("maintenance main isolation junction",[19.745,27.89,19.925,28.11],-.53,-.22,
                 self.housing,"wall-mounted junction over service door","maintenance")
        # Surface raceway and paired junctions meet the masonry at the
        # existing lintel. Cable through masonry is sealed inside the wall.
        self.raceway("sealed connection at maintenance lintel",
                     [(19.73,-.387,28),(20.18,-.387,28)], .026, False)
        self.box("west tunnel inlet junction",[20.075,27.89,20.25,28.11],-.52,-.23,
                 self.housing,"wall-mounted service tunnel junction","west-tunnel")
        self.raceway("west service tunnel overhead feed",
                     [(20.23,-.39,28),(23.77,-.39,28),(23.77,-.39,30.36),(23.94,-.39,30.36)], .032)
        for x in (20.45,21.5,22.55,23.60):
            self.box(f"tunnel drop clip {x}",[x-.031,27.96,x+.031,28.04],-.415,-.159,
                     self.mount,"ceiling anchored conduit saddle","west-tunnel")
        self.box("external feeder gasket junction",[24.06,30.16,24.26,30.56],.13,.485,
                 self.housing,"masonry backed weatherproof isolator","green")
        self.raceway("weatherproof exterior riser",
                     [(24.19,.24,30.36),(24.19,-.31,30.36),(24.6,-.31,30.36)],.032)
        self.raceway("buried generator duct",
                     [(24.6,-.31,30.36),(27.25,-.31,30.36),(27.25,-.31,30.80),
                      (27.25,.10,30.80),(27.25,.31,30.80),(27.25,.31,30.70)],.036,False)
        self.raceway("sealed external main cable passage",
                     [(23.94,-.39,30.36),(24.19,-.39,30.36),(24.19,-.31,30.36)],.032,False)
        return equipment, ground

def fixtures(assembly, zones):
    for zone in zones.values():
        if not zone["gameplay_eligible"] or zone["kind"]=="staging" or zone["id"]=="entry":
            continue
        name = zone["id"]
        walls = WALL_LIGHTS.get(name, [])
        for i,(x,y,z,dx,dz,maximum) in enumerate(walls,1):
            assembly.mounted_lamp(zone, f"college-load-{name}-wall-{i}", point(x,y,z),
                                  point(dx,0,dz), maximum, point(-dz,0,dx))
        if zone["kind"]=="outside" or name in ("lecture","library-stair","north-stair","east-cellar-stair"):
            continue
        a,b,c,d = zone["bounds"]
        dx,dz = c-a,d-b
        major = max(dx,dz)
        offsets = ((.25,.50,.75) if major>20 else (.33,.66) if major>9 else (.50,)) if zone["kind"]=="circulation" else ((.33,.66) if major>11 else (.50,))
        for i,f in enumerate(offsets,1):
            if walls and i==1:
                continue
            x = (a+c)/2 if dx<dz else a+dx*f
            z = (b+d)/2 if dx>=dz else b+dz*f
            # Center lines pass between architectural flights and landings;
            # the rim of a measured shaft gets a wall fixture instead.
            tangent = point(1,0,0) if dx>=dz else point(0,0,1)
            assembly.mounted_lamp(zone, f"college-load-{name}-{i}", point(x,zone["y"]+1.91,z),
                                  Vector((0,0,1)), zone["height"]+.35, tangent)

    zone = zones["maintenance"]
    assembly.mounted_lamp(zone, "college-load-maintenance-board", point(16.4,-1.09,32.4),
                          Vector((0,0,1)), 1.4, point(1,0,0))

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("output", type=Path)
    args = parser.parse_args(sys.argv[sys.argv.index("--")+1:])
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=True)
    source = BASE/"campus-grounds.glb"
    physics = BASE/"campus-grounds.hcs2"
    layout_path = BASE/"authored-layout.json"
    layout = json.loads(layout_path.read_bytes())
    zones = {z["id"]:z for z in layout["zones"]}
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.import_scene.gltf(filepath=str(source))
    bpy.context.view_layer.update()
    assembly = Assembly()
    # All support probes read the original native faces. Mount hardware
    # only after its complete contact and working approach are measured.
    for room,(name,x,z,dx,dz) in enumerate(SWITCHES):
        assert zones[name]["owner"] == room
        assembly.switch(zones[name],x,z,dx,dz)
    fixtures(assembly,zones)
    equipment, grade = assembly.install_equipment()
    bpy.ops.object.select_all(action='DESELECT')
    for obj in assembly.built:
        obj.select_set(True)
    bpy.context.view_layer.update()
    only = out/"service-equipment-only.glb"
    glb = out/"college-electrical.glb"
    hcs = out/"college-electrical.hcs2"
    bpy.ops.export_scene.gltf(filepath=str(only),export_format="GLB",use_selection=True,export_extras=True)
    append(source,only,glb)
    parts = native_write(physics,hcs,set(),assembly.built)
    bpy.ops.file.pack_all()
    blend = out/"college-electrical.blend"
    bpy.ops.wm.save_as_mainfile(filepath=str(blend))
    assert set(range(12)) <= {l["room"] for l in assembly.loads}
    result = dict(status="NATIVE_COLLEGE_ELECTRICAL_FIT", baseline_glb_sha256=sha(source),
                  baseline_physics_sha256=sha(physics), layout_sha256=sha(layout_path),
                  glb_sha256=sha(glb), physics_sha256=sha(hcs), blend_sha256=sha(blend),
                  added_parts=len(parts), added_triangles=sum(len(t) for _,t in parts),
                  original_terrain_footing_contacts=grade, equipment=equipment,
                  switches=assembly.controllers, loads=assembly.loads, cable_runs=assembly.routes,
                  sockets=assembly.sockets, sources={key:sha(ASSETS/(key+".glb"))
                           for key in sorted(set(assembly.assets) | {"button_12"})})
    (out/"electrical.json").write_text(json.dumps(result,indent=2)+"\n",encoding="utf-8",newline="\n")
    print("COLLEGE",len(assembly.loads),"mounted loads,",len(assembly.controllers),"switches,",len(parts),"shared parts")

if __name__ == "__main__":
    main()
