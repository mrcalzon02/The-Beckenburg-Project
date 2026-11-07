from pathlib import Path
import subprocess,tarfile,io,os,json,hashlib
R=Path.cwd();OUT=R/'development/levels/rebuild/native-upgrade-baselines';OUT.mkdir(exist_ok=True)
java=Path('C:/Program Files/Java/jdk-17/bin')
refs={'original':'ad9bb45c20c3754931637bdfa50968a35defeaf9','before-finish':'753e637b942fef3b2b7a5cd00b6bac9dc528274d','rebuilt-v1':'5ba51a1e5074ae2129cf485fbfc5bd219d01314c'}
helper='''package org.beckenburg;
public class ExportCastleDefaults {public static void main(String[] args)throws Exception {
java.nio.file.Path p=java.nio.file.Path.of(args[0]).toAbsolutePath();
System.setProperty("user.home",p.getParent().resolve("isolated-home").toString());
java.nio.file.Files.createDirectories(p.getParent());var scene=new CastleLevelEdits(p);scene.reset();scene.save();
System.out.println("EXPORTED "+p);}}
'''
manifest={}
for label,ref in refs.items():
    folder=R/'build/castle-native-baselines'/label;folder.mkdir(parents=True,exist_ok=True)
    raw=subprocess.check_output(['git','archive',ref,'src/main/java','water-module/src/main/java','lighting-module/src/main/java'])
    with tarfile.open(fileobj=io.BytesIO(raw)) as archive:
        assert all(Path(m.name).parts[0] in {'src','water-module','lighting-module'} and '..' not in Path(m.name).parts for m in archive.getmembers())
        archive.extractall(folder,filter='data')
    h=folder/'ExportCastleDefaults.java';h.write_text(helper,encoding='utf-8');classes=folder/'classes';classes.mkdir(exist_ok=True)
    sources=[p for base in ['src/main/java','water-module/src/main/java','lighting-module/src/main/java'] for p in (folder/base).rglob('*.java')]+[h];lst=folder/'sources.txt';lst.write_text('\n'.join(p.as_posix() for p in sources),encoding='utf-8')
    subprocess.run([str(java/'javac.exe'),'--release','17','-encoding','UTF-8','-d',str(classes),'@'+str(lst)],check=True)
    target=folder/'defaults.properties';subprocess.run([str(java/'java.exe'),'-Djava.awt.headless=true','-cp',str(classes)+';build/beckenburg-game.jar','org.beckenburg.ExportCastleDefaults',str(target)],check=True)
    data=target.read_bytes().decode('ISO-8859-1');canonical='\n'.join(sorted(line for line in data.splitlines() if line and not line.startswith('#')))+'\n'
    p=OUT/(label+'.properties');p.write_bytes(canonical.encode('ISO-8859-1'));manifest[label]={'source':ref,'path':p.relative_to(R).as_posix(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()}
(OUT/'source-manifest.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8');print('PASS actual original/default baselines',manifest)
