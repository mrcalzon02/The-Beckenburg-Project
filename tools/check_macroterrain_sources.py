"""Independent read-only integrity and perimeter-raster check for terrain batch."""
from pathlib import Path
import hashlib,importlib.util,json,math,struct
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild/terrain-overall-pass'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def polygon_contains(x,z,p):
    crossings=0
    for i in range(len(p)):
        ax,az=p[i-1];bx,bz=p[i]
        if (az<=z<bz) or (bz<=z<az):
            if x<ax+(z-az)*(bx-ax)/(bz-az):crossings+=1
    return crossings%2==1
def main():
    sites=json.loads((BASE/'macroterrain-specs.json').read_text())['sites'];results=[]
    for key,spec in sites.items():
        site=key.lower();sample=BASE/'samples'/site;world=BASE/'worlds'/site/'terrain-world';assembly=BASE/'assemblies'/site
        provenance=json.loads((sample/'provenance.json').read_text());native=json.loads((world/'verification.json').read_text());review=json.loads((assembly/'review.json').read_text())
        assert (world/'source-provenance.json').read_bytes()==(sample/'provenance.json').read_bytes()
        assert (world/'authored-cuts.tsv').read_bytes()==(sample/'cuts.tsv').read_bytes()
        assert not provenance['perimeter_fit_failures']
        assert native['disk_reopen_verified'] and native['punched_body_clear_and_unpunched_body_blocked']
        assert native['cutout_leaves']<=20000
        assert sha(world/'terrain-physics.hcs2')==native['physics_sha256']
        assert sha(assembly/'terrain-assembly.glb')==review['native_glb_sha256'] and sha(assembly/'terrain-assembly.hcs2')==review['native_physics_sha256']
        assert review['exact_render_native_terrain_parity'] and review['original_collision_parts_byte_preserved']
        for path,digest in review['preserved_sources'].items():assert sha(ROOT/path)==digest
        for capture in review['captures']:assert sha(ROOT/capture['path'])==capture['sha256']
        # Check every quarter-metre classification independently. Reconstruct
        # outside cut cells from authored dyadic rectangles; reject overlaps,
        # missing cells, or accidental intrusion into the designed landform.
        a,b,c,d=map(float,(sample/'settings.tsv').read_text().split());removed=set()
        for line in (sample/'cuts.tsv').read_text().splitlines():
            fields=line.split('\t')
            if fields[-1]!='authored-landform-edge':continue
            x0,z0,x1,z1=map(float,fields[:4])
            for z in range(round((z0-b)*4),round((z1-b)*4)):
                for x in range(round((x0-a)*4),round((x1-a)*4)):
                    assert (x,z) not in removed;removed.add((x,z))
        checked=0
        for z in range(round((d-b)*4)):
            for x in range(round((c-a)*4)):
                assert ((x,z) in removed)==(not polygon_contains(a+(x+.5)/4,b+(z+.5)/4,spec['boundary'])),(key,x,z)
                checked+=1
        # Prove exact original-part byte preservation in the HCS2 union.
        old=ROOT/f'development/levels/rebuild/furnishings/{site}/furnished-physics.hcs2';old=old.read_bytes();ground=(world/'terrain-physics.hcs2').read_bytes();composite=(assembly/'terrain-assembly.hcs2').read_bytes()
        assert composite[12:]==old[12:]+ground[12:]
        _,op,ot=struct.unpack_from('>III',old);_,gp,gt=struct.unpack_from('>III',ground);assert struct.unpack_from('>III',composite)==(0x48435332,op+gp,ot+gt)
        results.append(dict(id=key,quarter_metre_cells_checked=checked,cutouts=native['cutout_leaves'],clear_hole_probes=native['clear_hole_probes'],terrain_triangles=gt,chunk_count=native.get('obj_chunks',1),excavation_rims_open=len(provenance['excavation_rim_requirements'])))
    result=dict(status='PASS_SCOPED_INTEGRITY_NOT_TERRAIN_ACCEPTANCE',sites=results,checker_sha256=sha(Path(__file__)),limits='No visual/route/terrain construction acceptance inferred. Existing native movement failures are in native-route-fit.json; canonical van, foundations/retaining rims/guards/water/runtime remain open.')
    (BASE/'verification/source-integrity.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print('PASS18saved terrain/composite sources;',sum(r['quarter_metre_cells_checked'] for r in results),'independent quarter-metre footprint cells;',sum(r['terrain_triangles'] for r in results),'native terrain triangles')
if __name__=='__main__':main()
