"""Restore floors/caps outside actual stair shafts across remaining candidates.

Append closed0.16m plates. Preserve every original resource and native part;
do not flatten terrain, refill saved earth holes or move props to hide failures.
"""
from pathlib import Path
import argparse,io,json,math,struct,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from structural_pass import deck_openings
from build_roster_decks import difference as split_difference,area,clip
from planar_structure import plate
from review_roster_macroterrain import records,sha
from append_static_glb import append
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'floor-closure-pass';CAP=BASE/'captures/floor-closure-batch'


def current_source(site):
    for folder,name in [('terrain-occupancy-pass/assemblies','occupied-terrain'),('deck-fit-pass','deck-terrain'),('terrain-fit-pass','supported-terrain')]:
        p=BASE/folder/site.lower()/(name+'.glb')
        if p.exists():return p
    raise FileNotFoundError(site)


def difference(poly,cutter):
    # Reject disjoint cutters before splitting on their infinite edge lines.
    # Otherwise valid but needless pieces explode around circular stair decks.
    if max(p[0] for p in poly)<=min(p[0] for p in cutter) or min(p[0] for p in poly)>=max(p[0] for p in cutter) or max(p[1] for p in poly)<=min(p[1] for p in cutter) or min(p[1] for p in poly)>=max(p[1] for p in cutter):return [poly]
    signed=sum(p[0]*q[1]-q[0]*p[1] for p,q in zip(cutter,cutter[1:]+cutter[:1]))
    if signed<0:cutter=list(reversed(cutter))
    common=poly
    for a,b in zip(cutter,cutter[1:]+cutter[:1]):
        common=clip(common,a,b,True)
        if not common:return [poly]
    if area(common)<1e-10:return [poly]
    return split_difference(poly,cutter)


def clean_polygon(poly):
    changed=True
    while changed and len(poly)>3:
        changed=False
        for i,p in enumerate(poly):
            a,b=poly[i-1],poly[(i+1)%len(poly)];length=math.dist(a,b)
            if length and abs((p[0]-a[0])*(b[1]-a[1])-(p[1]-a[1])*(b[0]-a[0]))/length<.00001:
                poly=poly[:i]+poly[i+1:];changed=True;break
    return poly


def footprint(zone):
    a,b,c,d=zone['bounds']
    if zone.get('shape')=='circle':
        return [((a+c)/2+zone['radius']*math.cos(2*math.pi*i/64),(b+d)/2+zone['radius']*math.sin(2*math.pi*i/64)) for i in range(64)]
    assert not zone.get('shape'),('Unsupported authored shape',zone['id'])
    return [(a,b),(c,b),(c,d),(a,d)]


