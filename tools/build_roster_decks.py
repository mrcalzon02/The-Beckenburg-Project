"""Build the missing authored exterior decks, preserving previous candidates.

Background Blender only. Fifteen constructed envelopes across five sites;
all29 roster decisions are recorded separately. Not a terrain flattening pass.
"""
from pathlib import Path
import argparse,collections,io,json,math,struct,sys
import bpy,bmesh
from mathutils import Vector

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from build_roster_foundations import construct,material,prism
from build_blockouts import slab
from structural_pass import deck_holes
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild'
OUT=BASE/'deck-fit-pass'
CAP=BASE/'captures/deck-fit-batch'


def clip(poly,a,b,inside):
    out=[]
    def side(p):return (b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0])
    for p,q in zip(poly,poly[1:]+poly[:1]):
        u,v=side(p),side(q);pu=u>=-1e-9 if inside else u<=1e-9;qu=v>=-1e-9 if inside else v<=1e-9
        if pu:out.append(p)
        if pu!=qu and abs(u-v)>1e-12:
            t=u/(u-v);out.append((p[0]+t*(q[0]-p[0]),p[1]+t*(q[1]-p[1])))
    clean=[]
    for point in out:
        if not clean or math.dist(point,clean[-1])>.000001:clean.append(point)
    if len(clean)>1 and math.dist(clean[0],clean[-1])<.000001:clean.pop()
    return clean if len(clean)>=3 else []


def area(poly):return abs(sum(p[0]*q[1]-q[0]*p[1] for p,q in zip(poly,poly[1:]+poly[:1])))/2


def difference(poly,cutter):
    signed=sum(p[0]*q[1]-q[0]*p[1] for p,q in zip(cutter,cutter[1:]+cutter[:1]))
    if signed<0:cutter=list(reversed(cutter))
    result=[];remaining=poly
    for a,b in zip(cutter,cutter[1:]+cutter[:1]):
        if not remaining:break
        outside=clip(remaining,a,b,False)
        if outside and area(outside)>1e-10:result.append(outside)
        remaining=clip(remaining,a,b,True)
    return result


