"""Authored shared stair bays, physical shaft/floor revisions and saved layouts."""
from pathlib import Path
import copy,json,math,struct,sys
import bpy,bmesh
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from build_stair_landings import rect,convex_top
from build_stair_openings import corridor,bounds,volume
from build_roster_floor_closures import difference,clean_polygon,footprint
from build_roster_decks import area,clip
from structural_pass import stair_flights,deck_openings
from build_blockouts import box
from planar_structure import plate
from compose_room_terrain import split_native
from append_static_glb import append
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-bay-pass'
PLANS={
 'WLD-05':dict(points={
  'L15':[[11,-6,34.5],[5,-6,34.5],[5,-6,33.65],[5,-10,27.5],[5,-10,26.25],[11,-10,26.25],[11,-10,27.5]],
  'L16':[[11,-10,31],[11,-10,27.5],[11,-10,26.25],[5,-10,26.25],[5,-10,27.5],[5,-6,33.65],[5,-6,34.5],[5,-6,52],[11,-6,52]]},build=['L15'],shared={'L16':'L15'},reason='Separate lower shaft from room centre and upper return walkway; both links share the actual revised west-ore stair.'),
 'WLD-09':dict(points={
  'L08':[[50,-6,35],[52,-6,35],[52,-6,39],[42,-6,48],[42,-6,58],[43.75,-6,58],[43.75,-6,56],[45,-6,55]],
  'L11':[[50,-6,35],[52,-6,35],[52,-6,39],[42,-6,48],[42,-6,58],[43.75,-6,58],[43.75,-6,56],[45,-6,55],[70,0,35]],
  'L12':[[70,0,35],[45,-6,55],[43.75,-6,56],[43.75,-6,58],[70,-6,58],[70,-6,55]],
  'L18':[[58,-10,43],[70,-5,43],[74,-5,43],[74,0,28.05],[74,0,27],[70,0,27]]},build=[],retain_flights=['L18'],shared={'L11':'L09','L12':'L09'},landing_pockets=[dict(link='L11',centre=[45,-6,55],width=2.5)],hub_landings=[dict(link='L11',elevation=0,polygon=[[69,34],[71.25,34],[71.25,36.25],[69,36.25]])],reason='Use one existing main east stair with a lower service catwalk; remove duplicate converging columns that block each other and the return stair.'),
 'WLD-12':dict(points={
  'L16':[[31,0,33.4],[28,0,33.4],[28,0,32.3],[28,-3,28],[28,-3,26.5],[31,-3,26.5],[31,-3,28]],
  'L17':[[31,-3,32],[31,-3,36],[11,0,42]]},build=['L16','L17'],shared={},reason='West-edge descending bay and separate lower return lead-in avoid both crossed columns and the upper trestle walkway.')}

def intersection(poly,cutter):
    if sum(p[0]*q[1]-q[0]*p[1] for p,q in zip(cutter,cutter[1:]+cutter[:1]))<0:cutter=list(reversed(cutter))
    for a,b in zip(cutter,cutter[1:]+cutter[:1]):
        poly=clip(poly,a,b,True)
        if not poly:break
    return poly

def subtract_tops(polygons,e,objects):
    for obj in objects:
        if obj.type!='MESH':continue
        role=obj.get('role','')
        if role not in ['floor','ceiling-cap','structural-stair-step','structural-stair-landing']:continue
        obj.data.calc_loop_triangles();cuts=[]
        for t in obj.data.loop_triangles:
            ps=[obj.matrix_world@obj.data.vertices[i].co for i in t.vertices]
            if any(abs(v.z-e)>.0001 for v in ps) or (ps[1]-ps[0]).cross(ps[2]-ps[0]).z<=0:continue
            cuts.append([(round(v.x,5),round(-v.y,5)) for v in ps])
        if cuts and role in ['structural-stair-step','structural-stair-landing']:
            hull=convex_top([p for c in cuts for p in c])
            if abs(area(hull)-sum(area(c) for c in cuts))<1e-6:cuts=[hull]
        for cut in cuts:polygons=[r for poly in polygons for r in difference(poly,cut)]
    return polygons

def add_plate(name,polys,e,mat,role,link=None):
    accepted=[]
    for poly in polys:
        poly=clean_polygon(poly)
        if len(poly)<3 or area(poly)<1e-8:continue
        span=max(math.dist(a,b) for a in poly for b in poly)
        if 2*area(poly)/span>.0001:accepted.append(poly)
    if not accepted:return []
    made=plate(name,accepted,e,.16,mat)
    for o in made:
        o['role']=role
        if link:o['link']=link
    return made

