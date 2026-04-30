#!/usr/bin/env bash

##
# TimeTrack — self-contained gradlew
# Sets JAVA_HOME, ANDROID_HOME, and GRADLE_USER_HOME to local tools/ and .gradle-home/
# before delegating to the bundled Gradle distribution.
##

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export JAVA_HOME="${SCRIPT_DIR}/tools/jdk"
export ANDROID_HOME="${SCRIPT_DIR}/tools/android-sdk"
export ANDROID_SDK_ROOT="${SCRIPT_DIR}/tools/android-sdk"
export GRADLE_USER_HOME="${SCRIPT_DIR}/.gradle-home"
export PATH="${JAVA_HOME}/bin:${PATH}"

mkdir -p "${GRADLE_USER_HOME}"

exec "${SCRIPT_DIR}/tools/gradle/gradle-8.11.1/bin/gradle" "$@"
