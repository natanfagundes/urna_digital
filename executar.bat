@echo off
chcp 65001 > nul
cd /d "%~dp0"
if not exist out\main mkdir out\main
dir /s /b src\main\java\*.java > out\fontes.txt
javac -encoding UTF-8 -d out\main @out\fontes.txt
if errorlevel 1 (
    echo.
    echo Erro na compilacao. Verifique se o JDK 11 ou superior esta instalado ^(comando javac^).
    pause
    exit /b 1
)
start "" javaw -cp out\main urna.Main
