@echo off
setlocal

set "APP_HOME=%~dp0"

if exist "%APP_HOME%runtime\bin\javaw.exe" (
    set "JAVA_EXE=%APP_HOME%runtime\bin\javaw.exe"
) else if exist "%APP_HOME%desktopApp 0.8\runtime\bin\javaw.exe" (
    set "JAVA_EXE=%APP_HOME%desktopApp 0.8\runtime\bin\javaw.exe"
) else (
    set "JAVA_EXE=javaw.exe"
)

set "CLASSPATH=%APP_HOME%lib\*"

:: Opciones JVM optimizadas para Intel Atom N455 / Canaima Windows 7 SP1 (1GB-2GB RAM)
:: -Xms16m -Xmx96m: Limita memoria Java para evitar swapping a disco lento
:: -XX:+UseSerialGC: Recolector ligero de 1 hilo (no roba CPU en Atom)
:: -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true: Evita fugas y crashes en Intel GMA 3150
start "" "%JAVA_EXE%" -Xms16m -Xmx96m -XX:+UseSerialGC -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=20 -Dsun.java2d.d3d=false -Dsun.java2d.noddraw=true -Dfile.encoding=UTF-8 -cp "%CLASSPATH%" net.newpipe.app.Win7Main %*
exit /b 0
