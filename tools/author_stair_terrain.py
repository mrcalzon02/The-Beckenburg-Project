"""Saved editor stair benches and basement punches on preserved site landforms."""
from pathlib import Path
import hashlib,json,math,os,shutil,subprocess,sys
ROOT=Path(__file__).resolve().parents[1];BASE=ROOT/'development/levels/rebuild';OUT=BASE/'stair-terrain-pass'
PLANS={'WLD-03':['L04','L07'],'WLD-04':['L03','L04','L06','L11'],'WLD-12':['L16','L17']}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def source(site):
    return next((p,s) for p,s in [(BASE/'stair-bay-pass'/site.lower(),'bayed-stairs'),(BASE/'stair-opening-pass'/site.lower(),'opened-stairs'),(BASE/'stair-landing-pass'/site.lower(),'supported-stairs')] if (p/'construction.json').exists())
def basement_cuts(a,b,radius):
    step=.25;rows=[];dx=b[0]-a[0];dz=b[2]-a[2];length2=dx*dx+dz*dz
    for iz in range(math.floor((min(a[2],b[2])-radius)/step),math.ceil((max(a[2],b[2])+radius)/step)):
        z=(iz+.5)*step;cells=[]
        for ix in range(math.floor((min(a[0],b[0])-radius)/step),math.ceil((max(a[0],b[0])+radius)/step)):
            x=(ix+.5)*step;t=max(0,min(1,((x-a[0])*dx+(z-a[2])*dz)/length2));y=a[1]+(b[1]-a[1])*t
            if y<-.1 and math.hypot(x-a[0]-dx*t,z-a[2]-dz*t)<=radius:cells.append(ix)
        if not cells:continue
        start=previous=cells[0]
        for ix in cells[1:]+[cells[-1]+2]:
            if ix!=previous+1:rows.append([start*step,iz*step,(previous+1)*step,(iz+1)*step]);start=ix
            previous=ix
    return rows
