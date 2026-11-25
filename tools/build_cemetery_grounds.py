"""Assemble the shared cemetery grounds and the mortuary's separate receiving wing."""
import copy
import csv
import json
from pathlib import Path
import struct
import sys

import bmesh
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/"tools"))
from append_static_glb import append, read
from build_architectural_doorways import frame_template
from build_circulation_construction import native_write
from build_college_grounds import import_scene, place_review_van, terrain_tree
from build_deliberate_entrances import write
from build_roster_enclosed_roofs import mesh as roof_mesh, slab as roof_slab
from build_roster_foundations import material
from check_roster_decks import read_parts
from compose_room_terrain import split_native
from export_roster_collision import face_keys, native_faces
from planar_structure import plate
from prepare_cemetery_grounds import BASE, BASELINE, PASS, SITES, VAN
from review_roster_macroterrain import records, sha

STEM = "authored-grounds"
CAPTURES = BASE/"captures/cemetery-grounds-batch"
CABINET = "level_blockout_archive_cabinet.001"
CABINET_ROOT = "WLD-07-MORTUARY:mort-office:4:0"
CABINET_DELTA = 4.0


def rel(path):
    return path.relative_to(ROOT).as_posix()


def terrain_surface(site, original, world, program):
    old = terrain_tree(original)
    vertices, triangles = [], []
    for file in sorted((world/"scene-package/meshes").glob("site*.obj")):
        offset = len(vertices)
        for line in file.read_text(encoding="utf-8").splitlines():
            fields = line.split()
            if fields and fields[0]=="v":
                x,y,z = map(float,fields[1:])
                vertices.append((x,-z,y))
            elif fields and fields[0]=="f":
                triangles.append(tuple(offset+int(i.split("/")[0])-1 for i in fields[1:]))
    geometry = bpy.data.meshes.new(site+" saved ground")
    geometry.from_pydata(vertices,[],triangles)
    geometry.update()
    ground = bpy.data.objects.new(geometry.name,geometry)
    bpy.context.collection.objects.link(ground)
    ground["role"] = "saved editor terrain / cemetery geographic grounds"
    for mat in original.data.materials:
        geometry.materials.append(mat)
    gravel = len(geometry.materials)
    geometry.materials.append(material("Compacted burial-ground gravel",(.42,.37,.30)))
    with (program/"saved-grid.tsv").open(encoding="utf-8") as stream:
        cells = {(float(r["x"]),float(r["z"])):int(r["material"]) for r in csv.DictReader(stream,delimiter="\t")}
    uv = geometry.uv_layers.new(name="World metre terrain UV")
    for face in geometry.polygons:
        x,z = face.center.x,-face.center.y
        if cells.get((round(x/2)*2,round(z/2)*2))==6:
            face.material_index = gravel
        else:
            candidate = old.find_nearest(face.center)[2]
            if candidate is not None:
                face.material_index = original.data.polygons[candidate].material_index
        for loop in face.loop_indices:
            vertex = geometry.vertices[geometry.loops[loop].vertex_index].co
            uv.data[loop].uv = vertex.x/8,vertex.y/8
    assert face_keys(records(ground))==face_keys(native_faces(world/"terrain-physics.hcs2"))
    return ground


def translate_cabinet_node(path):
    document,binary=read(path)
    nodes=document["nodes"]
    i=next(index for index,node in enumerate(nodes) if node.get("name")==CABINET_ROOT)
    node=nodes[i]
    assert i in document["scenes"][document.get("scene",0)]["nodes"] and "matrix" not in node
    assert len(node["children"])==1 and nodes[node["children"][0]]["name"]==CABINET
    node["translation"][0]+=CABINET_DELTA
    raw=json.dumps(document,separators=(",",":"),ensure_ascii=False).encode("utf-8")
    raw+=b" "*((-len(raw))%4)
    path.write_bytes(struct.pack("<IIIII",0x46546c67,2,28+len(raw)+len(binary),len(raw),0x4e4f534a)
                     +raw+struct.pack("<II",len(binary),0x004e4942)+binary)


