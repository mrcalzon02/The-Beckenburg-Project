@echo off
setlocal
cd /d "%~dp0"
if not exist "build\beckenburg-game.jar" (
 echo Missing packaged JAR. Run build.cmd e2e once or use the installed game's --dev-room flag.
 exit /b 1
)
java -Djava.awt.headless=false -cp "build\beckenburg-game.jar" org.beckenburg.DevAssetShowroom --dev-room %*
exit /b %ERRORLEVEL%
