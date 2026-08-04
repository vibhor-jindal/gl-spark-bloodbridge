@echo off
REM Convenience wrapper — opens start-all.ps1
setlocal
cd /d "%~dp0.."
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-all.ps1" %*
