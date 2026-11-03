@echo off
setlocal
cd /d "%~dp0.."
if not exist build\test mkdir build\test
set "BECK_EDITOR_AGENT_CP=build\classes"
if not "%~1"=="" set "BECK_EDITOR_AGENT_CP=%~1"
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\EditorAgentServiceTest.java src\test\java\org\beckenburg\EditorChangeIntegrationTest.java src\test\java\org\beckenburg\EditorDiskRevisionTest.java src\test\java\org\beckenburg\CastleSnapshotContractTest.java src\test\java\org\beckenburg\EditorSchemaCompatibilityTest.java src\test\java\org\beckenburg\EditorAgentGuiTest.java src\test\java\org\beckenburg\AssetImportTopologyGuiTest.java src\test\java\org\beckenburg\AssetImportBoxSelectionGuiTest.java src\test\java\org\beckenburg\AssetImportFaceModelingGuiTest.java src\test\java\org\beckenburg\EditorTopologyWorkspaceGuiTest.java src\test\java\org\beckenburg\AssetImportNormalsGuiTest.java src\test\java\org\beckenburg\AssetImportMaterialsGuiTest.java src\test\java\org\beckenburg\AssetImportUvGuiTest.java src\test\java\org\beckenburg\AssetImportGlbGuiTest.java src\test\java\org\beckenburg\AssetImportPerspectiveGuiTest.java src\test\java\org\beckenburg\AssetImportSurfaceGuiTest.java src\test\java\org\beckenburg\AssetImportLocalFrameGuiTest.java src\test\java\org\beckenburg\GlbSourceAuthorityTest.java src\test\java\org\beckenburg\editor\EditorMeshMaterialTest.java src\test\java\org\beckenburg\editor\EditorGlbAuthoringTest.java src\test\java\org\beckenburg\AssetImportTransformGuiTest.java src\test\java\org\beckenburg\AssetImportMeshRepairGuiTest.java src\test\java\org\beckenburg\AssetImportPublicationRevisionGuiTest.java src\test\java\org\beckenburg\LevelAuthoringIdentityTest.java src\test\java\org\beckenburg\LevelAuthoringRecoveryTest.java src\test\java\org\beckenburg\CastleDocumentRecoveryTest.java src\test\java\org\beckenburg\AssetImportRuntimeTest.java src\test\java\org\beckenburg\CastleEditorGeometryTest.java src\test\java\org\beckenburg\CastleLightingPlacementRepairTest.java src\test\java\org\beckenburg\GroundRecoverySupportParityTest.java src\test\java\org\beckenburg\AuthoredTestSupport.java src\test\java\org\beckenburg\VerificationPreferencesFactory.java
if errorlevel 1 exit /b 1
for %%T in (GroundRecoverySupportParityTest EditorAgentServiceTest EditorChangeIntegrationTest EditorDiskRevisionTest CastleSnapshotContractTest EditorSchemaCompatibilityTest LevelAuthoringIdentityTest LevelAuthoringRecoveryTest CastleDocumentRecoveryTest AssetImportRuntimeTest CastleEditorGeometryTest CastleLightingPlacementRepairTest) do (
  java -Djava.awt.headless=true -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.%%T
  if errorlevel 1 exit /b 1
)
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.EditorAgentGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportTopologyGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportBoxSelectionGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportFaceModelingGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportPublicationRevisionGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportMeshRepairGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportTransformGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.EditorTopologyWorkspaceGuiTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportNormalsGuiTest
if errorlevel 1 exit /b 1
python tools\verify_editor_agent_cli.py --classpath "%BECK_EDITOR_AGENT_CP%;build\test"
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportMaterialsGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportUvGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportGlbGuiTest
if errorlevel 1 exit /b 1
java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.GlbSourceAuthorityTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportPerspectiveGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportSurfaceGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportLocalFrameGuiTest
if errorlevel 1 exit /b 1

echo PASS native editor service, real human model/material UV editors, recovery and headless protocol
