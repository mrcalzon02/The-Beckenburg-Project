@echo off
setlocal
cd /d "%~dp0.."
if not exist build\test mkdir build\test
set "BECK_EDITOR_AGENT_CP=build\classes"
if not "%~1"=="" set "BECK_EDITOR_AGENT_CP=%~1"
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\EditorAgentServiceTest.java src\test\java\org\beckenburg\EditorAgentGuiTest.java src\test\java\org\beckenburg\LevelAuthoringIdentityTest.java src\test\java\org\beckenburg\LevelAuthoringRecoveryTest.java src\test\java\org\beckenburg\CastleDocumentRecoveryTest.java src\test\java\org\beckenburg\CastleEditorGeometryTest.java src\test\java\org\beckenburg\CastleLightingPlacementRepairTest.java src\test\java\org\beckenburg\GroundRecoverySupportParityTest.java src\test\java\org\beckenburg\AuthoredTestSupport.java src\test\java\org\beckenburg\VerificationPreferencesFactory.java
if errorlevel 1 exit /b 1
for %%T in (GroundRecoverySupportParityTest EditorAgentServiceTest LevelAuthoringIdentityTest LevelAuthoringRecoveryTest CastleDocumentRecoveryTest CastleEditorGeometryTest CastleLightingPlacementRepairTest) do (
  java -Djava.awt.headless=true -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.%%T
  if errorlevel 1 exit /b 1
)
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.EditorAgentGuiTest
if errorlevel 1 exit /b 1
python tools\verify_editor_agent_cli.py --classpath "%BECK_EDITOR_AGENT_CP%;build\test"
if errorlevel 1 exit /b 1
echo PASS native editor service, real human editor, recovery and headless protocol
