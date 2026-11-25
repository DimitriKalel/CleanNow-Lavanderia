@echo off
chcp 65001 > nul
cd /d "%~dp0"
if not exist target\cleannow.jar (
    echo Compilando o CleanNow pela primeira vez, aguarde...
    call mvn -q -DskipTests package
    if errorlevel 1 (
        echo.
        echo Nao foi possivel compilar. Verifique se o Java e o Maven estao instalados.
        pause
        exit /b 1
    )
)
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -jar target\cleannow.jar %*
pause