def moved_collision(part):
    name,triangles,raw=part
    if name.split(":",1)[-1]!=CABINET:
        return part
    shifted=bytearray(raw)
    start=2+struct.unpack_from(">H",raw)[0]+5
    for vertex in range(triangles):
        for offset in (0,12,24):
            p=start+vertex*37+offset
            struct.pack_into(">f",shifted,p,struct.unpack_from(">f",raw,p)[0]+CABINET_DELTA)
    return name,triangles,bytes(shifted)


def replace_terrain_on_export(source, target, retired, additions, world, annex):
    native_write(source,target,retired,additions)
    retained = [p for p in split_native(target) if not p[0].startswith("terrain-")]
    if annex:
        assert sum(p[0].split(":",1)[-1]==CABINET for p in retained)==1
        retained=[moved_collision(part) for part in retained]
    saved = (world/"terrain-physics.hcs2").read_bytes()
    magic,count,triangles = struct.unpack_from(">III",saved)
    target.write_bytes(struct.pack(">III",magic,len(retained)+count,sum(p[1] for p in retained)+triangles)
                       +b"".join(p[2] for p in retained)+saved[12:])


def geometry(site, objects, layout):
    annex = site.endswith("MORTUARY")
    ground = next(o for o in objects.values() if o.get("role","").startswith("saved editor terrain"))
    retired = {ground.name}
    retired.update(o.name for o in objects.values() if o.get("role","").startswith("rough route support"))
    assert len(retired)-1 == (24 if annex else 22)
    retired.update(name for name in objects if name.startswith(("Ceiling cap ossuary","Ceiling cap chamber")))
    # The actual -3 m ossuary floor already supports both ends of the flights.
    # Old 14 m "landings" were coplanar strips over that continuous floor.
    retired.update(("L14 landing1","L15 landing0"))
    built, portals = [], []
    walls = material("Burial-ground limestone masonry",(.48,.45,.37))
    crypt = material("Crypt dressed stone and cover slabs",(.38,.36,.30))
    infill = material("Vale Mortuary plaster and brick",(.52,.47,.42))
    slate = material("Chapel and ossuary pavilion slate",(.22,.26,.27))
    template = frame_template()

    def solid(name,bounds,bottom,top,mat,role,zone):
        x0,z0,x1,z1 = bounds
        assert x1>x0 and z1>z0 and top>bottom, (name,bounds)
        part, = plate(site+" "+name,[[(x0,z0),(x1,z0),(x1,z1),(x0,z1)]],top,top-bottom,mat)
        part["role"],part["zone"] = role,zone
        built.append(part)

    def remove_face(room,face):
        names = [n for n in objects if n.startswith((f"Wall {room} {face}",f"Flat route shell Wall {room} {face}",f"Header {room} {face}"))]
        assert names,(site,room,face)
        retired.update(names)

    def wall(name,axis,boundary,low,high,openings,scope,mat,replace=()):
        for room,face in replace:
            remove_face(room,face)
        def segment(label,a,b,bottom,top):
            if b-a<.0001:
                return
            bounds=(a,boundary-.12,b,boundary+.12) if axis=="z" else (boundary-.12,a,boundary+.12,b)
            solid(name+" "+label,bounds,bottom,top,mat,"complete wall and door bearing",scope)
        cursor=low
        for i,(along,width,height,rooms) in enumerate(openings):
            left,right=along-.62*width,along+.62*width
            assert cursor<=left<right<=high,(name,left,right,low,high)
            segment(f"pier {i}",cursor,left,0,3)
            segment(f"lintel {i}",left,right,height*2.275/2.125,3)
            cursor=right
            id=f"{site} {name} {i+1}"
            part=template.copy()
            part.data=template.data.copy()
            bpy.context.collection.objects.link(part)
            part.name=id+" frame"
            for vertex in part.data.vertices:
                x,y,v=vertex.co
                u,n=x*width,y*.24/.18
                h=v*height/2.125
                vertex.co=(along-u,-boundary-n,h) if axis=="z" else (boundary+n,-along-u,h)
            part["role"],part["source_asset"]="indexed built doorway","level_blockout_doorway"
            built.append(part)
            portals.append(dict(id=id,axis=axis,boundary=boundary,along=along,clear_width=width,
                                clear_height=height,sill_y=0,rooms=rooms))
        segment("return",cursor,high,0,3)

    for room,side,lo,hi,plane,center,wide,other in (
        ("chapel","S",6,24,56,15,1.8,"entry"),("chapel","N",6,24,38,15,1.6,"garden"),
        ("cottage","S",32,46,56,39,1.6,"entry"),("cottage","N",32,46,38,39,1.6,"east-graves"),
        ("west-stair","S",6,16,20,7.5,1.7,"garden"),("east-stair","S",40,50,20,48.5,1.7,"east-graves"),
    ):
        wall(room+" "+side,"z",plane,lo,hi,[(center,wide,2.2,[room,other])],room,walls,[(room,side)])
    wall("caretaker tool-room partition","x",46,38,56,[(47,1.5,2.16,["cottage","tool"])],
         "cottage",walls,[("cottage","E"),("tool","W")])

    solid("ossuary full deck",(16,12,40,20),-.30,0,crypt,"buried roof bearing on the vault walls","ossuary")
    solid("archive full cover",(16,0,28,12),-.30,0,crypt,"buried archive roof","chamber")
    for name,lo,hi,x0,x1 in (("west-stair",6,16,8.5,14),("east-stair",40,50,42,47.5)):
        # The original eight floor objects leave a float-precision seam under
        # the turning landing. One deck has a single surveyed rectangular well.
        retired.update(obj.name for obj in objects.values()
                       if obj.get("zone")==name and obj.get("role")=="floor")
        cells=[(lo,12,hi,15.1),(lo,15.1,x0,16.9),(x1,15.1,hi,16.9),(lo,16.9,hi,20)]
        deck,=plate(site+" "+name+" continuous landing",
                    [[(a,b),(c,b),(c,d),(a,d)] for a,b,c,d in cells],0,.22,crypt)
        deck["role"],deck["zone"]="continuous deck around original stair well",name
        built.append(deck)
        for side,z0,z1 in (("north",14.93,15.08),("south",16.92,17.07)):
            solid(f"{name} {side} stair enclosure",(x0,z0,x1,z1),-3,1.08,crypt,
                  "continuous masonry stairwell cheek and guard",name)
        toe=(x1,x1+.16) if name=="west-stair" else (x0-.16,x0)
        solid(name+" low end upper-well guard",(toe[0],14.93,toe[1],17.07),0,1.08,crypt,
              "upper deck return guard over full lower head clearance",name)

    for name,a,b,c,d,rise in (("chapel",6,38,24,56,4.9),("west-stair",6,12,16,20,2.2),("east-stair",40,12,50,20,2.2)):
        mid=(a+c)/2
        eave,ridge=3.22,3.22+rise
        built.extend([
            roof_slab(site+" "+name+" west slope",[(a,eave,b),(mid,ridge,b),(mid,ridge,d),(a,eave,d)],.22,slate),
            roof_slab(site+" "+name+" east slope",[(mid,ridge,b),(c,eave,b),(c,eave,d),(mid,ridge,d)],.22,slate),
        ])
        for z0,z1,side in ((b,b+.23,"north"),(d-.23,d,"south")):
            triangle=[(a,3,z0),(c,3,z0),(mid,ridge-.22,z0)]
            built.append(roof_mesh(site+" "+name+" "+side+" masonry gable",triangle+[(x,y,z1) for x,y,_ in triangle],
                         [(2,1,0),(3,4,5),(0,1,4,3),(1,2,5,4),(2,0,3,5)],walls))

    zones={zone["id"]:zone for zone in layout["zones"]}
    links={link["id"]:link for link in layout["links"]}
    old_guides={}
    def guide(id,points,width=None):
        old_guides[id]=copy.deepcopy(links[id])
        links[id]["points"]=[[x,0,z] for x,z in points]
        if width is not None:
            links[id]["width"]=width
        links[id]["note"]="Circulation guidance over saved ground and through the constructed doorway faces."
    guide("L03",[(28,66),(23,62),(15,62),(15,56),(15,47)],1.60)
    guide("L04",[(28,66),(33,62),(39,62),(39,56),(39,47)],1.45)
    guide("L05",[(15,47),(15,38),(15,29)],1.45)
    guide("L08",[(41,29),(39,34),(39,38),(39,47)],1.45)
    guide("L12",[(15,29),(11.5,24),(7.5,23),(7.5,20),(7.5,16),(8.5,16)],1.45)
    guide("L13",[(41,29),(45,24),(48.5,23),(48.5,20),(48.5,16),(47.5,16)],1.45)
    if annex:
        cabinet=objects[CABINET]
        assert cabinet.parent.name==CABINET_ROOT and len(cabinet.parent.children)==1
        cabinet.parent.location.x+=CABINET_DELTA
        bpy.context.view_layer.update()
        wall("visitor hall south entry","z",64,56,60,[(58,1.8,2.24,["mort-hall","entry"])],
             "mort-hall",infill,[("mort-hall","S")])
        wall("visitor hall shared wall","x",60,40,64,
             [(47,1.5,2.16,["mort-viewing","mort-hall"]),(59,1.5,2.16,["mort-reception","mort-hall"])],
             "mort-hall",infill,[("mort-hall","E"),("mort-reception","W"),("mort-viewing","W")])
        wall("public and preparation separation","x",72,40,58,[],"mort-prep",infill,
             [("mort-viewing","E"),("mort-prep","W"),("mort-cold","W")])
        wall("public and receiving separation","z",64,60,72,[],"mort-reception",infill,[("mort-reception","S")])
        wall("transfer west wall","x",72,58,66,
             [(60.5,1.55,2.16,["mort-reception","mort-transfer"]),(65,1.38,2.18,["mort-bay","mort-transfer"])],
             "mort-transfer",infill,[("mort-reception","E"),("mort-office","W"),("mort-bay","E")])
        # The bay's exterior side resumes after the actual two-metre overlap with the transfer hall.
        wall("receiving east exterior return","x",72,66,76,[],"mort-bay",infill)
        wall("cold storage service face","z",58,72,84,
             [(74,1.5,2.16,["mort-cold","mort-transfer"])],"mort-transfer",infill,[("mort-cold","S")])
        wall("staff office enclosure","x",76,58,66,
             [(62,1.3,2.16,["mort-office","mort-transfer"])],"mort-office",infill)
        wall("vehicle receiving door","z",76,60,72,
             [(66,2.65,2.42,["mort-bay","receiving-yard"])],"mort-bay",infill,[("mort-bay","S")])
        zones["mort-office"]["bounds"]=[76,58,84,66]
        layout["zones"].extend([
            dict(id="mort-transfer",name="Staff transfer vestibule",bounds=[72,58,76,66],y=0,height=3,
                 kind="circulation",purpose="Staff transfer from receiving to cold storage and office"),
            dict(id="receiving-yard",name="Discreet south receiving yard",bounds=[60,76,76,85],y=0,height=3,
                 kind="outside",purpose="Service vehicles and sheltered casket transfer"),
        ])
        guide("mort-16",[(28,66),(46,66),(58,66),(58,64),(58,59),(60,59),(66,59)])
        guide("mort-18",[(66,47),(66,54),(66,59),(69,60.5),(72,60.5),(74,60.5),(74,58),
                          (74,54),(78,54),(78,50),(78,45)])
        guide("mort-20",[(78,54),(74,54),(74,58),(74,62),(76,62),(80,62)],1.24)
        guide("mort-21",[(80,62),(76,62),(74,62),(74,60.5),(72,60.5),(69,60.5),(66,59)],1.24)
        guide("mort-22",[(66,59),(69,60.5),(72,60.5),(74,60.5),(74,65),(72,65),(69,65),(66,70)],1.28)
        layout["links"].append(dict(id="mort-25",a="stage",b="mort-bay",kind="level",width=1.4,
            points=[[x,0,z] for x,z in ((28,74.372),(32,74.372),(37,77),(41,80),(51,80),(61,81),(66,81),(66,76),(66,70))],
            note="Separate exterior service approach to the vehicle receiving door on saved yard grades."))
        layout["loops"][-1]=["mort-reception","mort-viewing","mort-hall","mort-reception"]
        layout["mortuary_normal_use"]={
            "public":["entry","mort-hall","mort-reception","mort-viewing"],
            "service":["receiving-yard","mort-bay","mort-transfer","mort-cold","mort-prep"],
            "consultation":["mort-reception","mort-transfer","mort-office"],
            "physical_separations":["viewing/preparation", "reception/receiving"],
            "staff_threshold":"Reception to mort-transfer remains the controlled-access portal for later door hardware.",
        }

    bpy.data.objects.remove(template,do_unlink=True)
    layout["authored_doorways"]=portals
    layout["staging"]["note"]="Original canonical van placement on the saved burial-ground roadhead."
    layout["staging"]["cargo_route"]=[[28,0,74.372],[28,0,72],[28,0,66]]
    return ground,retired,built,portals,old_guides


