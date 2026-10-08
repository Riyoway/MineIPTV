@echo off
setlocal
set GRADLE_VERSION=9.6.0
set ROOT=%~dp0
set CACHE=%ROOT%.gradle-bin
set DIST=%CACHE%\gradle-%GRADLE_VERSION%

where java >nul 2>nul || (
  echo Java 25 is required.
  exit /b 1
)

if not exist "%DIST%\bin\gradle.bat" (
  echo Downloading Gradle %GRADLE_VERSION%...
  if not exist "%CACHE%" mkdir "%CACHE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%CACHE%\gradle.zip'; Expand-Archive -Force '%CACHE%\gradle.zip' '%CACHE%'"
  if errorlevel 1 exit /b 1
)

call "%DIST%\bin\gradle.bat" build
endlocal
