@echo off
echo Limpando classes antigas...
rmdir /S /Q out\classes
mkdir out\classes

echo Gerando lista de arquivos...
(for /R src %%f in (*.java) do @echo "%%f") > arquivos.txt

echo Compilando todos os arquivos juntos...
setlocal EnableDelayedExpansion
set files=
for /F "tokens=*" %%f in (arquivos.txt) do (
    set files=!files! %%f
)
javac -d out\classes -cp "lib/*" !files!
endlocal

echo Gerando JAR com estrutura correta...
cd out\classes
jar cfm ..\HoStore.jar ..\..\MANIFEST.MF -C . .
cd ..\..

echo JAR criado com sucesso: out\HoStore.jar
pause
