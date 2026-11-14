@echo off
chcp 65001 > nul
cd /d "%~dp0"
where mvn > nul 2>&1
if errorlevel 1 (
    echo O Maven nao foi encontrado.
    echo.
    echo Para rodar os testes JUnit:
    echo   - instale o Maven ^(https://maven.apache.org^) e rode este arquivo de novo; ou
    echo   - abra a pasta no IntelliJ IDEA / Eclipse / VS Code e rode os testes de src\test\java.
    echo.
    echo Os testes tambem rodam automaticamente no GitHub a cada envio ^(aba Actions^).
    pause
    exit /b 1
)
call mvn -q test
pause
