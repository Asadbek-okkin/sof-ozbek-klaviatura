#!/usr/bin/env bash
# Gradle'siz APK yig'ish: aapt2 -> javac -> R8 (kichraytirish va kodni chalkashtirish) -> zipalign -> apksigner
# Windows (D:/android-tools) da ham, Linux / GitHub Actions ($ANDROID_HOME) da ham ishlaydi.
#   bash build.sh          -> SofOzbekKlaviatura.apk
#   bash build.sh admin    -> admin/SofOzbekAdmin.apk
# Imzo kaliti: KALIT_FAYL va KALIT_PAROL (bo'lmasa kalit.jks / "klaviatura").
set -euo pipefail
cd "$(dirname "$0")"
ILDIZ=$(pwd)
P=${1:-.}
if [ "$P" = "admin" ]; then NOM=SofOzbekAdmin.apk; else NOM=SofOzbekKlaviatura.apk; fi

if [ -d /d/android-tools ]; then
  SDK=/d/android-tools/sdk
  JDK=$(ls -d /d/android-tools/jdk-17*)
  export JAVA_HOME="$(cygpath -w "$JDK")"
  export PATH="$JDK/bin:$PATH"
  X=.exe; B=.bat
else
  SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
  X=; B=
fi
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
KS=${KALIT_FAYL:-$ILDIZ/kalit.jks}
PAROL=${KALIT_PAROL:-klaviatura}

cd "$P"
rm -rf out && mkdir -p out/res out/gen out/classes out/dex
ASSETS=(); [ -d assets ] && ASSETS=(-A assets)

"$BT/aapt2$X" compile --dir res -o out/res/res.zip
"$BT/aapt2$X" link -I "$JAR" --manifest AndroidManifest.xml \
  -o out/unsigned.apk --java out/gen out/res/res.zip --auto-add-overlay "${ASSETS[@]}" \
  --proguard out/aapt-qoidalar.pro

if ! javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$JAR" -nowarn -Xlint:-options \
  -d out/classes $(find src out/gen -name '*.java'); then
  echo "javac xato"; exit 1
fi
(cd out/classes && jar cf ../classes.jar .)

# R8: keraksiz kodni olib tashlaydi va nomlarni chalkashtiradi (dasturni buzib o'rganish qiyinlashadi)
java -cp "$BT/lib/d8.jar" com.android.tools.r8.R8 --release --min-api 21 --lib "$JAR" \
  --pg-conf out/aapt-qoidalar.pro --pg-conf "$ILDIZ/proguard.pro" \
  --pg-map-output out/mapping.txt --output out/dex out/classes.jar
(cd out/dex && jar uf ../unsigned.apk classes.dex)

"$BT/zipalign$X" -f -p 4 out/unsigned.apk out/aligned.apk

if [ ! -f "$KS" ]; then
  if [ -n "${CI:-}" ]; then echo "Imzo kaliti topilmadi (KALIT_FAYL)"; exit 1; fi
  keytool -genkeypair -keystore "$KS" -alias klaviatura -keyalg RSA -keysize 2048 \
    -validity 10000 -storepass "$PAROL" -keypass "$PAROL" \
    -dname "CN=Sof Ozbek Klaviatura, O=Okkin, C=UZ"
fi
"$BT/apksigner$B" sign --ks "$KS" --ks-pass "pass:$PAROL" --key-pass "pass:$PAROL" \
  --v1-signing-enabled true --v2-signing-enabled true --out "$NOM" out/aligned.apk
"$BT/apksigner$B" verify "$NOM" && ls -la "$NOM"
