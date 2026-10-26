#!/usr/bin/env bash
set -euo pipefail
PS_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export GRADLE_USER_HOME="$PS_ROOT/.gradle-user"
export CMAKE_BUILD_PARALLEL_LEVEL="${CMAKE_BUILD_PARALLEL_LEVEL:-6}"
cd "$PS_ROOT/runtime/platforms/android"
./gradlew :app:assembleGithubRelease \
  -Parmsx2.applicationId=com.gamepad.pspad \
  '-Parmsx2.appLabel=GamePad PS' \
  -Parmsx2.versionName=0.4.1 \
  -Parmsx2.versionCode=8 \
  -Parmsx2.minSdk=35 \
  -Parmsx2.march=armv8.2-a+fp16+dotprod \
  -Parmsx2.hostPageSize=0x1000 \
  -Parmsx2.nativeLibName=emucore_4k \
  -Parmsx2.pgo=optimize \
  "-Parmsx2.pgoProfile=$PS_ROOT/runtime/platforms/android/pgo/armsx2.profdata" \
  -Pandroid.builder.sdkDownload=false \
  --console=plain --max-workers=6 "$@"
