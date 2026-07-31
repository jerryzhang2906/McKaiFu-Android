#!/bin/sh
DIRNAME=$(dirname "$0")
CLASSPATH=$DIRNAME/gradle/wrapper/gradle-wrapper.jar
if [ ! -f "$CLASSPATH" ]; then
    echo "Downloading Gradle wrapper..."
    curl -L -o /tmp/gradle.zip "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
    unzip -q /tmp/gradle.zip -d /tmp/gradle
    cp /tmp/gradle/gradle-8.5/lib/gradle-wrapper-*.jar "$CLASSPATH"
fi
exec java -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