def build(source):
    site=source["id"]
    folder=PASS/site.lower()
    world=PASS/"worlds"/site.lower()/"terrain-world"
    program=PASS/"program"/site.lower()
    folder.mkdir(parents=True,exist_ok=True)
    layout=json.loads((ROOT/source["layout"]).read_bytes())
    meta=json.loads((ROOT/source["construction"]).read_bytes())
    assert sha(ROOT/source["glb"])==source["glb_sha256"] and sha(ROOT/source["native"])==source["native_sha256"]
    objects=import_scene(ROOT/source["glb"])
    original,retired,built,portals,old_guides=geometry(site,objects,layout)
    terrain=terrain_surface(site,original,world,program)
    for name in retired:
        bpy.data.objects.remove(objects[name],do_unlink=True)
    layout_file=folder/"authored-layout.json"
    world_files={rel(p):sha(p) for p in world.rglob("*") if p.is_file() and not p.name.endswith(".lock")}
    layout["authoritative_saved_terrain"]=dict(world=rel(world),files_sha256=world_files)
    write(layout_file,layout)
    glb,native,blend=(folder/(STEM+suffix) for suffix in (".glb",".hcs2",".blend"))
    bpy.ops.wm.save_as_mainfile(filepath=str(blend),compress=True)
    bpy.ops.object.select_all(action="DESELECT")
    for obj in [terrain,*built]:
        obj.select_set(True)
    temp=ROOT/"build"/(site.lower()+"-grounds-additions.glb")
    bpy.ops.export_scene.gltf(filepath=str(temp),export_format="GLB",export_extras=True,use_selection=True)
    append(ROOT/source["glb"],temp,glb,remove_mesh_names=sorted(retired))
    annex=site.endswith("MORTUARY")
    if annex:
        translate_cabinet_node(glb)
    replace_terrain_on_export(ROOT/source["native"],native,retired,built,world,annex)
    status="CEMETERY_AUTHORED_GROUNDS_AND_DOORWAYS_RUNTIME_OPEN"
    meta.update(status=status,source_glb=source["glb"],source_physics=source["native"],
                source_glb_sha256=source["glb_sha256"],source_physics_sha256=source["native_sha256"],
                before_layout=source["layout"],before_layout_sha256=sha(ROOT/source["layout"]),
                before_segment_map=copy.deepcopy(meta["source_segment_map"]),
                before_stair_components=copy.deepcopy(meta["stair_components"]),
                stair_components=[name for name in meta["stair_components"] if name.split(":",1)[-1] not in retired],
                layout=rel(layout_file),
                layout_sha256=sha(layout_file),glb_sha256=sha(glb),physics_sha256=sha(native),
                blend_sha256=sha(blend),saved_world=rel(world),saved_world_files_sha256=world_files,
                terrain_mesh=terrain.name,new_portals=portals,removed_source_meshes=sorted(retired),
                asset_refit=({"id":"level_blockout_archive_cabinet","placement":CABINET_ROOT,
                              "delta_x_m":CABINET_DELTA,"floor_y":0} if annex else {}),
                new_interface_parts=[o.name for o in built],builder=rel(Path(__file__)),builder_sha256=sha(Path(__file__)))
    write(folder/"construction.json",meta)
    write(folder/"quarantine.json",dict(source=source,retired=sorted(retired),superseded_guides=old_guides))
    return dict(source,construction=rel(folder/"construction.json"),layout=rel(layout_file),
                glb=rel(glb),native=rel(native),blend=rel(blend),glb_sha256=sha(glb),native_sha256=sha(native))


