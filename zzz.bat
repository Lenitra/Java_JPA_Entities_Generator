@echo off
rem ===========================================================
rem Compile et exécute la classe Test.java en utilisant Java 17
rem ===========================================================

rem Se placer dans le dossier du script
cd /d "%~dp0" || (
    echo ERREUR : impossible de se placer dans le dossier "%~dp0".
    pause
    exit /b 1
)

rem -------------------------------------------------------------------
rem Spécifiez ici le chemin vers le JDK 17 à utiliser
set "JDK_PATH=D:\jdk-17.0.12"
rem -------------------------------------------------------------------

rem Vérifier que le JDK existe
if not exist "%JDK_PATH%\bin\javac.exe" (
    echo ERREUR : javac introuvable dans "%JDK_PATH%\bin". Vérifiez le chemin JDK_PATH.
    pause
    exit /b 1
)

rem Définir JAVA_HOME et mettre le JDK 17 en tête du PATH
set "JAVA_HOME=%JDK_PATH%"
set "PATH=%JAVA_HOME%\bin;%%PATH%%"

rem Afficher la version du JDK utilisé
"%JAVA_HOME%\bin\java" -version

rem Compilation pour Java 17
echo Compilation de Test.java pour Java 17...
"%JAVA_HOME%\bin\javac" --release 17 Test.java || (
    echo ERREUR : la compilation de Test.java a échoué.
    pause
    exit /b 1
)

rem Exécution de la classe Test
echo Exécution de Test avec Java 17...
"%JAVA_HOME%\bin\java" -cp . Test
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% neq 0 (
    echo ERREUR : l'exécution de la classe Test a retourné le code %EXIT_CODE%.
) else (
    echo Exécution terminée avec succès.
)

@REM Supprimer tout les fichiers .class
del /q *.class >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo ERREUR : impossible de supprimer les fichiers .class.
) else (
    echo Fichiers .class supprimés avec succès.
)


pause
exit /b %EXIT_CODE%
