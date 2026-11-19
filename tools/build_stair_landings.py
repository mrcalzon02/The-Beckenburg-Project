"""Authored stair-end platforms and full-width corner lead-ins.

Preserve route coordinates and unrelated assets. New geometry is visible and
uses the same native coordinates; no movement tolerance or blocker bypass.
"""
from pathlib import Path
import argparse,collections,csv,io,json,math,struct,sys,copy
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from build_roster_floor_closures import difference,clean_polygon
from build_roster_decks import area
from build_roster_foundations import prism
from planar_structure import plate
from structural_pass import stair_flights
from build_blockouts import box
from append_static_glb import append
from compose_room_terrain import split_native
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-landing-pass';CAP=BASE/'captures/stair-landing-fit-batch'


def rect(p,q,half):
    run=math.hypot(q[0]-p[0],q[2]-p[2]);nx=-(q[2]-p[2])/run*half;nz=(q[0]-p[0])/run*half
    return [(p[0]+nx,p[2]+nz),(q[0]+nx,q[2]+nz),(q[0]-nx,q[2]-nz),(p[0]-nx,p[2]-nz)]

def convex_top(points):
    points=sorted(set(points))
    def cross(a,b,c):return (b[0]-a[0])*(c[1]-a[1])-(b[1]-a[1])*(c[0]-a[0])
    def half(seq):
        result=[]
        for p in seq:
            while len(result)>1 and cross(result[-2],result[-1],p)<=0:result.pop()
            result.append(p)
        return result
    return half(points)[:-1]+half(list(reversed(points)))[:-1]