def review(row):
    site=row["id"]
    folder=(ROOT/row["glb"]).parent
    meta=json.loads((folder/"construction.json").read_bytes())
    layout=json.loads((ROOT/row["layout"]).read_bytes())
    original,original_buffer=read(ROOT/meta["source_glb"])
    current,current_buffer=read(ROOT/row["glb"])
    retired=set(meta["removed_source_meshes"])
    assert current_buffer[:len(original_buffer)]==original_buffer
    for key in ("meshes","materials","accessors","bufferViews","images","textures"):
        prefix=original.get(key,[])
        assert current.get(key,[])[:len(prefix)]==prefix,key
    for i,node in enumerate(original["nodes"]):
        expected=dict(node)
        if node.get("name") in retired:
            expected.pop("mesh",None)
        if meta["asset_refit"] and node.get("name")==CABINET_ROOT:
            expected["translation"]=[node["translation"][0]+CABINET_DELTA,*node["translation"][1:]]
        assert current["nodes"][i]==expected,node.get("name")
    parts_before=[p for p in split_native(ROOT/meta["source_physics"]) if not p[0].startswith("terrain-")
                  and p[0].split(":",1)[-1] not in retired]
    if meta["asset_refit"]:
        parts_before=[moved_collision(part) for part in parts_before]
    assert split_native(ROOT/row["native"])[:len(parts_before)]==parts_before
    objects=import_scene(ROOT/row["glb"])
    assert face_keys(records(objects[meta["terrain_mesh"]]))==face_keys(native_faces(ROOT/meta["saved_world"]/"terrain-physics.hcs2"))
    parts=read_parts(ROOT/row["native"])
    if meta["asset_refit"]:
        name=next(name for name in parts if name.split(":",1)[-1]==CABINET)
        assert face_keys(records(objects[CABINET]))==face_keys(parts[name][1]),name
    for name in meta["new_interface_parts"]:
        obj=objects[name]
        assert face_keys(records(obj))==face_keys(parts[name][1]),name
        solid=bmesh.new()
        solid.from_mesh(obj.data)
        bmesh.ops.remove_doubles(solid,verts=list(solid.verts),dist=.00001)
        assert all(e.is_manifold for e in solid.edges) and solid.calc_volume(signed=True)>0,name
        solid.free()
    vertices,faces=[],[]
    for obj in objects.values():
        start=len(vertices)
        vertices.extend(obj.matrix_world@vertex.co for vertex in obj.data.vertices)
        faces.extend(tuple(start+i for i in face.vertices) for face in obj.data.polygons)
    tree=BVHTree.FromPolygons(vertices,faces)
    def ray(a,b):
        a,b=Vector(a),Vector(b)
        return tree.ray_cast(a,(b-a).normalized(),(b-a).length)[0]
    ray_count,sill_count=0,0
    for door in meta["new_portals"]:
        fixed,along=door["boundary"],door["along"]
        for offset in (-.35,0,.35):
            span=along+door["clear_width"]*offset
            for y in (.3,1.14,door["clear_height"]-.06):
                a,b=((span,-fixed-.48,y),(span,-fixed+.48,y)) if door["axis"]=="z" else ((fixed-.48,-span,y),(fixed+.48,-span,y))
                assert ray(a,b) is None,(door["id"],a,b)
                ray_count+=1
            for distance in (-.28,-.05,.05,.28):
                x,z=(span,fixed+distance) if door["axis"]=="z" else (fixed+distance,span)
                hit=ray((x,-z,.16),(x,-z,-.26))
                assert hit is not None and abs(hit.z)<.041,(door["id"],x,z,hit)
                sill_count+=1
    cover_count=0
    for x,z in [(x,z) for x in (17,21,27,33,38) for z in (12.8,15,18.3)]+[(18,3),(22,6),(26,9)]:
        hit=ray((x,-z,-.9),(x,-z,.2))
        assert hit is not None and abs(hit.z+.30)<.00001,(x,z,hit)
        cover_count+=1
    roof_count=0
    for x0,z0,x1,z1 in ((6,38,24,56),(6,12,16,20),(40,12,50,20)):
        for fx in (.15,.5,.85):
            for fz in (.15,.5,.85):
                x,z=x0+(x1-x0)*fx,z0+(z1-z0)*fz
                hit=ray((x,-z,12),(x,-z,3.05))
                assert hit is not None,(site,x,z,"uncovered roof")
                roof_count+=1
    if site.endswith("MORTUARY"):
        for p,q in [((71.5,-46,1.2),(72.5,-46,1.2)),((66,-63.5,1.2),(66,-64.5,1.2))]:
            assert ray(p,q) is not None,(p,q,"superseded shortcut remains")
    van_count=place_review_van(layout["staging"])
    annex=site.endswith("MORTUARY")
    views=[("overview",(53,-100,100),(30,-43,0),112),("arrival",(22,-70,1.62),(28,-78,1.30),0),
           ("chapel",(15,-62,1.62),(15,-48,2.5),0),("west-crypt-entry",(8.4,-24,1.62),(7.5,-17,1.4),0),
           ("vault",(32,-16,-1.38),(20,-16,-1.15),0),
           ("west-crypt-cutaway",(0,-33,24),(12,-16,-.20),25)]
    if annex:
        views=[("overview",(61,-105,108),(45,-43,0),124),
               ("public-entry",(58,-69,1.62),(58,-56,1.4),0),
               ("receiving-door",(64,-82,1.62),(66,-73,1.5),0),
               ("staff-transfer",(74,-63,1.62),(74,-54,1.4),0)]
    out=CAPTURES/site.lower()
    out.mkdir(parents=True,exist_ok=True)
    scene=bpy.context.scene
    scene.render.engine="BLENDER_WORKBENCH"
    scene.display.shading.color_type="MATERIAL"
    scene.display.shading.show_cavity=True
    scene.render.resolution_x,scene.render.resolution_y=1240,900
    bpy.ops.object.camera_add()
    camera=bpy.context.object
    scene.camera=camera
    rendered=[]
    for name,pos,target,scale in views:
        camera.data.type="ORTHO" if scale else "PERSP"
        camera.data.ortho_scale=scale or 1
        camera.data.lens=25
        camera.location=pos
        camera.rotation_euler=(Vector(target)-camera.location).to_track_quat("-Z","Y").to_euler()
        hidden=[]
        if name=="west-crypt-cutaway":
            hidden=[part for part in objects if part==meta["terrain_mesh"]
                    or part.startswith(("Ceiling cap west-stair",site+" west-stair west slope",
                       site+" west-stair east slope",site+" west-stair north masonry gable",
                       site+" west-stair south masonry gable",site+" west-stair S",site+" west-stair south stair enclosure",
                       "Wall west-stair W","Wall ossuary W", "Wall ossuary S", site+" ossuary full deck"))]
            for part in hidden:
                objects[part].hide_render=True
        path=out/(name+".png")
        scene.render.filepath=str(path)
        bpy.ops.render.render(write_still=True)
        for part in hidden:
            objects[part].hide_render=False
        rendered.append(dict(path=rel(path),sha256=sha(path),source_glb_sha256=row["glb_sha256"],hidden=hidden))
    write(folder/"export-verification.json",dict(retained_native_records=len(parts_before),closed_parts=len(meta["new_interface_parts"]),
        door_profile_rays=ray_count,door_sill_rays=sill_count,cover_soffit_rays=cover_count,
        roof_cover_rays=roof_count,old_shortcuts_closed=2 if annex else 0,
        glb_sha256=row["glb_sha256"],native_sha256=row["native_sha256"],saved_editor_terrain_exact=True,
        van_triangle_count=van_count,van_source_sha256=sha(VAN/"geometry.json"),captures=rendered))
    print("CEMETERY_EXPORT_REVIEW",site,ray_count,cover_count,len(parts_before),flush=True)


def main():
    roster=json.loads(BASELINE.read_bytes())
    if "--review-only" not in sys.argv:
        rows=[build(row) if row["id"] in SITES else row for row in roster["active_sources"]]
        roster["active_sources"]=rows
        roster["baseline"]=rel(BASELINE)
        for entry in roster["sites"]:
            if entry["id"] in SITES:
                entry["decision"]="CEMETERY_AUTHORED_GROUNDS_AND_DOORWAYS_RUNTIME_OPEN"
        write(PASS/"roster.json",roster)
    else:
        rows=json.loads((PASS/"roster.json").read_bytes())["active_sources"]
    if "--build-only" not in sys.argv:
        for row in rows:
            if row["id"] in SITES:
                review(row)


if __name__=="__main__":
    main()
