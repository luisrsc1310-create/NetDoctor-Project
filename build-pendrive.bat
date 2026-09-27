@echo off
setlocal EnableDelayedExpansion
title NetDiag Pro - Build EXE Pendrive

echo ============================================================
echo   NetDiag Pro - Gerador de EXE Portavel para Pendrive
echo   Resultado: pendrive-dist\NetDiag Pro\NetDiag Pro.exe
echo   (Nao requer Java instalado no computador destino)
echo ============================================================
echo.

:: ============================================================
:: DETECCAO DO JDK
:: Ordem: JAVA_HOME ja definido > .jdks do IntelliJ > PATH
:: ============================================================

if "%JAVA_HOME%"=="" (
    for /d %%d in ("%USERPROFILE%\.jdks\*") do (
        if exist "%%d\bin\jpackage.exe" (
            set "JAVA_HOME=%%d"
        )
    )
)

if "%JAVA_HOME%"=="" (
    where java >nul 2>&1
    if !errorlevel! equ 0 (
        for /f "tokens=*" %%i in ('where java') do (
            for %%p in ("%%~dpi..") do set "JAVA_HOME=%%~fp"
        )
    )
)

:: ============================================================
:: DETECCAO DO MAVEN (IntelliJ embutido ou PATH)
:: ============================================================

set "MVN_CMD=mvn"
for /d %%d in ("C:\Program Files\JetBrains\IntelliJ IDEA*") do (
    if exist "%%d\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=%%d\plugins\maven\lib\maven3\bin\mvn.cmd"
    )
)

:: ============================================================
:: VALIDACOES
:: ============================================================

echo [1/4] Verificando pre-requisitos...

if "%JAVA_HOME%"=="" (
    echo.
    echo [ERRO] JDK nao encontrado!
    echo.
    echo  O script procurou em:
    echo    1. Variavel de ambiente JAVA_HOME
    echo    2. %USERPROFILE%\.jdks\   ^(JDKs gerenciados pelo IntelliJ^)
    echo    3. PATH do sistema
    echo.
    echo  Solucao: defina manualmente no inicio deste script:
    echo    set "JAVA_HOME=C:\Users\PC\.jdks\openjdk-21.x.x"
    echo.
    echo  Ou instale o JDK 21+: https://adoptium.net/
    echo.
    goto :erro
)

echo       JDK encontrado em: %JAVA_HOME%

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
    echo.
    echo [ERRO] jpackage.exe nao encontrado em %JAVA_HOME%\bin\
    echo        jpackage exige JDK 14+. Verifique a versao do JDK.
    echo.
    goto :erro
)

echo       jpackage: OK

"%MVN_CMD%" --version >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERRO] Maven nao encontrado no PATH nem no IntelliJ em C:\Program Files\JetBrains\
    echo.
    goto :erro
)

echo       Maven: OK
echo.

:: ============================================================
:: COMPILACAO - Gera o fat-jar com todas as dependencias
:: ============================================================

echo [2/4] Compilando projeto com Maven...
echo.

call "%MVN_CMD%" clean package -q

if errorlevel 1 (
    echo.
    echo [ERRO] Falha na compilacao. Verifique os erros acima.
    goto :erro
)

if not exist "target\net-diagnostico-1.0.0.jar" (
    echo [ERRO] JAR nao foi gerado. Verifique o pom.xml.
    goto :erro
)

echo       JAR gerado: target\net-diagnostico-1.0.0.jar
echo.

:: ============================================================
:: PREPARACAO - Pasta de entrada para o jpackage
:: ============================================================

:: jpackage precisa do JAR em uma pasta separada
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
mkdir "target\jpackage-input"
copy /y "target\net-diagnostico-1.0.0.jar" "target\jpackage-input\netdiag-pro.jar" >nul

:: Limpa saida anterior
if exist "pendrive-dist" rmdir /s /q "pendrive-dist"

:: ============================================================
:: GERACAO DO EXE com jpackage
:: ============================================================

echo [3/4] Gerando EXE portavel com jpackage...
echo       (Isso pode levar alguns minutos na primeira vez)
echo.

"%JAVA_HOME%\bin\jpackage.exe" ^
    --type app-image ^
    --name "NetDiag Pro" ^
    --app-version "1.0.0" ^
    --description "Ferramenta de Diagnostico de Rede e TI" ^
    --vendor "NetDiag" ^
    --input "target\jpackage-input" ^
    --main-jar "netdiag-pro.jar" ^
    --main-class "com.netdiag.Launcher" ^
    --dest "pendrive-dist" ^
    --java-options "--add-opens java.base/java.lang=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.lang.reflect=ALL-UNNAMED" ^
    --java-options "--add-opens java.base/java.io=ALL-UNNAMED" ^
    --java-options "--add-opens java.desktop/sun.awt=ALL-UNNAMED" ^
    --java-options "--add-opens java.desktop/sun.java2d=ALL-UNNAMED" ^
    --java-options "-Dfile.encoding=UTF-8"

if errorlevel 1 (
    echo.
    echo [ERRO] Falha ao gerar o EXE com jpackage.
    echo.
    echo  Causas comuns:
    echo    - JavaFX nao esta no module-path. Veja nota abaixo.
    echo    - Falta o WiX Toolset (necessario apenas para --type msi/exe installer,
    echo      nao para app-image que estamos usando).
    echo.
    goto :erro
)

echo.
echo       EXE gerado com sucesso.
echo.

:: ============================================================
:: RESULTADO FINAL
:: ============================================================

echo [4/4] Finalizando...
echo.
echo ============================================================
echo   BUILD CONCLUIDO COM SUCESSO!
echo ============================================================
echo.
echo   Pasta gerada: %CD%\pendrive-dist\NetDiag Pro\
echo.
echo   Como usar:
echo     1. Copie a pasta "pendrive-dist\NetDiag Pro\" para o pendrive
echo     2. No computador destino, abra a pasta no pendrive
echo     3. Clique direito em "NetDiag Pro.exe"
echo        ^> "Executar como administrador"  (para funcoes de rede)
echo        ^> Duplo clique normal            (para diagnostico basico)
echo.
echo   Nao requer instalacao de Java no computador destino!
echo.

goto :fim

:erro
echo.
echo ============================================================
echo   BUILD FALHOU - Verifique os erros acima
echo ============================================================
echo.
pause
exit /b 1

:fim
pause
endlocal