def main(preview_root=None):
    sys.path.insert(0,str(ROOT/'build/structure-review-python'));from PIL import Image
    java=Path('C:/Program Files/Java/jdk-17/bin');build=ROOT/'build/stair-terrain-builder';build.mkdir(exist_ok=True);jar=ROOT/'build/beckenburg-game.jar';subprocess.run([str(java/'javac.exe'),'-cp',str(jar),'-d',str(build),str(ROOT/'tools/java/RosterTerrainBuild.java')],check=True)
    for site,ids in PLANS.items():
        folder,stem=source(site);r=json.loads((folder/'construction.json').read_text());layout=ROOT/r['layout'];data=json.loads(layout.read_text());links={l['id']:l for l in data['links']};old=next(p for p in [BASE/'terrain-occupancy-pass/worlds'/site.lower()/'terrain-world',BASE/'terrain-overall-pass/worlds'/site.lower()/'terrain-world'] if p.exists());world=OUT/'worlds'/site.lower()/'terrain-world';samples=OUT/'samples'/site.lower();samples.mkdir(parents=True,exist_ok=True);preserved={p.relative_to(ROOT).as_posix():sha(p) for p in old.rglob('*') if p.is_file() and not p.name.endswith('.lock')}
        if not world.exists():shutil.copytree(old,world,ignore=shutil.ignore_patterns('*.lock'))
        image=Image.open(old/'tile_0_0.png');assert image.size==(65,65);heights=[int(v)/65535*64-32 for v in image.get_flattened_data()];(samples/'heights.txt').write_bytes(('\n'.join(format(y,'.12f') for y in heights)+'\n').encode());(samples/'settings.tsv').write_bytes((old.parents[2]/'samples'/site.lower()/'settings.tsv').read_bytes());(samples/'cuts.tsv').write_bytes((old/'authored-cuts.tsv').read_bytes());assert not (old/'authored-path-grading.sha256').exists(),'Existing authored grading requires explicit reconciliation'
        grades=[];cuts=[];program=[]
        for key in ids:
            link=links[key];points=[[p[0],p[2],p[1]-.35] for p in link['points']];half=link['width']/2+.8;shoulder=3.5;name='Authored stair bed '+site+' '+key;grades.append('\t'.join(map(str,[name,half,shoulder,*[v for p in points for v in p]])));program.append(dict(link=key,points=link['points'],bed_offset_metres=-.35,half_width_metres=half,shoulder_metres=shoulder))
            if site=='WLD-12':
                for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
                    cuts.extend([*rect,'stair-basement-'+key+'-'+str(i)] for rect in basement_cuts(a,b,link['width']/2+.1))
        # Existing lower paths are constraints on the same landform. A high
        # stair bench must not lower a hillside only into a lower route's body.
        # Keep one shared editor layer for these full-site circulation datums.
        for link in data['links']:
            if link.get('player_access')=='SEALED_NO_ROUTE':continue
            for i,(a,b) in enumerate(zip(link['points'],link['points'][1:])):
                if abs(a[1]-b[1])>.01:continue
                points=[[p[0],p[2],p[1]-.35] for p in [a,b]];half=link['width']/2+.8;shoulder=3.5;grades.append('\t'.join(map(str,['Lower circulation constraints '+site,half,shoulder,*[v for p in points for v in p]])));program.append(dict(link=link['id'],segment=i,points=[a,b],bed_offset_metres=-.35,half_width_metres=half,shoulder_metres=shoulder,purpose='Protect the existing lower circulation plane during upper stair excavation'))
        (samples/'grades.tsv').write_bytes(('\n'.join(grades)+'\n').encode())
        if cuts:
            with (samples/'cuts.tsv').open('ab') as f:f.write(('\n'.join('\t'.join(map(str,r)) for r in cuts)+'\n').encode())
        provenance=dict(id=site,source_world=old.relative_to(ROOT).as_posix(),source_height_sha256=sha(old/'tile_0_0.png'),layout=layout.relative_to(ROOT).as_posix(),layout_sha256=sha(layout),program=program,grading_limit='LOWER_ONLY',new_basement_punches=cuts,method='Actual editor non-destructive lower-only bench layers and persistent footprint topology punches; retain source landforms/source16bitPNG and previous holes; no room-pad snapping or render-only hole.')
        (samples/'provenance.json').write_bytes((json.dumps(provenance,indent=2)+'\n').encode());subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(build)+os.pathsep+str(jar),'org.beckenburg.RosterTerrainBuild','--cut-only-grades','--input-root',str(OUT/'samples'),'--output-root',str(OUT/'worlds'),'--preview-root',str(preview_root or BASE/'captures/stair-terrain-batch'),site.lower()],check=True)
        assert all(sha(ROOT/name)==digest for name,digest in preserved.items());assert sha(world/'tile_0_0.png')==sha(old/'tile_0_0.png')
        report=dict(provenance,source_glb=(folder/(stem+'.glb')).relative_to(ROOT).as_posix(),source_glb_sha256=sha(folder/(stem+'.glb')),source_physics=(folder/(stem+'.hcs2')).relative_to(ROOT).as_posix(),source_physics_sha256=sha(folder/(stem+'.hcs2')),source_world_files_preserved=preserved,world=world.relative_to(ROOT).as_posix(),world_files_sha256={p.relative_to(ROOT).as_posix():sha(p) for p in world.rglob('*') if p.is_file() and not p.name.endswith('.lock')},author_sha256=sha(Path(__file__)),builder_sha256=sha(ROOT/'tools/java/RosterTerrainBuild.java'),dependency_jar_sha256=sha(jar),status='SAVED_EDITOR_STAIR_BED_AND_BASEMENT_PUNCH_CANDIDATE_NOT_TERRAIN_ACCEPTANCE',remaining='Actual composite/export/native regression and full-site landscape, retaining/cave/edge/arrival acceptance; no runtime integration or unlock.')
        (OUT/(site.lower()+'-world.json')).write_bytes((json.dumps(report,indent=2)+'\n').encode());print('SAVED_STAIR_TERRAIN',site,len(grades),len(cuts),flush=True)
if __name__=='__main__':main()
