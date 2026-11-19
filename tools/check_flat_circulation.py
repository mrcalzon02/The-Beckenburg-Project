"""Independent preservation, closed-solid, native and editable-source checks."""
from pathlib import Path
import collections,json,sys,math
import bpy,bmesh
from mathutils import Vector
from mathutils.bvhtree import BVHTree
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'development/levels/blockouts'))
from structural_pass import deck_openings
from build_blockouts import wall_crossings
from build_roster_floor_closures import difference
from build_roster_decks import area
from check_macroterrain_exports import inspect
from check_roster_decks import read_parts
from review_roster_macroterrain import records,sha
from export_roster_collision import face_keys
from append_static_glb import read
BASE=ROOT/'development/levels/rebuild/flat-circulation-pass'

def ceiling_ownership(r):
    data=json.loads((ROOT/r['layout']).read_text());zones={z['id']:z for z in data['zones']};rows=[]
    def inside(p,poly):
        signs=[(b[0]-a[0])*(p[1]-a[1])-(b[1]-a[1])*(p[0]-a[0]) for a,b in zip(poly,poly[1:]+poly[:1])]
        return all(v>=-1e-7 for v in signs) or all(v<=1e-7 for v in signs)
    for key,revision in r.get('ceiling_revisions',{}).items():
        upper=revision['upper_floor_owner'];height=revision['new_ceiling_top'];points=[];faces=[]
        for obj in bpy.context.scene.objects:
            if obj.type!='MESH' or obj.get('role')!='floor' or obj.get('zone')!=upper:continue
            for xyz,flags in records(obj):
                i=len(points);points.extend([xyz[j:j+3] for j in [0,3,6]]);faces.append((i,i+1,i+2))
        assert faces,(r['id'],key,'No actual upper floor mesh owner')
        tree=BVHTree.FromPolygons(points,faces);a,b,c,d=zones[key]['bounds'];holes=deck_openings(height,data);samples=[];failed=[]
        for ix in range(math.ceil((c-a)/.5)):
            x=a+(ix+.5)*(c-a)/math.ceil((c-a)/.5)
            for iz in range(math.ceil((d-b)/.5)):
                z=b+(iz+.5)*(d-b)/math.ceil((d-b)/.5)
                if any(inside((x,z),hole['polygon']) for hole in holes):continue
                top=tree.ray_cast(Vector((x,height+.04,z)),Vector((0,-1,0)),.35)[0];under=tree.ray_cast(Vector((x,height-.3,z)),Vector((0,1,0)),.35)[0];valid=top is not None and under is not None and abs(top.y-height)<.001 and abs(under.y-(height-.16))<.001;row=dict(point=[x,height,z],top_y=None if top is None else top.y,underside_y=None if under is None else under.y,valid=valid);samples.append(row)
                if not valid:failed.append(row)
        for obj in bpy.context.scene.objects:
            if obj.type=='MESH' and obj.get('zone')==key and obj.get('role') in ['rough shell wall','rough opening header','ceiling-cap']:assert max((obj.matrix_world@v.co).z for v in obj.data.vertices)<=height+.001,(r['id'],obj.name,'lower structural shell protrudes above shared datum')
        result=dict(site=r['id'],zone=key,upper_floor=upper,samples=len(samples),failures=failed,actual_probes=samples,scope='Actual complete retained upper floor triangles, top and160mmunderside at half-metre grid outside declared stair openings; not full room optical/runtime/fixture acceptance');rows.append(result)
    return rows


