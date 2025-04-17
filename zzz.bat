@echo off
rem ===========================================================
rem 0. Se placer dans le dossier du script
rem ===========================================================
cd /d "%~dp0" || (
    echo ERREUR : impossible de se placer dans le dossier "%~dp0".
    pause
    exit /b 1
)
setlocal enabledelayedexpansion

echo === [0] INITIALISATION DE L'ENVIRONNEMENT
if exist "tmp" rd /s /q "tmp"

echo === [1] COPIE DU TEMPLATE ET CREATION DES DOSSIERS
if not exist "Template" (
    echo ERREUR : le dossier "Template" est introuvable.
    pause
    exit /b 1
)
xcopy "Template" "tmp" /E /I /Y >nul || (
    echo ERREUR : échec de la copie du template.
    pause
    exit /b 1
)

set "ENTITY_DIR=tmp\src\main\java\entities"
set "DAO_DIR=tmp\src\main\java\dao"
set "DAO_IMPL_DIR=%DAO_DIR%\bdd"
set "REFERENCE_DIR=%ENTITY_DIR%\references"
set "PERSISTENCE_FILE=tmp\src\main\resources\META-INF\persistence.xml"

for %%D in (
    "%ENTITY_DIR%" "%DAO_DIR%" "%DAO_IMPL_DIR%" "%REFERENCE_DIR%"
) do (
    if not exist "%%~D" mkdir "%%~D"
)

echo === [2] TRAITEMENT DES ENTITES
if not exist "entities.txt" (
    echo ERREUR : le fichier "entities.txt" est introuvable.
    pause
    exit /b 1
)
for /f "usebackq delims=" %%L in ("entities.txt") do (
    set "LINE=%%L"
    set "LINE=!LINE: =!"
    if "!LINE:~0,2!"=="c:" (
        set "NAME=!LINE:~2!"
        (
            echo package entities;
            echo;
            echo public class !NAME! extends AbstractEntity {
            echo;
            echo }
        )>"!ENTITY_DIR!\!NAME!.java"

        (
            echo package dao;
            echo;
            echo public interface !NAME!Dao extends Dao^< !NAME! ^> {
            echo }
        )>"!DAO_DIR!\!NAME!Dao.java"

        (
            echo package dao.bdd;
            echo;
            echo import entities.!NAME!;
            echo import dao.!NAME!Dao;
            echo;
            echo public class !NAME!DaoImpl extends AbstractDaoImpl^< !NAME! ^> implements !NAME!Dao {
            echo }
        )>"!DAO_IMPL_DIR!\!NAME!DaoImpl.java"

    ) else if "!LINE:~0,2!"=="e:" (
        set "NAME=!LINE:~2!"
        (
            echo package entities.references;
            echo;
            echo public enum !NAME! {
            echo    // TODO : ajouter les valeurs
            echo }
        )>"!REFERENCE_DIR!\!NAME!.java"
    )
)

echo === [3] MODIFICATION DE persistence.xml
if not exist "!PERSISTENCE_FILE!" (
    echo ERREUR : le fichier "!PERSISTENCE_FILE!" est introuvable.
    pause
    exit /b 1
)
if not exist "bddConf.txt" (
    echo ERREUR : le fichier "bddConf.txt" est introuvable.
    pause
    exit /b 1
)

rem Lecture des deux lignes de bddConf.txt
set "COUNT=0"
for /f "usebackq delims=" %%a in ("bddConf.txt") do (
    set /a COUNT+=1
    if !COUNT! equ 1 set "UNIT_NAME=%%a"
    if !COUNT! equ 2 set "BDD_NAME=%%a"
)
if "!UNIT_NAME!"=="" echo AVERTISSEMENT : UNIT_NAME est vide.
if "!BDD_NAME!"=="" echo AVERTISSEMENT : BDD_NAME est vide.

rem Création du fichier temporaire
if exist "!PERSISTENCE_FILE!.tmp" del /q "!PERSISTENCE_FILE!.tmp"

for /f "usebackq delims=" %%L in ("!PERSISTENCE_FILE!") do (
    set "LINE=%%L"
    set "LINE=!LINE:DBNAME=!BDD_NAME!!"
    set "LINE=!LINE:PERSISTENCEUNITNAME=!UNIT_NAME!!"
    >>"!PERSISTENCE_FILE!.tmp" echo !LINE!
)

move /Y "!PERSISTENCE_FILE!.tmp" "!PERSISTENCE_FILE!" >nul || (
    echo ERREUR : impossible de remplacer "!PERSISTENCE_FILE!".
    pause
    exit /b 1
)

echo === [4] DÉPLOIEMENT DANS PROJECT_PATH
if not exist "projectPath.txt" (
    echo ERREUR : le fichier "projectPath.txt" est introuvable.
    pause
    exit /b 1
)
set /p "PROJECT_PATH="<projectPath.txt
if "!PROJECT_PATH!"=="" (
    echo ERREUR : projectPath.txt est vide.
    pause
    exit /b 1
)
if not exist "!PROJECT_PATH!" (
    echo ERREUR : le chemin "!PROJECT_PATH!" n'existe pas.
    pause
    exit /b 1
)

rem Copier le contenu de tmp vers PROJECT_PATH
xcopy "tmp\*" "!PROJECT_PATH!\" /E /I /Y >nul || (
    echo ERREUR : échec de la copie du contenu de tmp vers "!PROJECT_PATH!".
    pause
    exit /b 1
)

rem Supprimer le dossier temporaire
rd /s /q "tmp"

echo ----------------------
echo         TERMINE
echo ----------------------
pause
endlocal
