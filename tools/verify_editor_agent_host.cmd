@echo off
setlocal
cd /d "%~dp0.."
if not exist build\test mkdir build\test
set "BECK_EDITOR_AGENT_CP=build\classes"
if not "%~1"=="" set "BECK_EDITOR_AGENT_CP=%~1"
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\EditorAgentServiceTest.java src\test\java\org\beckenburg\EditorAgentMissionPropsTest.java src\test\java\org\beckenburg\EditorAgentMissionDoorTest.java src\test\java\org\beckenburg\EditorChangeIntegrationTest.java src\test\java\org\beckenburg\EditorDiskRevisionTest.java src\test\java\org\beckenburg\CastleSnapshotContractTest.java src\test\java\org\beckenburg\EditorSchemaCompatibilityTest.java src\test\java\org\beckenburg\EditorAgentGuiTest.java src\test\java\org\beckenburg\AssetImportTopologyGuiTest.java src\test\java\org\beckenburg\AssetImportBoxSelectionGuiTest.java src\test\java\org\beckenburg\AssetImportFaceModelingGuiTest.java src\test\java\org\beckenburg\EditorTopologyWorkspaceGuiTest.java src\test\java\org\beckenburg\AssetImportNormalsGuiTest.java src\test\java\org\beckenburg\AssetImportMaterialsGuiTest.java src\test\java\org\beckenburg\G2TexturedModelGuiTest.java src\test\java\org\beckenburg\editor\EditorGltfFileTest.java src\test\java\org\beckenburg\AssetImportUvGuiTest.java src\test\java\org\beckenburg\AssetImportGlbGuiTest.java src\test\java\org\beckenburg\AssetImportPerspectiveGuiTest.java src\test\java\org\beckenburg\AssetImportSurfaceGuiTest.java src\test\java\org\beckenburg\AssetImportLocalFrameGuiTest.java src\test\java\org\beckenburg\AssetImportBevelGuiTest.java src\test\java\org\beckenburg\ModelEditorGuiTestSupport.java src\test\java\org\beckenburg\AssetImportSlideGuiTest.java src\test\java\org\beckenburg\AssetImportSnapGuiTest.java src\test\java\org\beckenburg\AssetImportWorkspaceGuiTest.java src\test\java\org\beckenburg\editor\EditorModelWorkspaceTest.java src\test\java\org\beckenburg\editor\EditorVertexSnapTest.java src\test\java\org\beckenburg\editor\EditorMeshUpdateTest.java src\test\java\org\beckenburg\editor\EditorMaterialPencilTest.java src\test\java\org\beckenburg\editor\EditorUvRelaxTest.java src\test\java\org\beckenburg\editor\EditorPaintRecoveryTest.java src\test\java\org\beckenburg\AssetImportBoundaryGuiTest.java src\test\java\org\beckenburg\ModelWireframeBufferTest.java src\test\java\org\beckenburg\editor\EditorMeshBevelTest.java src\test\java\org\beckenburg\editor\EditorMeshQuadTest.java src\test\java\org\beckenburg\editor\EditorMeshEdgesTest.java src\test\java\org\beckenburg\GlbSourceAuthorityTest.java src\test\java\org\beckenburg\editor\EditorMeshMaterialTest.java src\test\java\org\beckenburg\editor\EditorGlbAuthoringTest.java src\test\java\org\beckenburg\AssetImportTransformGuiTest.java src\test\java\org\beckenburg\AssetImportMeshRepairGuiTest.java src\test\java\org\beckenburg\AssetImportPublicationRevisionGuiTest.java src\test\java\org\beckenburg\LevelAuthoringIdentityTest.java src\test\java\org\beckenburg\LevelAuthoringRecoveryTest.java src\test\java\org\beckenburg\CastleDocumentRecoveryTest.java src\test\java\org\beckenburg\AssetImportRuntimeTest.java src\test\java\org\beckenburg\CastleEditorGeometryTest.java src\test\java\org\beckenburg\CastleLightingPlacementRepairTest.java src\test\java\org\beckenburg\GroundRecoverySupportParityTest.java src\test\java\org\beckenburg\AuthoredTestSupport.java src\test\java\org\beckenburg\VerificationPreferencesFactory.java src\test\java\org\beckenburg\HouseTemplateCompatibilityTest.java src\test\java\org\beckenburg\HouseRebuildUpgradeTest.java src\test\java\org\beckenburg\HouseRebuildPowerTest.java src\test\java\org\beckenburg\GhostElectricalTest.java src\test\java\org\beckenburg\AuthoredLevelNetworkTest.java src\test\java\org\beckenburg\LanSafeAreaBarrierTest.java src\test\java\org\beckenburg\SafeAreaBarrierTest.java src\test\java\org\beckenburg\EditorSceneRetentionTest.java src\test\java\org\beckenburg\SourceImageSharingTest.java
if errorlevel 1 exit /b 1
for %%T in (GroundRecoverySupportParityTest EditorAgentServiceTest EditorAgentMissionPropsTest EditorAgentMissionDoorTest EditorChangeIntegrationTest EditorDiskRevisionTest CastleSnapshotContractTest EditorSchemaCompatibilityTest LevelAuthoringIdentityTest HouseTemplateCompatibilityTest HouseRebuildUpgradeTest SourceImageSharingTest LevelAuthoringRecoveryTest CastleDocumentRecoveryTest AssetImportRuntimeTest CastleEditorGeometryTest CastleLightingPlacementRepairTest) do (
  java -Djava.awt.headless=true -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.%%T
  if errorlevel 1 exit /b 1
)
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.EditorAgentGuiTest
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\GhostPracticeGuiTest.java
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.GhostPracticeGuiTest
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

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.G2TexturedModelGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportUvGuiTest
if errorlevel 1 exit /b 1

