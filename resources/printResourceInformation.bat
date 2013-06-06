@echo off

del used_resources.txt

echo "Writing used_resources.txt"
dir /s/b . | findstr /e ".txt" > used_resources_temp.txt

@ECHO OFF
SETLOCAL DisableDelayedExpansion
SET "r=%__CD__%"
FOR /R . %%F IN (*) DO (
  SET "p=%%F"
  SETLOCAL EnableDelayedExpansion
  ECHO(.\!p:%r%=! >> used_resources.txt
  ENDLOCAL
)

REM here, we'll have to do some hacky windows version detecting
echo "Copying resources..."

if exist "C:\Users\All Users\ntuser.dat" goto win7
if exist "C:\Documents and Settings\All Users\ntuser.dat" goto winxp
:win7
REM under win vista+ robocopy is available.
robocopy . ..\class\ /E /NJH /NJS /NP
goto end

:winxp
REM under windows nt-xp, we have to use the older xcopy
xcopy /Y/E * ..\class\
goto end


:end
REM done
echo "done."
