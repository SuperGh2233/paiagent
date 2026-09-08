@echo off
rem ===========================================================================
rem PaiAgent knowledge-graph dashboard launcher (Windows).
rem Usage:  double-click in repo root, or:
rem         start-dashboard.bat [project-dir] [--port N] [--no-open]
rem Requires: Node.js >= 18. Read-only dashboard for .ua/knowledge-graph.json.
rem Bound to 127.0.0.1 with a per-run access token printed in the URL.
rem Stop: Ctrl+C
rem ===========================================================================

setlocal enabledelayedexpansion

rem ---- script dir = tools/viewer; default project root = its parent ----
set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

rem ---- first non-flag arg = project dir; default = repo root ----
set "PROJECT_ROOT="
for %%A in (%*) do (
  if not defined PROJECT_ROOT (
    if /i not "%%~A"=="--port" if /i not "%%~A"=="--no-open" if /i not "%%~A"=="--help" if /i not "%%~A"=="-h" (
      set "PROJECT_ROOT=%%~fA"
    )
  )
)
if not defined PROJECT_ROOT set "PROJECT_ROOT=%SCRIPT_DIR%\.."

set "VIEWER=%SCRIPT_DIR%\bin\viewer.mjs"

rem ---- preflight ----
if not exist "%VIEWER%" (
  echo [ERROR] Viewer not found: %VIEWER%
  echo         tools/viewer looks incomplete.
  pause
  exit /b 1
)
if not exist "%PROJECT_ROOT%\.ua\knowledge-graph.json" (
  echo [ERROR] No knowledge graph found at %PROJECT_ROOT%\.ua\knowledge-graph.json
  echo         Run /understand first to generate it.
  pause
  exit /b 1
)

rem ---- locate node ----
where node >nul 2>nul
if %errorlevel%==0 (
  set "NODE_CMD=node"
) else (
  if exist "%ProgramFiles%\nodejs\node.exe" (
    set "NODE_CMD=%ProgramFiles%\nodejs\node.exe"
  ) else if exist "%ProgramFiles(x86)%\nodejs\node.exe" (
    set "NODE_CMD=%ProgramFiles(x86)%\nodejs\node.exe"
  ) else (
    echo [ERROR] Node.js ^>=18 not found. Install Node.js first.
    pause
    exit /b 1
  )
)

echo Starting PaiAgent knowledge-graph dashboard...
echo Graph: %PROJECT_ROOT%\.ua\knowledge-graph.json
echo Access token is random per run; the printed URL embeds it.
echo Press Ctrl+C to stop.
echo.

rem ---- launch (foreground). Pass extra args through to node ----
"%NODE_CMD%" "%VIEWER%" "%PROJECT_ROOT%" %*

endlocal
