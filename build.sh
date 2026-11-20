#!/usr/bin/env bash
# Gradle'siz APK yig'ish: aapt2 -> javac -> d8 -> zipalign -> apksigner
set -euo pipefail
cd "$(dirname "$0")"
T=/d/android-tools
JDK=$(ls -d "$T"/jdk-17*)
export JAVA_HOME="$(cygpath -w "$JDK")"
export PATH="$JDK/bin:$PATH"
BT="$T/sdk/build-tools/34.0.0"
JAR="$T/sdk/platforms/android-34/android.jar"

rm -rf out && mkdir -p out/res out/gen out/classes out/dex

"$BT/aapt2.exe" compile --dir res -o out/res/res.zip
"$BT/aapt2.exe" link -I "$JAR" --manifest AndroidManifest.xml \
  -o out/unsigned.apk --java out/gen out/res/res.zip --auto-add-overlay -A assets

if ! javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$JAR" -nowarn -Xlint:-options \
  -d out/classes $(find src out/gen -name '*.java'); then
  echo "javac xato"; exit 1
fi

"$BT/d8.bat" --release --min-api 21 --lib "$JAR" --output out/dex $(find out/classes -name '*.class')
(cd out/dex && "$JDK/bin/jar.exe" uf ../unsigned.apk classes.dex)

"$BT/zipalign.exe" -f -p 4 out/unsigned.apk out/aligned.apk

KS=kalit.jks
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -alias klaviatura -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass klaviatura -keypass klaviatura \
    -dname "CN=Sof Ozbek Klaviatura, O=Okkin, C=UZ"
fi
"$BT/apksigner.bat" sign --ks "$KS" --ks-pass pass:klaviatura --key-pass pass:klaviatura \
  --out SofOzbekKlaviatura.apk out/aligned.apk
"$BT/apksigner.bat" verify SofOzbekKlaviatura.apk && ls -la SofOzbekKlaviatura.apk
