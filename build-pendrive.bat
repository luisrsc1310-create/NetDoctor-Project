@echo off
title NetDiag Pro - Build EXE Pendrive

echo ============================================================
echo   NetDiag Pro - Gerador de EXE Portavel para Pendrive
echo   Resultado: pendrive-dist\NetDiag Pro\NetDiag Pro.exe
echo ============================================================
echo.

:: ============================================================
:: CONFIGURACAO DIRETA (caminhos fixos, sem depender de deteccao)
:: ============================================================

:: Java 26 (do IntelliJ) - usado apenas pelo jpackage
set "JAVA_HOME=C:\Users\PC\.jdks\openjdk-26.0.1"

:: Java 21 (instalado no sistema) - usado pelo Maven para compilar
set "JAVA_HOME_MVN=C:\Program Files\Java\jdk-21.0.12.1"

set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "PROJECT_DIR=%~dp0"

:: Remove barra final do PROJECT_DIR se houver
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

:: ============================================================
:: VALIDACOES
:: ============================================================

echo [1/4] Verificando pre-requisitos...

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo [ERRO] JDK nao encontrado em: %JAVA_HOME%
    echo        Ajuste a variavel JAVA_HOME neste script.
    goto :erro
)
echo       JDK: %JAVA_HOME%

if not exist "%MVN_CMD%" (
    echo [ERRO] Maven nao encontrado em: %MVN_CMD%
    echo        Ajuste a variavel MVN_CMD neste script.
    goto :erro
)
echo       Maven: OK
echo.

:: ============================================================
:: COMPILACAO
:: ============================================================

echo [2/4] Compilando projeto com Maven...
echo.

cd /d "%PROJECT_DIR%"
set "JAVA_HOME=%JAVA_HOME_MVN%"
"%MVN_CMD%" clean package -q
set "JAVA_HOME=C:\Users\PC\.jdks\openjdk-26.0.1"
echo Maven terminou.

if not exist "%PROJECT_DIR%\target\net-diagnostico-1.0.0.jar" (
    echo [ERRO] JAR nao foi gerado. Verifique os erros do Maven acima.
    goto :erro
)
echo       JAR gerado OK.
echo.

:: ============================================================
:: PREPARACAO DO INPUT PARA JPACKAGE
:: ============================================================

if exist "%PROJECT_DIR%\target\jpackage-input" rmdir /s /q "%PROJECT_DIR%\target\jpackage-input"
mkdir "%PROJECT_DIR%\target\jpackage-input"
copy /y "%PROJECT_DIR%\target\net-diagnostico-1.0.0.jar" "%PROJECT_DIR%\target\jpackage-input\netdiag-pro.jar" >nul

if exist "%PROJECT_DIR%\pendrive-dist" rmdir /s /q "%PROJECT_DIR%\pendrive-dist"

:: ============================================================
:: GERACAO DO EXE
:: ============================================================

echo [3/4] Gerando EXE com jpackage...
echo       (Aguarde, pode demorar 1-2 minutos)
echo.

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "NetDiag Pro" ^
    --app-version "1.0.0" ^
    --description "Ferramenta de Diagnostico de Rede e TI" ^
    --vendor "NetDiag" ^
    --input "%PROJECT_DIR%\target\jpackage-input" ^
    --main-jar "netdiag-pro.jar" ^
    --main-class "com.netdiag.Launcher" ^
    --dest "%PROJECT_DIR%\pendrive-dist" ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.lang.reflect=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.io=ALL-UNNAMED" ^
    --java-options "--add-opens java.desktop/sun.awt=ALL-UNNAMED" ^
    --java-options "--add-opens java.desktop/sun.java2d=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if not exist "%PROJECT_DIR%\pendrive-dist\NetDiag Pro\NetDiag Pro.exe" (
    echo [ERRO] jpackage falhou. EXE nao foi gerado.
    goto :erro
)

echo       EXE gerado OK.
echo.

:: ============================================================
:: SUCESSO
:: ============================================================

echo [4/4] Concluido!
echo.
echo ============================================================
echo   BUILD CONCLUIDO COM SUCESSO!
echo ============================================================
echo.
echo   Pasta gerada: %PROJECT_DIR%\pendrive-dist\NetDiag Pro\
echo.
echo   Como usar:
echo     1. Copie a pasta "pendrive-dist\NetDiag Pro\" para o pendrive
echo     2. No computador destino: clique direito em NetDiag Pro.exe
echo        ^> "Executar como administrador"
echo.
pause
exit /b 0

:erro
echo.
echo ============================================================
echo   BUILD FALHOU
echo ============================================================
echo.
pause
exit /b 1
