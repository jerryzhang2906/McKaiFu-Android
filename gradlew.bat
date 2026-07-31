@rem Gradle startup script
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%OS%"=="Windows_NT" setlocal

set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar
if not exist "%CLASSPATH%" (
    echo Downloading Gradle wrapper...
    powershell -Command "Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.5-bin.zip' -OutFile '%TEMP%\gradle.zip'; Expand-Archive -Path '%TEMP%\gradle.zip' -DestinationPath '%TEMP%\gradle'; copy '%TEMP%\gradle\gradle-8.5\lib\gradle-wrapper-*.jar' '%DIRNAME%gradle\wrapper\gradle-wrapper.jar'"
    if errorlevel 1 (
        echo Failed to download Gradle. Please install Gradle manually.
        exit /b 1
    )
)

"%JAVA_HOME%\bin\java.exe" -Dorg.gradle.appname=gradlew -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
