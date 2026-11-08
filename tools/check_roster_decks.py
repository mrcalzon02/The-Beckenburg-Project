"""Independent actual-export preservation, solids, coverage and support audit."""
from pathlib import Path
import collections,json,math,struct,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from check_macroterrain_exports import inspect
from check_roster_foundations import tree
from review_roster_macroterrain import records,sha
from export_roster_collision import native_faces,face_keys
BASE=ROOT/'development/levels/rebuild/deck-fit-pass'


def read_parts(path):
    raw=path.read_bytes();magic,count,total=struct.unpack_from('>III',raw);assert magic==0x48435332
    cursor=12;parts={}
    for _ in range(count):
        n=struct.unpack_from('>H',raw,cursor)[0];cursor+=2;name=raw[cursor:cursor+n].decode();cursor+=n
        closed,size=struct.unpack_from('>BI',raw,cursor);cursor+=5;rs=[]
        for i in range(size):
            *xyz,flags=struct.unpack_from('>9fB',raw,cursor);cursor+=37
            vs=[Vector(xyz[j:j+3]) for j in [0,3,6]]
            assert (vs[1]-vs[0]).cross(vs[2]-vs[0]).length_squared>=1e-24,(path,name,'degenerate triangle')
            rs.append((xyz,flags))
        assert name not in parts;parts[name]=(closed,rs)
    assert cursor==len(raw) and sum(len(t) for c,t in parts.values())==total
    return parts


