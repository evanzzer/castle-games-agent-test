#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source scripts/android-env.sh
out="$PWD/build"
rm -rf "$out/classes" "$out/dex"
mkdir -p "$out/classes" "$out/dex"
"$bt/aapt" package -f -M AndroidManifest.xml -S res -A assets -I "$jar" -F "$out/base.apk"
sources=()
while IFS= read -r source; do sources+=("$source"); done < <(rg --files src -g "*.java")
javac --release 8 -classpath "$jar" -d "$out/classes" "${sources[@]}"
"$bt/d8" --min-api 23 --lib "$jar" --output "$out/dex" "$out/classes"/castle/*.class
(cd "$out/dex" && "$bt/aapt" add "$out/base.apk" classes.dex)
"$bt/zipalign" -f 4 "$out/base.apk" "$out/aligned.apk"
if [[ ! -f "$signing_key" ]]; then
 mkdir -p "$(dirname "$signing_key")"
 keytool -genkeypair -keystore "$signing_key" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Android Debug' -keyalg RSA -keysize 2048 -validity 10000
fi
"$bt/apksigner" sign --ks "$signing_key" --ks-pass pass:android --out "$out/Castle-Wars-Rebuilt.apk" "$out/aligned.apk"
"$bt/apksigner" verify "$out/Castle-Wars-Rebuilt.apk"
