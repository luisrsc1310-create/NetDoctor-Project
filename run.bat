@echo off
:: ============================================================
::  NetDiag Pro - Launcher Portavel (para uso direto do pendrive)
::  - Nao requer Java instalado no computador
::  - Execute como Administrador para funcoes de rede e impressoras
:: ============================================================

title NetDiag Pro

:: Localiza a pasta raiz onde este bat esta (funciona em qualquer letra de unidade)
set "BASE_DIR=%~dp0"
set "BASE_DIR=%BASE_DIR:~0,-1%"

set "JRE=%BASE_DIR%\jre"
set "JAR=%BASE_DIR%\app\netdiag-pro.jar"

:: ── Valida estrutura ──────────────────────────────────────────
if not exist "%JRE%\bin\java.exe" (
    echo.
    echo [ERRO] JRE embutido nao encontrado.
    echo.
    echo Certifique-se de que a pasta "pendrive-dist" foi copiada
    echo completamente para o pendrive, incluindo a subpasta "jre".
    echo.
    echo Se voce acabou de baixar apenas o run.bat, va em:
    echo   https://github.com/SEU_USUARIO/net-diagnostico
    echo e baixe o release completo (pendrive-dist.zip).
    echo.
    pause
    exit /b 1
)

if not exist "%JAR%" (
    echo.
    echo [ERRO] Arquivo de aplicacao nao encontrado: %JAR%
    echo        Certifique-se de que a pasta "app" esta presente.
    echo.
    pause
    exit /b 1
)

:: ── Configuracao de DLLs nativas (JavaFX) ────────────────────
:: Adiciona o bin do JRE ao PATH para as DLLs nativas serem encontradas
set "PATH=%JRE%\bin;%PATH%"

:: ── Inicializacao ────────────────────────────────────────────
echo.
echo   NetDiag Pro - Iniciando...
echo.

"%JRE%\bin\java.exe" ^
    -Djava.library.path="%JRE%\bin" ^
    --add-opens java.base/java.lang=ALL-UNNAMED ^
    --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
    --add-opens java.base/java.io=ALL-UNNAMED ^
    --add-opens java.desktop/sun.awt=ALL-UNNAMED ^
    --add-opens java.desktop/sun.java2d=ALL-UNNAMED ^
    -jar "%JAR%"

:: ── Tratamento de erro de saida ───────────────────────────────
if errorlevel 1 (
    echo.
    echo [AVISO] NetDiag Pro encerrou com codigo de erro.
    echo.
    echo Dicas:
    echo  - Para configurar IP ou limpar spooler, execute como Administrador:
    echo    Clique direito em run.bat ^> "Executar como administrador"
    echo  - Se a janela nao abriu, verifique se ha uma mensagem de erro acima.
    echo.
    pause
)
