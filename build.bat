@echo off
setlocal
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0gradle-bootstrap.ps1" :neoforge:build --no-daemon %*
exit /b %ERRORLEVEL%
