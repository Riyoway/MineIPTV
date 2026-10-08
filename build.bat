@echo off
setlocal
cd /d "%~dp0"
set TARGET=%~1
if "%TARGET%"=="" set TARGET=all
if /I "%TARGET%"=="all" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" :fabric:build :neoforge:build :forge:build --no-daemon
) else if /I "%TARGET%"=="fabric" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" :fabric:build --no-daemon
) else if /I "%TARGET%"=="neoforge" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" :neoforge:build --no-daemon
) else if /I "%TARGET%"=="forge" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" :forge:build --no-daemon
) else (
  echo Usage: build.bat [all^|fabric^|neoforge^|forge]
  exit /b 2
)
exit /b %ERRORLEVEL%
