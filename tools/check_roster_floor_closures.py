"""Independent exported solids, retained assets and stair-clearance sampling."""
from pathlib import Path
import collections,json,math,sys
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from check_macroterrain_exports import inspect
from check_roster_decks import read_parts
from check_roster_foundations import tree
from review_roster_macroterrain import records,sha
from export_roster_collision import native_faces,face_keys
from append_static_glb import read
BASE=ROOT/'development/levels/rebuild/floor-closure-pass'


def main():
    results=[];optical=[]
    for contract in sorted(BASE.glob('*/construction.json')):
        r=json.loads(contract.read_text());folder=contract.parent;source=ROOT/r['source_glb'];candidate=folder/'closed-floors.glb';old=ROOT/r['source_physics'];native=folder/'closed-floors.hcs2'
        for p,h in [(source,r['source_glb_sha256']),(old,r['source_physics_sha256']),(candidate,r['glb_sha256']),(native,r['physics_sha256'])]:assert sha(p)==h,p
        a=inspect(source,False);b=inspect(candidate,False);expected={p['name'] for p in r['plates']};assert set(b)-set(a)==expected and not(set(a)-set(b)),r['id']
        for name in a:
            assert a[name]['parts']==b[name]['parts'] and a[name]['images']==b[name]['images'],(r['id'],name,'source mesh/UV/image changed')
            assert a[name]['matrix']==b[name]['matrix'],(r['id'],name,'source transform changed')
        olddoc,oldbin=read(source);newdoc,newbin=read(candidate);assert newbin[:len(oldbin)]==oldbin
        for key in ['nodes','meshes','accessors','bufferViews','images','textures','materials']:
            assert newdoc.get(key,[])[:len(olddoc.get(key,[]))]==olddoc.get(key,[]),(r['id'],key,'source resource changed')
        raw=old.read_bytes();assert native.read_bytes()[12:len(raw)]==raw[12:]
        parts=read_parts(native);extras=[];area_sum=0
        bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(candidate));meshes={o.name:o for o in bpy.context.scene.objects if o.type=='MESH'}
        for plate in r['plates']:
            name=plate['name'];rs=records(meshes[name]);assert face_keys(rs)==face_keys(parts[name][1]) and parts[name][0]==1,(r['id'],name,'render/native mismatch')
            edges=collections.Counter();volume=0;origin=rs[0][0][:3];top_area=0;sampled=0
            for xyz,flags in rs:
                ps=[tuple(xyz[i:i+3]) for i in [0,3,6]];va,vb,vc=[[xyz[i+k]-origin[k] for k in range(3)] for i in [0,3,6]]
                volume+=(va[0]*(vb[1]*vc[2]-vb[2]*vc[1])+va[1]*(vb[2]*vc[0]-vb[0]*vc[2])+va[2]*(vb[0]*vc[1]-vb[1]*vc[0]))/6
                for p,q in zip(ps,ps[1:]+ps[:1]):edges[tuple(sorted((p,q)))]+=1
                # Exported game coordinates are X,Y,Z. Positive Y is a top.
                normal=(Vector(ps[1])-Vector(ps[0])).cross(Vector(ps[2])-Vector(ps[0]))
                if flags&1 and all(abs(p[1]-plate['top_y'])<1e-4 for p in ps):
                    top_area+=abs(normal.y)/2
                    if sampled<8:
                        x=sum(p[0] for p in ps)/3;z=sum(p[2] for p in ps)/3;e=plate['top_y'];optical.append([r['id'],old,sha(old),native,sha(native),name,x,e,z]);sampled+=1
            assert all(n==2 for n in edges.values()) and volume>1e-8,(r['id'],name,'open/inverted solid')
            assert abs(volume-top_area*.16)<max(.00001,top_area*.00001),(r['id'],name,'incorrect closed thickness')
            assert abs(top_area-plate['area_m2'])<.0001,(r['id'],name,'top area changed');area_sum+=top_area;extras.extend(rs)
        assert face_keys(native_faces(native))==face_keys(native_faces(old)+extras),(r['id'],'unexpected collision changes')
        shaft_probes=0
        if expected:
            by_e={}
            for p in r['plates']:by_e.setdefault(p['top_y'],[]).append(meshes[p['name']])
            data=json.loads((ROOT/r['layout']).read_text())
            for e,objects in by_e.items():
                bv,_=tree(objects)
                for link in data['links']:
                    for p,q in zip(link['points'],link['points'][1:]):
                        run=math.hypot(q[0]-p[0],q[2]-p[2])
                        if run<.01:continue
                        # Independent standing-body intersections with the slab,
                        # rather than trusting the generated shaft polygons.
                        for i in range(math.ceil(run/.25)+1):
                            t=i/math.ceil(run/.25);feet=p[1]+(q[1]-p[1])*t
                            if not(feet+.001<e and feet+1.8>e-.16):continue
                            if link['kind']!='stairs' and abs(p[1]-q[1])<.01:continue
                            for offset in [0,max(0,link['width']/2-.27),-max(0,link['width']/2-.27)]:
                                x=p[0]+(q[0]-p[0])*t-(q[2]-p[2])/run*offset;z=p[2]+(q[2]-p[2])*t+(q[0]-p[0])/run*offset
                                hit=bv.ray_cast(Vector((x,-z,e+.03)),Vector((0,0,-1)),.22)[0]
                                assert hit is None,(r['id'],link['id'],x,e,z,'new plate crosses standing stair body');shaft_probes+=1
        results.append(dict(id=r['id'],preserved_meshes=len(a),closed_new_parts=len(expected),new_triangles=len(extras),restored_area_m2=area_sum,shaft_body_samples_clear=shaft_probes,glb_sha256=sha(candidate),physics_sha256=sha(native)))
        print('PASS_EXPORTED_FLOOR_CAP',r['id'],len(expected),len(extras),shaft_probes,flush=True)
    out=BASE/'verification';out.mkdir(exist_ok=True);(out/'optical-input.tsv').write_bytes(('\n'.join('\t'.join(map(str,r)) for r in optical)+'\n').encode())
    (out/'independent-export.json').write_bytes((json.dumps(dict(status='PASS_SCOPED_EXPORTED_FLOOR_CAP_CHECKS',sites=results,checker_sha256=sha(Path(__file__)),scope='Every source node/resource/mesh/UV/image/pose preserved; every appended triangle matches native and is nondegenerate; exact-edge closed outward solids with0.16m volume/area thickness; independent standing-body shaft samples. Native optical and full continuous movement reported separately; not complete map/runtime acceptance.'),indent=2)+'\n').encode())
if __name__=='__main__':main()
