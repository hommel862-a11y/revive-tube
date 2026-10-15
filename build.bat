@echo off
setlocal
echo ==============================================
echo Compilando YouTube Lite para Windows 7 / Canaima
echo ==============================================

set "JAVA_HOME=E:\jdk21\jdk-21.0.12.1+1"
if not exist "%JAVA_HOME%\bin\javac.exe" (
    set "JAVA_HOME=%~dp0runtime"
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Error: No se encontro javac.exe en %JAVA_HOME%
    exit /b 1
)

set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAR=%JAVA_HOME%\bin\jar.exe"
set "SRC_DIR=%~dp0src\main\java"
set "OUT_DIR=%~dp0build\classes"
set "LIB_DIR=%~dp0lib"

if not exist "%OUT_DIR%" mkdir "%OUT_DIR%"

powershell -NoProfile -Command "(Get-ChildItem -Path '%SRC_DIR%\*.java' -Recurse | ForEach-Object { '\"' + ($_.FullName.Replace('\', '/')) + '\"' }) | Set-Content '%OUT_DIR%\sources.txt' -Encoding ASCII"

echo Compilando codigo fuente Java...
"%JAVAC%" -encoding UTF-8 -cp "%LIB_DIR%\*" -d "%OUT_DIR%" @"%OUT_DIR%\sources.txt"
if errorlevel 1 (
    echo Error en la compilacion.
    exit /b 1
)

echo Empaquetando desktopApp.jar...
"%JAR%" cfe "%LIB_DIR%\desktopApp.jar" net.newpipe.app.Win7Main -C "%OUT_DIR%" .
if errorlevel 1 (
    echo Error al empaquetar jar.
    exit /b 1
)

echo.
echo ==============================================
echo Compilacion exitosa: %LIB_DIR%\desktopApp.jar
echo ==============================================
exit /b 0
