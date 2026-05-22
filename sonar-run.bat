@echo off
REM ═══════════════════════════════════════════════════════════════════════════
REM  CodeSync – SonarQube Analysis Launcher
REM  Usage: sonar-run.bat <sonar-token>
REM  Example: sonar-run.bat sqa_abc123def456
REM ═══════════════════════════════════════════════════════════════════════════

set SONAR_TOKEN=%1

REM ── Locate Maven (tries PATH, then the local Maven Wrapper) ──────────
set MVN_CMD=mvn
where mvn >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    set MVN_CMD=%~dp0auth-service\mvnw.cmd
    if not exist "%~dp0auth-service\mvnw.cmd" (
        echo.
        echo  ERROR: Maven not found. Install Maven or use the Maven Wrapper.
        pause
        exit /b 1
    )
)

if "%SONAR_TOKEN%"=="" (
    echo.
    echo  ERROR: SonarQube token is required.
    echo  Usage: sonar-run.bat ^<sonar-token^>
    echo  Get a token from: http://localhost:9000 -^> My Account -^> Security
    echo.
    pause
    exit /b 1
)

echo.
echo ══════════════════════════════════════════════════════
echo   CodeSync Backend – SonarQube Analysis
echo ══════════════════════════════════════════════════════
echo.

cd /d "%~dp0"

set "JAVA_HOME=%~dp0.jdk\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo [1/2] Running Maven build + tests with JaCoCo Coverage...
call "%MVN_CMD%" clean verify -f pom.xml
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  BUILD FAILED. Fix compilation / test errors before running Sonar.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Sending results to SonarQube...
call "%MVN_CMD%" sonar:sonar -f pom.xml ^
  -Dsonar.token=%SONAR_TOKEN% ^
  -Dsonar.projectKey=codesync-backend ^
  -Dsonar.projectName="CodeSync Backend" ^
  -Dsonar.host.url=http://localhost:9000

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  SONAR ANALYSIS FAILED.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ══════════════════════════════════════════════════════
echo   Analysis complete!
echo   Open: http://localhost:9000/dashboard?id=codesync-backend
echo ══════════════════════════════════════════════════════
echo.
pause
