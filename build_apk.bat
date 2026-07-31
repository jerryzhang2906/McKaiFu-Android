@echo off
chcp 65001 >nul
title McKaiFu APK Builder
echo ============================================
echo   McKaiFu 开服大师 - APK构建脚本
echo ============================================
echo.

REM Check Java
if "%JAVA_HOME%"=="" (
    echo [错误] JAVA_HOME 未设置，请安装JDK 17+
    echo 例如: set JAVA_HOME=C:\Program Files\Java\jdk-17
    pause
    exit /b 1
)

echo [1/3] 检查Gradle包装器...
if not exist "gradle\wrapper\gradle-wrapper.jar" (
    echo 下载Gradle包装器...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile '%TEMP%\gradle.zip' -UseBasicParsing; Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%TEMP%\gradle' -Force; Get-ChildItem '%TEMP%\gradle\gradle-8.5\lib\gradle-wrapper-*.jar' | Select-Object -First 1 | %%{ Copy-Item $_.FullName 'gradle\wrapper\gradle-wrapper.jar' }"
    if errorlevel 1 (
        echo [错误] 下载Gradle失败，请手动下载 https://services.gradle.org/distributions/gradle-8.5-bin.zip
        echo 并将 gradle-8.5\lib\gradle-wrapper-*.jar 复制到 gradle\wrapper\gradle-wrapper.jar
        pause
        exit /b 1
    )
)

echo [2/3] 构建Debug APK...
call gradlew assembleDebug
if errorlevel 1 (
    echo [错误] 构建失败，请检查错误信息
    pause
    exit /b 1
)

echo [3/3] 构建Release APK...
call gradlew assembleRelease

echo.
echo ============================================
echo  构建完成！
echo.
echo Debug APK: app\build\outputs\apk\debug\
echo Release APK: app\build\outputs\apk\release\
echo ============================================
pause
