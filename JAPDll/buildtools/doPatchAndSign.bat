cd %1\buildtools\
patchexe.exe %2
%REM Ok the following line does nothing more than CODESIGNPASSWD=`UserInput.exe`
for /f " usebackq" %%i in (`UserInput.exe`) do @set CODESIGNPASSWD=%%i
signtool sign /f %CODESIGNCERT% /p %CODESIGNPASSWD% /d japdll.dll /du www.jondos.de /t http://timestamp.comodoca.com/authenticode %2