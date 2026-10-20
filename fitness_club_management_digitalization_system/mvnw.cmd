@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, Windows version
@REM ----------------------------------------------------------------------------

@ECHO OFF
SETLOCAL

set ERROR_CODE=0

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=.
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

set MAVEN_OPTS=

if not exist %WRAPPER_JAR% (
  echo Maven Wrapper jar not found: %WRAPPER_JAR%
  echo Please run this script from the project root.
  exit /b 1
)

set JAVA_EXE=java.exe
if defined JAVA_HOME set JAVA_EXE="%JAVA_HOME%\bin\java.exe"

%JAVA_EXE% %MAVEN_OPTS% -classpath %WRAPPER_JAR% -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 set ERROR_CODE=%ERRORLEVEL%

ENDLOCAL & exit /b %ERROR_CODE%

