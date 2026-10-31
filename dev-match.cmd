@echo off
setlocal
cd /d "%~dp0"
rem Developer-only hidden eighth-role observer. Requires checkout build output.
if not exist "build\beckenburg-game.jar" (
  echo Beckenburg game JAR missing. Run build.cmd e2e once from this checkout.
  exit /b 1
)
echo Launching private seven-client AI match with ADMIN ^(YOU^) observer...
echo Controls: 1-6 follow hunters, 7 follow ghost, 0 free, WASD, Space/Ctrl, F, M, X.
java -Djava.awt.headless=false -ea -cp "build\beckenburg-game.jar" org.beckenburg.DevMatchSession --dev-ai-match --admin %*
if errorlevel 1 (
  echo Development match failed. Check build\dev-match\result.txt and workflow diagnostics.
  exit /b 1
)
endlocal
