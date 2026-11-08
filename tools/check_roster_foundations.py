"""Independent terrain/underside contact, closed-solid and HCS2 parity check."""
from pathlib import Path
import collections,hashlib,json,struct,sys
import bpy
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from export_roster_collision import native_faces,face_keys
from review_roster_macroterrain import records
BASE=ROOT/'development/levels/rebuild/terrain-fit-pass'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def tree(objects):
    points=[];faces=[];owners=[]
    for obj in objects:
        offset=len(points);points.extend(obj.matrix_world@v.co for v in obj.data.vertices)
        for face in obj.data.polygons:faces.append(tuple(offset+i for i in face.vertices));owners.append(obj.name)
    return BVHTree.FromPolygons(points,faces),owners
def main():
    result=[];rejected_found=0;receiver_ties=[]
    for key in json.loads((BASE/'support-policies.json').read_text())['sites']:
        folder=BASE/key.lower();report=json.loads((folder/'construction.json').read_text());source=ROOT/report['source_glb'];candidate=folder/'supported-terrain.glb';old=ROOT/report['source_physics'];native=folder/'supported-terrain.hcs2'
        assert sha(source)==report['source_glb_sha256'] and sha(candidate)==report['glb_sha256'] and sha(old)==report['source_physics_sha256'] and sha(native)==report['physics_sha256']
        raw=native.read_bytes();oldraw=old.read_bytes();assert raw[12:len(oldraw)]==oldraw[12:]
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
        terrain=[o for o in bpy.context.scene.objects if o.type=='MESH' and (o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name)]
        load_objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH' and o not in terrain and 'water' not in o.name.lower()}
        soil,_=tree(terrain);loads,owners=tree(list(load_objects.values()));receiver_trees={};oldterrain=face_keys([r for o in terrain for r in records(o)])
        for part in report['construction']:
            if part['kind']=='connected-bearing-beam':continue
            for point,expected in zip(part['terrain_probe_points'],part['ground_y']):
                x,z=point;hit=soil.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)))[0];assert hit is not None and abs(hit.z-expected)<1e-4,(key,part['name'],'ground mismatch')
            assert max(part['bottom_y'])<max(part['ground_y'])-.3,(key,part['name'],'not embedded')
            point=part['bearing_point']
            if part['kind']=='skirt':assert part['supports'].startswith('Floor '),'Masonry skirt must bear structural floor, not a crossing route strip'
            x,z=point;hit,normal,index,_=loads.ray_cast(Vector((x,-z,part['top_y']-.04)),Vector((0,0,1)),.08)
            assert hit is not None and normal.z<-.5 and abs(hit.z-part['top_y'])<1e-4,(key,part['name'],'no actual upper bearing')
            # Overlapping old route strips can be coplanar. Prove contact
            # with the declared mesh itself rather than trusting BVH tie order.
            receiver=part['supports'];assert receiver in load_objects
            if receiver not in receiver_trees:receiver_trees[receiver]=tree([load_objects[receiver]])[0]
            declared,declared_normal,_,_=receiver_trees[receiver].ray_cast(Vector((x,-z,part['top_y']-.04)),Vector((0,0,1)),.08)
            assert declared is not None and declared_normal.z<-.5 and abs(declared.z-part['top_y'])<1e-4,(key,part['name'],'declared mesh has no bearing contact')
            if owners[index]!=receiver:receiver_ties.append(dict(site=key,part=part['name'],declared=receiver,coplanar=owners[index],actual_y=hit.z))
            if part['kind']=='pile':
                poly=part['footprint'];direct=min(p[0] for p in poly)<=x<=max(p[0] for p in poly) and min(p[1] for p in poly)<=z<=max(p[1] for p in poly)
                if not direct:
                    beams=[b for b in report['construction'] if b.get('supported_by')==part['name']];assert beams
                    for beam in beams:
                        assert beam['top_y']==part['top_y'] and min(beam['bottom_y'])<part['top_y']
                        p=beam['footprint'];assert min(v[0] for v in p)<=x<=max(v[0] for v in p) and min(v[1] for v in p)<=z<=max(v[1] for v in p)
        if key=='WLD-11':
            rejected=json.loads((BASE/'rejected/marsh-reservation-posts.json').read_text())
            for part in rejected['construction']:
                if part['kind']!='pile':continue
                x=sum(p[0] for p in part['footprint'])/4;z=sum(p[1] for p in part['footprint'])/4
                hit=loads.ray_cast(Vector((x,-z,part['top_y']-.04)),Vector((0,0,1)),.08)[0]
                if hit is None:rejected_found+=1
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(candidate));meshes={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};new=[]
        for part in report['construction']:
            obj=meshes[part['name']];triangles=records(obj);edges=collections.Counter();volume=0
            for xyz,_ in triangles:
                vs=[Vector(xyz[i:i+3]) for i in [0,3,6]];volume+=vs[0].dot(vs[1].cross(vs[2]))/6
                keys=[tuple(round(c,5) for c in p) for p in vs]
                for a,b in zip(keys,keys[1:]+keys[:1]):edges[tuple(sorted((a,b)))]+=1
            assert all(n==2 for n in edges.values()) and volume>1e-7,(key,part['name'],'not closed/outward')
            new.extend(triangles)
        assert face_keys(native_faces(native))==face_keys(native_faces(old)+new),(key,'native union mismatch')
        currentterrain=[o for o in meshes.values() if o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name]
        assert face_keys([r for o in currentterrain for r in records(o)])==oldterrain,(key,'terrain export changed')
        result.append(dict(id=key,closed_new_parts=report['new_parts'],triangles=len(new),actual_lower_upper_contact=True,old_collision_bytes_preserved=True,terrain_preserved=True))
        print('BEARING_CONTACT_PARITY',key,len(new),flush=True)
    assert rejected_found>0,'Rejected floating-post negative did not reproduce'
    output=dict(status='PASS_SCOPED_CONTACT_AND_EXPORT_CHECKS',sites=result,rejected_floating_posts_detected=rejected_found,coplanar_receiver_ties=receiver_ties,checker_sha256=sha(Path(__file__)),scope='Actual native terrain rays, independently queried owning source mesh underside contact, embedded bottoms, explicit beam connections, closed outward new geometry, exact exported/native union and retained terrain. Existing coplanar route duplicates remain DEV-389. No engineering load simulation, full terrain or runtime acceptance.')
    (BASE/'verification/bearing-contact.json').write_bytes((json.dumps(output,indent=2)+'\n').encode())
if __name__=='__main__':main()
