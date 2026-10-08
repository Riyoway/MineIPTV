@echo off
setlocal
set GRADLE_VERSION=9.6.0
set ROOT=%~dp0
set CACHE=%ROOT%.gradle-bin
set DIST=%CACHE%\gradle-%GRADLE_VERSION%

if not exist "%DIST%\bin\gradle.bat" call "%ROOT%build.bat"
if not exist "%DIST%\bin\gradle.bat" exit /b 1
call "%DIST%\bin\gradle.bat" runClient
endlocal
