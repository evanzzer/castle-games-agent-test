#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
sdk="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
bt="$sdk/build-tools/33.0.1"
runner="${1:-UiCheck}"
mkdir -p build/ui/classes build/ui/dex
cat > build/ui/AndroidManifest.xml <<XML
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="game.castlewars.rebuilt.tests">
<uses-sdk android:minSdkVersion="23" android:targetSdkVersion="33"/>
<application android:label="Castle Wars UI Checks"/>
<instrumentation android:name="castle.$runner" android:targetPackage="game.castlewars.rebuilt"/>
</manifest>
XML
javac --release 8 -cp "$sdk/platforms/android-33/android.jar:build/classes" -d build/ui/classes tests/*UiCheck.java tests/TerrainBakeCheck.java
"$bt/d8" --min-api 23 --lib "$sdk/platforms/android-33/android.jar" --classpath build/classes --output build/ui/dex build/ui/classes/castle/*UiCheck.class build/ui/classes/castle/TerrainBakeCheck.class
"$bt/aapt" package -f -M build/ui/AndroidManifest.xml -I "$sdk/platforms/android-33/android.jar" -F build/ui/base.apk
(cd build/ui/dex && "$bt/aapt" add ../base.apk classes.dex)
"$bt/zipalign" -f 4 build/ui/base.apk build/ui/aligned.apk
"$bt/apksigner" sign --ks build/debug.jks --ks-pass pass:android --out build/ui/check.apk build/ui/aligned.apk
"$sdk/platform-tools/adb" -s "${ANDROID_SERIAL:-emulator-5556}" install --no-incremental -r build/ui/check.apk
"$sdk/platform-tools/adb" -s "${ANDROID_SERIAL:-emulator-5556}" shell am instrument -w "${@:2}" "game.castlewars.rebuilt.tests/castle.$runner"