def build(site,layout,failed):
    source=BASE/'floor-closure-pass'/site.lower()/'closed-floors.glb';old=source.with_suffix('.hcs2');data=json.loads(layout.read_text());source_layout=layout;source_data=copy.deepcopy(data);links={l['id']:l for l in source_data['links']};bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
    original=[o for o in bpy.context.scene.objects if o.type=='MESH'];native_parts=split_native(old);native_by_mesh={name.split(':',1)[-1]:name for name,n,raw in native_parts};removed=[];built=[];proof=[];trims=[];decisions=[]
    requests=[];revisions={};findings=[];replacements=[];bridges=[]
    for link_id,index in sorted(failed):
        link=links[link_id];p,q=link['points'][index:index+2];low,high=(p,q) if p[1]<q[1] else (q,p);run=math.hypot(high[0]-low[0],high[2]-low[2]);dx=(high[0]-low[0])/run;dz=(high[2]-low[2])/run;rise=high[1]-low[1];count=math.ceil(rise/.18);tread=run/count
        mat=next(o.data.materials[0] for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='structural-stair-step' and o.get('link')==link_id)
        start=[high[0]-dx*tread,high[1],high[2]-dz*tread];end=[low[0]-dx*.05,low[1],low[2]-dz*.05];cut=rect(start,end,link['width']/2);required=0;interfering=[]
        for obj in list(bpy.context.scene.objects):
            if obj.type!='MESH' or obj.get('role')!='structural-stair-landing' or obj.get('link')!=link_id:continue
            points=[obj.matrix_world@v.co for v in obj.data.vertices]
            if abs(max(v.z for v in points)-high[1])>.0001:continue
            obj.data.calc_loop_triangles();overlap=0
            for tri in obj.data.loop_triangles:
                ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
                if any(abs(v.z-high[1])>.0001 for v in ps):continue
                poly=[(v.x,-v.y) for v in ps];overlap+=area(poly)-sum(area(r) for r in difference(poly,cut))
            if overlap>.00001:
                required=max(required,max((v.x-high[0])*(-dx)+(-v.y-high[2])*(-dz) for v in points)+.05);interfering.append(obj.name)
        newhigh=list(high)
        if required>0:
            if run-required<count*.24:
                findings.append(dict(link=link_id,segment=index,type='CORNER_REQUIRES_LONGER_AUTHORED_BAY',required_shift=required,available_run=run,minimum_run=count*.24))
            else:
                newhigh=[high[0]-dx*required,high[1],high[2]-dz*required];ordinal=sum(abs(a[1]-b[1])>.01 for a,b in zip(link['points'][:index],link['points'][1:index+1]));oldsteps=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.get('role')=='structural-stair-step' and o.get('link')==link_id and (' flight'+str(ordinal)+' step') in o.name];assert len(oldsteps)==count,(site,link_id,'source flight identity mismatch')
                for obj in oldsteps:removed.append(obj.name);bpy.data.objects.remove(obj,do_unlink=True)
                collection=bpy.data.collections.new('Authored stair corner '+link_id);bpy.context.scene.collection.children.link(collection);before=set(bpy.context.scene.objects);notes=[]
                replacement=dict(link,points=[low,newhigh]);assert stair_flights(replacement,collection,mat,box,notes)
                made=[o for o in bpy.context.scene.objects if o not in before]
                for number,obj in enumerate(made):obj.name='Rebuilt stair '+site+' '+link_id+' flight'+str(ordinal)+' part'+str(number);built.append(obj)
                newp,newq=(list(low),newhigh) if p[1]<q[1] else (newhigh,list(low));revisions[(link_id,index)]=(newp,newq);bridges.append(dict(link=link_id,source_segment=index,points=[list(high),newhigh],width=link['width']));requests.append((link_id,index,'corner-bridge',list(high),newhigh,link['width'],mat));replacements.append(dict(link=link_id,source_segment=index,old_points=[p,q],new_points=[newp,newq],shift_metres=required,original_landings_retained=interfering,new_stair_parts=[o.name for o in made]))
        actual_low=low;actual_high=newhigh
        for label,a,b in [('low',[actual_low[0]-dx*.8,actual_low[1],actual_low[2]-dz*.8],actual_low),('high',actual_high,[actual_high[0]+dx*.8,actual_high[1],actual_high[2]+dz*.8])]:requests.append((link_id,index,label,a,b,link['width'],mat))
    segment_map={}
    for link in data['links']:
        original=links[link['id']];points=[];mapping={}
        for i,(p,q) in enumerate(zip(original['points'],original['points'][1:])):
            if not points:points.append(p)
            pair=revisions.get((link['id'],i))
            if pair:
                np,nq=pair
                if np!=p:points.append(np)
                mapping[str(len(points)-1)]=i;points.append(nq)
                if nq!=q:points.append(q)
            else:
                if abs(p[1]-q[1])>.01:mapping[str(len(points)-1)]=i
                points.append(q)
        link['points']=points;segment_map[link['id']]=mapping
    # Newly rotated tread boxes must expose their final world matrices before
    # they are used as physical top-surface cutters.
    bpy.context.view_layer.update()
    for link_id,index,label,p,q,width,mat in requests:
        e=p[1];polygons=[rect(p,q,width/2)];receivers=[]
        for obj in list(bpy.context.scene.objects):
            if obj.type!='MESH':continue
            role=obj.get('role','')
            if not(role in ['floor','ceiling-cap','structural-stair-step','structural-stair-landing'] or role.startswith(('rough route support','saved editor terrain')) or obj.name.startswith('Authored ') and 'terrain' in obj.name):continue
            obj.data.calc_loop_triangles()
            cutters=[]
            for tri in obj.data.loop_triangles:
                ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
                if any(abs(v.z-e)>.0001 for v in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
                # Snap authored planar cutters to 10 micrometres before Boolean
                # subtraction. Float-transformed collinear edges otherwise form
                # discarded slivers that can take a macroscopic wedge with them.
                cutters.append([(round(v.x,5),round(-v.y,5)) for v in ps])
            # These construction parts are convex boxes/plates. Subtract their
            # whole top, avoiding numerical diagonal fragments at landing joins.
            if cutters and role in ['structural-stair-step','structural-stair-landing']:
                hull=convex_top([p for cut in cutters for p in cut])
                if abs(area(hull)-sum(area(c) for c in cutters))<1e-6:cutters=[hull]
            for cut in cutters:
                remaining=[r for poly in polygons for r in difference(poly,cut)]
                if sum(area(poly) for poly in remaining)<sum(area(poly) for poly in polygons)-1e-8:receivers.append(obj.name)
                polygons=remaining
        accepted=[]
        for poly in polygons:
            poly=clean_polygon(poly);span=max(math.dist(a,b) for a in poly for b in poly)
            if area(poly)>1e-8 and 2*area(poly)/span>=.0001:accepted.append(poly)
        new=[]
        if accepted:
            new=plate(f'Authored stair end {site} {link_id} {index} {label}',accepted,e,.16,mat)
            for obj in new:obj['role']='structural-stair-landing';obj['link']=link_id;built.append(obj);proof.append(dict(name=obj.name,link=link_id,segment=index,end=label,top_y=e,bottom_y=e-.16,pad_polygon=rect(p,q,width/2)))
        decisions.append(dict(link=link_id,segment=index,end=label,authored_point=q if label=='low' else p,actual_existing_top_receivers=sorted(set(receivers)),authored_patch_polygons=accepted,new_parts=[o.name for o in new]))
    # Bake only newly authored geometry before export so native/world floats and
    # exported vertices share identical arithmetic and triangle diagonals.
    bpy.ops.object.select_all(action='DESELECT')
    for obj in built:
        obj.select_set(True);bpy.context.view_layer.objects.active=obj
        bpy.ops.object.transform_apply(location=True,rotation=True,scale=True)
        bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.triangulate(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free();obj.select_set(False)
    folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True);CAP.mkdir(parents=True,exist_ok=True);bpy.ops.wm.save_as_mainfile(filepath=str(folder/'supported-stairs.blend'),compress=True)
    if built:
        bpy.ops.object.select_all(action='DESELECT')
        for obj in built:obj.select_set(True)
        addition=ROOT/'build'/('stair-end-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);retained=append(source,addition,folder/'supported-stairs.glb',remove_mesh_names=removed)
    else:(folder/'supported-stairs.glb').write_bytes(source.read_bytes());retained=dict(no_new_geometry=True)
    removed_native=[native_by_mesh[n] for n in removed];kept=[r for r in native_parts if r[0] not in removed_native];parts=[(o.name,records(o)) for o in built]
    payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(n for name,n,raw in kept)+sum(len(ts) for name,ts in parts))+b''.join(raw for name,n,raw in kept)
    for name,triangles in parts:
        encoded=name.encode();payload+=struct.pack('>H',len(encoded))+encoded+struct.pack('>BI',1,len(triangles))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in triangles)
    (folder/'supported-stairs.hcs2').write_bytes(payload)
    original_stairs=json.loads((BASE/'furnishings'/site.lower()/'physics-contract.json').read_text())['stair_components'];stairs=[n for n in original_stairs if n not in removed_native]+[n for n,ts in parts];assert len(stairs)==len(set(stairs))
    # Review actual export, not the still-open authoring scene.
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/'supported-stairs.glb'));expected={n:face_keys(ts) for n,ts in parts}
    for obj in bpy.context.scene.objects:
        if obj.name in expected:assert face_keys(records(obj))==expected.pop(obj.name),(site,obj.name,'export/native mismatch')
    assert not expected
    scene=bpy.context.scene;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.display.shading.show_cavity=True;scene.display.shading.show_shadows=True;scene.render.resolution_x=1200;scene.render.resolution_y=900;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.world=bpy.data.worlds.new('Native actual stair-end fit review');a,b,c,d=data['site_bounds'];span=max(c-a,d-b);center=Vector(((a+c)/2,-(b+d)/2,1));bpy.ops.object.camera_add();cam=bpy.context.object;cam.data.type='ORTHO';cam.data.ortho_scale=span*1.55;scene.camera=cam;captures=[]
    for label,offset in [('whole-site',Vector((span*.8,-span*.9,span*.8))),('new-landings',Vector((span*.8,-span*.8,span*.5)))]:
        if label=='new-landings':
            for obj in bpy.context.scene.objects:
                if obj.type=='MESH' and obj.name not in {n for n,ts in parts}:obj.hide_render=True
        cam.location=center+offset;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();p=CAP/f'{site.lower()}-{label}.png';scene.render.filepath=str(p);bpy.ops.render.render(write_still=True);captures.append(dict(path=p.relative_to(ROOT).as_posix(),sha256=sha(p),type='native full scene' if label=='whole-site' else 'native isolated actual landing changes; context hidden'))
    if revisions:
        data['stair_corner_revisions']=replacements;layout=folder/'authored-layout.json';layout.write_bytes((json.dumps(data,indent=2)+'\n').encode('utf-8'))
    report=dict(id=site,status='ACTUAL_STAIR_END_SUPPORT_CANDIDATE_FULL_ROUTE_GATES_OPEN',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=old.relative_to(ROOT).as_posix(),source_physics_sha256=sha(old),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),targeted_segments=[dict(link=l,segment=i) for l,i in sorted(failed)],new_parts=len(parts),new_triangles=sum(len(ts) for n,ts in parts),platforms=proof,landing_trims=trims,removed_source_meshes=removed,removed_native_parts=removed_native,decisions=decisions,stair_components=stairs,retained_glb_resources=retained,glb_sha256=sha(folder/'supported-stairs.glb'),physics_sha256=sha(folder/'supported-stairs.hcs2'),generator_sha256=sha(Path(__file__)),captures=captures,source_layout=source_layout.relative_to(ROOT).as_posix(),source_layout_sha256=sha(source_layout),flight_replacements=replacements,corner_bridges=bridges,source_segment_map=segment_map,open_findings=findings,remaining='Independent solids/material/pivot/native checks and all25flat/continuous stair regressions. Wall/terrain penetrations, crossed flights, circulation, supports/guards and actual runtime/network/hardware gates remain open.')
    (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode('utf-8'));print('AUTHORED_STAIR_ENDS',site,len(proof),len(trims),flush=True)


def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args(sys.argv[sys.argv.index('--')+1:]);contacts=list(csv.DictReader((BASE/'floor-closure-pass/verification/failure-contacts.tsv').open(encoding='utf-8'),delimiter='\t'));roster={r['id']:r for r in json.loads((BASE/'terrain-overall-pass/terrain-envelopes.json').read_text())['sites']}
    for site in args.sites:build(site,ROOT/roster[site]['layout'],{(r['link'],int(r['segment'])) for r in contacts if r['site']==site})
if __name__=='__main__':main()
