@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul || (echo Java 17+ JDK required & exit /b 1)
if exist build\classes rmdir /s /q build\classes
if exist build\test rmdir /s /q build\test
if not exist build\classes mkdir build\classes
if not exist build\test mkdir build\test
> build\sources-main.txt (
  for %%F in (src\main\java\hbnet\*.java) do @echo "src/main/java/hbnet/%%~nxF"
  for %%F in (src\main\java\org\beckenburg\*.java) do @echo "src/main/java/org/beckenburg/%%~nxF"
  for %%F in (src\main\java\org\beckenburg\editor\*.java) do @echo "src/main/java/org/beckenburg/editor/%%~nxF"
  for %%F in (src\main\java\org\beckenburg\editor\ai\*.java) do @echo "src/main/java/org/beckenburg/editor/ai/%%~nxF"
  for %%F in (src\main\java\dev\mrcalzon\weather\*.java) do @echo "src/main/java/dev/mrcalzon/weather/%%~nxF"
)
>> build\sources-main.txt (
  for %%F in (water-module\src\main\java\dev\mrcalzon\water\*.java) do @echo "water-module/src/main/java/dev/mrcalzon/water/%%~nxF"
  for %%F in (lighting-module\src\main\java\dev\mrcalzon\lighting\*.java) do @echo "lighting-module/src/main/java/dev/mrcalzon/lighting/%%~nxF"
)
> build\sources-tests.txt (
  for %%F in (src\test\java\hbnet\*.java) do @echo "src/test/java/hbnet/%%~nxF"
  for %%F in (src\test\java\org\beckenburg\*.java) do @echo "src/test/java/org/beckenburg/%%~nxF"
)
javac --release 17 -encoding UTF-8 -d build\classes @build\sources-main.txt || exit /b 1
rem Asset thumbnails are baked from reviewed 3D model captures and shipped inside JAR.
java -Djava.awt.headless=true -cp build\classes org.beckenburg.UiAssetBake . build\classes\org\beckenburg build\Beckenburg.ico || exit /b 1
if not exist build\classes\META-INF mkdir build\classes\META-INF
copy /Y water-module\src\main\resources\META-INF\*.txt build\classes\META-INF\ >nul || exit /b 1
if exist assets\runtime\sounds (
  if not exist build\classes\org\beckenburg\sounds mkdir build\classes\org\beckenburg\sounds
  copy /Y assets\runtime\sounds\*.sounds build\classes\org\beckenburg\sounds\ >nul
)
if exist assets\runtime\water (
  if not exist build\classes\org\beckenburg\water mkdir build\classes\org\beckenburg\water
  copy /Y assets\runtime\water\*.water build\classes\org\beckenburg\water\ >nul
)
if exist assets\runtime\mission-board (
  if not exist build\classes\org\beckenburg\mission-board mkdir build\classes\org\beckenburg\mission-board
  copy /Y assets\runtime\mission-board\*.png build\classes\org\beckenburg\mission-board\ >nul || exit /b 1
)
if exist assets\runtime\water-imports (
  if not exist build\classes\org\beckenburg\water-imports mkdir build\classes\org\beckenburg\water-imports
  copy /Y assets\runtime\water-imports\*.png build\classes\org\beckenburg\water-imports\ >nul
)
if not exist assets\runtime\directional-sprites.zip (echo ERROR: required in-game sprite assets missing & exit /b 1)
if not exist build\classes\org\beckenburg mkdir build\classes\org\beckenburg
copy /Y assets\runtime\directional-sprites.zip build\classes\org\beckenburg\directional-sprites.zip >nul || exit /b 1
if not exist assets\runtime\character-directions.zip (echo ERROR: required approved character direction sprites missing & exit /b 1)
copy /Y assets\runtime\character-directions.zip build\classes\org\beckenburg\character-directions.zip >nul || exit /b 1
if not exist assets\runtime\twins-directions.zip (echo ERROR: independent Twins PNGs missing & exit /b 1)
copy /Y assets\runtime\twins-directions.zip build\classes\org\beckenburg\twins-directions.zip >nul || exit /b 1
if not exist assets\runtime\lore-master-directions.zip (echo ERROR: approved Lore Master sprites missing & exit /b 1)
copy /Y assets\runtime\lore-master-directions.zip build\classes\org\beckenburg\lore-master-directions.zip >nul || exit /b 1
if not exist assets\runtime\castle-support-directions.zip (echo ERROR: Castle NPC approved directions missing & exit /b 1)
copy /Y assets\runtime\castle-support-directions.zip build\classes\org\beckenburg\castle-support-directions.zip >nul || exit /b 1
if not exist build\classes\org\beckenburg\elder-masters mkdir build\classes\org\beckenburg\elder-masters
for %%G in (elder-blade-demon elder-plague-demon elder-nothic) do (
 if not exist "assets\runtime\elder-masters\%%G.png" (echo ERROR missing Elder master %%G & exit /b 1)
 copy /Y "assets\runtime\elder-masters\%%G.png" "build\classes\org\beckenburg\elder-masters\%%G.png" >nul || exit /b 1
)
rem Reassemble the nine verified sprite payload parts, preserving binary bytes.
for %%N in (00 01 02 03 04 05 06 07 08) do (
  if not exist "assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part%%N" (echo ERROR: missing ghost sprite part %%N & exit /b 1)
)
copy /b "assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part00"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part01"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part02"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part03"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part04"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part05"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part06"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part07"+"assets\runtime\ghost-sprite-parts\ghost-sprite-expansion-v3.zip.part08" "build\classes\org\beckenburg\ghost-sprite-expansion-v3.zip" >nul || exit /b 1
if not exist assets\runtime\ui\psx-horror-ui-free-assets.zip (echo ERROR: PSX Horror UI archive missing & exit /b 1)
copy /Y assets\runtime\ui\psx-horror-ui-free-assets.zip build\classes\org\beckenburg\psx-horror-ui-free-assets.zip >nul || exit /b 1
rem Ship all ten background scenes in the actual desktop JAR (not the art review folder).
if not exist build\classes\org\beckenburg\menu-backgrounds mkdir build\classes\org\beckenburg\menu-backgrounds
set /a BECK_MENU_BG_COUNT=0
for %%B in (art\menu-backgrounds\batch-01\*_menu_1920x1080.jpg) do (
  if not exist "%%~fB" (echo ERROR: Missing menu scene %%B & exit /b 1)
  copy /Y "%%~fB" "build\classes\org\beckenburg\menu-backgrounds\%%~nxB" >nul || exit /b 1
  set /a BECK_MENU_BG_COUNT+=1 >nul
)
if not "%BECK_MENU_BG_COUNT%"=="10" (echo ERROR: Expected exactly ten menu backgrounds & exit /b 1)
if not exist build\classes\org\beckenburg\music mkdir build\classes\org\beckenburg\music
for %%T in (ambient-cold-unresolved-drone.zip ambient-sombra-profunda.zip castle-candlelit-shadows.zip hunt-final-surge.zip ritual-final-strike.zip.0 ritual-final-strike.zip.1 ritual-of-ash.zip.0 ritual-of-ash.zip.1) do (
  if not exist "assets\runtime\music\%%T" (echo ERROR: required music archive %%T missing & exit /b 1)
  copy /Y "assets\runtime\music\%%T" "build\classes\org\beckenburg\music\%%T" >nul || exit /b 1
)
rem Optional CC0 NOX Foley subset: only curated, converted WAVs; no source archive.
rem Preserve all twelve original WAVs and twenty-five textured weather VFX GLBs.
if not exist build\classes\org\beckenburg\weather mkdir build\classes\org\beckenburg\weather
for %%W in (audio_00 audio_01 audio_02 audio_03 audio_04 audio_05 atmosphere cloud_to_ground fork horizontal_arc sheet) do (
  if not exist "assets\runtime\weather\%%W.zip" (echo ERROR: missing Shared PSX Weather v1 pack %%W & exit /b 1)
  copy /Y "assets\runtime\weather\%%W.zip" "build\classes\org\beckenburg\weather\%%W.zip" >nul || exit /b 1
)
if exist assets\runtime\sfx (
  if not exist build\classes\org\beckenburg\sfx mkdir build\classes\org\beckenburg\sfx
  for %%S in (assets\runtime\sfx\*.wav) do (
    if exist "%%~fS" copy /Y "%%~fS" "build\classes\org\beckenburg\sfx\%%~nxS" >nul || exit /b 1
  )
)
if not exist assets\runtime\site-geometry.txt (echo ERROR: site geometry manifest missing & exit /b 1)
if not exist build\classes\org\beckenburg\sites mkdir build\classes\org\beckenburg\sites
for /f "usebackq delims=" %%S in ("assets\runtime\site-geometry.txt") do (
  if not exist "assets\runtime\sites\%%S" (echo ERROR: missing native site geometry %%S & exit /b 1)
  copy /Y "assets\runtime\sites\%%S" "build\classes\org\beckenburg\sites\%%S" >nul || exit /b 1
)
if not exist build\classes\org\beckenburg\models mkdir build\classes\org\beckenburg\models
for %%M in (light_switch_1 ceiling_lamp_1_off door_1_loose shelf_mp_6 chair_mp_5 ceiling_lamp_1_on cross_mp_2 flashlight_mp_1 armchair_1 sofa_1 dinner_table_1 wardrobe_mp_1 display_cabinet_mp_2 tv_mp_1 tv_table_1_4 lamp_3_on lamp_3_off ceiling_lamp_4_on ceiling_lamp_4_off pc_keyboard_mp_1 vape_mp_1 dosimeter_1 dosimeter_2 walkie_talkie_2 handheld_tablet_1 monitoring_screen_mp_1 pc_mp_1 button_12 book_mp_1 open_book_mp_1 dosimeter_4 walkie_talkie_1 exploration_emf_detector exploration_flashlight exploration_flashlight_poquet exploration_photo_camera exploration_spirit_box exploration_thermal_camera exploration_thermometer exploration_voice_recorder ggbot_car08_van_shell ggbot_car01 ggbot_car05 ggbot_car07 kenney_road_asphalt_straight kenney_road_asphalt_side kenney_road_asphalt_damaged kenney_detail_light_single kenney_detail_dumpster_closed kenney_detail_bench kenney_detail_barrier_type_a blender_sound_recorder_1 blender_sound_recorder_2 blender_sound_recorder_3) do (
  if not exist assets\runtime\models\%%M.glb (echo ERROR: required licensed model %%M missing & exit /b 1)
  copy /Y assets\runtime\models\%%M.glb build\classes\org\beckenburg\models\%%M.glb >nul || exit /b 1
)
rem Include the 150 individually curated Blender equipment models and state forms.
rem PackagedSourceModelsTest checks that every one is approved, loaded and textured.
for %%M in (assets\\runtime\\models\\blender_*.glb) do (
  if not exist "%%M" (echo ERROR: required authored equipment model %%M missing & exit /b 1)
  copy /Y "%%M" "build\\classes\\org\\beckenburg\\models\\%%~nxM" >nul || exit /b 1
)
rem Authored level scenery and original native-resolution wall/floor images.
for %%M in (bed_4 bed_5 bed_1 bed_2 bedside_table_1 toilet_mp_1 carpet_mp_1 photo_frame_mp_1 painting_1 clock_1 wooden_crate_1 pipe_1 book_mp_2 plate_mp_1 lamp_1_on lamp_1_off lamp_2_on lamp_2_off lamp_5_on lamp_5_off ceiling_lamp_2_on ceiling_lamp_2_off ceiling_lamp_mp_1_on ceiling_lamp_mp_1_off) do (
  if not exist assets\runtime\models\%%M.glb (echo ERROR: missing level scenery %%M & exit /b 1)
  copy /Y assets\runtime\models\%%M.glb build\classes\org\beckenburg\models\%%M.glb >nul || exit /b 1
)
for %%M in (assets\runtime\models\level_*.glb) do copy /Y "%%M" "build\classes\org\beckenburg\models\%%~nxM" >nul || exit /b 1
for /f "usebackq delims=" %%M in ("assets\runtime\level-clutter-models.txt") do copy /Y "assets\runtime\models\%%M.glb" "build\classes\org\beckenburg\models\%%M.glb" >nul || exit /b 1
if not exist build\classes\org\beckenburg\level-textures mkdir build\classes\org\beckenburg\level-textures
copy /Y assets\runtime\level-textures\*.png build\classes\org\beckenburg\level-textures\ >nul || exit /b 1
rem Author all 126 tiered PSX GLBs and embedded low-resolution texture atlases.
java -Djava.awt.headless=true -cp build\classes org.beckenburg.TierModelForge build\classes\org\beckenburg\models || exit /b 1
rem Extract only the curated CC0 castle GLBs with embedded original texture PNGs.
java -Djava.awt.headless=true -cp build\classes org.beckenburg.KenneyMedievalAssets assets\source\kenney_retro-medieval-kit.zip build\classes\org\beckenburg\models || exit /b 1
rem GGBot source meshes are approved GLBs already committed in assets\runtime\models.
rem Regular game builds package the checked-in models above; NEVER invoke
rem Blender or download original design archives. Source re-authoring is separate.

