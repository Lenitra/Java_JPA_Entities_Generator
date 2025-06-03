@echo off
echo [1/5] Changement de répertoire vers le dossier du script...
cd /d "%~dp0"
echo Répertoire actuel : %cd%

echo [2/5] Détection du JDK...



@REM Try to read javapath.txt
if exist "config/javapath.txt" (
    for /f "usebackq delims=" %%j in ("config/javapath.txt") do set "JAVA_HOME=%%j"
) else (
    @REM set "JAVA_HOME=D:\jdk-17.0.12"
)


set "JAVA_EXEC=%JAVA_HOME%\bin\java.exe"
set "JAVAC_EXEC=%JAVA_HOME%\bin\javac.exe"

if not exist "%JAVA_EXEC%" (
    echo
    echo
    echo
    echo ----------------------------------------------------------------------------
    echo Erreur : Le fichier java.exe n'a pas été trouvé dans %JAVA_HOME%\bin.
    echo Vérifiez que le chemin vers le JDK est correct.
    echo Et le modifier dans le script javapath.txt si nécessaire.
    echo ----------------------------------------------------------------------------
    pause
    exit /b 1
)
if not exist "%JAVAC_EXEC%" (
    echo ...
    echo ...
    echo ...
    echo ----------------------------------------------------------------------------
    echo Erreur : Le fichier javac.exe n'a pas été trouvé dans %JAVA_HOME%\bin.
    echo Vérifiez que le chemin vers le JDK est correct.
    echo Et le modifier dans le script javapath.txt si nécessaire.
    echo ----------------------------------------------------------------------------
    pause
    exit /b 1
)
echo JAVA_HOME forcé : %JAVA_HOME%


echo [3/5] Préparation du dossier de compilation...
set "BIN_DIR=bin"
if not exist "%BIN_DIR%" (
    mkdir "%BIN_DIR%"
    echo Dossier 'bin' créé
) else (
    echo Dossier 'bin' déjà présent
)


echo [4/5] Compilation de src/RunMain.java...
"%JAVAC_EXEC%" -d "%BIN_DIR%" src/RunMain.java
if errorlevel 1 (
    echo Échec de la compilation. Vérifiez votre code source.
    pause
    exit /b 1
) else (
    echo Compilation réussie !
)


echo [5/5] Exécution de la classe src/RunMain...
"%JAVA_EXEC%" -XX:+ShowCodeDetailsInExceptionMessages -cp "%BIN_DIR%" RunMain
echo Exécution terminée.