def main():
    results=[]
    for site in json.loads((BASE/'deck-policies.json').read_text())['sites']:
        folder=BASE/site.lower();r=json.loads((folder/'construction.json').read_text());source=ROOT/r['source_glb'];target=folder/'deck-terrain.glb';old=ROOT/r['source_physics'];native=folder/'deck-terrain.hcs2';layout=ROOT/r['layout']
        for p,h in [(source,r['source_glb_sha256']),(old,r['source_physics_sha256']),(target,r['glb_sha256']),(native,r['physics_sha256']),(layout,r['layout_sha256'])]:assert sha(p)==h,p
        a=inspect(source,False);b=inspect(target,False)
        for name,before in a.items():
            after=b[name];assert before['parts']==after['parts'] and before['images']==after['images'],(site,name,'source geometry/UV/image changed')
            assert max(abs(x-y) for p,q in zip(before['matrix'],after['matrix']) for x,y in zip(p,q))<1e-5,(site,name,'source pose changed')
        new=set(b)-set(a);expected={p['name'] for p in r['decks']+r['supports']};assert new==expected,(site,new^expected)
        assert native.read_bytes()[12:len(old.read_bytes())]==old.read_bytes()[12:]
        parts=read_parts(native);bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(target))
        objects={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
        for name in new:
            rs=records(objects[name]);closed,native_rs=parts[name];assert closed==1 and face_keys(rs)==face_keys(native_rs),(site,name,'render/native mismatch')
            edges=collections.Counter();volume=0;origin=rs[0][0][:3]
            for xyz,flags in rs:
                vs=[Vector(xyz[i:i+3]) for i in [0,3,6]]
                va,vb,vc=[[float(xyz[i+k])-origin[k] for k in range(3)] for i in [0,3,6]]
                volume+=(va[0]*(vb[1]*vc[2]-vb[2]*vc[1])+va[1]*(vb[2]*vc[0]-vb[0]*vc[2])+va[2]*(vb[0]*vc[1]-vb[1]*vc[0]))/6
                ks=[tuple(round(c,5) for c in v) for v in vs]
                for p,q in zip(ks,ks[1:]+ks[:1]):edges[tuple(sorted((p,q)))]+=1
            assert volume>1e-8 and all(n==2 for n in edges.values()),(site,name,'not closed outward solid',volume,[(k,v) for k,v in edges.items() if v!=2])
        deck_names={p['name'] for p in r['decks']};decks,deck_owners=tree([objects[n] for n in deck_names])
        oldfloors,old_owners=tree([o for n,o in objects.items() if n in a and (o.get('role')=='floor' or o.get('role','').startswith('rough route support'))])
        layout_data=json.loads(layout.read_text());zones={z['id']:z for z in layout_data['zones']};coverage=collections.Counter();seen=set()
        for zone_id in r['zones']:
            z=zones[zone_id];x0,z0,x1,z1=z['bounds'];e=z['y'];holes=next(p['apertures'] for p in r['coplanar_subtractions'] if p['zone']==zone_id)
            for ix in range(math.ceil((x1-x0)/.25)):
                for iz in range(math.ceil((z1-z0)/.25)):
                    x=x0+(ix+.5)*.25;y=z0+(iz+.5)*.25
                    if x>=x1 or y>=z1 or (x,y,e) in seen:continue
                    seen.add((x,y,e));hit,normal,_,_=decks.ray_cast(Vector((x,-y,e+.04)),Vector((0,0,-1)),.08)
                    opening=any(h[0]<x<h[2] and h[1]<y<h[3] for h in holes)
                    if opening:
                        assert hit is None,(site,zone_id,x,y,'new slab closes authored stair opening');coverage['clear_aperture_samples']+=1;continue
                    prior=oldfloors.ray_cast(Vector((x,-y,e+.04)),Vector((0,0,-1)),.08)[0]
                    if prior is not None and abs(prior.z-e)<.0001:
                        coverage['preserved_source_top_samples']+=1
                        if hit is not None:
                            # A grid point may lie exactly on the shared edge
                            # of two disjoint top polygons. Probe both interiors.
                            for dx,dz in [(.0002,0),(-.0002,0),(0,.0002),(0,-.0002)]:
                                origin=Vector((x+dx,-y-dz,e+.04));d=decks.ray_cast(origin,Vector((0,0,-1)),.08)[0];p=oldfloors.ray_cast(origin,Vector((0,0,-1)),.08)[0]
                                assert not(d is not None and p is not None and abs(p.z-e)<.0001),(site,x,y,'overlapping top interiors')
                            coverage['shared_edge_samples']+=1
                        continue
                    assert hit is not None and normal.z>.99 and abs(hit.z-e)<1e-4,(site,zone_id,x,y,'missing authored deck top')
                    bottom,normal,_,_=decks.ray_cast(Vector((x,-y,e-.20)),Vector((0,0,1)),.08)
                    assert bottom is not None and normal.z<-.99 and abs(bottom.z-(e-.16))<1e-4,(site,x,y,'missing closed underside')
                    coverage['new_closed_deck_samples']+=1
        terrain=[o for o in objects.values() if o.get('role','').startswith('saved editor terrain') or o.name.startswith('Authored ') and 'terrain' in o.name];soil,_=tree(terrain)
        for p in r['supports']:
            if p['kind']=='connected-bearing-beam':continue
            for (x,z),expected_y in zip(p['terrain_probe_points'],p['ground_y']):
                hit=soil.ray_cast(Vector((x,-z,40)),Vector((0,0,-1)))[0];assert hit is not None and abs(hit.z-expected_y)<1e-4
            receiver,_=tree([objects[p['supports']]]);x,z=p['bearing_point'];hit,normal,_,_=receiver.ray_cast(Vector((x,-z,p['top_y']-.04)),Vector((0,0,1)),.08)
            assert hit is not None and normal.z<-.5 and abs(hit.z-p['top_y'])<1e-4,(site,p['name'],'detached upper bearing')
            assert max(p['bottom_y'])<max(p['ground_y'])-.3,(site,p['name'],'unembedded support')
        results.append(dict(id=site,original_meshes_preserved=len(a),closed_new_parts=len(new),new_triangles=r['new_triangles'],coverage=dict(coverage),supports=len(r['supports']),source_glb_sha256=sha(source),glb_sha256=sha(target),physics_sha256=sha(native)))
        print('PASS_EXPORTED_DECK',site,len(new),dict(coverage),flush=True)
    out=BASE/'verification';out.mkdir(exist_ok=True)
    (out/'export-contact-coverage.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_NATIVE_EXPORT_DECK_CHECKS',sites=results,checker_sha256=sha(Path(__file__)),scope='Every original mesh/UV/image/pose preserved; every appended native triangle finite/nondegenerate, closed outward and exact exported/native; quarter-metre deck grid checks actual top/underside, source top exclusivity, authored apertures and named terrain/bearing contacts. Not finished framing, terrain/boundary, runtime or gameplay acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
