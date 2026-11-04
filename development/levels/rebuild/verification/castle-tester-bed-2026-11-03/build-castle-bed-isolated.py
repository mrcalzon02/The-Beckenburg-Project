import pathlib,subprocess,tarfile,io,shutil,zipfile,json,hashlib
root=pathlib.Path.cwd();out=root/'build/castle-bed-isolated';out.mkdir(exist_ok=True)
base=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
source=out/'source';source.mkdir(exist_ok=True)
raw=subprocess.check_output(['git','archive',base,'src','water-module/src','lighting-module/src','regression-tests.txt'])
with tarfile.open(fileobj=io.BytesIO(raw)) as tar:tar.extractall(source,filter='data')
overlays=['src/main/java/org/beckenburg/CastleUpper.java','src/main/java/org/beckenburg/LevelAssets.java','src/main/java/org/beckenburg/BeckenburgRegressionSuite.java','src/test/java/org/beckenburg/CastleTesterBedTest.java','src/test/java/org/beckenburg/PreparedLightBatchParityTest.java','regression-tests.txt']
for f in overlays:(source/f).parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/f,source/f)
classes=out/'build/classes';tests=out/'build/test';classes.mkdir(parents=True,exist_ok=True);tests.mkdir(parents=True,exist_ok=True)
main=[*list((source/'src/main/java').rglob('*.java')),*list((source/'water-module/src/main/java').rglob('*.java')),*list((source/'lighting-module/src/main/java').rglob('*.java'))]
test=list((source/'src/test/java/hbnet').glob('*.java'))+list((source/'src/test/java/org/beckenburg').glob('*.java'))+[source/'src/test/java/org/beckenburg/editor/EditorGlbAuthoringTest.java',source/'src/test/java/org/beckenburg/editor/EditorMeshMaterialTest.java']
(out/'main.txt').write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in main),encoding='utf-8')
(out/'tests.txt').write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in test),encoding='utf-8')
javac=r'C:\Program Files\Java\jdk-17\bin\javac.exe'
subprocess.run([javac,'--release','17','-encoding','UTF-8','-d',str(classes),'@'+str(out/'main.txt')],check=True)
subprocess.run([javac,'--release','17','-encoding','UTF-8','-cp',str(classes),'-d',str(tests),'@'+str(out/'tests.txt')],check=True)
jar=out/'castle-bed-verified.jar'
with zipfile.ZipFile(root/'build/beckenburg-game.jar') as src,zipfile.ZipFile(jar,'w',compression=zipfile.ZIP_DEFLATED) as dest:
 for i in src.infolist():
  if not i.filename.endswith('.class') and not i.is_dir():dest.writestr(i,src.read(i))
 for p in classes.rglob('*.class'):dest.write(p,p.relative_to(classes).as_posix())
with zipfile.ZipFile(jar) as z:assert z.read('org/beckenburg/models/level_castle_four_poster_bed.glb')==(root/'assets/runtime/models/level_castle_four_poster_bed.glb').read_bytes()
(out/'provenance.json').write_text(json.dumps({'base_commit':base,'source_overlays':{f:hashlib.sha256((root/f).read_bytes()).hexdigest() for f in overlays},'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'purpose':'Isolated baseline main/test compilation with only Castle bed and parity-diagnostic changes; shared runtime resource JAR copied without any classes; other task left untouched'},indent=2)+'\n')
print('PASS isolated current-scope build',len(main),'main sources',len(test),'tests; no concurrent safe-area source/classes',flush=True)