java -Djava.awt.headless=true -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.EditorSceneRetentionTest
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

java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorMeshBevelTest --stress
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportBevelGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportSlideGuiTest
if errorlevel 1 exit /b 1

java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportBoundaryGuiTest
if errorlevel 1 exit /b 1

java -Djava.awt.headless=true -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.ModelWireframeBufferTest
if errorlevel 1 exit /b 1

java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorMeshQuadTest
if errorlevel 1 exit /b 1

java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorMeshEdgesTest --stress
if errorlevel 1 exit /b 1

java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorMeshUpdateTest
if errorlevel 1 exit /b 1
java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorVertexSnapTest --stress
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportSnapGuiTest
if errorlevel 1 exit /b 1

java -Xmx128m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorPaintRecoveryTest
if errorlevel 1 exit /b 1
java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorUvRelaxTest --stress
if errorlevel 1 exit /b 1
java -Xmx64m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorMaterialPencilTest
if errorlevel 1 exit /b 1
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\MaterialReviewMipTest.java
if errorlevel 1 exit /b 1
java -Xmx96m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.MaterialReviewMipTest
if errorlevel 1 exit /b 1
java -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.editor.EditorModelWorkspaceTest
if errorlevel 1 exit /b 1
java -Djava.util.prefs.PreferencesFactory=org.beckenburg.VerificationPreferencesFactory -Xmx256m -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.AssetImportWorkspaceGuiTest
rem Reopen actual historical Castle documents in the packaged Windows host.
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\CastleRebuildUpgradeTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=true -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.CastleRebuildUpgradeTest
if errorlevel 1 exit /b 1

echo PASS native editor service, real human model/material UV editors, recovery and headless protocol

rem Museum records use the real Castle E-key handler in visible native windows.
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\CastleStoryTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=false -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.CastleStoryTest build\editor-agent-evidence\castle-story
if errorlevel 1 exit /b 1

rem Shared loose objects through actual Castle native pickup/rotate/throw keys.
javac --release 17 -encoding UTF-8 -cp "%BECK_EDITOR_AGENT_CP%" -d build\test src\test\java\org\beckenburg\CastleLooseClutterTest.java
if errorlevel 1 exit /b 1
java -Djava.awt.headless=false -ea -cp "%BECK_EDITOR_AGENT_CP%;build\test" org.beckenburg.CastleLooseClutterTest build\editor-agent-evidence\castle-loose-ui
if errorlevel 1 exit /b 1
