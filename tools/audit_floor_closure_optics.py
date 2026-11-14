"""Run native bidirectional structural opacity checks from actual export probes."""
from pathlib import Path
import csv,hashlib,json,os,subprocess
ROOT=Path(__file__).resolve().parents[1]


def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    out=ROOT/'development/levels/rebuild/floor-closure-pass/verification';build=ROOT/'build/floor-optical-probe';build.mkdir(exist_ok=True);java=Path('C:/Program Files/Java/jdk-17/bin');jar=ROOT/'build/beckenburg-game.jar'
    source=ROOT/'tools/java/FloorClosureOpticalProbe.java';kernel=ROOT/'src/main/java/org/beckenburg/AuthoredSitePhysics.java'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(kernel),str(source)],check=True)
    result=subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'FloorClosureOpticalProbe',str(out/'optical-input.tsv'),str(out/'optical-probes.tsv')],capture_output=True,text=True);(out/'optical-native.log').write_bytes((result.stdout+result.stderr).encode());assert result.returncode==0,result.stdout+result.stderr
    p=out/'optical-probes.tsv';p.write_bytes(p.read_bytes().replace(b'\r\n',b'\n'));rows=list(csv.DictReader(p.open(encoding='utf-8'),delimiter='\t'));assert all(r['after_clear_down']=='false' and r['after_clear_up']=='false' for r in rows)
    report=dict(status='PASS_NATIVE_BIDIRECTIONAL_STRUCTURAL_OPACITY',samples=len(rows),prior_open_rays_closed=sum(r['before_clear_down']=='true' or r['before_clear_up']=='true' for r in rows),source_hashes={p.relative_to(ROOT).as_posix():sha(p) for p in [source,kernel,jar,Path(__file__),out/'optical-input.tsv']},limitation='Actual native geometry rays through new0.16m plates; not installed circuit/torch fixtures, all joins, runtime or multiplayer acceptance.')
    (out/'optical-native.json').write_bytes((json.dumps(report,indent=2)+'\n').encode());print(result.stdout.strip())
if __name__=='__main__':main()