def floor_surface_ownership(r,old):
    rows=[];floors={}
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH' or obj.get('role')!='floor':continue
        obj.data.calc_loop_triangles()
        for tri in obj.data.loop_triangles:
            ps=[obj.matrix_world@obj.data.vertices[i].co for i in tri.vertices]
            if max(v.z for v in ps)-min(v.z for v in ps)<.00001 and (ps[1]-ps[0]).cross(ps[2]-ps[0]).z>0:floors.setdefault(round(ps[0].z,4),[]).append(([(v.x,-v.y) for v in ps],min((obj.matrix_world@v.co).z for v in obj.data.vertices)))
    for proof in r.get('floor_surface_ownership',{}).get('removed_redundant_prisms',[]):
        ts=old[proof['native_name']][1];vertices={tuple(xyz[i:i+3]) for xyz,flags in ts for i in [0,3,6]};top=proof['top_y'];ys={round(v[1],5) for v in vertices};assert len(ys)==2,'Only a real flat extruded rough prism can be redundant';assert {(round(v[0],5),round(v[2],5)) for v in vertices}=={(round(v[0],5),round(v[2],5)) for v in vertices if abs(v[1]-top)<.00001},'Partial high pad is not an entire covered prism'
        remaining=[]
        for xyz,flags in ts:
            ps=[Vector(xyz[i:i+3]) for i in [0,3,6]]
            if all(abs(v.y-top)<.00001 for v in ps) and (ps[1]-ps[0]).cross(ps[2]-ps[0]).y>0:remaining.append([(v.x,v.z) for v in ps])
        original_area=sum(area(p) for p in remaining);assert original_area>0
        bottom=min(v[1] for v in vertices)
        for cut,floor_bottom in floors.get(round(top,4),[]):
            if floor_bottom>bottom+.00001:continue
            remaining=[q for p in remaining for q in difference(p,cut)]
            if not remaining:break
        uncovered=sum(area(p) for p in remaining);assert uncovered<1e-9,(r['id'],proof['name'],'Removed route prism is not fully owned by actual floors')
        rows.append(dict(name=proof['name'],native_name=proof['native_name'],actual_source_top_area=original_area,uncovered_area=uncovered,original_bottom_y=bottom,full_depth_floor_coverage_verified=True,extruded_prism_verified=True))
    return rows


def fixed_context_boundaries(r):
    data=json.loads((ROOT/r['layout']).read_text());before=json.loads((ROOT/r['before_layout']).read_text());sealed=[l for l in data['links'] if l.get('player_access')=='SEALED_NO_ROUTE'];assert sealed==[l for l in before['links'] if l.get('player_access')=='SEALED_NO_ROUTE'];vertices=[];faces=[]
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH' or obj.get('role') not in ['floor','ceiling-cap','rough shell wall','rough opening header','fixed-context-infill']:continue
        offset=len(vertices);vertices.extend(obj.matrix_world@v.co for v in obj.data.vertices);faces.extend(tuple(offset+i for i in poly.vertices) for poly in obj.data.polygons)
    tree=BVHTree.FromPolygons(vertices,faces);checks=[]
    for link in sealed:
        candidates=[]
        for zone in data['zones']:
            if zone['id'] not in [link['a'],link['b']]:continue
            a,b,c,d=zone['bounds']
            for side,cuts in wall_crossings(zone,dict(links=[link])).items():
                for start,width,_ in cuts:
                    x=start+width/2 if side in ['N','S'] else a if side=='W' else c;z=b if side=='N' else d if side=='S' else start+width/2;normal=Vector((0,1,0)) if side in ['N','S'] else Vector((1,0,0));candidates.append((Vector((x,-z,zone['y']+1.2)),normal))
        if not candidates and link['kind']!='level':
            p=max(link['points'],key=lambda p:p[1]);candidates.append((Vector((p[0],-p[2],p[1])),Vector((0,0,1))))
        assert candidates,(r['id'],link['id'],'No actual fixed boundary')
        rays=[tree.ray_cast(point+normal*.35,-normal,.7)[0] is not None for point,normal in candidates];assert all(rays),(r['id'],link['id'],'Previously sealed route now open');checks.append(dict(link=link['id'],boundary_rays=len(rays),all_blocked=True))
    return dict(site=r['id'],sealed_links=len(sealed),checks=checks,scope='Fresh actual exported boundary-centre rays and unchanged sealed link definitions; full width/body/around/under/ghost/object/runtime search-envelope acceptance remains separate')


