@echo off
chcp 65001 > nul
cd /d "%~dp0"
if not exist out mkdir out
dir /s /b src\*.java > out\fontes.txt
javac -encoding UTF-8 -d out @out\fontes.txt
if errorlevel 1 (
    echo.
    echo Erro na compilacao. Verifique se o JDK esta instalado ^(comando javac^).
    pause
    exit /b 1
)
java -cp out urna.Main
