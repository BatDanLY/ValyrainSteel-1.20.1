@echo off
setlocal

set "SOURCE=C:\programmering JAVA\ValyrainSteelV2\build\libs\valyrainsteel-1.0-SNAPSHOT.jar"
set "DEST=C:\Users\mrdan\AppData\Roaming\ModrinthApp\profiles\GOT Testing\mods\"

if not exist "%SOURCE%" (
    echo ERROR: Source file not found:
    echo %SOURCE%
    pause
    exit /b 1
)

copy /Y "%SOURCE%" "%DEST%"

if errorlevel 1 (
    echo Failed to copy file.
    pause
    exit /b 1
)

echo Successfully copied and overwrote the existing JAR.