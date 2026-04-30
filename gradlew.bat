@rem TimeTrack — Windows gradlew (not used in primary dev environment; kept for completeness)
@echo off
setlocal
set SCRIPT_DIR=%~dp0
set JAVA_HOME=%SCRIPT_DIR%tools\jdk
set ANDROID_HOME=%SCRIPT_DIR%tools\android-sdk
set ANDROID_SDK_ROOT=%SCRIPT_DIR%tools\android-sdk
set GRADLE_USER_HOME=%SCRIPT_DIR%.gradle-home
set PATH=%JAVA_HOME%\bin;%PATH%
"%SCRIPT_DIR%tools\gradle\gradle-8.11.1\bin\gradle.bat" %*
