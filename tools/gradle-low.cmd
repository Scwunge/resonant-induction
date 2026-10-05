@echo off
rem Runs gradlew at BelowNormal priority on 4 of 12 cores so it does not affect games.
rem The exit code is not reliable through start; check the log for BUILD SUCCESSFUL.
pushd "%~dp0.."
start "" /b /wait /belownormal /affinity F00 "%CD%\gradlew.bat" --console=plain %*
popd
