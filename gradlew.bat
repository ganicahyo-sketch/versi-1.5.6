@echo off
setlocal
set "GRADLE_VERSION=9.4.1"
if not "%GRADLE_HOME%"=="" if exist "%GRADLE_HOME%\bin\gradle.bat" goto use_home
where gradle >NUL 2>&1
if %ERRORLEVEL% EQU 0 goto use_path
set "CACHE_BASE=%USERPROFILE%\.gradle\wrapper\dists\manual"
set "DIST=%CACHE_BASE%\gradle-%GRADLE_VERSION%"
set "BIN=%DIST%\bin\gradle.bat"
if exist "%BIN%" goto run_bin
if not exist "%DIST%" mkdir "%DIST%"
set "TMP=%DIST%\gradle.zip"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%TMP%'"
powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%TMP%' -DestinationPath '%CACHE_BASE%' -Force"
del /q "%TMP%"
goto run_bin
:use_home
"%GRADLE_HOME%\bin\gradle.bat" %*
goto end
:use_path
gradle %*
goto end
:run_bin
"%BIN%" %*
:end
endlocal
