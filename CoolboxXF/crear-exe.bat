@echo off
chcp 65001 >nul
setlocal
cd /d "%~dp0"

echo ==== 1/2 Compilando con Maven ====
call mvn -q clean package -DskipTests
if errorlevel 1 ( echo ERROR en Maven & pause & exit /b 1 )

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
  echo ERROR: JAVA_HOME no apunta a un JDK 21 con jpackage. JAVA_HOME=%JAVA_HOME%
  pause & exit /b 1
)

echo ==== 2/2 Generando SysVentas.exe con jpackage ====
if exist dist rmdir /s /q dist
"%JAVA_HOME%\bin\jpackage.exe" ^
  --type app-image ^
  --name SysVentas ^
  --input target\input ^
  --main-jar SysVentas.jar ^
  --main-class pe.edu.upeu.sysventas.App ^
  --dest dist ^
  --add-modules java.base,java.sql,java.naming,java.desktop,java.prefs,java.management,jdk.management,java.xml,java.logging,java.net.http,jdk.unsupported,jdk.crypto.ec,jdk.localedata
if errorlevel 1 ( echo ERROR en jpackage & pause & exit /b 1 )

echo.
echo LISTO: dist\SysVentas\SysVentas.exe
echo Copia TODA la carpeta dist\SysVentas para llevarla a otra PC.
pause
