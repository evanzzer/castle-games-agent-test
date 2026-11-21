#!/usr/bin/env bash
# Shared SDK configuration for builds and Android instrumentation.
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
bt="$sdk/build-tools/${BUILD_TOOLS_VERSION:-33.0.1}"
jar="$sdk/platforms/android-${ANDROID_PLATFORM_VERSION:-33}/android.jar"
for required in "$bt/aapt" "$bt/d8" "$bt/zipalign" "$bt/apksigner" "$jar"; do
    if [[ ! -e "$required" ]]; then
        printf 'Missing Android SDK component: %s\n' "$required" >&2
        exit 1
    fi
done

signing_key="${CASTLE_SIGNING_KEY:-$PWD/.local/signing/debug.jks}"