def build(key,policy,layout):
    source=BASE/'terrain-fit-pass'/key.lower()/'supported-terrain.glb'
    physics=source.with_suffix('.hcs2');data=json.loads(layout.read_text())
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(source))
    originals=list(o for o in bpy.context.scene.objects if o.type=='MESH')
    terrain=[o for o in originals if o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name]
    zones={z['id']:z for z in data['zones']}
    built=[];proof=[];cut_records=[];residues=[]
    mat=material('Rough exterior deck / timber' if policy['material']=='timber' else 'Rough exterior deck / stone',(.31,.25,.17) if policy['material']=='timber' else (.38,.37,.34))
    for zone_id in policy['zones']:
        zone=zones[zone_id];assert zone['kind']=='outside' and not zone.get('shape')
        elevation=zone['y'];holes=deck_holes(elevation,data)
        a,b,d,e=zone['bounds'];polygons=[[(a,b),(d,b),(d,e),(a,e)]]
        for x0,z0,x1,z1 in holes:
            polygons=[q for poly in polygons for q in difference(poly,[(x0,z0),(x1,z0),(x1,z1),(x0,z1)])]
        cutters=[]
        for obj in originals+built:
            role=obj.get('role','')
            if not (role=='floor' or role.startswith('rough route support')):continue
            obj.data.calc_loop_triangles();triangles=[]
            for tri in obj.data.loop_triangles:
                ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
                normal=(ps[1]-ps[0]).cross(ps[2]-ps[0])
                if normal.z<=0 or any(abs(v.z-elevation)>.0001 for v in ps):continue
                poly=[(v.x,-v.y) for v in ps]
                if max(v[0] for v in poly)<=a or min(v[0] for v in poly)>=d or max(v[1] for v in poly)<=b or min(v[1] for v in poly)>=e:continue
                triangles.append(poly)
            if triangles:cutters.append(obj.name)
            for triangle in triangles:polygons=[q for poly in polygons for q in difference(poly,triangle)]
        for poly in polygons:
            # Remove projected collinear seam vertices before prism fan
            # triangulation. Otherwise float export can emit zero-area faces.
            changed=True
            while changed and len(poly)>3:
                changed=False
                for i,p in enumerate(poly):
                    a=poly[i-1];b=poly[(i+1)%len(poly)];length=math.dist(a,b)
                    if length and abs((p[0]-a[0])*(b[1]-a[1])-(p[1]-a[1])*(b[0]-a[0]))/length<.00001:
                        poly=poly[:i]+poly[i+1:];changed=True;break
            longest=max(math.dist(p,q) for p in poly for q in poly)
            if 2*area(poly)/longest<.0001:
                residues.append(dict(zone=zone_id,area_m2=area(poly),longest_span_metres=longest,reason='Sub0.1mm projected source-edge residue; no retained walking plate'));continue
            obj=prism('Floor exterior deck '+zone_id+' '+str(len(built)),poly,elevation,[elevation-.16]*len(poly),mat)
            obj['role']='floor';obj['zone']=zone_id
            built.append(obj);proof.append(dict(name=obj.name,zone=zone_id,top_y=elevation,bottom_y=elevation-.16,footprint=poly,apertures=holes))
        cut_records.append(dict(zone=zone_id,coplanar_receivers=cutters,apertures=holes))
    deck_objects=list(built)
    support_policy=json.loads((BASE/'terrain-fit-pass/support-policies.json').read_text())['sites'][key]
    support_data=dict(data,zones=[zones[z] for z in policy['zones']])
    supports,findings,support_proof=construct(support_data,terrain,support_policy,include_route_piles=False)
    # A previously bearing route edge can yield an identical support request.
    # Keep its exact original part instead of stacking duplicate solids.
    oldkeys={tuple(sorted(face_keys(records(o)).items())) for o in originals if o.name.startswith(('Bearing skirt ','Bearing pile ','Pile bearing beam '))}
    support_names={};retained=[];kept_proof=[]
    assert len(supports)==len(support_proof)
    for obj,row in zip(supports,support_proof):
        stamp=tuple(sorted(face_keys(records(obj)).items()))
        if stamp in oldkeys:
            bpy.data.objects.remove(obj,do_unlink=True);continue
        oldkeys.add(stamp);planned=row['name'];obj.name='Deck bearing '+str(len(retained));support_names[planned]=obj.name
        row['name']=obj.name;retained.append(obj);kept_proof.append(row)
    for row in kept_proof:
        if row.get('supported_by') in support_names:row['supported_by']=support_names[row['supported_by']]
    support_proof=kept_proof
    built.extend(retained)
    folder=OUT/key.lower();folder.mkdir(parents=True,exist_ok=True);CAP.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(folder/'deck-terrain.blend'),compress=True)
    bpy.ops.export_scene.gltf(filepath=str(folder/'deck-terrain.glb'),export_format='GLB',export_extras=True)
    parts=[(o.name,records(o)) for o in built]
    old=physics.read_bytes();magic,count,total=struct.unpack_from('>III',old);assert magic==0x48435332
    stream=io.BytesIO();stream.write(struct.pack('>III',magic,count+len(parts),total+sum(len(t) for n,t in parts)));stream.write(old[12:])
    for name,triangles in parts:
        raw=name.encode();stream.write(struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(triangles)))
        for xyz,flags in triangles:stream.write(struct.pack('>9fB',*xyz,flags))
    (folder/'deck-terrain.hcs2').write_bytes(stream.getvalue())
    expected={n:face_keys(t) for n,t in parts}
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'deck-terrain.glb'))
    for obj in bpy.context.scene.objects:
        if obj.name in expected:assert face_keys(records(obj))==expected.pop(obj.name),(key,obj.name,'native mismatch')
    assert not expected
    scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True
    scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Physical deck review')
    a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.55;scene.camera=cam
    captures=[]
    for name,offset in [('overview',Vector((span*.8,-span*.9,span*.8))),('deck-side',Vector((span*.8,-span*.8,span*.25)))]:
        cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();path=CAP/f'{key.lower()}-{name}.png';scene.render.filepath=str(path);bpy.ops.render.render(write_still=True);captures.append(dict(path=path.relative_to(ROOT).as_posix(),sha256=sha(path),type='native exported physical deck candidate'))
    report=dict(id=key,status='PHYSICAL_DECK_CANDIDATE_NOT_RUNTIME_ACCEPTANCE',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=physics.relative_to(ROOT).as_posix(),source_physics_sha256=sha(physics),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),zones=policy['zones'],decks=proof,coplanar_subtractions=cut_records,discarded_boundary_residues=residues,supports=support_proof,open_findings=findings,new_deck_parts=len(deck_objects),new_support_parts=len(retained),new_triangles=sum(len(t) for n,t in parts),glb_sha256=sha(folder/'deck-terrain.glb'),physics_sha256=sha(folder/'deck-terrain.hcs2'),generator_sha256=sha(Path(__file__)),captures=captures,limitations='Rough closed slabs and bearing candidates. Guards, intermediate framing, terrain conflicts, all-site boundaries, canonical van, runtime integration and gameplay/network/performance remain open. Prior source bytes, model pivots/materials and saved terrain holes must be independently verified.')
    (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
    print('PHYSICAL_DECK',key,len(deck_objects),len(retained),len(findings),flush=True)


def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:])
    policies=json.loads((OUT/'deck-policies.json').read_text())['sites']
    roster={s['id']:s for s in json.loads((BASE/'terrain-overall-pass/terrain-envelopes.json').read_text())['sites']}
    for site in args.sites:build(site,policies[site],ROOT/roster[site]['layout'])
if __name__=='__main__':main()
