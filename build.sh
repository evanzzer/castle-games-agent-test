#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
bt="$sdk/build-tools/${BUILD_TOOLS_VERSION:-33.0.1}"
jar="$sdk/platforms/android-33/android.jar"
out="$PWD/build"
mkdir -p "$out/classes" "$out/dex"
"$bt/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$jar" -F "$out/base.apk"
javac --release 8 -classpath "$jar" -d "$out/classes" src/castle/*.java
"$bt/d8" --min-api 23 --lib "$jar" --output "$out/dex" $(find "$out/classes" -name '*.class')
(cd "$out/dex" && "$bt/aapt" add "$out/base.apk" classes.dex)
"$bt/zipalign" -f 4 "$out/base.apk" "$out/aligned.apk"
if [[ ! -f "$out/debug.jks" ]]; then
 keytool -genkeypair -keystore "$out/debug.jks" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug' -keyalg RSA -keysize 2048 -validity 10000
fi
"$bt/apksigner" sign --ks "$out/debug.jks" --ks-pass pass:android --out "$out/Castle-Wars-Rebuilt.apk" "$out/aligned.apk"
"$bt/apksigner" verify "$out/Castle-Wars-Rebuilt.apk"