def build(site,plan,source_options=None,output_stem="bayed-stairs"):
    folder=OUT/site.lower();folder.mkdir(parents=True,exist_ok=True)
    options=[(BASE/'stair-opening-pass'/site.lower(),'opened-stairs'),(BASE/'stair-landing-pass'/site.lower(),'supported-stairs')];prior,stem=next((p,s) for p,s in (source_options or options) if (p/(s+'.glb')).exists());r=json.loads((prior/'construction.json').read_text());r.setdefault('stair_components',json.loads((BASE/'furnishings'/site.lower()/'physics-contract.json').read_text())['stair_components']);source=prior/(stem+'.glb');native=prior/(stem+'.hcs2');before_layout=ROOT/r['layout'];before=json.loads(before_layout.read_text());data=copy.deepcopy(before);links={l['id']:l for l in data['links']};changed=set(plan['points']);mapping=copy.deepcopy(r.get('source_segment_map',{}))
    for key,points in plan['points'].items():
        old=links[key]['points'];assert points[0]==old[0] and points[-1]==old[-1],(site,key,'endpoint contract')
        old_indices=[i for i,(a,b) in enumerate(zip(old,old[1:])) if abs(a[1]-b[1])>.01];links[key]['points']=points
        if points!=old and old_indices:
            assert len(old_indices)==1
            identity=mapping.get(key,{}).get(str(old_indices[0]),old_indices[0]);mapping[key]={str(i):identity for i,(a,b) in enumerate(zip(points,points[1:])) if abs(a[1]-b[1])>.01}
    data['stair_bay_revision']=dict(reason=plan['reason'],shared_physical_flights=plan['shared']);layout=folder/'authored-layout.json';layout.write_bytes((json.dumps(data,indent=2)+'\n').encode());bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source));original=[o for o in bpy.context.scene.objects if o.type=='MESH'];removed=[];built=[];changes=[];materials={key:next(o.data.materials[0] for o in original if o.get('role')=='structural-stair-step' and (o.get('link')==key or key=='L08')) for key in changed};floor_program={(o.get('role'),o.get('zone')):o.data.materials[0] for o in original if o.get('role') in ['floor','ceiling-cap'] and o.get('zone')}
    upper_tops=[]
    for o in original:
        # Existing terminal platforms also support adjoining flat routes. Keep
        # them as real landings; only the conflicting solid tread columns move.
        if o.get('link') in changed and o.get('link') not in plan.get('retain_flights',[]) and o.get('role')=='structural-stair-step':
            high=max(p[1] for p in links[o['link']]['points']);o.data.calc_loop_triangles()
            for t in o.data.loop_triangles:
                ps=[o.matrix_world@o.data.vertices[i].co for i in t.vertices]
                if all(abs(v.z-high)<.0001 for v in ps) and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:upper_tops.append((o['link'],high,[(v.x,-v.y) for v in ps]))
            removed.append(o.name);bpy.data.objects.remove(o,do_unlink=True)
    for key in plan['build']:
        collection=bpy.data.collections.new('Authored bay '+key);bpy.context.scene.collection.children.link(collection);old=set(bpy.context.scene.objects);notes=[];link=dict(links[key],landings_authored_in_zones=True);assert stair_flights(link,collection,materials[key],box,notes)
        for i,o in enumerate(o for o in bpy.context.scene.objects if o not in old):o.name=('Orthogonal stair ' if plan.get('preserve_upper_hubs') is False else 'Bayed stair ')+site+' '+key+' step'+str(i);built.append(o)
    cutters=[];seen_segments=set()
    retained_segments={tuple(sorted((tuple(a),tuple(b)))) for link in before['links'] if link['id'] not in changed for a,b in zip(link['points'],link['points'][1:])}
    for key in sorted(changed):
        if key in plan.get('retain_flights',[]):continue
        link=links[key]
        for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
            signature=tuple(sorted((tuple(a),tuple(b))))
            if signature in retained_segments or signature in seen_segments:continue
            seen_segments.add(signature)
            run=math.hypot(b[0]-a[0],b[2]-a[2]);dx=(b[0]-a[0])/run;dz=(b[2]-a[2])/run
            for suffix,p,q in [('body',a,b),('start',a,[a[0]-dx*.35,a[1],a[2]-dz*.35]),('end',b,[b[0]+dx*.35,b[1],b[2]+dz*.35])]:
                for wall in [False,True]:
                    c=corridor('Bay shaft '+key+' '+str(i)+' '+suffix+str(wall),p,q,link['width'],-.25 if wall or abs(p[1]-q[1])>.01 else .005);c['wall_cutter']=wall;cutters.append(c)
    for pocket in plan.get('landing_pockets',[]):
        x,y,z=pocket['centre'];c=box('Measured stair landing pocket',x,y-.2,z,pocket['width'],.4,pocket['width'],materials[pocket['link']],bpy.context.scene.collection);c['wall_cutter']=False;c['landing_pocket']=True;cutters.append(c)
    bpy.context.view_layer.update()
    for obj in list(bpy.context.scene.objects):
        if obj.type!='MESH' or obj.get('role') not in ['rough shell wall','floor','ceiling-cap']:continue
        near=[c for c in cutters if c['wall_cutter']==(obj.get('role')=='rough shell wall') and (not c.get('landing_pocket') or obj.get('role')=='floor') and all(a[0]<b[1] and a[1]>b[0] for a,b in zip(bounds(obj),bounds(c)))]
        if not near:continue
        duplicate=obj.copy();duplicate.data=obj.data.copy();bpy.context.scene.collection.objects.link(duplicate);v=volume(duplicate);bm=bmesh.new();bm.from_mesh(duplicate.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8);bm.to_mesh(duplicate.data);bm.free()
        for cutter in near:
            bpy.context.view_layer.objects.active=duplicate;mod=duplicate.modifiers.new('Physical bay opening','BOOLEAN');mod.operation='DIFFERENCE';mod.solver='EXACT';mod.object=cutter;bpy.ops.object.modifier_apply(modifier=mod.name)
        delta=v-volume(duplicate)
        if delta<1e-7:bpy.data.objects.remove(duplicate,do_unlink=True);continue
        name=obj.name;removed.append(name);bpy.data.objects.remove(obj,do_unlink=True)
        if duplicate.data.polygons:duplicate.name='Bayed shell '+name;built.append(duplicate);new=duplicate.name
        else:new=None;bpy.data.objects.remove(duplicate,do_unlink=True)
        changes.append(dict(source_mesh=name,new_mesh=new,removed_volume_m3=delta))
    for c in cutters:bpy.data.objects.remove(c,do_unlink=True)
    # Restore only vacated old shafts, inside actual authored floor/cap footprints.
    zones={z['id']:z for z in data['zones']};restored=[]
    for (role,zone),mat in floor_program.items():
        z=zones[zone];e=z['y']+(z['height'] if role=='ceiling-cap' else 0);oldholes=[h['polygon'] for h in deck_openings(e,before)];newholes=[h['polygon'] for h in deck_openings(e,data)];polys=[]
        for hole in oldholes:
            poly=intersection(hole,footprint(z))
            if not poly:continue
            remaining=[poly]
            for new in newholes:remaining=[r for p in remaining for r in difference(p,new)]
            polys.extend(remaining)
        polys=subtract_tops(polys,e,list(bpy.context.scene.objects));made=add_plate('Restored bay '+site+' '+zone+' '+role,polys,e,mat,role)
        for o in made:o['zone']=zone;built.append(o);restored.append(o.name)
    for number,pocket in enumerate(plan.get('landing_pockets',[])):
        x,y,z=pocket['centre'];w=pocket['width']/2;poly=[(x-w,z-w),(x+w,z-w),(x+w,z+w),(x-w,z+w)];polys=subtract_tops([poly],y,list(bpy.context.scene.objects));built.extend(add_plate('Measured toe landing '+site+' '+str(number),polys,y,materials[pocket['link']],'structural-stair-landing',pocket['link']))
    for number,hub in enumerate(plan.get('hub_landings',[])):
        polys=subtract_tops([hub['polygon']],hub['elevation'],list(bpy.context.scene.objects));built.extend(add_plate('East stair hub bearing ledge '+str(number),polys,hub['elevation'],materials[hub['link']],'structural-stair-landing',hub['link']))
    # Preserve the upper standing surface of removed converging tread columns
    # as a shallow hub landing; the conflicting lower columns stay removed.
    for number,(key,e,poly) in enumerate(upper_tops if plan.get("preserve_upper_hubs",True) else []):
        bpy.context.view_layer.update();polys=subtract_tops([poly],e,list(bpy.context.scene.objects));built.extend(add_plate('Retained upper hub '+site+' '+key+' '+str(number),polys,e,materials[key],'structural-stair-landing',key))
    # Intermediate flats and complete end footprints are visible structural plates.
    for key in sorted(changed):
        link=links[key];requests=[]
        for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
            run=math.hypot(b[0]-a[0],b[2]-a[2]);dx=(b[0]-a[0])/run;dz=(b[2]-a[2])/run
            if abs(a[1]-b[1])<.01:requests.append((i,a,b))
            else:
                low,high=(a,b) if a[1]<b[1] else (b,a);ux=(high[0]-low[0])/run;uz=(high[2]-low[2])/run;requests.extend([(i,[low[0]-ux*.8,low[1],low[2]-uz*.8],low),(i,high,[high[0]+ux*.8,high[1],high[2]+uz*.8])])
        for i,end,other in [(0,link['points'][0],link['points'][1]),(len(link['points'])-2,link['points'][-1],link['points'][-2])]:
            run=math.hypot(end[0]-other[0],end[2]-other[2]);extended=[end[0]+(end[0]-other[0])/run*.35,end[1],end[2]+(end[2]-other[2])/run*.35];requests.append((i,end,extended))
        for number,(i,a,b) in enumerate(requests):
            bpy.context.view_layer.update();polys=subtract_tops([rect(a,b,link['width']/2)],a[1],list(bpy.context.scene.objects));made=add_plate('Bay landing '+site+' '+key+' '+str(number),polys,a[1],materials[key],'structural-stair-landing',key);built.extend(made)
    bpy.ops.object.select_all(action='DESELECT')
    for o in built:
        o.select_set(True);bpy.context.view_layer.objects.active=o;bpy.ops.object.transform_apply(location=False,rotation=True,scale=True);bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bmesh.ops.triangulate(bm,faces=list(bm.faces));assert all(e.is_manifold for e in bm.edges),(site,o.name,'new solid open');bm.to_mesh(o.data);bm.free();o.select_set(False)
    built=[o for o in built if len(o.data.polygons)>0]
    bpy.ops.wm.save_as_mainfile(filepath=str(folder/(output_stem+'.blend')),compress=True)
    for o in built:o.select_set(True)
    addition=ROOT/'build'/('bay-addition-'+site.lower()+'.glb');bpy.ops.export_scene.gltf(filepath=str(addition),export_format='GLB',export_extras=True,use_selection=True);retained=append(source,addition,folder/(output_stem+'.glb'),remove_mesh_names=removed);oldparts=split_native(native);names={n.split(':',1)[-1]:n for n,count,raw in oldparts};removed_native=[names[n] for n in removed];kept=[p for p in oldparts if p[0] not in removed_native];parts=[(o.name,records(o)) for o in built];payload=struct.pack('>III',0x48435332,len(kept)+len(parts),sum(n for name,n,raw in kept)+sum(len(ts) for name,ts in parts))+b''.join(raw for name,n,raw in kept)
    for name,ts in parts:
        raw=name.encode();payload+=struct.pack('>H',len(raw))+raw+struct.pack('>BI',1,len(ts))+b''.join(struct.pack('>9fB',*xyz,flags) for xyz,flags in ts)
    (folder/(output_stem+'.hcs2')).write_bytes(payload);stairs=[n for n in r['stair_components'] if n not in removed_native]+[o.name for o in built if o.get('role') in ['structural-stair-step','structural-stair-landing']];bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(folder/(output_stem+'.glb')));expected={n:face_keys(ts) for n,ts in parts}
    for o in bpy.context.scene.objects:
        if o.name in expected:assert o.type=='MESH',(site,o.name,o.type);assert face_keys(records(o))==expected.pop(o.name),(site,o.name,'export/native mismatch')
    assert not expected
    report=dict(id=site,status='AUTHORED_SHARED_STAIR_BAY_CANDIDATE_TERRAIN_RUNTIME_GATES_OPEN',source_glb=source.relative_to(ROOT).as_posix(),source_glb_sha256=sha(source),source_physics=native.relative_to(ROOT).as_posix(),source_physics_sha256=sha(native),before_layout=before_layout.relative_to(ROOT).as_posix(),before_layout_sha256=sha(before_layout),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),before_segment_map=r.get('source_segment_map',{}),source_segment_map=mapping,before_stair_components=r['stair_components'],stair_components=stairs,removed_source_meshes=removed,removed_native_parts=removed_native,new_parts=len(parts),new_triangles=sum(len(ts) for n,ts in parts),restored_shaft_parts=restored,shell_changes=changes,design=plan,retained_glb_resources=retained,glb_sha256=sha(folder/(output_stem+'.glb')),physics_sha256=sha(folder/(output_stem+'.hcs2')),generator_sha256=sha(Path(__file__)),remaining='Independent exact source/solid/native and all25route regression; real saved terrain grading/punches, guard/framing/finish and runtime ownership integration remain open.')
    (folder/'construction.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print('AUTHORED_SHARED_STAIR_BAY',site,len(parts),len(removed),flush=True)
if __name__=='__main__':
    for site,plan in PLANS.items():build(site,plan)