if /I "%~1"=="package" goto package_jar_only
javac --release 17 -encoding UTF-8 -cp build\classes -d build\test @build\sources-tests.txt || exit /b 1
rem E2E has exactly one blocking criterion: the actual packaged TCP investigation.
rem Art/audio/balance diagnostics execute in the separate non-blocking workflow step.
if /I "%~1"=="e2e" goto package_jar_only
java -Djava.awt.headless=true -ea -cp build\classes;build\test org.beckenburg.SpiritBoxResponsesTest || exit /b 1
java -Djava.awt.headless=true -ea -cp build\classes;build\test org.beckenburg.SoundCoverageTest || exit /b 1
java -Djava.awt.headless=true -ea -cp build\classes;build\test org.beckenburg.AudioDeviceRecoveryTest || exit /b 1
java -Djava.awt.headless=true -ea -cp build\classes;build\test org.beckenburg.EnvironmentAudioTest || exit /b 1
rem Verify authored multi-storey levels before the historical release suite.
for %%T in (MergedMaterialMetadataTest HollowCreekFacilitiesAssetTest HollowCreekPavilionAssetTest HollowCreekRefinementAssetsTest HidingBudgetTest HollowCreekDockAssetsTest HollowCreekTentAssetsTest HollowCreekBoundaryAssetsTest HollowCreekTerrainAssetTest TerrainFoundationAssetsTest DotsRayFieldTest OpticalEvidenceTest LanOpticalEvidenceNetworkTest BlackpineLightingTest BlackpineDeckDepthTest AuthoredLevelPhysicsTest AuthoredLevelReachabilityTest AuthoredLevelStateTest AuthoredLevelNetworkTest AuthoredLevelRitualCaseTest AuthoredUtilitiesTest SoloElevationTest EquipmentAssetAssignmentsTest LevelRenderAcceptanceTest WorldViewHandednessTest LevelSurfaceIntegrityTest LivedLevelDressingTest LooseClutterTest LooseClutterNetworkTest HauntedFixturesTest VanHumanScaleTest VanRackStockTest VanRackNetworkTest InteractionFocusTest InteractionNetworkTest HeldDeviceReadoutTest FixtureSoundscapeTest DomesticFitoutTest LevelEnclosureTest PhysicalLightingTest SurroundingClearanceTest) do java -Djava.awt.headless=true -ea -cp build\classes;build\test org.beckenburg.%%T || exit /b 1
rem Both release and E2E verify the same freshly assembled jar payload.
:package_jar_only
if not exist build\classes\org\beckenburg mkdir build\classes\org\beckenburg
if defined BECKENBURG_RELEASE_VERSION (
  > build\classes\org\beckenburg\release.properties echo version=%BECKENBURG_RELEASE_VERSION%
  >> build\classes\org\beckenburg\release.properties echo source_sha=%BECKENBURG_SOURCE_SHA%
)
rem Ship the independently compiled WATER module as well as the weather/game classes.
rem Without this directory, building an authored House from the packaged JAR
rem fails with NoClassDefFoundError: dev/mrcalzon/water/WaterDocument.
jar --create --file build\beckenburg-game.jar --main-class org.beckenburg.InvestigationGame -C build\classes hbnet -C build\classes org\beckenburg -C build\classes dev\mrcalzon\weather -C build\classes dev\mrcalzon\water -C build\classes dev\mrcalzon\lighting || exit /b 1
rem Fail before any packaged game smoke if runtime module classes are missing.
jar --list --file build\beckenburg-game.jar | findstr /C:"dev/mrcalzon/lighting/LightingModule.class" >nul || (echo ERROR: lighting module missing from game JAR & exit /b 1)
jar --list --file build\beckenburg-game.jar | findstr /C:"dev/mrcalzon/water/WaterDocument.class" >nul || (echo ERROR: packaged water document missing from game JAR & exit /b 1)
jar --list --file build\beckenburg-game.jar | findstr /C:"dev/mrcalzon/water/WaterModule.class" >nul || (echo ERROR: packaged water simulation missing from game JAR & exit /b 1)
rem Offline verification from assembled JAR in every Windows packaging mode.
java -Djava.awt.headless=true -cp build\beckenburg-game.jar org.beckenburg.PackagedAudioAudit || exit /b 1
if /I "%~1"=="package" goto packaged
if /I "%~1"=="e2e" goto e2e
rem Package mode builds the complete jar without asserting test acceptance.
rem Normal build mode still runs the complete regression manifest.
:packaged
if /I "%~1"=="package" exit /b 0
rem Release mode never skips any tests in regression-tests.txt.
java -Djava.awt.headless=true -ea -cp build\classes org.beckenburg.BeckenburgRegressionSuite || exit /b 1
echo Built build\beckenburg-game.jar
if /I "%~1"=="run" java -jar build\beckenburg-game.jar
exit /b 0
:e2e
rem Objectively functional packaged multiplayer acceptance. Visual NPC art, lore,
rem subjective aesthetics and balance checks must not prevent game releases.
if not exist build\e2e mkdir build\e2e
java -Djava.awt.headless=true -ea -cp build\beckenburg-game.jar;build\test org.beckenburg.BeckenburgFullRoundE2ETest || exit /b 1
echo Verified packaged jar through independent TCP multiplayer end-to-end case
exit /b 0
