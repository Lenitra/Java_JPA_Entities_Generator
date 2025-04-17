@echo off
rem —===========================================================
rem 0. On se place dans le dossier du script pour être sûr des chemins
rem —===========================================================
cd /d "%~dp0"
setlocal enabledelayedexpansion

echo === [0] INITIALISATION DE L'ENVIRONNEMENT
@REM suppression du dossier tmp s'il existe
if exist "tmp" rmdir /S /Q "tmp"


echo === [1] COPIE DU TEMPLATE VERS TMP ET CREATION DES DOSSIERS
if not exist "Template" (
    echo ERREUR : le dossier "Template" est introuvable.
    pause
    exit /b
)
xcopy /E /I /Y "Template" "tmp" > nul

rem — Dossiers de sortie
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
    exit /b
)
for /f "usebackq delims=" %%L in ("entities.txt") do (
    set "LINE=%%L"
    set "LINE=!LINE: =!"
    if "!LINE:~0,2!"=="c:" (
        set "NAME=!LINE:~2!"
        (
            echo package entities;
            echo.
            echo public class %NAME% extends AbstractEntity {
            echo.
            echo }
        ) > "%ENTITY_DIR%\%NAME%.java"

        (
            echo package dao;
            echo.
            echo public interface %NAME%Dao extends Dao^<%NAME%^> {
            echo }
        ) > "%DAO_DIR%\%NAME%Dao.java"

        (
            echo package dao.bdd;
            echo.
            echo import entities.%NAME%;
            echo import dao.%NAME%Dao;
            echo.
            echo public class %NAME%DaoImpl extends AbstractDaoImpl^<%NAME%^> implements %NAME%Dao {
            echo }
        ) > "%DAO_IMPL_DIR%\%NAME%DaoImpl.java"
    ) else if "!LINE:~0,2!"=="e:" (
        set "NAME=!LINE:~2!"
        (
            echo package entities.references;
            echo.
            echo public enum %NAME% {
            echo     // TODO : ajouter les valeurs
            echo }
        ) > "%REFERENCE_DIR%\%NAME%.java"
    )
)



echo === [3] LECTURE ET MODIFICATION DE persistence.xml
if not exist "%PERSISTENCE_FILE%" (
    echo ERREUR : le fichier "%PERSISTENCE_FILE%" est introuvable.
    pause
    exit /b
)
if not exist "bddConf.txt" (
    echo ERREUR : le fichier "bddConf.txt" est introuvable.
    pause
    exit /b
)

rem — Lecture des deux lignes de bddConf.txt
set "COUNT=0"
for /f "usebackq delims=" %%a in ("bddConf.txt") do (
    set /a COUNT+=1
    if !COUNT! EQU 1 set "UNIT_NAME=%%a"
    if !COUNT! EQU 2 set "BDD_NAME=%%a"
)
if "%UNIT_NAME%"=="" echo AVERTISSEMENT : UNIT_NAME vide.
if "%BDD_NAME%"=="" echo AVERTISSEMENT : BDD_NAME vide.


rem — On supprime l’ancien temporaires
if exist "%PERSISTENCE_FILE%.tmp" del "%PERSISTENCE_FILE%.tmp"

rem — Remplacement ligne à ligne
for /f "usebackq delims=" %%L in ("%PERSISTENCE_FILE%") do (
    set "LINE=%%L"
    setlocal enabledelayedexpansion
    set "LINE=!LINE:DBNAME=%BDD_NAME%!"
    set "LINE=!LINE:PERSISTENCEUNITNAME=%UNIT_NAME%!"
    >> "%PERSISTENCE_FILE%.tmp" echo !LINE!
    endlocal
)

move /Y "%PERSISTENCE_FILE%.tmp" "%PERSISTENCE_FILE%" > nul
if errorlevel 1 (
    echo ERREUR : impossible de remplacer "%PERSISTENCE_FILE%".
    pause
    exit /b
)


echo ----------------------
echo         TERMINE
echo ----------------------
pause
endlocal