def build(site,layout):
    source=current_source(site);physics=source.with_suffix('.hcs2');data=json.loads(layout.read_text());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
    originals=[o for o in bpy.context.scene.objects if o.type=='MESH'];surfaces={}
    for obj in originals:
        if not(obj.get('role') in ['floor','ceiling-cap','structural-stair-landing','structural-stair-step'] or obj.get('role','').startswith('rough route support')):continue
        obj.data.calc_loop_triangles()
        for tri in obj.data.loop_triangles:
            ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices];e=sum(p.z for p in ps)/3
            if max(p.z for p in ps)-min(p.z for p in ps)>.0001 or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
            surfaces.setdefault(round(e,3),[]).append((obj.name,[(p.x,-p.y) for p in ps],obj.data.materials[0]))
    built=[];proof=[];decisions=[];noise=[]
    for zone in data['zones']:
        if zone['kind'] in ['outside','staging']:continue
        for role,e in [('floor',zone['y']),('ceiling-cap',zone['y']+zone['height'])]:
            polygons=[footprint(zone)];openings=deck_openings(e,data)
            for opening in openings:polygons=[q for poly in polygons for q in difference(poly,opening['polygon'])]
            if zone.get('shape')=='circle':
                inner=zone.get('inner_radius',0) or (1 if role=='floor' and e>0 else 0)
                if inner:
                    a,b,c,d=zone['bounds'];cut=[((a+c)/2+inner*math.cos(2*math.pi*i/64),(b+d)/2+inner*math.sin(2*math.pi*i/64)) for i in range(64)];polygons=[q for poly in polygons for q in difference(poly,cut)]
            if role=='ceiling-cap':
                for above in data['zones']:
                    if abs(above['y']-e)<.01 and (above['kind'] not in ['outside','staging'] or above.get('constructed_surface')):
                        # Retain authored upper-floor/core ownership exactly.
                        a,b,c,d=above['bounds'];polygons=[q for poly in polygons for q in difference(poly,[(a,b),(c,b),(c,d),(a,d)])]
            available=surfaces.get(round(e,3),[]);used=[]
            a,b,c,d=zone['bounds']
            for name,triangle,mat in available:
                if max(p[0] for p in triangle)<=a or min(p[0] for p in triangle)>=c or max(p[1] for p in triangle)<=b or min(p[1] for p in triangle)>=d:continue
                polygons=[q for poly in polygons for q in difference(poly,triangle)];used.append(name)
            fallback=next((o.data.materials[0] for o in originals if o.get('role')=='floor' and o.data.materials),None);assert fallback is not None
            mat=next((mat for name,t,mat in available if name.startswith(('Floor '+zone['id'],'Ceiling cap '+zone['id']))),fallback)
            added=0;restored=0;accepted=[]
            for poly in polygons:
                poly=clean_polygon(poly);surface=area(poly);span=max(math.dist(p,q) for p in poly for q in poly)
                if surface<1e-8 or 2*surface/span<.0001:
                    noise.append(dict(zone=zone['id'],role=role,area_m2=surface,reason='Sub0.1mm source-edge residual'));continue
                accepted.append(poly);restored+=surface
            if accepted:
                name=f'Restored {role} {zone["id"]} {len(built)}';merged=plate(name,accepted,e,.16,mat);added=len(merged)
                for obj in merged:
                    obj['role']=role;obj['zone']=zone['id'];built.append(obj);obj.data.calc_loop_triangles();actual_area=0
                    for tri in obj.data.loop_triangles:
                        ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices];normal=(ps[1]-ps[0]).cross(ps[2]-ps[0])
                        if normal.z>0 and all(abs(p.z-e)<.0001 for p in ps):
                            actual_area+=normal.z/2;surfaces.setdefault(round(e,3),[]).append((obj.name,[(p.x,-p.y) for p in ps],mat))
                    proof.append(dict(name=obj.name,zone=zone['id'],role=role,top_y=e,bottom_y=e-.16,area_m2=actual_area))
            decisions.append(dict(zone=zone['id'],role=role,elevation=e,actual_shafts=openings,retained_receivers=sorted(set(used)),new_parts=added,restored_area_m2=restored))
    folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);CAP.mkdir(parents=True,exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(folder/'closed-floors.blend'),compress=True)
    if built:
        bpy.ops.object.select_all(action='DESELECT')
        for o in built:o.select_set(True)
        addition=ROOT/'build'/('floor-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);preserved=append(source,addition,folder/'closed-floors.glb')
    else:
        (folder/'closed-floors.glb').write_bytes(source.read_bytes());preserved=dict(original_static_resources_preserved=True,no_new_geometry=True)
    parts=[(o.name,records(o)) for o in built];raw=physics.read_bytes();magic,count,total=struct.unpack_from('>III',raw);assert magic==0x48435332;stream=io.BytesIO();stream.write(struct.pack('>III',magic,count+len(parts),total+sum(len(ts) for n,ts in parts)));stream.write(raw[12:])
    for name,triangles in parts:
        encoded=name.encode();stream.write(struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(triangles)))
        for xyz,flags in triangles:stream.write(struct.pack('>9fB',*xyz,flags))
    (folder/'closed-floors.hcs2').write_bytes(stream.getvalue())
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'closed-floors.glb'));expected={n:face_keys(ts) for n,ts in parts}
    for o in bpy.context.scene.objects:
        if o.name in expected:assert face_keys(records(o))==expected.pop(o.name),(site,o.name,'export/native mismatch')
    assert not expected
    scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Actual floor and cap closure review')
    a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.55;scene.camera=cam;captures=[]
    for label,offset in [('whole-site',Vector((span*.8,-span*.9,span*.8))),('repaired-plates',Vector((span*.6,-span*.7,span*.9)))]:
        if label=='repaired-plates':
            for o in bpy.context.scene.objects:
                if o.type=='MESH' and o.name not in {n for n,ts in parts}:o.hide_render=True
        cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();p=CAP/f'{site.lower()}-{label}.png';scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);captures.append(dict(path=p.relative_to(ROOT).as_posix(),sha256=sha(p),type='native full export overview' if label=='whole-site' else 'native isolated newly restored structural plates; context hidden for inspection'))
    report=dict(id=site,status='FLOOR_CAP_RESTORATION_CANDIDATE_RUNTIME_GATES_OPEN',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=physics.relative_to(ROOT).as_posix(),source_physics_sha256=sha(physics),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),new_parts=len(parts),new_triangles=sum(len(ts) for n,ts in parts),restored_area_m2=sum(p['area_m2'] for p in proof),plates=proof,decisions=decisions,discarded_boundary_noise=noise,retained_glb_resources=preserved,glb_sha256=sha(folder/'closed-floors.glb'),physics_sha256=sha(folder/'closed-floors.hcs2'),generator_sha256=sha(Path(__file__)),captures=captures,remaining='Independent exact export/solid/shaft/optical checks, continuous flat/stair regression, standing captures and whole-site structure/light/runtime/network/performance acceptance.')
    (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('RESTORED_FLOOR_CAPS',site,len(parts),report['restored_area_m2'],flush=True)


def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:]);roster={r['id']:r for r in json.loads((BASE/'terrain-overall-pass/terrain-envelopes.json').read_text())['sites']}
    for site in args.sites:build(site,ROOT/roster[site]['layout'])
if __name__=='__main__':main()
