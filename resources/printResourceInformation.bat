@echo off
echo "Writing used_resources.txt"
dir /s/b . | find ".txt" > used_resources.txt


REM here, we'll have to do some hacky windows version detecting
echo "Copying resources..."

if exist "C:\Users\All Users\ntuser.dat" goto win7
if exist "C:\Documents and Settings\All Users\ntuser.dat" goto winxp
:win7
REM under win vista+ robocopy is available.
robocopy . ..\class\ /E
goto end

:winxp
REM under windows nt-xp, we have to use the soon to be deprecated xcopy
xcopy /Y/E * ..\class\
goto end


:end
REM done
echo "done."
