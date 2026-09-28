@echo off
title NetDiag Pro - Build EXE Pendrive

echo ============================================================
echo   NetDiag Pro - Gerador de EXE Portavel para Pendrive
echo   Resultado: pendrive-dist\NetDiag Pro\NetDiag Pro.exe
echo ============================================================
echo.

:: CONFIGURACAO DIRETA (caminhos fixos, sem depender de deteccao)

set "JAVA_HOME=C:\Users\PC\.jdks\openjdk-26.0.1"

set "JAVA_HOME_MVN=C:\Program Files\Java\jdk-21.0.12.1"

set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
set "PROJECT_DIR=%~dp0"

if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

:: VALIDACOES

echo [1/4] Verificando pre-requisitos...

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo [ERRO] JDK nao encontrado em: %JAVA_HOME%
    echo        Ajuste a variavel JAVA_HOME neste script.
    goto :erro
)
echo       JDK jpackage: %JAVA_HOME%

if not exist "%JAVA_HOME_MVN%\bin\java.exe" (
    echo [ERRO] JDK 21 para Maven nao encontrado em: %JAVA_HOME_MVN%
    echo        Ajuste a variavel JAVA_HOME_MVN neste script.
    goto :erro
)
echo       JDK Maven   : %JAVA_HOME_MVN%

if not exist "%MVN_CMD%" (
    echo [ERRO] Maven nao encontrado em: %MVN_CMD%
    echo        Ajuste a variavel MVN_CMD neste script.
    goto :erro
)
echo       Maven: OK
echo.

:: COMPILACAO COM MAVEN

echo [2/4] Compilando projeto com Maven (Java 21)...
echo.

cd /d "%PROJECT_DIR%"
set "JAVA_HOME=%JAVA_HOME_MVN%"
"%MVN_CMD%" clean package -q
if %ERRORLEVEL% neq 0 (
    echo [ERRO] Maven retornou erro. Verifique o codigo-fonte.
    goto :erro
)

:: Restaura JAVA_HOME para o JDK 26 (jpackage)
set "JAVA_HOME=C:\Users\PC\.jdks\openjdk-26.0.1"

if not exist "%PROJECT_DIR%\target\net-diagnostico-1.0.0.jar" (
    echo [ERRO] JAR nao foi gerado em target\net-diagnostico-1.0.0.jar
    goto :erro
)
echo       JAR gerado OK.
echo.

:: PREPARACAO DO INPUT PARA JPACKAGE

echo [3/4] Preparando estrutura e gerando EXE com jpackage...

:: Recria a pasta de input limpa
if exist "%PROJECT_DIR%\target\jpackage-input" rmdir /s /q "%PROJECT_DIR%\target\jpackage-input"
mkdir "%PROJECT_DIR%\target\jpackage-input"
copy /y "%PROJECT_DIR%\target\net-diagnostico-1.0.0.jar" "%PROJECT_DIR%\target\jpackage-input\netdiag-pro.jar" >nul
if %ERRORLEVEL% neq 0 (
    echo [ERRO] Nao foi possivel copiar o JAR para a pasta de input.
    goto :erro
)

:: Remove dist anterior
if exist "%PROJECT_DIR%\pendrive-dist" rmdir /s /q "%PROJECT_DIR%\pendrive-dist"

echo       (Aguarde, pode demorar 1-2 minutos)
echo.

:: GERACAO DO EXE COM JPACKAGE
:: Usando --java-options=VALOR (sem espaco antes do =) para
:: evitar problemas de parsing de aspas no cmd.exe

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
    "--java-options=--add-opens java.base/java.lang=ALL-UNNAMED" ^
    "--java-options=--add-opens java.base/java.lang.reflect=ALL-UNNAMED" ^
    "--java-options=--add-opens java.base/java.io=ALL-UNNAMED" ^
    "--java-options=--add-opens java.desktop/sun.awt=ALL-UNNAMED" ^
    "--java-options=--add-opens java.desktop/sun.java2d=ALL-UNNAMED" ^
    "--java-options=-Dfile.encoding=UTF-8"

if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERRO] jpackage falhou com codigo %ERRORLEVEL%.
    goto :erro
)

if not exist "%PROJECT_DIR%\pendrive-dist\NetDiag Pro\NetDiag Pro.exe" (
    echo [ERRO] EXE nao foi encontrado apos o jpackage.
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
echo   BUILD FALHOU
echo.
pause
exit /b 1
