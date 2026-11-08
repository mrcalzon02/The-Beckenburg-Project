"""Independently inspect preserved meshes/UVs/pivots in exported composites."""
from pathlib import Path
import argparse,collections,hashlib,json,sys
import bpy
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/terrain-overall-pass'
def inspect(path,skip_construction=True):
    bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(path));result={}
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH' or skip_construction and obj.name.startswith(('Saved editor macroterrain','Bearing skirt ','Bearing pile ','Pile bearing beam ')):continue
        obj.data.calc_loop_triangles();parts=[]
        for tri in obj.data.loop_triangles:
            corners=[]
            for index,loop in zip(tri.vertices,tri.loops):
                v=obj.matrix_world@obj.data.vertices[index].co
                corners.append((tuple(round(c,4) for c in v),tuple(tuple(round(c,5) for c in uv.data[loop].uv) for uv in obj.data.uv_layers)))
            parts.append(tuple(sorted(corners)))
        images=sorted((n.image.name,n.image.size[:]) for mat in obj.data.materials if mat and mat.use_nodes for n in mat.node_tree.nodes if n.type=='TEX_IMAGE' and n.image)
        result[obj.name]=dict(parts=collections.Counter(parts),matrix=tuple(tuple(float(c) for c in row) for row in obj.matrix_world),images=images)
    return result
def main():
    p=argparse.ArgumentParser();p.add_argument('--bearing-pass',action='store_true');args=p.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    target=ROOT/'development/levels/rebuild/terrain-fit-pass' if args.bearing_pass else BASE
    sites=json.loads((target/('support-policies.json' if args.bearing_pass else 'macroterrain-specs.json')).read_text())['sites'];results=[]
    for key in sites:
        if args.bearing_pass:
            report=json.loads((target/key.lower()/'construction.json').read_text());original=ROOT/report['source_glb'];candidate=target/key.lower()/'supported-terrain.glb'
        else:original=ROOT/f'development/levels/rebuild/furnishings/{key.lower()}/furnished.glb';candidate=BASE/'assemblies'/key.lower()/'terrain-assembly.glb'
        before=inspect(original);after=inspect(candidate);assert before.keys()==after.keys(),key
        for name,b in before.items():
            a=after[name];assert b['parts']==a['parts'],(key,name,'geometry or UV changed')
            assert b['images']==a['images'],(key,name,'image bindings changed')
            assert max(abs(x-y) for p,q in zip(b['matrix'],a['matrix']) for x,y in zip(p,q))<1e-5,(key,name,'pivot/pose changed')
        results.append(dict(id=key,meshes=len(before),source_glb_sha256=hashlib.sha256(original.read_bytes()).hexdigest(),candidate_glb_sha256=hashlib.sha256(candidate.read_bytes()).hexdigest()))
        print('PRESERVED_EXPORTED_MESHES_UVS_PIVOTS',key,len(before),flush=True)
    report=dict(status='PASS_SCOPED_EXPORT_PRESERVATION',sites=results,position_quantization_metres=.0001,uv_quantization=.00001,pose_tolerance=.00001,checker_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),scope=f'Every original mesh except newly appended bearing construction and separately verified macroterrain; transformed triangle corner/UV layer, image name/dimensions and world transform in all{len(results)}actual candidate GLBs; no visual or gameplay acceptance inferred.')
    (target/'verification/export-preservation.json').write_bytes((json.dumps(report,indent=2)+'\n').encode())
if __name__=='__main__':main()
