"""Independent native-plane controls for before/after grounded route comparison."""
from pathlib import Path
import csv,hashlib,json,os,struct,subprocess
ROOT=Path(__file__).resolve().parents[1]
def plane(y,x0=-5,x1=5,flags=7):
    p=[(x0,y,-5),(x0,y,5),(x1,y,5),(x1,y,-5)]
    return [([*p[0],*p[1],*p[2]],flags),([*p[0],*p[2],*p[3]],flags)]
def encode(parts):
    out=bytearray(struct.pack('>III',0x48435332,len(parts),sum(len(f) for _,f in parts)))
    for name,faces in parts:
        raw=name.encode();out+=struct.pack('>H',len(raw))+raw+struct.pack('>BI',0,len(faces))
        for xyz,flags in faces:out+=struct.pack('>9fB',*xyz,flags)
    return bytes(out)
def main():
    build=ROOT/'build/route-grounding-controls';build.mkdir(parents=True,exist_ok=True);java='C:/Program Files/Java/jdk-17/bin/';jar=ROOT/'build/beckenburg-game.jar'
    legacy=build/'legacy/FurnishingRouteProbe.java';legacy.parent.mkdir(exist_ok=True);legacy.write_bytes(subprocess.check_output(['git','show','66942b34:tools/java/FurnishingRouteProbe.java'],cwd=ROOT))
    for variant,source in [('legacy',legacy),('current',ROOT/'tools/java/FurnishingRouteProbe.java')]:
        (build/variant).mkdir(exist_ok=True);subprocess.run([java+'javac.exe','-cp',str(jar),'-d',str(build/variant),str(ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'),str(source)],check=True)
    before=build/'before.hcs2';before.write_bytes(encode([('before floor',plane(-.01))]));before_sha=hashlib.sha256(before.read_bytes()).hexdigest();results=[]
    for case,parts,blocked in [('one-centimetre-ground-shift',[('floor',plane(0))],False),('new-low-roof',[('floor',plane(0)),('roof',plane(1.4,flags=6))],True),('missing-ground',[('far floor',plane(0,10,15))],True),('height-outside-original-budget',[('floor',plane(.05))],True)]:
        after=build/(case+'.hcs2');after.write_bytes(encode(parts));after_sha=hashlib.sha256(after.read_bytes()).hexdigest();manifest=build/(case+'.tsv')
        manifest.write_bytes(('\n'.join('\t'.join(map(str,['CONTROL',before,before_sha,after,after_sha,'path',x,0,0,'centre'])) for x in [0,.25])+'\n').encode())
        for variant in ['legacy','current']:
            output=build/(case+'-'+variant+'.tsv');subprocess.run([java+'java.exe','-Djava.awt.headless=true','-cp',str(build/variant)+os.pathsep+str(jar),'FurnishingRouteProbe',str(manifest),str(output)],check=True)
            rows=list(csv.DictReader(output.read_text().splitlines(),delimiter='\t'));assert len(rows)==2 and all(r['baseline_valid']=='true' for r in rows)
            expected=blocked or variant=='legacy'
            assert all((r['new_block']=='true')==expected for r in rows),(case,variant,rows)
            assert (rows[1]['new_sweep_block']=='true')==expected,(case,variant,rows)
            results.append(dict(case=case,variant=variant,new_blocks=sum(r['new_block']=='true' for r in rows),sweep_block=rows[1]['new_sweep_block']=='true'))
    (build/'results.json').write_bytes((json.dumps(dict(controls=results,source_sha256=hashlib.sha256((ROOT/'tools/java/FurnishingRouteProbe.java').read_bytes()).hexdigest(),scope='Separate scene grounding only; unchanged2cm budget, literal body and production movement queries. Roof/missing support/excessive height negatives remain rejected.'),indent=2)+'\n').encode())
    print('PASS independent grounding plane: legacy false positive reproduced; roof, gap and height-budget negatives preserved')
if __name__=='__main__':main()
