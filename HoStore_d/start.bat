@echo off
REM ------------------ HoStore Launcher ------------------
setlocal ENABLEDELAYEDEXPANSION
title HoStore – Inicializando…

REM 1) Diretório do script
cd /d "%~dp0"

REM 2) Backup e promoção do novo .jar, se existir
if exist "HoStore-novo.jar" (
    echo [Launcher] Atualização detectada. Aplicando...
    if exist "backup" (echo.) else md "backup"
    set "TS=%date:~-4%-%date:~3,2%-%date:~0,2%_%time:~0,2%-%time:~3,2%"
    set "TS=!TS: =0!"
    move /y "HoStore.jar" "backup\HoStore_%TS%.jar" >nul 2>&1
    move /y "HoStore-novo.jar" "HoStore.jar" >nul 2>&1
    echo [Launcher] Atualização aplicada com sucesso.
)

REM 3) Dispara o Atualizador em modo silencioso (não bloqueia)
start "Atualizador" /min java -jar "Atualizador.jar" silent

REM 4) Inicia o sistema principal e aguarda seu término
echo [Launcher] Iniciando HoStore...
java --enable-native-access=ALL-UNNAMED -jar "HoStore.jar"

if %errorlevel% neq 0 (
    echo.
    echo ============================================
    echo ERRO ao iniciar HoStore (código %errorlevel%).
    echo Verifique se o Java 17+ está instalado e tente novamente.
    echo Pressione qualquer tecla para sair...
    pause >nul
)
endlocal
