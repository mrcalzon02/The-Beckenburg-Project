import subprocess,pathlib
java=pathlib.Path('C:/Program Files/Java/jdk-17/bin')
subprocess.run([str(java/'javac.exe'),'--release','17','-encoding','UTF-8','-cp','build/classes','-d','build/classes','src/main/java/org/beckenburg/CastleFinish.java'],check=True)
subprocess.run([str(java/'jar.exe'),'--update','--file','build/beckenburg-game.jar','-C','build/classes','org/beckenburg/CastleFinish.class','-C','build/classes','org/beckenburg/CastleFinish$Fitting.class'],check=True)
print('PASS final source compiled and packaged CastleFinish delta after kitchen switch clearance correction')
subprocess.run([str(java/'java.exe'),'-ea','-cp','build/beckenburg-game.jar;build/test','org.beckenburg.CastleFinishPlacementTest','development/levels/rebuild/captures/castle-construction'],check=True)
