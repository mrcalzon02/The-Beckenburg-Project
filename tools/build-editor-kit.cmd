@echo off
setlocal
cd /d "%~dp0.."
where javac >nul 2>nul || (echo Java 17 JDK required & exit /b 1)
where jar >nul 2>nul || (echo Java 17 jar required & exit /b 1)
if exist build\editor-kit\classes rmdir /s /q build\editor-kit\classes
if not exist build\editor-kit\classes mkdir build\editor-kit\classes
javac --release 17 -encoding UTF-8 -d build\editor-kit\classes src\main\java\org\beckenburg\editor\*.java || exit /b 1
jar --create --file build\editor-kit\LevelEditorKit-Java17.jar -C build\editor-kit\classes org\beckenburg\editor || exit /b 1
if not exist build\editor-kit\LevelEditorKit-Java17.jar exit /b 1
echo Built standalone build\editor-kit\LevelEditorKit-Java17.jar