def main(sites=None):
    rows=[];ceilings=[];ownership=[];seals=[]
    for contract in sorted(BASE.glob('*/construction.json')):
        if sites and contract.parent.name.upper() not in sites:continue
        r=json.loads(contract.read_text());folder=contract.parent;source=ROOT/r['source_glb'];target=folder/'open-circulation.glb';native=folder/'open-circulation.hcs2'
        for p,h in [(source,r['source_glb_sha256']),(ROOT/r['source_physics'],r['source_physics_sha256']),(target,r['glb_sha256']),(native,r['physics_sha256']),(ROOT/r['layout'],r['layout_sha256'])]:assert sha(p)==h,p
        old=read_parts(ROOT/r['source_physics']);new=read_parts(native);removed=set(r['removed_native_parts']);added=set(new)-(set(old)-removed)
        assert set(old)-set(new)==removed
        for n in set(old)-removed:assert old[n]==new[n],(r['id'],n,'native source changed')
        assert len(added)==r['new_parts']
        a=inspect(source,False);b=inspect(target,False);removed_meshes=set(r['removed_source_meshes']);assert set(a)-set(b)==removed_meshes
        assert set(b)-set(a)==added
        for n in set(a)-removed_meshes:assert a[n]==b[n],(r['id'],n,'mesh/UV/image/pivot changed')
        doc,blob=read(source);after,after_blob=read(target);assert after_blob[:len(blob)]==blob
        for key in ['meshes','accessors','bufferViews','images','textures','materials']:assert after.get(key,[])[:len(doc.get(key,[]))]==doc.get(key,[]),(r['id'],key)
        for i,node in enumerate(doc['nodes']):
            expected=dict(node)
            if node.get('name') in removed_meshes:expected.pop('mesh',None)
            assert after['nodes'][i]==expected,(r['id'],i,'unexpected original node change')
        objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'};triangles=0
        for n in added:
            obj=objects[n];ts=records(obj);assert all(all(math.isfinite(v) for v in xyz) for xyz,flags in ts);assert all((obj.matrix_world@obj.data.vertices[t.vertices[1]].co-obj.matrix_world@obj.data.vertices[t.vertices[0]].co).cross(obj.matrix_world@obj.data.vertices[t.vertices[2]].co-obj.matrix_world@obj.data.vertices[t.vertices[0]].co).length>1e-10 for t in obj.data.loop_triangles);assert face_keys(ts)==face_keys(new[n][1]) and new[n][0]==1
            # GLB splits vertices at UV/material seams; close geometric edges.
            bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-8)
            assert all(e.is_manifold for e in bm.edges) and bm.calc_volume(signed=True)>0,(r['id'],n,'open or inverted')
            bm.free();triangles+=len(ts)
        scene=folder/'open-circulation.blend';bpy.ops.wm.open_mainfile(filepath=str(scene));objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
        for n in added:assert face_keys(records(objects[n]))==face_keys(new[n][1]),(r['id'],n,'editable/native mismatch')
        images={n.image for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE' and n.image}
        assert all(i.size[0]>0 and i.size[1]>0 and i.packed_file is not None for i in images)
        seals.append(fixed_context_boundaries(r));ceilings.extend(ceiling_ownership(r));ownership.append(dict(site=r['id'],removed_prisms=floor_surface_ownership(r,old)))
        rows.append(dict(id=r['id'],retained_source_meshes=len(a)-len(removed_meshes),replaced_source_parts=len(removed_meshes),closed_new_parts=len(added),new_triangles=triangles,packed_images=len(images),blend_sha256=sha(scene),glb_sha256=sha(target),physics_sha256=sha(native)))
        print('PASS_STAIR_BAY_SOURCE',r['id'],len(added),triangles,flush=True)
    (BASE/'verification/fixed-context-boundaries.json').write_bytes((json.dumps(dict(checks=seals,checker_sha256=sha(Path(__file__))),indent=2)+'\n').encode())
    (BASE/'verification/floor-surface-ownership.json').write_bytes((json.dumps(dict(checks=ownership,checker_sha256=sha(Path(__file__))),indent=2)+'\n').encode())
    (BASE/'verification/ceiling-ownership.json').write_bytes((json.dumps(dict(checks=ceilings,checker_sha256=sha(Path(__file__))),indent=2)+'\n').encode())
    assert not any(row['failures'] for row in ceilings),'Shared floor/ceiling ownership lacks actual full-depth coverage'
    (BASE/'verification/independent-sources.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_SOURCE_SOLID_NATIVE_EDITABLE_CHECKS',sites=rows,checker_sha256=sha(Path(__file__)),limitations='No full map, guard/support engineering, runtime/network or hardware acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
