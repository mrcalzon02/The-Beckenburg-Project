@echo off
setlocal
set "hunter_game=%~dp0build\playable-hunters\Beckenburg-Hunters\Beckenburg-Hunters.exe"
if not exist "%hunter_game%" (
  echo The local playable hunter build is missing.
  echo See docs\development\PLAYABLE_HUNTER_ANIMATION.md for build instructions.
  exit /b 1
)
start "" /D "%~dp0" "%hunter_game%" %*
endlocal
