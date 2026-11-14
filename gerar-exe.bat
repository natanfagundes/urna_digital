@echo off
rem Gera dist\UrnaDigital-windows.zip: o programa (UrnaDigital.exe) com um Java embutido.
rem Quem recebe o .zip so precisa descompactar e abrir o UrnaDigital.exe (nao precisa instalar Java).
chcp 65001 > nul
setlocal
cd /d "%~dp0"

rem O jpackage vem no JDK 14+, mas normalmente nao fica no PATH: descobre a pasta do JDK.
if defined JAVA_HOME (
    set "JDK=%JAVA_HOME%"
) else (
    for /f "tokens=2 delims==" %%i in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do (
        for /f "tokens=*" %%j in ("%%i") do set "JDK=%%j"
    )
)
if not exist "%JDK%\bin\jpackage.exe" (
    echo Nao encontrei o jpackage. Instale o JDK 17 ou superior ^(https://adoptium.net^).
    goto erro
)
echo Usando o JDK em: %JDK%

if exist build-exe rmdir /s /q build-exe
if exist dist rmdir /s /q dist
mkdir build-exe\classes
mkdir build-exe\jar

echo [1/4] Compilando...
dir /s /b src\main\java\*.java > build-exe\fontes.txt
"%JDK%\bin\javac" -encoding UTF-8 --release 11 -d build-exe\classes @build-exe\fontes.txt || goto erro

echo [2/4] Montando o .jar...
"%JDK%\bin\jar" --create --file build-exe\jar\urna-digital.jar --main-class urna.Main -C build-exe\classes . || goto erro

echo [3/4] Gerando o .exe com o Java embutido (leva cerca de 1 minuto)...
rem java.desktop = Swing, sons e imagens. As eleicoes ficam na pasta "eleicoes" ao lado do .exe.
"%JDK%\bin\jpackage" --type app-image --name UrnaDigital --app-version 2.0.0 ^
    --vendor "Projeto de Extensao - Engenharia de Software" --description "Urna Digital" ^
    --input build-exe\jar --main-jar urna-digital.jar --main-class urna.Main ^
    --icon empacotamento\urna.ico ^
    --add-modules java.desktop ^
    --jlink-options "--strip-debug --no-man-pages --no-header-files" ^
    --java-options "-Durna.dados=$APPDIR\..\eleicoes" ^
    --dest dist || goto erro

echo [4/4] Compactando...
powershell -NoProfile -Command "Compress-Archive -Path 'dist\UrnaDigital' -DestinationPath 'dist\UrnaDigital-windows.zip' -Force" || goto erro

echo.
echo Pronto! Envie o arquivo dist\UrnaDigital-windows.zip
echo Quem receber: descompacta e abre UrnaDigital\UrnaDigital.exe
if not defined CI pause
exit /b 0

:erro
echo.
echo Falhou ao gerar o .exe.
if not defined CI pause
exit /b 1
