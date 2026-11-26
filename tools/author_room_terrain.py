"""Excavate measured occupied terrain through real saved editor topology.

Preserve prior worlds and all source models. Reuse exact source height PNGs;
WLD17 is explicitly seeded from its existing structural terrain for its first
persistent editor world. This is a construction candidate, not acceptance.
"""
from pathlib import Path
import argparse,hashlib,json,math,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'terrain-occupancy-pass'


def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()


def main():
    p=argparse.ArgumentParser();p.add_argument('--sites',nargs='+',required=True);args=p.parse_args()
    sys.path.insert(0,str(ROOT/'build/structure-review-python'));from PIL import Image
    baseline={r['id']:r for r in json.loads((OUT/'baseline-room-terrain.json').read_text())['sites']}
    java=Path('C:/Program Files/Java/jdk-17/bin');build=ROOT/'build/room-terrain-builder';build.mkdir(exist_ok=True);jar=ROOT/'build/beckenburg-game.jar'
    subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(ROOT/'tools/java/RosterTerrainBuild.java')],check=True)
    for site in args.sites:
        row=baseline[site];layout=ROOT/row['layout'];assert sha(layout)==row['layout_sha256'] and sha(ROOT/row['source_glb'])==row['source_glb_sha256'];data=json.loads(layout.read_text())
        samples=OUT/'samples'/site.lower();samples.mkdir(parents=True,exist_ok=True);world=OUT/'worlds'/site.lower()/'terrain-world'
        original=BASE/'terrain-overall-pass/worlds'/site.lower()/'terrain-world'
        if not original.exists():original=BASE/site.lower()/'terrain-world'
        preserved={}
        if original.exists():
            preserved={p.relative_to(ROOT).as_posix():sha(p) for p in original.rglob('*') if p.is_file() and not p.name.endswith('.lock')}
            if not world.exists():shutil.copytree(original,world,ignore=shutil.ignore_patterns('*.lock'))
            props=dict(line.split('=',1) for line in (original/'world.properties').read_text().splitlines() if '=' in line)
            image=Image.open(original/'tile_0_0.png');assert image.size==(65,65)
            heights=[int(v)/65535*64-32 for v in image.get_flattened_data()]
            (samples/'heights.txt').write_bytes(('\n'.join(format(h,'.12f') for h in heights)+'\n').encode())
            # Reuse the existing published envelope, never infer it from new rooms.
            macro=BASE/'terrain-overall-pass/samples'/site.lower()/'settings.tsv'
            bounds=macro.read_bytes() if macro.exists() else ('\t'.join(map(str,data['site_bounds']))+'\n').encode()
            (samples/'settings.tsv').write_bytes(bounds);(samples/'cuts.tsv').write_bytes((original/'authored-cuts.tsv').read_bytes());(samples/'grades.tsv').write_bytes(b'')
            (samples/'provenance.json').write_bytes((json.dumps(dict(id=site,source_world=original.relative_to(ROOT).as_posix(),source_height_sha256=sha(original/'tile_0_0.png'),method='Exact existing saved source PNG decoded to identical16bit input; existing sculpt/topology retained; add occupied-room cut program only'),indent=2)+'\n').encode())
        else:
            assert site=='WLD-17','New source seed must be explicit'
            seed=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
            subprocess.run(['C:/Program Files/Blender Foundation/Blender 5.0/blender.exe','--background','--factory-startup','--python-exit-code','1','--python','tools/sample_roster_terrain.py','--','--sites',site.lower(),'--source-ref',seed],check=True)
            for p in (ROOT/'build/terrain-samples'/site.lower()).iterdir():
                if p.is_file():shutil.copyfile(p,samples/p.name)
        zones={z['id']:z for z in data['zones']};new=[]
        for finding in row['intrusions']:
            z=zones[finding['id']];a,b,c,d=z['bounds'];step=.25
            if z.get('shape')=='circle':
                cx=(a+c)/2;cz=(b+d)/2;radius=z['radius']
                # Quarter-metre cells intersecting the actual circular
                # foundation. Composite construction must close the stepped
                # excavation excess with a real terrain-bearing collar.
                for iz in range(math.floor(b/step),math.ceil(d/step)):
                    low=iz*step;far=0 if low<=cz<=low+step else min(abs(low-cz),abs(low+step-cz))
                    if far>=radius:continue
                    half=math.sqrt(radius*radius-far*far);left=math.floor((cx-half)/step)*step;right=math.ceil((cx+half)/step)*step
                    if right>left:new.append([left,low,right,low+step,'occupied-room-'+z['id']])
            else:new.append([math.floor(a/step)*step,math.floor(b/step)*step,math.ceil(c/step)*step,math.ceil(d/step)*step,'occupied-room-'+z['id']])
        assert new
        with (samples/'cuts.tsv').open('ab') as f:f.write(('\n'.join('\t'.join(map(str,r)) for r in new)+'\n').encode())
        subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'org.beckenburg.RosterTerrainBuild','--input-root',str(OUT/'samples'),'--output-root',str(OUT/'worlds'),'--preview-root',str(BASE/'captures/room-terrain-batch'),site.lower()],check=True)
        assert all(sha(ROOT/name)==h for name,h in preserved.items()),'Prior saved world changed'
        if original.exists():assert sha(world/'tile_0_0.png')==sha(original/'tile_0_0.png'),'Excavation replaced source heights'
        report=dict(id=site,source=row,source_world_preserved=preserved,new_cuts=new,world=world.relative_to(ROOT).as_posix(),saved_world_files={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')},author_sha256=sha(Path(__file__)),builder_sha256=sha(ROOT/'tools/java/RosterTerrainBuild.java'),dependency_jar_sha256=sha(jar),status='ACTUAL_SAVED_OCCUPIED_ROOM_EXCAVATION_CANDIDATE_NOT_MAP_ACCEPTANCE',remaining='Independent terrain/native/room coverage and route regression checks; foundation/rock/retaining/collar edges, remaining outdoor conflicts and runtime integration.')
        (OUT/(site.lower()+'-world.json')).write_bytes((json.dumps(report,indent=2)+'\n').encode());print('EXCAVATED_OCCUPIED_ROOMS',site,len(new),flush=True)
if __name__=='__main__':main()
