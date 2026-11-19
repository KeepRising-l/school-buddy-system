@echo off
chcp 65001 >nul
title School Buddy System - One-Click Launcher
cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-project.ps1" %*
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" (
    echo [ERROR] Startup aborted due to missing environment or error ^(Exit Code: %EXIT_CODE%^).
    pause
    exit /b %EXIT_CODE%
)

pause
